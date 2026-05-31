package com.example.homework;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class ApiFriendsResponse {
    @SerializedName("status")
    private String status;

    @SerializedName("message")
    private String message;

    @SerializedName("friends")
    private List<ApiUser> friends;

    public String getStatus() {
        return status == null ? "" : status;
    }

    public String getMessage() {
        return message == null ? "" : message;
    }

    public List<ApiUser> getFriends() {
        return friends == null ? new ArrayList<>() : friends;
    }
}
