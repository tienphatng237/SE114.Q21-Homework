package com.example.homework;

import com.google.gson.annotations.SerializedName;

public class ApiUser {
    @SerializedName("id")
    private int id;

    @SerializedName("name")
    private String name;

    @SerializedName("avatar_url")
    private String avatarUrl;

    @SerializedName("address")
    private String address;

    @SerializedName("description")
    private String description;

    @SerializedName("phone")
    private String phone;

    @SerializedName("email")
    private String email;

    public int getId() {
        return id;
    }

    public String getName() {
        return name == null ? "" : name;
    }

    public String getAvatarUrl() {
        return avatarUrl == null ? "" : avatarUrl;
    }

    public String getEmail() {
        return email == null ? "" : email;
    }

    public String getAddress() {
        return address == null ? "" : address;
    }

    public String getDescription() {
        return description == null ? "" : description;
    }

    public String getPhone() {
        return phone == null ? "" : phone;
    }
}
