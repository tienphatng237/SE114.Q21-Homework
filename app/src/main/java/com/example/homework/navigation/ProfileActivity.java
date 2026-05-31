package com.example.homework;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Patterns;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

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

        MaterialToolbar topAppBar = findViewById(R.id.top_app_bar);
        setSupportActionBar(topAppBar);
        setTitle(R.string.profile);

        Animation toolbarEnter = AnimationUtils.loadAnimation(this, R.anim.toolbar_enter);
        Animation panelEnter = AnimationUtils.loadAnimation(this, R.anim.panel_enter);
        topAppBar.startAnimation(toolbarEnter);
        findViewById(R.id.panel_main).startAnimation(panelEnter);

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
        userPreferences.saveAvatarUrl(selectedAvatarPath);
        renderAvatar();
        pulseView(findViewById(R.id.frame_avatar_picker));
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
        pulseView(findViewById(R.id.button_save));
        toast(R.string.message_profile_saved);
    }

    private void logout() {
        userPreferences.setLoggedOut();
        toast(R.string.message_logged_out);
        openLogin();
    }

    private void openHome() {
        startActivity(MainShellActivity.createIntent(this, MainShellActivity.TAB_HOME));
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main_options, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_about) {
            showAboutDialog();
            return true;
        }
        if (itemId == R.id.action_logout) {
            logout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showAboutDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.about_dialog_title)
                .setMessage(R.string.about_dialog_message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
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
