package com.example.homework;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.View;
import android.view.MenuItem;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

public class PostActivity extends AppCompatActivity {

    private enum PostSortMode {
        // Sắp xếp theo ngày từ cũ đến mới.
        DATE_ASC,
        // Sắp xếp theo ngày từ mới đến cũ.
        DATE_DESC,
        // Sắp xếp theo tên tác giả.
        AUTHOR
    }

    private EditText postInputEditText;
    private ListView postsListView;
    private UserPreferences userPreferences;
    private PostStorage postStorage;
    private PostListAdapter postListAdapter;
    private PostSortMode sortMode = PostSortMode.DATE_DESC;

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

        MaterialToolbar topAppBar = findViewById(R.id.top_app_bar);
        setSupportActionBar(topAppBar);
        setTitle(R.string.home);

        Animation toolbarEnter = AnimationUtils.loadAnimation(this, R.anim.toolbar_enter);
        Animation optionsBarEnter = AnimationUtils.loadAnimation(this, R.anim.toolbar_enter);
        Animation panelEnter = AnimationUtils.loadAnimation(this, R.anim.panel_enter);
        topAppBar.startAnimation(toolbarEnter);
        findViewById(R.id.home_options_bar).startAnimation(optionsBarEnter);
        findViewById(R.id.panel_main).startAnimation(panelEnter);

        postStorage = new PostStorage(this);
        postStorage.removeLegacySamplePosts();

        postInputEditText = findViewById(R.id.edit_post_input);
        MaterialButton postButton = findViewById(R.id.button_post);
        postsListView = findViewById(R.id.list_posts);
        postsListView.setLongClickable(true);
        MainBottomNavView bottomNavView = new MainBottomNavView(this);

        postListAdapter = new PostListAdapter(
                getLayoutInflater(),
                new AvatarStorage(this),
                this::confirmDeletePost
        );
        postsListView.setAdapter(postListAdapter);
        registerForContextMenu(postsListView);

        MaterialButton profileButton = findViewById(R.id.button_option_profile);
        MaterialButton sortOldestButton = findViewById(R.id.button_option_sort_oldest);
        MaterialButton sortNewestButton = findViewById(R.id.button_option_sort_newest);
        MaterialButton sortAuthorButton = findViewById(R.id.button_option_sort_author);

        postButton.setOnClickListener(view -> createPost());
        profileButton.setOnClickListener(view -> openProfile());
        sortOldestButton.setOnClickListener(view -> setSortMode(PostSortMode.DATE_ASC));
        sortNewestButton.setOnClickListener(view -> setSortMode(PostSortMode.DATE_DESC));
        sortAuthorButton.setOnClickListener(view -> setSortMode(PostSortMode.AUTHOR));

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
        pulseView(findViewById(R.id.button_post));
        toast(R.string.message_post_created);
    }

    @Override
    public void onCreateContextMenu(
            ContextMenu menu,
            View v,
            ContextMenu.ContextMenuInfo menuInfo
    ) {
        super.onCreateContextMenu(menu, v, menuInfo);
        if (v.getId() != R.id.list_posts) {
            return;
        }

        // Long-press vào một post sẽ mở menu riêng cho item đó.
        getMenuInflater().inflate(R.menu.menu_post_context, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info =
                (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        if (info == null) {
            return super.onContextItemSelected(item);
        }

        int position = info.position;
        int itemId = item.getItemId();
        if (itemId == R.id.action_copy_post) {
            copyPostContent(position);
            return true;
        }
        if (itemId == R.id.action_delete_post) {
            confirmDeletePost(position);
            return true;
        }
        return super.onContextItemSelected(item);
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
        startActivity(MainShellActivity.createIntent(this, MainShellActivity.TAB_PROFILE));
        finish();
    }

    private void copyPostContent(int position) {
        PostItem post = postListAdapter.getItem(position);
        ClipboardManager clipboardManager =
                (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboardManager == null) {
            toast(R.string.message_copy_failed);
            return;
        }

        // Copy nội dung bài viết vào clipboard để người dùng dán chỗ khác.
        clipboardManager.setPrimaryClip(
                ClipData.newPlainText(
                        getString(R.string.post_content_clip_label),
                        post.getContent()
                )
        );
        toast(R.string.message_post_copied);
    }

    private void loadPosts() {
        // Copy ra list mới để sort mà không làm thay đổi data gốc trong storage.
        java.util.List<PostItem> posts = new java.util.ArrayList<>(postStorage.getPosts());
        sortPosts(posts);
        postListAdapter.submitPosts(posts);
        postsListView.scheduleLayoutAnimation();
    }

    private void setSortMode(PostSortMode newSortMode) {
        sortMode = newSortMode;
        loadPosts();

        if (newSortMode == PostSortMode.DATE_ASC) {
            toast(R.string.message_sorted_by_date_oldest);
            return;
        }

        if (newSortMode == PostSortMode.DATE_DESC) {
            toast(R.string.message_sorted_by_date_newest);
            return;
        }

        toast(R.string.message_sorted_by_author);
    }

    private void sortPosts(java.util.List<PostItem> posts) {
        // Chuyển mode sort từ menu thành comparator cụ thể.
        if (sortMode == PostSortMode.AUTHOR) {
            posts.sort(
                    java.util.Comparator.comparing(
                                    PostItem::getAuthorName,
                                    String.CASE_INSENSITIVE_ORDER
                            )
                            .thenComparing(
                                    java.util.Comparator.comparingLong(PostItem::getCreatedAtMillis).reversed()
                            )
            );
            return;
        }

        java.util.Comparator<PostItem> comparator =
                java.util.Comparator.comparingLong(PostItem::getCreatedAtMillis);
        if (sortMode == PostSortMode.DATE_DESC) {
            comparator = comparator.reversed();
        }
        posts.sort(comparator);
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

    private void pulseView(View view) {
        view.animate()
                .scaleX(1.06f)
                .scaleY(1.06f)
                .setDuration(120)
                .withEndAction(() -> view.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(120)
                        .start())
                .start();
    }
}
