package com.taktak.app.ui.camera;

import android.os.Bundle;
import android.view.*;
import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import com.taktak.app.R;

public final class CameraPageFragment extends Fragment {
    @Override public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup parent, Bundle saved) {
        return inflater.inflate(R.layout.fragment_camera_page, parent, false);
    }
    @Override public void onViewCreated(@NonNull View view, Bundle saved) {
        ViewCompat.setAccessibilityHeading(view.findViewById(R.id.cameraTitle), true);
        if (getChildFragmentManager().findFragmentByTag("camera") == null) {
            getChildFragmentManager().beginTransaction().replace(R.id.cameraContent, new CameraFragment(), "camera").commitNow();
        }
    }
}
