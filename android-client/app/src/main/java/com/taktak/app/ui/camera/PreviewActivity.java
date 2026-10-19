package com.taktak.app.ui.camera;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.*;
import android.text.*;
import android.view.*;
import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.*;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.*;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.taktak.app.BuildConfig;
import com.taktak.app.R;
import com.taktak.app.data.model.FriendModels.User;
import com.taktak.app.databinding.ActivityPreviewBinding;
import com.taktak.app.databinding.ItemRecipientBinding;
import com.taktak.app.ui.auth.LoginActivity;
import com.taktak.app.viewmodel.*;
import java.util.*;

public final class PreviewActivity extends AppCompatActivity {
    public static final String DRAFT_ID = "draftId", GENERATION = "draftGeneration";
    private ActivityPreviewBinding binding;
    private PreviewViewModel model;
    private SessionViewModel session;
    private com.bumptech.glide.RequestManager images;
    private final RecipientsAdapter adapter = new RecipientsAdapter();
    private boolean authorized, rendering, navigating;
    private Object shownImage;
    private final androidx.activity.result.ActivityResultLauncher<String> permission = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), granted -> session.check(granted));

    @Override protected void onCreate(Bundle saved) {
        com.taktak.app.ui.theme.ThemeManager.applyBeforeCreate(this,
                new com.taktak.app.ui.theme.ThemePreferences(this).load(),
                com.taktak.app.ui.theme.ThemeManager.ScreenRole.PREVIEW);
        super.onCreate(saved); EdgeToEdge.enable(this);
        binding = ActivityPreviewBinding.inflate(getLayoutInflater()); setContentView(binding.getRoot());
        com.taktak.app.ui.theme.ThemeManager.configureBars(this,binding.getRoot(),
                com.google.android.material.color.MaterialColors.getColor(binding.getRoot(),
                        com.google.android.material.R.attr.colorSurface));
        images = Glide.with(this);
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (view, insets) -> {
            var safe = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime()
                    | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(safe.left, safe.top, safe.right, safe.bottom); return insets;
        });
        ViewCompat.setAccessibilityHeading(binding.previewTitle, true);
        ViewCompat.setAccessibilityHeading(binding.recipientHeading, true);
        model = new ViewModelProvider(this).get(PreviewViewModel.class);
        session = new ViewModelProvider(this).get(SessionViewModel.class);
        binding.recipientsList.setLayoutManager(new LinearLayoutManager(this)); binding.recipientsList.setAdapter(adapter);
        binding.captionInput.setFilters(new InputFilter[]{new CaptionFilter()});
        binding.captionInput.setText(model.caption()); updateCaptionCount();
        binding.captionInput.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence text, int start, int count, int after) { }
            public void onTextChanged(CharSequence text, int start, int before, int count) {
                model.caption(text.toString()); updateCaptionCount();
            }
            public void afterTextChanged(Editable value) { }
        });
        binding.allFriendsCheck.setOnCheckedChangeListener((button, checked) -> { if (!rendering) { model.selectAll(checked); } });
        binding.friendsMoreButton.setOnClickListener(view -> model.loadFriends());
        binding.sendFriendsButton.setOnClickListener(view -> model.sendFriends());
        binding.previewLogoutButton.setOnClickListener(view -> session.logout());
        binding.retakeButton.setOnClickListener(view -> finish());
        binding.previewSessionRetry.setOnClickListener(view -> {
            if (networkAllowed()) { session.check(true); }
            else { permission.launch("android.permission.ACCESS_LOCAL_NETWORK"); }
        });
        binding.savePrivateButton.setOnClickListener(view -> {
            if (model.state().getValue().retry()) { model.savePrivate(); return; }
            new MaterialAlertDialogBuilder(this).setMessage(R.string.preview_private_confirm)
                    .setNegativeButton(R.string.preview_cancel, null)
                    .setPositiveButton(R.string.preview_confirm, (dialog, which) -> model.savePrivate()).show();
        });
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { if (!model.state().getValue().sending()) { finish(); } }
        });
        model.state().observe(this, value -> render());
        session.state().observe(this, value -> {
            authorized = value.destination == SessionViewModel.Destination.CAMERA;
            binding.previewScroll.setVisibility(authorized ? View.VISIBLE : View.GONE);
            binding.previewSessionRetry.setVisibility(value.destination == SessionViewModel.Destination.ERROR
                    || value.destination == SessionViewModel.Destination.PERMISSION ? View.VISIBLE : View.GONE);
            binding.previewLogoutButton.setEnabled(value.destination != SessionViewModel.Destination.CHECKING);
            binding.retakeButton.setEnabled(!model.state().getValue().sending());
            if (!authorized) {
                clearImage(); binding.previewNotice.setText(value.message == 0 ? R.string.session_checking : value.message);
                binding.previewProgress.setVisibility(value.destination == SessionViewModel.Destination.CHECKING ? View.VISIBLE : View.INVISIBLE);
            } else {
                model.open(getIntent().getStringExtra(DRAFT_ID), getIntent().getLongExtra(GENERATION, Long.MIN_VALUE)); render();
            }
            if (value.destination == SessionViewModel.Destination.LOGIN && !navigating) {
                navigating = true;
                startActivity(new Intent(this, LoginActivity.class).putExtra(LoginActivity.SESSION_NOTICE, value.message)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK)); finish();
            }
        });
    }
    private void updateCaptionCount() {
        String value = binding.captionInput.getText().toString();
        binding.captionCount.setText(getString(R.string.preview_count, value.codePointCount(0, value.length())));
    }
    private void render() {
        if (!authorized) { return; }
        var value = model.state().getValue(); rendering = true;
        if (value.message() == R.string.preview_missing && binding.captionInput.length() > 0) { binding.captionInput.setText(""); }
        binding.previewTitle.setText(value.downloaded() == null ? R.string.preview_title
                : model.friendTransfer() ? R.string.preview_sent_title : R.string.camera_status_success);
        if (value.sending()) {
            binding.previewProgress.setVisibility(View.VISIBLE);
            if (value.progress() > 0 && value.progress() < 100) {
                binding.previewProgress.setIndeterminate(false);
                binding.previewProgress.setProgressCompat(value.progress(), true);
                binding.previewNotice.setText(getString(R.string.preview_uploading_progress, value.progress()));
            } else if (value.progress() == 100) {
                binding.previewProgress.setIndeterminate(true);
                binding.previewNotice.setText(R.string.preview_processing_server);
            } else {
                binding.previewProgress.setIndeterminate(true);
                binding.previewNotice.setText(value.message());
            }
        } else {
            binding.previewProgress.setIndeterminate(true);
            binding.previewProgress.setVisibility(value.loading() || value.loadingFriends() ? View.VISIBLE : View.INVISIBLE);
            binding.previewNotice.setText(value.message());
        }
        binding.retakeButton.setEnabled(!value.sending());
        binding.captionLayout.setEnabled(!model.selectionLocked() && value.file() != null);
        binding.allFriendsCheck.setEnabled(!model.selectionLocked()); binding.allFriendsCheck.setChecked(value.all());
        boolean ready = value.file() != null && !value.loading() && !value.sending() && value.downloaded() == null;
        binding.savePrivateButton.setEnabled(ready && (!model.selectionLocked() || !model.friendTransfer()));
        binding.savePrivateButton.setText(value.retry() ? R.string.preview_retry_private : R.string.preview_private);
        binding.sendFriendsButton.setEnabled(ready && (model.selectionLocked() ? model.friendTransfer()
                : !value.loadingFriends() && !value.friendsError() && !value.friends().isEmpty()
                    && (value.all() || !value.selected().isEmpty())));
        binding.sendFriendsButton.setText(value.retry() && model.friendTransfer() ? R.string.preview_retry_send : R.string.preview_send);
        binding.friendsNotice.setVisibility(value.loadingFriends() || value.friendsError() || value.friends().isEmpty() ? View.VISIBLE : View.GONE);
        binding.friendsNotice.setText(value.loadingFriends() ? R.string.auth_processing : value.friendsError()
                ? R.string.preview_friends_error : R.string.preview_empty);
        binding.recipientsList.setVisibility(value.friends().isEmpty() ? View.GONE : View.VISIBLE);
        binding.friendsMoreButton.setVisibility(value.friendsError() || value.hasNext() ? View.VISIBLE : View.GONE);
        binding.friendsMoreButton.setText(value.friendsError() ? R.string.session_retry : R.string.preview_more);
        binding.friendsMoreButton.setEnabled(!value.loadingFriends() && !value.sending());
        adapter.update(value); rendering = false;
        Object image = value.downloaded() != null ? value.downloaded() : value.file();
        binding.previewImage.setContentDescription(getString(value.downloaded() == null ? R.string.preview_image : R.string.moment_server_image));
        if (!Objects.equals(image, shownImage)) {
            shownImage = image;
            if (image == null) { clearImage(); }
            else { images.load(image).diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true).into(binding.previewImage); }
        }
    }
    private void clearImage() { images.clear(binding.previewImage); binding.previewImage.setImageDrawable(null); shownImage = null; }
    @Override protected void onStart() { super.onStart(); session.check(networkAllowed()); }
    @Override protected void onDestroy() { clearImage(); super.onDestroy(); }
    private boolean networkAllowed() {
        return !BuildConfig.DEBUG || Build.VERSION.SDK_INT < 37
                || checkSelfPermission("android.permission.ACCESS_LOCAL_NETWORK") == PackageManager.PERMISSION_GRANTED;
    }
    private record Recipient(User user, boolean checked, boolean enabled) { }
    private final class RecipientsAdapter extends ListAdapter<Recipient, RecipientsAdapter.Holder> {
        RecipientsAdapter() { super(new DiffUtil.ItemCallback<Recipient>() {
            @Override public boolean areItemsTheSame(Recipient first, Recipient second) { return first.user().id.equals(second.user().id); }
            @Override public boolean areContentsTheSame(Recipient first, Recipient second) {
                return first.checked() == second.checked() && first.enabled() == second.enabled()
                        && first.user().username.equals(second.user().username) && first.user().displayName.equals(second.user().displayName);
            }
        }); }
        void update(PreviewViewModel.State state) {
            submitList(state.friends().stream().map(user -> new Recipient(user,
                    state.all() || state.selected().contains(user.id), !model.selectionLocked())).collect(java.util.stream.Collectors.toList()));
        }
        @Override public Holder onCreateViewHolder(ViewGroup parent, int type) {
            return new Holder(ItemRecipientBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }
        @Override public void onBindViewHolder(Holder holder, int position) {
            Recipient item = getItem(position); User user = item.user(); var check = holder.binding.recipientCheck;
            check.setOnCheckedChangeListener(null); check.setText(getString(R.string.preview_identity, user.displayName, user.username));
            check.setChecked(item.checked()); check.setEnabled(item.enabled());
            check.setOnCheckedChangeListener((button, checked) -> model.toggle(user.id, checked));
        }
        final class Holder extends RecyclerView.ViewHolder {
            final ItemRecipientBinding binding;
            Holder(ItemRecipientBinding binding) { super(binding.getRoot()); this.binding = binding; }
        }
    }
}
