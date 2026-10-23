package com.taktak.app.ui.profile;

import android.os.Bundle;
import android.view.*;
import android.text.*;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.taktak.app.R;
import com.taktak.app.databinding.FragmentProfileBinding;
import com.taktak.app.viewmodel.*;

public final class ProfileFragment extends Fragment {
    private FragmentProfileBinding binding;
    private ProfileViewModel model;
    @Override public View onCreateView(@NonNull LayoutInflater inflater,ViewGroup parent,Bundle saved) {
        binding=FragmentProfileBinding.inflate(inflater,parent,false);return binding.getRoot();
    }
    @Override public void onViewCreated(@NonNull View view,Bundle saved) {
        model=new ViewModelProvider(requireActivity()).get(ProfileViewModel.class);
        var session=new ViewModelProvider(requireActivity()).get(SessionViewModel.class);
        binding.profileNameInput.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s,int start,int count,int after) {}
            public void onTextChanged(CharSequence s,int start,int before,int count) { model.draft(s.toString()); }
            public void afterTextChanged(Editable s) {}
        });
        binding.profileSave.setOnClickListener(v -> model.save());
        binding.profileRetry.setOnClickListener(v -> model.load());
        binding.profileChangeAvatar.setOnClickListener(v -> ((com.taktak.app.MainActivity)requireActivity()).chooseProfileAvatar());
        binding.profileAvatarEdit.setOnClickListener(v -> binding.profileChangeAvatar.performClick());
        binding.profileLogout.setOnClickListener(v -> session.logout());
        binding.profileThemeButton.setOnClickListener(v -> startActivity(new android.content.Intent(
                requireContext(), com.taktak.app.ui.theme.ThemeSettingsActivity.class)));
        binding.profileLanguageButton.setOnClickListener(v -> com.taktak.app.ui.language.LanguageManager.showLanguageDialog(requireContext(), null));
        binding.profileIntroButton.setOnClickListener(v -> com.taktak.app.ui.splash.IntroSettings.show(
                (com.taktak.app.MainActivity) requireActivity()));
        binding.profileMyQrButton.setOnClickListener(v -> {
            var state = model.state().getValue();
            if (state != null && state.profile != null) {
                com.taktak.app.ui.qr.MyQrCodeBottomSheet.newInstance(
                        state.profile.username,
                        state.profile.displayName,
                        state.profile.id
                ).show(getChildFragmentManager(), "MyQrCode");
            }
        });

        // Biometric App Lock
        setupBiometricSettings();

        androidx.core.view.ViewCompat.setAccessibilityHeading(binding.profileTitle,true);
        model.state().observe(getViewLifecycleOwner(),state -> {
            if(state.expired) { session.check(true); return; }
            binding.profileProgress.setVisibility(state.busy ? View.VISIBLE : View.INVISIBLE);
            binding.profileMessage.setVisibility(state.message==0 ? View.GONE : View.VISIBLE);
            if(state.message!=0) binding.profileMessage.setText(state.message);
            boolean ready=state.profile!=null;
            binding.profileSave.setEnabled(ready && !state.busy);
            binding.profileChangeAvatar.setEnabled(ready && !state.busy);
            binding.profileAvatarEdit.setEnabled(ready && !state.busy);
            binding.profileNameInput.setEnabled(ready && !state.busy);
            binding.profileRetry.setEnabled(!state.busy);
            binding.profileLogout.setEnabled(!state.busy);
            binding.profileThemeButton.setEnabled(!state.busy);
            binding.profileLanguageButton.setEnabled(!state.busy);
            binding.profileMyQrButton.setEnabled(ready && !state.busy);
            binding.profileIdentity.setVisibility(ready ? View.VISIBLE : View.GONE);
            binding.profileUsername.setVisibility(ready ? View.VISIBLE : View.GONE);
            if(ready) {
                binding.profileUsername.setText(getString(com.taktak.app.R.string.friends_username,state.profile.username));
                binding.profileIdentity.setText(state.profile.email);
                String draft=model.draft();
                if(draft!=null && !draft.contentEquals(binding.profileNameInput.getText())) binding.profileNameInput.setText(draft);
            }
            if(state.avatar==null) {
                Glide.with(this).clear(binding.profileAvatar);
                binding.profileAvatar.setImageResource(com.taktak.app.R.drawable.friend_avatar);
            } else {
                Glide.with(this).load(state.avatar).placeholder(com.taktak.app.R.drawable.friend_avatar)
                        .error(com.taktak.app.R.drawable.friend_avatar).diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(true).circleCrop().into(binding.profileAvatar);
            }
        });
        model.load();
    }

    private void setupBiometricSettings() {
        var bioPrefs = com.taktak.app.data.security.BiometricPreferences.get(requireContext());
        binding.profileBiometricSwitch.setChecked(bioPrefs.isBiometricEnabled());

        binding.profileBiometricCard.setOnClickListener(v -> binding.profileBiometricSwitch.performClick());

        binding.profileBiometricSwitch.setOnClickListener(v -> {
            boolean targetEnabled = binding.profileBiometricSwitch.isChecked();
            if (targetEnabled) {
                if (!com.taktak.app.data.security.BiometricHelper.canAuthenticate(requireContext())) {
                    binding.profileBiometricSwitch.setChecked(false);
                    android.widget.Toast.makeText(requireContext(),
                            R.string.biometric_not_supported, android.widget.Toast.LENGTH_SHORT).show();
                    return;
                }
                com.taktak.app.data.security.BiometricHelper.authenticate(
                        requireActivity(),
                        getString(R.string.biometric_prompt_title),
                        getString(R.string.biometric_prompt_subtitle),
                        new com.taktak.app.data.security.BiometricHelper.AuthCallback() {
                            @Override public void onSuccess() {
                                bioPrefs.setBiometricEnabled(true);
                                android.widget.Toast.makeText(requireContext(),
                                        R.string.biometric_enable_success, android.widget.Toast.LENGTH_SHORT).show();
                            }
                            @Override public void onError(int errorCode, @androidx.annotation.NonNull CharSequence errString) {
                                binding.profileBiometricSwitch.setChecked(false);
                            }
                            @Override public void onFailed() {
                                binding.profileBiometricSwitch.setChecked(false);
                            }
                        }
                );
            } else {
                com.taktak.app.data.security.BiometricHelper.authenticate(
                        requireActivity(),
                        getString(R.string.biometric_prompt_title),
                        getString(R.string.biometric_prompt_subtitle),
                        new com.taktak.app.data.security.BiometricHelper.AuthCallback() {
                            @Override public void onSuccess() {
                                bioPrefs.setBiometricEnabled(false);
                                android.widget.Toast.makeText(requireContext(),
                                        R.string.biometric_disable_success, android.widget.Toast.LENGTH_SHORT).show();
                            }
                            @Override public void onError(int errorCode, @androidx.annotation.NonNull CharSequence errString) {
                                binding.profileBiometricSwitch.setChecked(true);
                            }
                            @Override public void onFailed() {
                                binding.profileBiometricSwitch.setChecked(true);
                            }
                        }
                );
            }
        });
    }

    @Override public void onDestroyView() {
        if(binding!=null) Glide.with(this).clear(binding.profileAvatar);
        binding=null;super.onDestroyView();
    }
}
