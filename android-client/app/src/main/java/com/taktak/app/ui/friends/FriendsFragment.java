package com.taktak.app.ui.friends;

import android.os.Bundle;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.taktak.app.R;
import com.taktak.app.data.model.FriendModels.User;
import com.taktak.app.databinding.FragmentFriendsBinding;
import com.taktak.app.viewmodel.FriendsViewModel;
import com.taktak.app.viewmodel.SessionViewModel;

public final class FriendsFragment extends Fragment {
    private FragmentFriendsBinding binding;
    private FriendsViewModel model;
    private androidx.appcompat.app.AlertDialog confirmation;
    @Override public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup parent, Bundle saved) {
        binding = FragmentFriendsBinding.inflate(inflater, parent, false); return binding.getRoot();
    }
    @Override public void onViewCreated(@NonNull View view, Bundle saved) {
        model = new ViewModelProvider(requireActivity()).get(FriendsViewModel.class);
        ViewCompat.setAccessibilityHeading(binding.friendsTitle, true);
        var session = new ViewModelProvider(requireActivity()).get(SessionViewModel.class);
        var adapter = new FriendsAdapter(com.bumptech.glide.Glide.with(this), new FriendsAdapter.Actions() {
            public void send(String id) { model.send(id); }
            public void respond(String id, boolean accept) { model.respond(id, accept); }
            public void confirm(User user, boolean block) { FriendsFragment.this.confirm(user, block); }
            public void more(boolean incoming) { model.more(incoming); }
        });
        binding.friendsRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.friendsRecycler.setAdapter(adapter);
        binding.friendsRecycler.setItemAnimator(null);
        binding.friendsBack.setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());
        binding.friendsLogout.setOnClickListener(v -> session.logout());
        binding.friendsSearch.setOnClickListener(v -> search());
        binding.friendsQuery.setOnEditorActionListener((v, action, event) -> {
            if (action == EditorInfo.IME_ACTION_SEARCH) { search(); return true; }
            return false;
        });
        binding.friendsRefresh.setOnClickListener(v -> model.refresh());
        model.state().observe(getViewLifecycleOwner(), state -> {
            if (state.expired) {
                view.post(() -> {
                    if (isAdded() && binding != null) { model.acknowledgeSessionChange(); session.check(true); }
                });
                return;
            }
            binding.friendsProgress.setVisibility(state.busy ? View.VISIBLE : View.GONE);
            binding.friendsSearch.setEnabled(!state.busy && state.searchAllowed);
            binding.friendsQuery.setEnabled(!state.busy);
            binding.friendsRefresh.setEnabled(!state.busy);
            binding.friendsMessage.setVisibility(state.message == 0 ? View.GONE : View.VISIBLE);
            if (state.message != 0) { binding.friendsMessage.setText(state.message); }
            adapter.submit(state);
        });
        var current = model.state().getValue();
        if (current != null) { binding.friendsQuery.setText(current.query); }
        model.load();
    }
    private void search() {
        var controller = ViewCompat.getWindowInsetsController(binding.getRoot());
        if (controller != null) { controller.hide(WindowInsetsCompat.Type.ime()); }
        model.search(binding.friendsQuery.getText() == null ? "" : binding.friendsQuery.getText().toString());
    }
    private void confirm(User user, boolean block) {
        if (confirmation != null) { confirmation.dismiss(); }
        confirmation = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(block ? R.string.friends_block : R.string.friends_unfriend)
                .setMessage(getString(block ? R.string.friends_confirm_block : R.string.friends_confirm_unfriend, user.username))
                .setNegativeButton(R.string.friends_cancel, null)
                .setPositiveButton(block ? R.string.friends_block : R.string.friends_unfriend, (dialog, which) -> {
                    if (block) { model.block(user.id); } else { model.unfriend(user.id); }
                }).show();
    }
    @Override public void onDestroyView() {
        if (confirmation != null) { confirmation.dismiss(); confirmation = null; }
        binding.friendsRecycler.setAdapter(null);
        binding = null;
        super.onDestroyView();
    }
}
