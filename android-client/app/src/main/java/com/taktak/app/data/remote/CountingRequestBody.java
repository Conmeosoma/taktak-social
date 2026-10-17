package com.taktak.app.data.remote;

import java.io.IOException;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.Buffer;
import okio.BufferedSink;
import okio.ForwardingSink;
import okio.Okio;
import okio.Sink;

public final class CountingRequestBody extends RequestBody {
    public interface Listener {
        void onProgress(long bytesWritten, long contentLength);
    }
    private final RequestBody delegate;
    private final Listener listener;

    public CountingRequestBody(RequestBody delegate, Listener listener) {
        this.delegate = delegate;
        this.listener = listener;
    }

    @Override public MediaType contentType() {
        return delegate.contentType();
    }

    @Override public long contentLength() throws IOException {
        return delegate.contentLength();
    }

    @Override public void writeTo(BufferedSink sink) throws IOException {
        CountingSink countingSink = new CountingSink(sink, contentLength(), listener);
        BufferedSink bufferedSink = Okio.buffer(countingSink);
        delegate.writeTo(bufferedSink);
        bufferedSink.flush();
    }

    private static final class CountingSink extends ForwardingSink {
        private long bytesWritten = 0L;
        private final long contentLength;
        private final Listener listener;

        CountingSink(Sink delegate, long contentLength, Listener listener) {
            super(delegate);
            this.contentLength = contentLength;
            this.listener = listener;
        }

        @Override public void write(Buffer source, long byteCount) throws IOException {
            super.write(source, byteCount);
            bytesWritten += byteCount;
            if (listener != null) {
                listener.onProgress(bytesWritten, contentLength);
            }
        }
    }
}
