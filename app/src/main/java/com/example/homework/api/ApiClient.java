package com.example.homework;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class ApiClient {

    private static final String BASE_URL = "http://blackntt.net:8111/";
    private static ApiService apiService;

    private ApiClient() {
    }

    public static ApiService getService() {
        if (apiService != null) {
            return apiService;
        }

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(ApiService.class);
        return apiService;
    }
}
