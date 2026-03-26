package com.example.homework;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

public class ProfileActivity extends AppCompatActivity {

    private UserPreferences userPreferences;
    private AvatarStorage avatarStorage;
    private ProfileFormView formView;
    private MainBottomNavView bottomNavView;
    private String selectedAvatarPath = "";

    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), this::handleImagePicked);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        userPreferences = new UserPreferences(this);
        avatarStorage = new AvatarStorage(this);

        if (!hasProfileAccess()) {
            toast(R.string.message_profile_required);
            openLogin();
            return;
        }

        formView = new ProfileFormView(this);
        bottomNavView = new MainBottomNavView(this);
        formView.bindPickAvatar(view -> openImagePicker());
        formView.bindSave(view -> saveProfile());
        formView.bindLogout(view -> logout());
        bottomNavView.bindHome(view -> openHome());
        bottomNavView.bindProfile(view -> { });
        bottomNavView.showProfileSelected();
        showProfile();
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
        renderAvatar();
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

        userPreferences.saveProfile(updatedProfile);
        formView.updateTitle(name);
        renderAvatar();
        toast(R.string.message_profile_saved);
    }

    private void logout() {
        userPreferences.setLoggedIn(false);
        toast(R.string.message_logged_out);
        openLogin();
    }

    private void openHome() {
        startActivity(new Intent(this, PostActivity.class));
        finish();
    }

    private void openLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void toast(int messageId) {
        Toast.makeText(this, messageId, Toast.LENGTH_SHORT).show();
    }
}
