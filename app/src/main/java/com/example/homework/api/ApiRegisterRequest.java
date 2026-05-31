package com.example.homework;

public class ApiRegisterRequest {
    private final String name;
    private final String email;
    private final String phone;
    private final String password;

    public ApiRegisterRequest(String name, String email, String phone, String password) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.password = password;
    }
}
