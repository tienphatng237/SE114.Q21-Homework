package com.example.homework;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFragment extends Fragment {

    private UserPreferences userPreferences;
    private AvatarStorage avatarStorage;
    private ApiService apiService;
    private ProfileFormView formView;
    private String selectedAvatarPath = "";
    private ActivityResultLauncher<String> pickImageLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                this::handleImagePicked
        );
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        userPreferences = new UserPreferences(requireContext());
        avatarStorage = new AvatarStorage(requireContext());
        apiService = ApiClient.getService();

        if (!hasProfileAccess()) {
            toast(R.string.message_profile_required);
            openLogin();
            return;
        }

        MaterialToolbar topAppBar = view.findViewById(R.id.top_app_bar);
        ((AppCompatActivity) requireActivity()).setSupportActionBar(topAppBar);
        requireActivity().setTitle(R.string.profile);

        Animation toolbarEnter = AnimationUtils.loadAnimation(requireContext(), R.anim.toolbar_enter);
        Animation panelEnter = AnimationUtils.loadAnimation(requireContext(), R.anim.panel_enter);
        topAppBar.startAnimation(toolbarEnter);
        view.findViewById(R.id.panel_main).startAnimation(panelEnter);

        formView = new ProfileFormView(view);
        formView.bindPickAvatar(v -> openImagePicker());
        formView.bindSave(v -> saveProfile());
        formView.bindLogout(v -> logout());
        showProfile();
        syncProfileFromApiIfPossible();
    }

    private boolean hasProfileAccess() {
        return userPreferences.isLoggedIn() && userPreferences.hasRegisteredUser();
    }

    private void showProfile() {
        UserProfile profile = userPreferences.getProfile();
        selectedAvatarPath = avatarStorage.normalizeStoredPath(profile.getAvatarUrl());
        formView.showProfile(profile);
        renderAvatar();
    }

    private void openImagePicker() {
        pickImageLauncher.launch("image/*");
    }

    private void handleImagePicked(Uri imageUri) {
        if (imageUri == null) {
            return;
        }

        String importedAvatarPath = avatarStorage.importAvatar(imageUri, selectedAvatarPath);
        if (importedAvatarPath.isEmpty()) {
            toast(R.string.message_image_pick_failed);
            return;
        }

        selectedAvatarPath = importedAvatarPath;
        userPreferences.saveAvatarUrl(selectedAvatarPath);
        renderAvatar();
        pulseView(requireView().findViewById(R.id.frame_avatar_picker));
    }

    private void renderAvatar() {
        formView.renderAvatar(avatarStorage, selectedAvatarPath);
    }

    private void saveProfile() {
        String name = formView.getName();
        String email = formView.getEmail();

        if (name.isEmpty() || email.isEmpty()) {
            toast(R.string.message_fill_all_fields);
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            formView.showEmailError(getString(R.string.message_invalid_email));
            return;
        }

        UserProfile currentProfile = userPreferences.getProfile();
        UserProfile updatedProfile = new UserProfile(
                name,
                email,
                currentProfile.getPassword(),
                formView.getAddress(),
                selectedAvatarPath,
                formView.getDescription()
        );

        if (!userPreferences.saveProfile(updatedProfile)) {
            formView.showEmailError(getString(R.string.message_email_already_registered));
            return;
        }

        formView.updateTitle(name);
        renderAvatar();
        syncProfileToApiIfPossible(updatedProfile);
        pulseView(requireView().findViewById(R.id.button_save));
        toast(R.string.message_profile_saved);
    }

    private void syncProfileFromApiIfPossible() {
        String currentEmail = userPreferences.getCurrentEmail();
        int apiUserId = userPreferences.getApiUserId(currentEmail);
        if (apiUserId <= 0) {
            return;
        }

        apiService.getUserProfile(apiUserId).enqueue(new Callback<ApiProfileResponse>() {
            @Override
            public void onResponse(Call<ApiProfileResponse> call, Response<ApiProfileResponse> response) {
                if (!isAdded()) {
                    return;
                }

                ApiProfileResponse body = response.body();
                if (!response.isSuccessful() || body == null || body.getUser() == null) {
                    return;
                }

                ApiUser apiUser = body.getUser();
                userPreferences.saveApiPhone(currentEmail, apiUser.getPhone());
                UserProfile currentProfile = userPreferences.getProfile();
                UserProfile mergedProfile = new UserProfile(
                        fallbackText(apiUser.getName(), currentProfile.getName()),
                        currentProfile.getEmail(),
                        currentProfile.getPassword(),
                        fallbackText(apiUser.getAddress(), currentProfile.getAddress()),
                        fallbackText(apiUser.getAvatarUrl(), currentProfile.getAvatarUrl()),
                        fallbackText(apiUser.getDescription(), currentProfile.getDescription())
                );

                userPreferences.saveProfile(mergedProfile);
                selectedAvatarPath = avatarStorage.normalizeStoredPath(mergedProfile.getAvatarUrl());
                formView.showProfile(mergedProfile);
                renderAvatar();
            }

            @Override
            public void onFailure(Call<ApiProfileResponse> call, Throwable throwable) {
                // Không chặn thao tác profile local khi API tạm lỗi.
            }
        });
    }

    private void syncProfileToApiIfPossible(UserProfile updatedProfile) {
        String currentEmail = userPreferences.getCurrentEmail();
        int apiUserId = userPreferences.getApiUserId(currentEmail);
        if (apiUserId <= 0) {
            return;
        }

        String apiPhone = userPreferences.getApiPhone(currentEmail);
        ApiUpdateProfileRequest payload = new ApiUpdateProfileRequest(
                updatedProfile.getName(),
                updatedProfile.getAddress(),
                updatedProfile.getAvatarUrl(),
                updatedProfile.getDescription(),
                apiPhone
        );
        apiService.updateUserProfile(apiUserId, payload).enqueue(new Callback<ApiProfileResponse>() {
            @Override
            public void onResponse(Call<ApiProfileResponse> call, Response<ApiProfileResponse> response) {
                if (!isAdded()) {
                    return;
                }

                if (!response.isSuccessful()) {
                    toast(R.string.message_api_profile_update_failed);
                }
            }

            @Override
            public void onFailure(Call<ApiProfileResponse> call, Throwable throwable) {
                if (!isAdded()) {
                    return;
                }
                toast(R.string.message_api_profile_update_failed);
            }
        });
    }

    private String fallbackText(String apiValue, String localValue) {
        if (apiValue == null || apiValue.trim().isEmpty()) {
            return localValue == null ? "" : localValue;
        }
        return apiValue.trim();
    }

    private void logout() {
        userPreferences.setLoggedOut();
        toast(R.string.message_logged_out);
        openLogin();
    }

    private void showAboutDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.about_dialog_title)
                .setMessage(R.string.about_dialog_message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void openLogin() {
        startActivity(new Intent(requireContext(), LoginActivity.class));
        requireActivity().finish();
    }

    private void toast(int messageId) {
        Toast.makeText(requireContext(), messageId, Toast.LENGTH_SHORT).show();
    }

    private void pulseView(View view) {
        view.animate()
                .scaleX(1.06f)
                .scaleY(1.06f)
                .setDuration(120)
                .withEndAction(() -> view.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(120)
                        .start())
                .start();
    }
}
