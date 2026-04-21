package com.example.homework;

import android.content.Context;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import java.util.Locale;

public class ProfileFormView {

    private final Context context;
    private final TextView titleTextView;
    private final TextView avatarStatusTextView;
    private final EditText nameEditText;
    private final EditText emailEditText;
    private final EditText addressEditText;
    private final EditText descriptionEditText;
    private final ImageView avatarImageView;
    private final FrameLayout avatarPickerFrame;
    private final MaterialButton chooseImageButton;
    private final MaterialButton saveButton;
    private final MaterialButton logoutButton;

    public ProfileFormView(AppCompatActivity activity) {
        this(activity.findViewById(android.R.id.content));
    }

    public ProfileFormView(View rootView) {
        this.context = rootView.getContext();
        titleTextView = rootView.findViewById(R.id.text_profile_title);
        avatarStatusTextView = rootView.findViewById(R.id.text_avatar_status);
        nameEditText = rootView.findViewById(R.id.edit_name);
        emailEditText = rootView.findViewById(R.id.edit_email);
        addressEditText = rootView.findViewById(R.id.edit_address);
        descriptionEditText = rootView.findViewById(R.id.edit_description);
        avatarImageView = rootView.findViewById(R.id.image_avatar);
        avatarPickerFrame = rootView.findViewById(R.id.frame_avatar_picker);
        chooseImageButton = rootView.findViewById(R.id.button_choose_image);
        saveButton = rootView.findViewById(R.id.button_save);
        logoutButton = rootView.findViewById(R.id.button_logout);
    }

    public void bindPickAvatar(View.OnClickListener listener) {
        avatarPickerFrame.setOnClickListener(listener);
        chooseImageButton.setOnClickListener(listener);
    }

    public void bindSave(View.OnClickListener listener) {
        saveButton.setOnClickListener(listener);
    }

    public void bindLogout(View.OnClickListener listener) {
        logoutButton.setOnClickListener(listener);
    }

    public void showProfile(UserProfile profile) {
        nameEditText.setText(profile.getName());
        emailEditText.setText(profile.getEmail());
        addressEditText.setText(profile.getAddress());
        descriptionEditText.setText(profile.getDescription());
        updateTitle(profile.getName());
    }

    public void renderAvatar(AvatarStorage avatarStorage, String avatarPath) {
        avatarStorage.loadInto(avatarImageView, avatarPath);
        if (avatarPath.isEmpty()) {
            avatarStatusTextView.setText(R.string.no_image_selected);
            return;
        }

        avatarStatusTextView.setText(avatarStorage.getDisplayName(avatarPath));
    }

    public String getName() {
        return nameEditText.getText().toString().trim();
    }

    public String getEmail() {
        return emailEditText.getText().toString().trim();
    }

    public String getAddress() {
        return addressEditText.getText().toString().trim();
    }

    public String getDescription() {
        return descriptionEditText.getText().toString().trim();
    }

    public void showEmailError(String message) {
        emailEditText.setError(message);
        emailEditText.requestFocus();
    }

    public void updateTitle(String name) {
        String safeName = name == null || name.isEmpty()
                ? context.getString(R.string.default_profile_name)
                : toDisplayName(name);
        titleTextView.setText(context.getString(R.string.profile_title_format, safeName));
    }

    private String toDisplayName(String name) {
        if (name == null) {
            return "";
        }

        String trimmedName = name.trim();
        if (trimmedName.isEmpty()) {
            return "";
        }

        String firstCharacter = trimmedName.substring(0, 1).toUpperCase(Locale.getDefault());
        if (trimmedName.length() == 1) {
            return firstCharacter;
        }

        return firstCharacter + trimmedName.substring(1);
    }
}
