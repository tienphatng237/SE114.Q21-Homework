package com.example.homework;

import com.google.gson.annotations.SerializedName;

public class ApiPost {
    @SerializedName("id")
    private int id;

    @SerializedName("user_id")
    private int userId;

    @SerializedName("content")
    private String content;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("author")
    private ApiUser author;

    public int getId() {
        return id;
    }

    public int getUserId() {
        return userId;
    }

    public String getContent() {
        return content == null ? "" : content;
    }

    public String getCreatedAt() {
        return createdAt == null ? "" : createdAt;
    }

    public ApiUser getAuthor() {
        return author;
    }
}
