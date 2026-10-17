package com.taktak.app;

import android.app.Activity;
import android.os.SystemClock;
import androidx.lifecycle.ViewModelProvider;
import androidx.test.platform.app.InstrumentationRegistry;
import com.taktak.app.ui.camera.PreviewActivity;
import com.taktak.app.viewmodel.PreviewViewModel;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import static org.junit.Assert.*;

final class PreviewTestSupport {
    static PreviewViewModel model(PreviewActivity activity) { return new ViewModelProvider(activity).get(PreviewViewModel.class); }
    static void onPreview(Consumer<PreviewActivity> action) {
        long end=SystemClock.elapsedRealtime()+40000;
        while(SystemClock.elapsedRealtime()<end) {
            AtomicBoolean done=new AtomicBoolean();
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                for(Activity activity:androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance()
                        .getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED)) {
                    if(activity instanceof PreviewActivity preview) { action.accept(preview); done.set(true); }
                }
            });
            if(done.get()) return; SystemClock.sleep(100);
        }
        fail("PreviewActivity resumes");
    }
    static void ready() {
        long end=SystemClock.elapsedRealtime()+40000;
        while(SystemClock.elapsedRealtime()<end) {
            AtomicBoolean ready=new AtomicBoolean();
            onPreview(activity -> ready.set(activity.findViewById(R.id.previewScroll).isShown()
                    && model(activity).state().getValue().file()!=null && !model(activity).state().getValue().loading()));
            if(ready.get()) return; SystemClock.sleep(100);
        }
        fail("Preview has authenticated local draft");
    }
}
