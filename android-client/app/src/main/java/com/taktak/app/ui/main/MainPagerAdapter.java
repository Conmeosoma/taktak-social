package com.taktak.app.ui.main;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import com.taktak.app.ui.camera.CameraPageFragment;
import com.taktak.app.ui.friends.FriendsFragment;

public final class MainPagerAdapter extends FragmentStateAdapter {
    public MainPagerAdapter(FragmentActivity activity) { super(activity); }
    @NonNull @Override public Fragment createFragment(int position) {
        return switch (position) {
            case 0 -> new CameraPageFragment();
            case 1 -> new com.taktak.app.ui.history.HistoryFragment();
            case 2 -> new FriendsFragment();
            case 3 -> new com.taktak.app.ui.profile.ProfileFragment();
            default -> throw new IllegalArgumentException("Unknown main tab");
        };
    }
    @Override public int getItemCount() { return 4; }
}
