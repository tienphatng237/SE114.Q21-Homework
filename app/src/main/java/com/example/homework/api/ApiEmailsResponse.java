package com.example.homework;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class ApiEmailsResponse {
    @SerializedName("status")
    private String status;

    @SerializedName("message")
    private String message;

    @SerializedName("emails")
    private List<String> emails;

    public String getStatus() {
        return status == null ? "" : status;
    }

    public String getMessage() {
        return message == null ? "" : message;
    }

    public List<String> getEmails() {
        return emails == null ? new ArrayList<>() : emails;
    }
}
