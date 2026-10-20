package com.taktak.app.ui.qr;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.taktak.app.R;
import com.taktak.app.databinding.DialogMyQrCodeBinding;

public class MyQrCodeBottomSheet extends BottomSheetDialogFragment {
    public static final String DISMISSED = "qr_my_code_dismissed";
    private static final String ARG_USERNAME = "arg_username";
    private static final String ARG_DISPLAY_NAME = "arg_display_name";
    private static final String ARG_USER_ID = "arg_user_id";

    private DialogMyQrCodeBinding binding;
    private Bitmap qrBitmap;

    public static MyQrCodeBottomSheet newInstance(@NonNull String username, @NonNull String displayName, @Nullable String userId) {
        MyQrCodeBottomSheet sheet = new MyQrCodeBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_USERNAME, username);
        args.putString(ARG_DISPLAY_NAME, displayName);
        args.putString(ARG_USER_ID, userId);
        sheet.setArguments(args);
        return sheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DialogMyQrCodeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle args = getArguments();
        String username = args != null ? args.getString(ARG_USERNAME, "") : "";
        String displayName = args != null ? args.getString(ARG_DISPLAY_NAME, "") : "";
        String userId = args != null ? args.getString(ARG_USER_ID, null) : null;

        binding.qrUserDisplayName.setText(displayName.isEmpty() ? username : displayName);
        binding.qrUserUsername.setText("@" + username);

        String payload;
        try { payload = QrCodeUtils.buildFriendPayload(username, userId); }
        catch (IllegalArgumentException ignored) {
            binding.qrSubtitle.setText(com.taktak.app.R.string.qr_lookup_failed);
            binding.btnShareQr.setEnabled(false);
            binding.btnCloseQr.setOnClickListener(v -> dismiss()); return;
        }
        qrBitmap = QrCodeUtils.generateQrBitmap(payload, 512, Color.BLACK, Color.WHITE);
        binding.btnShareQr.setEnabled(qrBitmap != null);
        if (qrBitmap != null) {
            binding.qrImageView.setImageBitmap(qrBitmap);
        }

        binding.btnShareQr.setOnClickListener(v -> {
            Intent sendIntent = new Intent(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.qr_share_text, displayName, username, payload));
            sendIntent.setType("text/plain");
            startActivity(Intent.createChooser(sendIntent, getString(R.string.qr_btn_share)));
        });

        binding.btnCloseQr.setOnClickListener(v -> dismiss());
    }

    @Override
    public void onDestroyView() {
        if (binding != null) { binding.qrImageView.setImageDrawable(null); }
        if (qrBitmap != null) { qrBitmap.recycle(); qrBitmap = null; }
        super.onDestroyView();
        binding = null;
    }

    @Override public void onDismiss(@NonNull android.content.DialogInterface dialog) {
        super.onDismiss(dialog);
        getParentFragmentManager().setFragmentResult(DISMISSED, new Bundle());
    }
}
