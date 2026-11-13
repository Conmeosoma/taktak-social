package com.taktak.app.ui.ai;

import android.content.Intent;
import android.os.Bundle;
import android.text.*;
import android.view.*;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.*;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.taktak.app.R;
import com.taktak.app.data.model.AiModels.*;
import com.taktak.app.data.repository.AiRepository;
import com.taktak.app.databinding.*;
import com.taktak.app.ui.chat.MessagesFragment;
import com.taktak.app.ui.history.HistoryTimeFormatter;
import com.taktak.app.viewmodel.AiViewModel;
import java.util.List;

public final class AiFragment extends Fragment {
    private FragmentAiBinding binding;
    private AiViewModel model;
    private final Rows rows = new Rows();
    private androidx.activity.OnBackPressedCallback back;
    private androidx.appcompat.app.AlertDialog dialog;
    private int revision;
    private boolean rendering;
    @Override public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup parent, Bundle saved) {
        binding = FragmentAiBinding.inflate(inflater, parent, false); return binding.getRoot();
    }
    @Override public void onViewCreated(@NonNull View view, Bundle saved) {
        model = new ViewModelProvider(this).get(AiViewModel.class);
        binding.aiList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.aiList.setAdapter(rows); binding.aiList.setItemAnimator(null);
        back = new androidx.activity.OnBackPressedCallback(false) { public void handleOnBackPressed() { goBack(); } };
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), back);
        binding.aiBack.setOnClickListener(v -> goBack());
        binding.aiTheme.setOnClickListener(v -> startActivity(new Intent(requireContext(), com.taktak.app.ui.theme.ThemeSettingsActivity.class)));
        binding.aiNew.setOnClickListener(v -> model.newChat()); binding.aiMore.setOnClickListener(v -> model.more());
        binding.aiRetry.setOnClickListener(v -> { if (model.state().getValue().pending) model.retry(); else model.refresh(); });
        binding.aiEdit.setOnClickListener(v -> {
            dialog = new MaterialAlertDialogBuilder(requireContext()).setMessage(R.string.ai_discard_warning)
                    .setNegativeButton(android.R.string.cancel, null).setPositiveButton(android.R.string.ok, (d,w) -> model.discard()).show();
        });
        binding.aiSend.setOnClickListener(v -> {
            if (!model.needsConsent()) { model.send(true); return; }
            dialog = new MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.ai_title).setMessage(R.string.ai_consent)
                    .setNegativeButton(android.R.string.cancel, null).setPositiveButton(R.string.ai_agree, (d,w) -> model.send(true)).show();
        });
        binding.aiInput.setText(model.state().getValue().draft);
        binding.aiInput.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s,int start,int count,int after) { }
            public void onTextChanged(CharSequence s,int start,int before,int count) {
                if (!rendering) model.draft(s.toString()); updateSend();
            }
            public void afterTextChanged(Editable e) { }
        });
        binding.aiCaption.setOnClickListener(v -> suggest(R.string.ai_suggestion_caption));
        binding.aiWeekend.setOnClickListener(v -> suggest(R.string.ai_suggestion_weekend));
        binding.aiBirthday.setOnClickListener(v -> suggest(R.string.ai_suggestion_birthday));
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            public boolean onMove(@NonNull RecyclerView r,@NonNull RecyclerView.ViewHolder a,@NonNull RecyclerView.ViewHolder b) { return false; }
            public int getSwipeDirs(@NonNull RecyclerView r,@NonNull RecyclerView.ViewHolder h) {
                var state = model.state().getValue(); return state.chat || state.busy ? 0 : super.getSwipeDirs(r,h);
            }
            public void onSwiped(@NonNull RecyclerView.ViewHolder h,int direction) {
                int pos = h.getBindingAdapterPosition();
                if (pos >= 0 && pos < rows.conversations.size()) confirmDelete(rows.conversations.get(pos).id);
                rows.notifyDataSetChanged();
            }
        }).attachToRecyclerView(binding.aiList);
        model.state().observe(getViewLifecycleOwner(), this::render);
        if (saved == null) model.refresh();
    }
    private void suggest(int text) { binding.aiInput.setText(text); binding.aiInput.requestFocus(); }
    private void goBack() {
        var s = model.state().getValue(); if (s.busy || s.pending) return;
        if (s.chat) model.backToList(); else ((MessagesFragment) requireParentFragment()).closeAi();
    }
    private void confirmDelete(String id) {
        dialog = new MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.ai_delete).setMessage(R.string.ai_delete_warning)
                .setNegativeButton(android.R.string.cancel, null).setPositiveButton(R.string.ai_delete, (d,w) -> model.delete(id)).show();
    }
    private void render(AiViewModel.State s) {
        if (binding == null) return;
        if (s.expired) { binding.aiInput.setText(""); rows.conversations = List.of(); rows.messages = List.of(); rows.notifyDataSetChanged();
            ((MessagesFragment) requireParentFragment()).closeAi(); return; }
        binding.aiSubtitle.setText(s.chat ? R.string.ai_new : R.string.ai_title);
        binding.aiPrivacy.setVisibility(s.chat || s.conversations.isEmpty() ? View.VISIBLE : View.GONE);
        binding.aiProgress.setVisibility(s.busy ? View.VISIBLE : View.INVISIBLE);
        int notice = s.busy && s.pending ? R.string.ai_thinking : s.error;
        binding.aiStatus.setVisibility(notice == 0 ? View.GONE : View.VISIBLE); if (notice != 0) binding.aiStatus.setText(notice);
        binding.aiActions.setVisibility(s.error != 0 && !s.busy ? View.VISIBLE : View.GONE);
        binding.aiEdit.setVisibility(s.pending ? View.VISIBLE : View.GONE);
        binding.aiBack.setEnabled(!s.busy && !s.pending); binding.aiTheme.setEnabled(!s.busy);
        binding.aiMore.setVisibility(s.hasNext ? View.VISIBLE : View.GONE); binding.aiMore.setEnabled(!s.busy && !s.pending);
        binding.aiNew.setVisibility(!s.chat ? View.VISIBLE : View.GONE); binding.aiNew.setEnabled(!s.busy);
        binding.aiHint.setVisibility(!s.chat ? View.VISIBLE : View.GONE);
        binding.aiHint.setText(s.conversations.isEmpty() ? R.string.ai_first : R.string.ai_list_hint);
        binding.aiComposer.setVisibility(s.chat ? View.VISIBLE : View.GONE); binding.aiInput.setEnabled(!s.busy && !s.pending);
        boolean empty = s.chat ? s.messages.isEmpty() : s.conversations.isEmpty();
        binding.aiEmpty.setVisibility(empty ? View.VISIBLE : View.GONE); binding.aiList.setVisibility(empty ? View.GONE : View.VISIBLE);
        binding.aiWelcome.setText(s.chat ? R.string.ai_new_welcome : R.string.ai_welcome);
        binding.aiDetail.setText(s.chat ? R.string.ai_new_detail : R.string.ai_empty_detail);
        binding.aiSuggestions.setVisibility(s.chat ? View.VISIBLE : View.GONE);
        binding.aiCaption.setEnabled(!s.busy && !s.pending); binding.aiWeekend.setEnabled(!s.busy && !s.pending); binding.aiBirthday.setEnabled(!s.busy && !s.pending);
        if (s.sentRevision != revision) { revision = s.sentRevision; rendering = true; binding.aiInput.setText(s.draft); rendering = false; }
        boolean appended = rows.messages.size() < s.messages.size(); rows.chat = s.chat;
        rows.conversations = s.conversations; rows.messages = s.messages; rows.notifyDataSetChanged();
        if (appended && s.chat && !s.hasNext) binding.aiList.scrollToPosition(Math.max(0, s.messages.size()-1));
        updateSend();
    }
    private void updateSend() {
        if (binding == null || model == null) return; var s = model.state().getValue();
        binding.aiSend.setEnabled(s.configured && !s.busy && !s.pending && AiRepository.validText(binding.aiInput.getText()==null ? "" : binding.aiInput.getText().toString()));
    }
    @Override public void onResume() { super.onResume(); back.setEnabled(true); model.checkSession(); }
    @Override public void onPause() { back.setEnabled(false); if (dialog != null) { dialog.dismiss(); dialog = null; } super.onPause(); }
    @Override public void onDestroyView() { binding.aiList.setAdapter(null); binding = null; super.onDestroyView(); }
    private final class Rows extends RecyclerView.Adapter<Row> {
        List<Conversation> conversations = List.of(); List<Message> messages = List.of(); boolean chat;
        public int getItemCount() { return chat ? messages.size() : conversations.size(); }
        @NonNull public Row onCreateViewHolder(@NonNull ViewGroup parent,int type) {
            return new Row(ItemAiBinding.inflate(LayoutInflater.from(parent.getContext()),parent,false));
        }
        public void onBindViewHolder(@NonNull Row h,int pos) {
            var b = h.binding; boolean user = chat && "user".equals(messages.get(pos).role);
            b.aiAvatar.setVisibility(user ? View.GONE : View.VISIBLE);
            var params = (LinearLayout.LayoutParams) b.aiBubble.getLayoutParams();
            params.setMarginStart(dp(user ? 84 : 0)); params.setMarginEnd(dp(user ? 0 : chat ? 36 : 0)); b.aiBubble.setLayoutParams(params);
            b.aiBubble.setBackgroundResource(user ? R.drawable.ai_primary : R.drawable.ai_card);
            b.aiText.setTextColor(requireContext().getColor(user ? R.color.ai_onbrand : R.color.ai_text));
            b.aiText.setTextIsSelectable(chat);
            if (chat) { b.aiText.setText(messages.get(pos).content); b.aiBubble.setOnClickListener(null); b.aiBubble.setOnLongClickListener(null); }
            else {
                Conversation c = conversations.get(pos); b.aiText.setText(c.title + "\n" + HistoryTimeFormatter.format(requireContext(), c.updatedAt));
                b.aiBubble.setOnClickListener(v -> model.open(c.id));
                b.aiBubble.setOnLongClickListener(v -> { if (!model.state().getValue().busy) confirmDelete(c.id); return true; });
            }
        }
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private static final class Row extends RecyclerView.ViewHolder {
        final ItemAiBinding binding; Row(ItemAiBinding binding) { super(binding.getRoot()); this.binding = binding; }
    }
}
