package com.example.homework;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class ApiPostsResponse {
    @SerializedName("status")
    private String status;

    @SerializedName("data")
    private List<ApiPost> data;

    public String getStatus() {
        return status == null ? "" : status;
    }

    public List<ApiPost> getData() {
        return data == null ? new ArrayList<>() : data;
    }
}
