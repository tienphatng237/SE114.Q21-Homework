package com.example.homework;

import com.google.gson.annotations.SerializedName;

public class ApiPostResponse {
    @SerializedName("status")
    private String status;

    @SerializedName("message")
    private String message;

    @SerializedName("data")
    private ApiPost data;

    public String getStatus() {
        return status == null ? "" : status;
    }

    public String getMessage() {
        return message == null ? "" : message;
    }

    public ApiPost getData() {
        return data;
    }
}
