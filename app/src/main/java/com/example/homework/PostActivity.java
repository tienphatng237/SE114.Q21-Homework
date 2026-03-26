package com.example.homework;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;

public class PostActivity extends AppCompatActivity {

    private EditText postInputEditText;
    private UserPreferences userPreferences;
    private PostStorage postStorage;
    private PostListAdapter postListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post);

        userPreferences = new UserPreferences(this);
        if (!userPreferences.isLoggedIn() || !userPreferences.hasRegisteredUser()) {
            toast(R.string.message_profile_required);
            openLogin();
            return;
        }

        postStorage = new PostStorage(this);
        postStorage.removeLegacySamplePosts();

        postInputEditText = findViewById(R.id.edit_post_input);
        MaterialButton postButton = findViewById(R.id.button_post);
        ListView postsListView = findViewById(R.id.list_posts);
        MainBottomNavView bottomNavView = new MainBottomNavView(this);

        postListAdapter = new PostListAdapter(
                getLayoutInflater(),
                new AvatarStorage(this),
                this::confirmDeletePost
        );
        postsListView.setAdapter(postListAdapter);

        postButton.setOnClickListener(view -> createPost());
        bottomNavView.bindHome(view -> { });
        bottomNavView.bindProfile(view -> openProfile());
        bottomNavView.showHomeSelected();

        loadPosts();
    }

    private void createPost() {
        String content = postInputEditText.getText().toString().trim();
        if (content.isEmpty()) {
            toast(R.string.message_post_empty);
            return;
        }

        UserProfile profile = userPreferences.getProfile();
        String authorName = profile.getName().isEmpty()
                ? getString(R.string.default_post_author)
                : profile.getName();

        PostItem post = new PostItem(
                authorName,
                profile.getAvatarUrl(),
                postStorage.createCurrentDateLabel(),
                content
        );

        postStorage.addPost(post);
        postInputEditText.setText("");
        loadPosts();
        toast(R.string.message_post_created);
    }

    private void loadPosts() {
        postListAdapter.submitPosts(postStorage.getPosts());
    }

    private void confirmDeletePost(int position) {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.delete_post_title)
                .setMessage(R.string.delete_post_message)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.delete_post_action, (dialogInterface, which) -> deletePost(position))
                .create();

        dialog.setOnShowListener(currentDialog -> {
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                    .setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        });
        dialog.show();
    }

    private void deletePost(int position) {
        if (!postStorage.deletePostAt(position)) {
            return;
        }

        loadPosts();
        toast(R.string.message_post_deleted);
    }

    private void openProfile() {
        startActivity(new Intent(this, ProfileActivity.class));
        finish();
    }

    private void openLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void toast(int messageId) {
        Toast.makeText(this, messageId, Toast.LENGTH_SHORT).show();
    }
}
