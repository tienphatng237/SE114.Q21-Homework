package com.example.homework;

public class UserProfile {
    private final String name;
    private final String email;
    private final String password;
    private final String address;
    private final String avatarUrl;
    private final String description;

    public UserProfile(String name, String email, String password, String address,
                       String avatarUrl, String description) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.address = address;
        this.avatarUrl = avatarUrl;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getAddress() {
        return address;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public String getDescription() {
        return description;
    }
}
