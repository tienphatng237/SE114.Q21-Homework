package com.example.homework;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PostStorage {

    private static final String KEY_POSTS = "posts";
    private static final String KEY_AUTHOR_NAME = "author_name";
    private static final String KEY_AUTHOR_AVATAR = "author_avatar";
    private static final String KEY_DATE = "date";
    private static final String KEY_CONTENT = "content";

    private final Context context;
    private final SharedPreferences preferences;

    public PostStorage(Context context) {
        this.context = context.getApplicationContext();
        preferences = this.context.getSharedPreferences(UserPreferences.PREFS_NAME, Context.MODE_PRIVATE);
    }

    public List<PostItem> getPosts() {
        List<PostItem> posts = new ArrayList<>();
        String rawPosts = preferences.getString(KEY_POSTS, "[]");

        try {
            JSONArray postArray = new JSONArray(rawPosts);
            for (int i = 0; i < postArray.length(); i++) {
                JSONObject postObject = postArray.optJSONObject(i);
                if (postObject == null) {
                    continue;
                }

                posts.add(new PostItem(
                        postObject.optString(KEY_AUTHOR_NAME),
                        postObject.optString(KEY_AUTHOR_AVATAR),
                        postObject.optString(KEY_DATE),
                        postObject.optString(KEY_CONTENT)
                ));
            }
        } catch (JSONException exception) {
            preferences.edit().remove(KEY_POSTS).apply();
        }

        return posts;
    }

    public void addPost(PostItem post) {
        List<PostItem> posts = getPosts();
        posts.add(0, post);
        savePosts(posts);
    }

    public boolean deletePostAt(int position) {
        List<PostItem> posts = getPosts();
        if (position < 0 || position >= posts.size()) {
            return false;
        }

        posts.remove(position);
        savePosts(posts);
        return true;
    }

    public void removeLegacySamplePosts() {
        String sampleContent = context.getString(R.string.sample_post_content);
        List<PostItem> filteredPosts = new ArrayList<>();
        List<PostItem> currentPosts = getPosts();

        for (PostItem post : currentPosts) {
            if (!sampleContent.equals(post.getContent())) {
                filteredPosts.add(post);
            }
        }

        if (filteredPosts.size() != currentPosts.size()) {
            savePosts(filteredPosts);
        }
    }

    public String createCurrentDateLabel() {
        return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
    }

    private void savePosts(List<PostItem> posts) {
        JSONArray postArray = new JSONArray();
        for (PostItem post : posts) {
            JSONObject postObject = new JSONObject();
            try {
                postObject.put(KEY_AUTHOR_NAME, post.getAuthorName());
                postObject.put(KEY_AUTHOR_AVATAR, post.getAuthorAvatarPath());
                postObject.put(KEY_DATE, post.getDateLabel());
                postObject.put(KEY_CONTENT, post.getContent());
                postArray.put(postObject);
            } catch (JSONException ignored) {
                // Bỏ qua bài lỗi để không làm hỏng toàn bộ danh sách.
            }
        }

        preferences.edit().putString(KEY_POSTS, postArray.toString()).apply();
    }
}
