package com.example.homework;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface ApiService {
    @GET("api/posts")
    Call<ApiPostsResponse> getAllPosts();

    @POST("api/login")
    Call<ApiLoginResponse> login(@Body ApiLoginRequest payload);

    @POST("api/posts")
    Call<ApiPostResponse> createPost(@Body ApiCreatePostRequest payload);

    @DELETE("api/posts/{post_id}")
    Call<ApiDeleteResponse> deletePost(@Path("post_id") int postId);

    @GET("api/posts/user/{user_id}")
    Call<ApiPostsResponse> getPostsByUser(@Path("user_id") int userId);

    @POST("api/register")
    Call<ApiRegisterResponse> register(@Body ApiRegisterRequest payload);

    @GET("api/users/emails")
    Call<ApiEmailsResponse> getAllUserEmails();

    @GET("api/users/{user_id}/friends")
    Call<ApiFriendsResponse> getUserFriends(@Path("user_id") int userId);

    @GET("api/users/{user_id}/profile")
    Call<ApiProfileResponse> getUserProfile(@Path("user_id") int userId);

    @PATCH("api/users/{user_id}/profile")
    Call<ApiProfileResponse> updateUserProfile(
            @Path("user_id") int userId,
            @Body ApiUpdateProfileRequest payload
    );
}
