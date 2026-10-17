package com.taktak.app;

import android.app.Activity;
import androidx.fragment.app.FragmentManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.taktak.app.ui.camera.CameraFragment;

final class MainNavigationTestSupport {
    private MainNavigationTestSupport() { }
    static void friends(Activity activity) {
        ((BottomNavigationView) activity.findViewById(R.id.bottomNavigation)).setSelectedItemId(R.id.nav_friends);
    }
    static CameraFragment camera(Activity activity) {
        return camera(((androidx.fragment.app.FragmentActivity) activity).getSupportFragmentManager());
    }
    private static CameraFragment camera(FragmentManager manager) {
        for (var fragment : manager.getFragments()) {
            if (fragment instanceof CameraFragment camera) { return camera; }
            CameraFragment child = camera(fragment.getChildFragmentManager());
            if (child != null) { return child; }
        }
        return null;
    }
}
