package com.taktak.app.ui.history;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.bumptech.glide.Glide;
import com.google.android.material.tabs.TabLayout;
import com.taktak.app.R;
import com.taktak.app.databinding.FragmentHistoryBinding;
import com.taktak.app.viewmodel.HistoryViewModel;
import com.taktak.app.viewmodel.SessionViewModel;

public final class HistoryFragment extends Fragment {
    private FragmentHistoryBinding binding;
    private HistoryViewModel model;
    private HistoryAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup parent, Bundle saved) {
        binding = FragmentHistoryBinding.inflate(inflater, parent, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle saved) {
        model = new ViewModelProvider(requireActivity()).get(HistoryViewModel.class);
        var session = new ViewModelProvider(requireActivity()).get(SessionViewModel.class);

        ViewCompat.setAccessibilityHeading(binding.historyTitle, true);

        // Setup Tabs (Đã nhận / Đã gửi)
        binding.historyTabs.removeAllTabs();
        binding.historyTabs.addTab(binding.historyTabs.newTab().setText(R.string.history_tab_received));
        binding.historyTabs.addTab(binding.historyTabs.newTab().setText(R.string.history_tab_sent));

        binding.historyTabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    model.switchTab(HistoryViewModel.Tab.RECEIVED);
                } else {
                    model.switchTab(HistoryViewModel.Tab.SENT);
                }
            }

            @Override public void onTabUnselected(TabLayout.Tab tab) { }
            @Override public void onTabReselected(TabLayout.Tab tab) {
                model.refresh();
            }
        });

        adapter = new HistoryAdapter(Glide.with(this), model, () -> model.more(), item -> {
            Intent intent = new Intent(requireContext(), MomentViewerActivity.class);
            intent.putExtra(MomentViewerActivity.EXTRA_MOMENT_ID, item.id);
            intent.putExtra(MomentViewerActivity.EXTRA_SENDER_NAME, item.sender.displayName);
            intent.putExtra(MomentViewerActivity.EXTRA_SENDER_ID,item.sender.id);
            intent.putExtra(MomentViewerActivity.EXTRA_SENDER_USERNAME, item.sender.username);
            intent.putExtra(MomentViewerActivity.EXTRA_CAPTION, item.caption);
            intent.putExtra(MomentViewerActivity.EXTRA_CREATED_AT, item.createdAt);
            boolean isSender = model.state().getValue() != null && model.state().getValue().activeTab == HistoryViewModel.Tab.SENT;
            intent.putExtra(MomentViewerActivity.EXTRA_IS_SENDER, isSender);
            startActivity(intent);
        });

        binding.historyRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.historyRecycler.setAdapter(adapter);
        binding.historyRecycler.setItemAnimator(null);

        binding.historyRefresh.setOnClickListener(v -> model.refresh());

        binding.btnViewRecap.setOnClickListener(v -> showRecapDialog());
        binding.historyMemoriesCard.setOnClickListener(v -> showRecapDialog());

        model.state().observe(getViewLifecycleOwner(), state -> {
            if (state.expired) {
                view.post(() -> {
                    if (isAdded() && binding != null) {
                        model.acknowledgeSessionChange();
                        session.check(true);
                    }
                });
                return;
            }

            int selectedTab = (state.activeTab == HistoryViewModel.Tab.RECEIVED) ? 0 : 1;
            if (binding.historyTabs.getSelectedTabPosition() != selectedTab) {
                var tab = binding.historyTabs.getTabAt(selectedTab);
                if (tab != null && !tab.isSelected()) {
                    tab.select();
                }
            }

            boolean isReceived = state.activeTab == HistoryViewModel.Tab.RECEIVED;
            binding.historyTitle.setText(isReceived ? R.string.history_title : R.string.history_sent_title);
            binding.historyEmptyTitle.setText(isReceived ? R.string.history_empty : R.string.history_empty_sent);
            binding.historyEmptyDetail.setText(isReceived ? R.string.history_empty_detail : R.string.history_empty_sent_detail);

            binding.historyProgress.setVisibility(state.busy ? View.VISIBLE : View.INVISIBLE);
            binding.historyRefresh.setEnabled(!state.busy);

            binding.historyMessage.setVisibility(state.message == 0 ? View.GONE : View.VISIBLE);
            if (state.message != 0) {
                binding.historyMessage.setText(state.message);
            }

            boolean isEmpty = state.loaded && state.moments.isEmpty();
            binding.historyEmptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            binding.historyRecycler.setVisibility(isEmpty ? View.GONE : View.VISIBLE);

            adapter.submit(state.moments, state.hasNext, state.busy);
        });

        model.load();
    }

    private void showRecapDialog() {
        String content = getString(R.string.recap_stat_moments) + "\n\n"
                + getString(R.string.recap_stat_friends) + "\n\n"
                + getString(R.string.recap_stat_streak);

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.recap_dialog_title)
                .setMessage(content)
                .setPositiveButton(R.string.recap_close, null)
                .show();
    }

    @Override
    public void onDestroyView() {
        if(adapter!=null) adapter.close();
        if (binding != null) {
            binding.historyTabs.clearOnTabSelectedListeners();
            binding.historyRecycler.setAdapter(null);
            binding = null;
        }
        adapter = null;
        super.onDestroyView();
    }
}
