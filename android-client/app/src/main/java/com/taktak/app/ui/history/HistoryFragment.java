package com.taktak.app.ui.history;

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

        adapter = new HistoryAdapter(Glide.with(this), model, () -> model.more());
        binding.historyRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.historyRecycler.setAdapter(adapter);
        binding.historyRecycler.setItemAnimator(null);

        binding.historyRefresh.setOnClickListener(v -> model.refresh());

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

    @Override
    public void onDestroyView() {
        if (binding != null) {
            binding.historyRecycler.setAdapter(null);
            binding = null;
        }
        adapter = null;
        super.onDestroyView();
    }
}
