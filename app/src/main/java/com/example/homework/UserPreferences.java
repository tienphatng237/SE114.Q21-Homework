package com.example.homework;

import android.content.Context;
import android.content.SharedPreferences;

public class UserPreferences {
    public static final String EXTRA_EMAIL = "extra_email";

    static final String PREFS_NAME = "sketch_auth_prefs";
    private static final String KEY_NAME = "name";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_ADDRESS = "address";
    private static final String KEY_AVATAR_URL = "avatar_url";
    private static final String KEY_DESCRIPTION = "description";
    private static final String KEY_LOGGED_IN = "logged_in";

    private final SharedPreferences preferences;

    public UserPreferences(Context context) {
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public boolean hasRegisteredUser() {
        return !getEmail().isEmpty() && !getPassword().isEmpty();
    }

    public boolean validateCredentials(String email, String password) {
        return getEmail().equalsIgnoreCase(email.trim())
                && getPassword().equals(password);
    }

    public void register(UserProfile profile) {
        saveProfile(profile);
        setLoggedIn(false);
    }

    public void saveProfile(UserProfile profile) {
        preferences.edit()
                .putString(KEY_NAME, profile.getName())
                .putString(KEY_EMAIL, profile.getEmail())
                .putString(KEY_PASSWORD, profile.getPassword())
                .putString(KEY_ADDRESS, profile.getAddress())
                .putString(KEY_AVATAR_URL, profile.getAvatarUrl())
                .putString(KEY_DESCRIPTION, profile.getDescription())
                .apply();
    }

    public UserProfile getProfile() {
        return new UserProfile(
                getName(),
                getEmail(),
                getPassword(),
                preferences.getString(KEY_ADDRESS, ""),
                preferences.getString(KEY_AVATAR_URL, ""),
                preferences.getString(KEY_DESCRIPTION, "")
        );
    }

    public String getName() {
        return preferences.getString(KEY_NAME, "");
    }

    public String getEmail() {
        return preferences.getString(KEY_EMAIL, "");
    }

    public String getPassword() {
        return preferences.getString(KEY_PASSWORD, "");
    }

    public String findPasswordByEmail(String email) {
        if (getEmail().equalsIgnoreCase(email.trim())) {
            return getPassword();
        }
        return null;
    }

    public boolean isLoggedIn() {
        return preferences.getBoolean(KEY_LOGGED_IN, false);
    }

    public void setLoggedIn(boolean loggedIn) {
        preferences.edit().putBoolean(KEY_LOGGED_IN, loggedIn).apply();
    }
}
