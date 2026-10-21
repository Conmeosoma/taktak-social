package com.taktak.app.ui.theme;

import android.app.Application;
import android.graphics.Bitmap;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStore;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.taktak.app.ui.history.HistoryImageCache;
import com.taktak.app.viewmodel.HistoryViewModel;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** RAM-only Retrofit fixture; no account writes, network, camera or gallery. */
@RunWith(AndroidJUnit4.class)
public class HistoryCachePrivacyTest {
    private MediaThemeFixture fixture;
    private ViewModelStore store;
    private HistoryViewModel model;

    @Before public void prepare() throws Exception {
        assertEquals("true", InstrumentationRegistry.getArguments().getString("taktak.allowMediaQa"));
        var app = (Application) InstrumentationRegistry.getInstrumentation().getTargetContext().getApplicationContext();
        fixture = new MediaThemeFixture(app);
        HistoryImageCache.clear();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            store = new ViewModelStore();
            model = new ViewModelProvider(store, new ViewModelProvider.AndroidViewModelFactory(app)).get(HistoryViewModel.class);
        });
    }
    @After public void cleanup() throws Exception {
        if (store != null) { InstrumentationRegistry.getInstrumentation().runOnMainSync(store::clear); }
        HistoryImageCache.clear();
        if (fixture != null) { fixture.close(); }
    }
    @Test public void cachedImageCannotBypassRevocationAndFreshGrantWorksAgain() throws Exception {
        assertTrue(load());
        fixture.denyImage = true;
        assertFalse("A cached decode is not permission to redisplay a revoked photo", load());
        fixture.denyImage = false;
        assertTrue("A later successful grant must not be permanently tombstoned", load());
    }
    @Test public void oldRepositoryCannotPublishCachedImageAfterSessionInvalidation() throws Exception {
        assertTrue(load());
        var field = com.taktak.app.data.repository.AuthRepository.class.getDeclaredField("client");
        field.setAccessible(true);
        ((com.taktak.app.data.remote.ApiClient) field.get(fixture.auth)).sessions.clear();
        assertFalse(load());
    }
    private boolean load() throws Exception {
        var done = new CountDownLatch(1);
        var loaded = new AtomicBoolean();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> model.loadImage(MediaThemeFixture.MOMENT,
                new HistoryViewModel.ImageCallback() {
                    @Override public void onLoaded(Bitmap bitmap) { loaded.set(true); done.countDown(); }
                    @Override public void onError() { done.countDown(); }
                }));
        assertTrue("Image callback completed", done.await(10, TimeUnit.SECONDS));
        return loaded.get();
    }
}
