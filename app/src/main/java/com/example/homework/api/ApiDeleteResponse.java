package com.example.homework;

import com.google.gson.annotations.SerializedName;

public class ApiDeleteResponse {
    @SerializedName("status")
    private String status;

    @SerializedName("message")
    private String message;

    public String getStatus() {
        return status == null ? "" : status;
    }

    public String getMessage() {
        return message == null ? "" : message;
    }
}
