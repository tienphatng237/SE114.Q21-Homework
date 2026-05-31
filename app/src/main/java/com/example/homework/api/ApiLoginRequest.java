package com.example.homework;

public class ApiLoginRequest {
    private final String email;
    private final String password;

    public ApiLoginRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }
}
