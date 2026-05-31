package com.example.homework;

import com.google.gson.annotations.SerializedName;

public class ApiCreatePostRequest {
    @SerializedName("user_id")
    private final int userId;

    @SerializedName("content")
    private final String content;

    public ApiCreatePostRequest(int userId, String content) {
        this.userId = userId;
        this.content = content;
    }
}
