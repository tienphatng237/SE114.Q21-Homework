package com.example.homework;

import com.google.gson.annotations.SerializedName;

public class ApiUpdateProfileRequest {
    @SerializedName("name")
    private final String name;

    @SerializedName("address")
    private final String address;

    @SerializedName("avatar_url")
    private final String avatarUrl;

    @SerializedName("description")
    private final String description;

    @SerializedName("phone")
    private final String phone;

    public ApiUpdateProfileRequest(
            String name,
            String address,
            String avatarUrl,
            String description,
            String phone
    ) {
        this.name = name;
        this.address = address;
        this.avatarUrl = avatarUrl;
        this.description = description;
        this.phone = phone;
    }
}
