package com.taktak.app.ui.profile;

import android.os.*;
import android.widget.ImageView;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.taktak.app.R;
import com.taktak.app.data.AuthGraph;
import java.util.concurrent.*;

/** Tải qua ApiClient để vẫn refresh JWT; không ghi avatar private vào cache Glide. */
public final class AvatarLoader implements AutoCloseable {
    private final RequestManager images;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ThreadPoolExecutor worker=new ThreadPoolExecutor(2,2,0,TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(64),new ThreadPoolExecutor.DiscardOldestPolicy());
    private volatile boolean closed;
    public AvatarLoader(RequestManager images) { this.images=images; }
    public void load(String id,ImageView view) {
        clear(view); if(id==null || closed) return;
        Object ticket=new Object();view.setTag(R.id.avatarRequest,ticket);
        var app=view.getContext().getApplicationContext();
        try { worker.execute(() -> {
            if(closed) return;
            try {
                var repository=AuthGraph.repository(app).profiles();byte[] bytes=repository.avatar(id);
                if(closed || bytes==null) return;
                main.post(() -> {
                    if(!closed && view.getTag(R.id.avatarRequest)==ticket) repository.publishIfCurrent(() ->
                            images.load(bytes).diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true).circleCrop().into(view));
                });
            } catch(Exception ignored) { /* Avatar mặc định khi không có ảnh/quyền/kết nối. */ }
        }); } catch(RejectedExecutionException ignored) { }
    }
    public void clear(ImageView view) { view.setTag(R.id.avatarRequest,null);images.clear(view);view.setImageResource(R.drawable.friend_avatar); }
    @Override public void close() { closed=true;main.removeCallbacksAndMessages(null);worker.shutdownNow(); }
}
