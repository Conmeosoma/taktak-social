package com.taktak.app.ui.main;

import android.os.Bundle;
import android.view.*;
import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import com.taktak.app.R;
import com.taktak.app.databinding.FragmentPendingTabBinding;

/** Điểm đến điều hướng 5A; không giả dữ liệu/API chưa triển khai. */
public final class PendingTabFragment extends Fragment {
    public static PendingTabFragment newInstance(boolean profile) {
        var fragment = new PendingTabFragment(); var args = new Bundle();
        args.putBoolean("profile", profile); fragment.setArguments(args); return fragment;
    }
    @Override public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup parent, Bundle saved) {
        var binding = FragmentPendingTabBinding.inflate(inflater, parent, false);
        boolean profile = requireArguments().getBoolean("profile");
        binding.getRoot().setId(profile ? R.id.profilePage : R.id.historyPage);
        binding.pendingTitle.setText(profile ? R.string.nav_profile : R.string.nav_history);
        binding.pendingMessage.setText(profile ? R.string.profile_pending : R.string.history_pending);
        ViewCompat.setAccessibilityHeading(binding.pendingTitle, true);
        return binding.getRoot();
    }
}
