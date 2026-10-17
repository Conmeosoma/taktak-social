package com.taktak.app.data.remote;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** HTTP fixture chỉ trong JVM test, không nằm trong APK hoặc thay backend thật. */
final class TestHttpServer implements AutoCloseable {
    interface Handler { Reply handle(Incoming request) throws Exception; }
    static final class Incoming {
        final String path;
        final String authorization;
        final String body;
        final Map<String, String> headers;
        Incoming(String path, String authorization, String body, Map<String, String> headers) {
            this.path = path; this.authorization = authorization; this.body = body; this.headers = headers;
        }
        Incoming(String path, String authorization, String body) { this(path, authorization, body, Map.of()); }
        @Override public String toString() { return "Incoming[redacted]"; }
    }
    static final class Reply {
        final int status;
        final String body;
        final String headers;
        Reply(int status, String body) { this(status, body, ""); }
        Reply(int status, String body, String headers) { this.status = status; this.body = body; this.headers = headers; }
    }
    private final ServerSocket socket;
    private final ExecutorService workers = Executors.newCachedThreadPool();
    private volatile boolean closed;

    TestHttpServer(Handler handler) throws IOException {
        socket = new ServerSocket(0, 50, java.net.InetAddress.getByName("127.0.0.1"));
        workers.execute(() -> {
            while (!closed) {
                try {
                    Socket client = socket.accept();
                    workers.execute(() -> serve(client, handler));
                } catch (IOException ignored) { if (!closed) { throw new IllegalStateException("Test server accept failed."); } }
            }
        });
    }
    String baseUrl() { return "http://127.0.0.1:" + socket.getLocalPort() + "/"; }

    private void serve(Socket client, Handler handler) {
        try (client) {
            client.setSoTimeout(15000);
            var input = client.getInputStream();
            ByteArrayOutputStream header = new ByteArrayOutputStream();
            int match = 0;
            while (match < 4 && header.size() < 32768) {
                int next = input.read();
                if (next < 0) { return; }
                header.write(next);
                match = (next == (match % 2 == 0 ? '\r' : '\n')) ? match + 1 : 0;
            }
            String[] lines = header.toString(StandardCharsets.UTF_8).split("\r\n");
            Map<String, String> headers = new HashMap<>();
            for (int index = 1; index < lines.length; index++) {
                int colon = lines[index].indexOf(':');
                if (colon > 0) { headers.put(lines[index].substring(0, colon).toLowerCase(java.util.Locale.ROOT), lines[index].substring(colon + 1).trim()); }
            }
            int length = Integer.parseInt(headers.getOrDefault("content-length", "0"));
            if (length < 0 || length > 65536) { throw new IOException("Test body too large."); }
            byte[] bytes = input.readNBytes(length);
            Reply reply = handler.handle(new Incoming(lines[0].split(" ")[1], headers.get("authorization"),
                    new String(bytes, StandardCharsets.UTF_8), headers));
            byte[] output = reply.body.getBytes(StandardCharsets.UTF_8);
            String responseHeader = "HTTP/1.1 " + reply.status + " Test\r\nContent-Type: application/json\r\nContent-Length: "
                    + output.length + "\r\nConnection: close\r\n" + reply.headers + "\r\n";
            client.getOutputStream().write(responseHeader.getBytes(StandardCharsets.UTF_8));
            client.getOutputStream().write(output);
        } catch (Exception ignored) { /* Client timeout/fixture error làm assertion bên gọi thất bại, không log credential. */ }
    }

    @Override public void close() throws IOException { closed = true; socket.close(); workers.shutdownNow(); }
}
