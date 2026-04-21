package com.example.homework;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class UserPreferences {
    public static final String EXTRA_EMAIL = "extra_email";

    static final String PREFS_NAME = "sketch_auth_prefs";
    private static final String KEY_USERS = "users";
    private static final String KEY_CURRENT_EMAIL = "current_email";
    private static final String KEY_LOGGED_IN = "logged_in";

    private static final String KEY_NAME = "name";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_ADDRESS = "address";
    private static final String KEY_AVATAR_URL = "avatar_url";
    private static final String KEY_DESCRIPTION = "description";

    private final SharedPreferences preferences;

    public UserPreferences(Context context) {
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        migrateLegacySingleUserIfNeeded();
    }

    public boolean hasRegisteredUser() {
        return !getUsers().isEmpty();
    }

    public boolean isEmailRegistered(String email) {
        return findUserByEmail(email) != null;
    }

    public boolean validateCredentials(String email, String password) {
        UserProfile user = findUserByEmail(email);
        return user != null && user.getPassword().equals(password);
    }

    public boolean register(UserProfile profile) {
        UserProfile normalizedProfile = normalizeProfile(profile);
        if (normalizedProfile.getEmail().isEmpty() || isEmailRegistered(normalizedProfile.getEmail())) {
            return false;
        }

        List<UserProfile> users = getUsers();
        users.add(normalizedProfile);
        saveUsers(users);
        setCurrentEmail(normalizedProfile.getEmail());
        setLoggedIn(false);
        return true;
    }

    public boolean saveProfile(UserProfile profile) {
        UserProfile normalizedProfile = normalizeProfile(profile);
        if (normalizedProfile.getEmail().isEmpty()) {
            return false;
        }

        List<UserProfile> users = getUsers();
        String activeEmail = getCurrentEmail();
        if (activeEmail.isEmpty()) {
            activeEmail = normalizedProfile.getEmail();
        }

        int activeIndex = findUserIndex(users, activeEmail);
        int conflictIndex = findUserIndex(users, normalizedProfile.getEmail());
        if (conflictIndex != -1 && conflictIndex != activeIndex) {
            return false;
        }

        if (activeIndex == -1) {
            users.add(normalizedProfile);
        } else {
            users.set(activeIndex, normalizedProfile);
        }

        saveUsers(users);
        setCurrentEmail(normalizedProfile.getEmail());
        return true;
    }

    public boolean saveAvatarUrl(String avatarUrl) {
        String currentEmail = getCurrentEmail();
        if (currentEmail.isEmpty()) {
            return false;
        }

        List<UserProfile> users = getUsers();
        int index = findUserIndex(users, currentEmail);
        if (index == -1) {
            return false;
        }

        UserProfile current = users.get(index);
        users.set(index, new UserProfile(
                current.getName(),
                current.getEmail(),
                current.getPassword(),
                current.getAddress(),
                avatarUrl,
                current.getDescription()
        ));
        saveUsers(users);
        return true;
    }

    public UserProfile getProfile() {
        UserProfile currentProfile = getCurrentProfile();
        if (currentProfile != null) {
            return currentProfile;
        }

        List<UserProfile> users = getUsers();
        if (!users.isEmpty()) {
            return users.get(0);
        }

        return emptyProfile();
    }

    public UserProfile getCurrentProfile() {
        return findUserByEmail(getCurrentEmail());
    }

    public String findPasswordByEmail(String email) {
        UserProfile user = findUserByEmail(email);
        if (user == null) {
            return null;
        }
        return user.getPassword();
    }

    public boolean isLoggedIn() {
        return preferences.getBoolean(KEY_LOGGED_IN, false);
    }

    public void setLoggedIn(boolean loggedIn) {
        if (!loggedIn) {
            setLoggedOut();
            return;
        }

        preferences.edit()
                .putBoolean(KEY_LOGGED_IN, true)
                .putString(KEY_CURRENT_EMAIL, normalizeEmail(getCurrentEmail()))
                .apply();
    }

    public void setLoggedInUser(String email) {
        preferences.edit()
                .putBoolean(KEY_LOGGED_IN, true)
                .putString(KEY_CURRENT_EMAIL, normalizeEmail(email))
                .apply();
    }

    public void setLoggedOut() {
        preferences.edit()
                .putBoolean(KEY_LOGGED_IN, false)
                .putString(KEY_CURRENT_EMAIL, "")
                .apply();
    }

    public String getCurrentEmail() {
        return preferences.getString(KEY_CURRENT_EMAIL, "");
    }

    private List<UserProfile> getUsers() {
        List<UserProfile> users = new ArrayList<>();
        String rawUsers = preferences.getString(KEY_USERS, "[]");

        try {
            JSONArray userArray = new JSONArray(rawUsers);
            for (int i = 0; i < userArray.length(); i++) {
                JSONObject userObject = userArray.optJSONObject(i);
                if (userObject == null) {
                    continue;
                }

                users.add(new UserProfile(
                        userObject.optString(KEY_NAME),
                        normalizeEmail(userObject.optString(KEY_EMAIL)),
                        userObject.optString(KEY_PASSWORD),
                        userObject.optString(KEY_ADDRESS),
                        userObject.optString(KEY_AVATAR_URL),
                        userObject.optString(KEY_DESCRIPTION)
                ));
            }
        } catch (JSONException ignored) {
            preferences.edit().remove(KEY_USERS).apply();
        }

        return users;
    }

    private void saveUsers(List<UserProfile> users) {
        JSONArray userArray = new JSONArray();
        for (UserProfile user : users) {
            JSONObject userObject = new JSONObject();
            try {
                userObject.put(KEY_NAME, user.getName());
                userObject.put(KEY_EMAIL, normalizeEmail(user.getEmail()));
                userObject.put(KEY_PASSWORD, user.getPassword());
                userObject.put(KEY_ADDRESS, user.getAddress());
                userObject.put(KEY_AVATAR_URL, user.getAvatarUrl());
                userObject.put(KEY_DESCRIPTION, user.getDescription());
                userArray.put(userObject);
            } catch (JSONException ignored) {
                // Bỏ qua account lỗi để không làm hỏng danh sách user còn lại.
            }
        }

        preferences.edit().putString(KEY_USERS, userArray.toString()).apply();
    }

    private UserProfile findUserByEmail(String email) {
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail.isEmpty()) {
            return null;
        }

        for (UserProfile user : getUsers()) {
            if (normalizeEmail(user.getEmail()).equals(normalizedEmail)) {
                return user;
            }
        }

        return null;
    }

    private int findUserIndex(List<UserProfile> users, String email) {
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail.isEmpty()) {
            return -1;
        }

        for (int i = 0; i < users.size(); i++) {
            if (normalizeEmail(users.get(i).getEmail()).equals(normalizedEmail)) {
                return i;
            }
        }

        return -1;
    }

    private UserProfile normalizeProfile(UserProfile profile) {
        return new UserProfile(
                safeTrim(profile.getName()),
                normalizeEmail(profile.getEmail()),
                profile.getPassword(),
                safeTrim(profile.getAddress()),
                safeTrim(profile.getAvatarUrl()),
                safeTrim(profile.getDescription())
        );
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            return "";
        }
        return email.trim().toLowerCase(Locale.US);
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private UserProfile emptyProfile() {
        return new UserProfile("", "", "", "", "", "");
    }

    private void migrateLegacySingleUserIfNeeded() {
        if (preferences.contains(KEY_USERS)) {
            return;
        }

        String legacyEmail = preferences.getString(KEY_EMAIL, "");
        String legacyPassword = preferences.getString(KEY_PASSWORD, "");
        String legacyName = preferences.getString(KEY_NAME, "");
        String legacyAddress = preferences.getString(KEY_ADDRESS, "");
        String legacyAvatarUrl = preferences.getString(KEY_AVATAR_URL, "");
        String legacyDescription = preferences.getString(KEY_DESCRIPTION, "");

        if (legacyEmail.isEmpty() && legacyPassword.isEmpty()) {
            return;
        }

        List<UserProfile> legacyUsers = new ArrayList<>();
        legacyUsers.add(new UserProfile(
                legacyName,
                legacyEmail,
                legacyPassword,
                legacyAddress,
                legacyAvatarUrl,
                legacyDescription
        ));
        saveUsers(legacyUsers);
        setCurrentEmail(legacyEmail);
    }

    private void setCurrentEmail(String email) {
        preferences.edit().putString(KEY_CURRENT_EMAIL, normalizeEmail(email)).apply();
    }
}
