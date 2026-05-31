package com.example.homework;

import com.google.gson.annotations.SerializedName;

public class ApiRegisterResponse {
    @SerializedName("status")
    private String status;

    @SerializedName("message")
    private String message;

    @SerializedName("user")
    private ApiUser user;

    public String getStatus() {
        return status == null ? "" : status;
    }

    public String getMessage() {
        return message == null ? "" : message;
    }

    public ApiUser getUser() {
        return user;
    }
}
