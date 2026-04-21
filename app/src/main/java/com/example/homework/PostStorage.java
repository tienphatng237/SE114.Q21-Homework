package com.example.homework;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PostStorage {

    private static final String KEY_POSTS = "posts";
    private static final String KEY_ID = "id";
    private static final String KEY_AUTHOR_NAME = "author_name";
    private static final String KEY_AUTHOR_AVATAR = "author_avatar";
    private static final String KEY_DATE = "date";
    private static final String KEY_CONTENT = "content";
    private static final String KEY_CREATED_AT = "created_at";
    private static final String KEY_HIDDEN_POSTS_PREFIX = "hidden_posts_";
    private static final String DATE_PATTERN = "dd/MM/yyyy";

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

                long createdAtMillis = resolveCreatedAt(postObject);
                posts.add(new PostItem(
                        resolvePostId(postObject, createdAtMillis),
                        postObject.optString(KEY_AUTHOR_NAME),
                        postObject.optString(KEY_AUTHOR_AVATAR),
                        postObject.optString(KEY_DATE),
                        postObject.optString(KEY_CONTENT),
                        // created_at được dùng để sort date chuẩn, không phụ thuộc vào chuỗi ngày.
                        createdAtMillis
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

    public List<PostItem> getVisiblePosts(String email) {
        List<PostItem> visiblePosts = new ArrayList<>();
        for (PostItem post : getPosts()) {
            if (!isPostHiddenForUser(email, post.getId())) {
                visiblePosts.add(post);
            }
        }
        return visiblePosts;
    }

    public List<PostItem> getHiddenPosts(String email) {
        List<PostItem> hiddenPosts = new ArrayList<>();
        for (PostItem post : getPosts()) {
            if (isPostHiddenForUser(email, post.getId())) {
                hiddenPosts.add(post);
            }
        }
        return hiddenPosts;
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

    public boolean deletePostById(String postId) {
        List<PostItem> posts = getPosts();
        for (int index = 0; index < posts.size(); index++) {
            if (posts.get(index).getId().equals(postId)) {
                posts.remove(index);
                savePosts(posts);
                return true;
            }
        }
        return false;
    }

    public boolean hidePostForUser(String email, String postId) {
        String hiddenPostsKey = createHiddenPostsKey(email);
        if (hiddenPostsKey.isEmpty() || postId == null || postId.isEmpty()) {
            return false;
        }

        List<String> hiddenPostIds = getHiddenPostIds(hiddenPostsKey);
        if (hiddenPostIds.contains(postId)) {
            return false;
        }

        hiddenPostIds.add(postId);
        saveHiddenPostIds(hiddenPostsKey, hiddenPostIds);
        return true;
    }

    public boolean unhidePostForUser(String email, String postId) {
        String hiddenPostsKey = createHiddenPostsKey(email);
        if (hiddenPostsKey.isEmpty() || postId == null || postId.isEmpty()) {
            return false;
        }

        List<String> hiddenPostIds = getHiddenPostIds(hiddenPostsKey);
        if (!hiddenPostIds.remove(postId)) {
            return false;
        }

        saveHiddenPostIds(hiddenPostsKey, hiddenPostIds);
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
        return new SimpleDateFormat(DATE_PATTERN, Locale.getDefault()).format(new Date());
    }

    private void savePosts(List<PostItem> posts) {
        JSONArray postArray = new JSONArray();
        for (PostItem post : posts) {
            JSONObject postObject = new JSONObject();
            try {
                postObject.put(KEY_ID, post.getId());
                postObject.put(KEY_AUTHOR_NAME, post.getAuthorName());
                postObject.put(KEY_AUTHOR_AVATAR, post.getAuthorAvatarPath());
                postObject.put(KEY_DATE, post.getDateLabel());
                postObject.put(KEY_CONTENT, post.getContent());
                postObject.put(KEY_CREATED_AT, post.getCreatedAtMillis());
                postArray.put(postObject);
            } catch (JSONException ignored) {
                // Bỏ qua item lỗi để không làm hỏng toàn bộ danh sách.
            }
        }

        preferences.edit().putString(KEY_POSTS, postArray.toString()).apply();
    }

    private boolean isPostHiddenForUser(String email, String postId) {
        String hiddenPostsKey = createHiddenPostsKey(email);
        if (hiddenPostsKey.isEmpty()) {
            return false;
        }
        return getHiddenPostIds(hiddenPostsKey).contains(postId);
    }

    private List<String> getHiddenPostIds(String hiddenPostsKey) {
        List<String> hiddenPostIds = new ArrayList<>();
        String rawHiddenPosts = preferences.getString(hiddenPostsKey, "[]");

        try {
            JSONArray hiddenPostsArray = new JSONArray(rawHiddenPosts);
            for (int index = 0; index < hiddenPostsArray.length(); index++) {
                String postId = hiddenPostsArray.optString(index, "");
                if (!postId.isEmpty()) {
                    hiddenPostIds.add(postId);
                }
            }
        } catch (JSONException ignored) {
            preferences.edit().remove(hiddenPostsKey).apply();
        }

        return hiddenPostIds;
    }

    private void saveHiddenPostIds(String hiddenPostsKey, List<String> hiddenPostIds) {
        JSONArray hiddenPostsArray = new JSONArray();
        for (String postId : hiddenPostIds) {
            hiddenPostsArray.put(postId);
        }
        preferences.edit().putString(hiddenPostsKey, hiddenPostsArray.toString()).apply();
    }

    private String createHiddenPostsKey(String email) {
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail.isEmpty()) {
            return "";
        }
        return KEY_HIDDEN_POSTS_PREFIX + normalizedEmail;
    }

    private long resolveCreatedAt(JSONObject postObject) {
        // Ưu tiên đọc timestamp cũ nếu đã có, còn dữ liệu legacy thì suy ra từ chuỗi ngày.
        long createdAt = postObject.optLong(KEY_CREATED_AT, -1L);
        if (createdAt >= 0L) {
            return createdAt;
        }

        String dateLabel = postObject.optString(KEY_DATE, "");
        if (dateLabel.isEmpty()) {
            return System.currentTimeMillis();
        }

        try {
            Date parsedDate = new SimpleDateFormat(DATE_PATTERN, Locale.getDefault()).parse(dateLabel);
            if (parsedDate != null) {
                return parsedDate.getTime();
            }
        } catch (ParseException ignored) {
            // Nếu dữ liệu cũ không parse được thì vẫn giữ app chạy bình thường.
        }

        return System.currentTimeMillis();
    }

    private String resolvePostId(JSONObject postObject, long createdAtMillis) {
        String existingId = postObject.optString(KEY_ID, "");
        if (!existingId.isEmpty()) {
            return existingId;
        }

        String authorName = postObject.optString(KEY_AUTHOR_NAME, "");
        String content = postObject.optString(KEY_CONTENT, "");
        return "legacy_" + createdAtMillis + "_" + Math.abs((authorName + "|" + content).hashCode());
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            return "";
        }
        return email.trim().toLowerCase(Locale.US);
    }
}
