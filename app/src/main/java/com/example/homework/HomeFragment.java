package com.example.homework;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class HomeFragment extends Fragment {

    private enum PostSortMode {
        DATE_ASC,
        DATE_DESC,
        AUTHOR
    }

    private EditText postInputEditText;
    private ListView postsListView;
    private MaterialButton postButton;
    private UserPreferences userPreferences;
    private PostStorage postStorage;
    private PostListAdapter postListAdapter;
    private PostSortMode sortMode = PostSortMode.DATE_DESC;
    private boolean showingHiddenPosts = false;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        userPreferences = new UserPreferences(requireContext());
        if (!userPreferences.isLoggedIn() || !userPreferences.hasRegisteredUser()) {
            toast(R.string.message_profile_required);
            openLogin();
            return;
        }

        MaterialToolbar topAppBar = view.findViewById(R.id.top_app_bar);
        ((AppCompatActivity) requireActivity()).setSupportActionBar(topAppBar);
        requireActivity().setTitle(R.string.home);

        Animation toolbarEnter = AnimationUtils.loadAnimation(requireContext(), R.anim.toolbar_enter);
        Animation optionsBarEnter = AnimationUtils.loadAnimation(requireContext(), R.anim.toolbar_enter);
        Animation panelEnter = AnimationUtils.loadAnimation(requireContext(), R.anim.panel_enter);
        topAppBar.startAnimation(toolbarEnter);
        view.findViewById(R.id.home_options_bar).startAnimation(optionsBarEnter);
        view.findViewById(R.id.panel_main).startAnimation(panelEnter);

        postStorage = new PostStorage(requireContext());
        postStorage.removeLegacySamplePosts();

        postInputEditText = view.findViewById(R.id.edit_post_input);
        postButton = view.findViewById(R.id.button_post);
        MaterialButton sortOldestButton = view.findViewById(R.id.button_option_sort_oldest);
        MaterialButton sortNewestButton = view.findViewById(R.id.button_option_sort_newest);
        MaterialButton sortAuthorButton = view.findViewById(R.id.button_option_sort_author);
        MaterialButton hiddenPostsButton = view.findViewById(R.id.button_option_hidden);
        postsListView = view.findViewById(R.id.list_posts);
        postsListView.setLongClickable(true);

        postListAdapter = new PostListAdapter(
                LayoutInflater.from(requireContext()),
                new AvatarStorage(requireContext()),
                this::confirmDeletePost
        );
        postsListView.setAdapter(postListAdapter);
        registerForContextMenu(postsListView);

        postButton.setOnClickListener(view1 -> createPost());
        sortOldestButton.setOnClickListener(view1 -> setSortMode(PostSortMode.DATE_ASC));
        sortNewestButton.setOnClickListener(view1 -> setSortMode(PostSortMode.DATE_DESC));
        sortAuthorButton.setOnClickListener(view1 -> setSortMode(PostSortMode.AUTHOR));
        hiddenPostsButton.setOnClickListener(view1 -> toggleHiddenPosts());

        updateTopBarSubtitle(topAppBar);
        updateHiddenButtonLabel(hiddenPostsButton);
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
        pulseView(postButton);
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

        requireActivity().getMenuInflater().inflate(R.menu.menu_post_context, menu);
        MenuItem hideMenuItem = menu.findItem(R.id.action_hide_post);
        if (hideMenuItem != null) {
            hideMenuItem.setTitle(showingHiddenPosts
                    ? R.string.context_unhide_post
                    : R.string.context_hide_post);
        }
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
        if (itemId == R.id.action_hide_post) {
            if (showingHiddenPosts) {
                unhidePost(position);
            } else {
                hidePost(position);
            }
            return true;
        }
        if (itemId == R.id.action_delete_post) {
            confirmDeletePost(position);
            return true;
        }
        return super.onContextItemSelected(item);
    }

    private void confirmDeletePost(int position) {
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.delete_post_title)
                .setMessage(R.string.delete_post_message)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.delete_post_action, (dialogInterface, which) -> deletePost(position))
                .create();

        dialog.setOnShowListener(currentDialog -> {
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                    .setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
        });
        dialog.show();
    }

    private void deletePost(int position) {
        PostItem post = postListAdapter.getItem(position);
        if (!postStorage.deletePostById(post.getId())) {
            return;
        }

        loadPosts();
        toast(R.string.message_post_deleted);
    }

    private void hidePost(int position) {
        PostItem post = postListAdapter.getItem(position);
        String currentEmail = userPreferences.getCurrentEmail();
        if (!postStorage.hidePostForUser(currentEmail, post.getId())) {
            return;
        }

        loadPosts();
        toast(R.string.message_post_hidden);
    }

    private void unhidePost(int position) {
        PostItem post = postListAdapter.getItem(position);
        String currentEmail = userPreferences.getCurrentEmail();
        if (!postStorage.unhidePostForUser(currentEmail, post.getId())) {
            return;
        }

        loadPosts();
        toast(R.string.message_post_unhidden);
    }

    private void copyPostContent(int position) {
        PostItem post = postListAdapter.getItem(position);
        ClipboardManager clipboardManager =
                (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboardManager == null) {
            toast(R.string.message_copy_failed);
            return;
        }

        clipboardManager.setPrimaryClip(
                ClipData.newPlainText(
                        getString(R.string.post_content_clip_label),
                        post.getContent()
                )
        );
        toast(R.string.message_post_copied);
    }

    private void loadPosts() {
        String currentEmail = userPreferences.getCurrentEmail();
        List<PostItem> posts = new ArrayList<>(showingHiddenPosts
                ? postStorage.getHiddenPosts(currentEmail)
                : postStorage.getVisiblePosts(currentEmail));
        sortPosts(posts);
        postListAdapter.submitPosts(posts);
        postsListView.scheduleLayoutAnimation();
    }

    private void toggleHiddenPosts() {
        showingHiddenPosts = !showingHiddenPosts;
        MaterialToolbar topAppBar = requireView().findViewById(R.id.top_app_bar);
        MaterialButton hiddenPostsButton = requireView().findViewById(R.id.button_option_hidden);
        updateTopBarSubtitle(topAppBar);
        updateHiddenButtonLabel(hiddenPostsButton);
        loadPosts();
        toast(showingHiddenPosts ? R.string.message_showing_hidden_posts : R.string.message_showing_home_feed);
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

    private void sortPosts(List<PostItem> posts) {
        if (sortMode == PostSortMode.AUTHOR) {
            posts.sort(
                    Comparator.comparing(
                                    PostItem::getAuthorName,
                                    String.CASE_INSENSITIVE_ORDER
                            )
                            .thenComparing(Comparator.comparingLong(PostItem::getCreatedAtMillis).reversed())
            );
            return;
        }

        Comparator<PostItem> comparator = Comparator.comparingLong(PostItem::getCreatedAtMillis);
        if (sortMode == PostSortMode.DATE_DESC) {
            comparator = comparator.reversed();
        }
        posts.sort(comparator);
    }

    private void updateTopBarSubtitle(MaterialToolbar topAppBar) {
        topAppBar.setSubtitle(showingHiddenPosts
                ? getString(R.string.home_hidden_subtitle)
                : getString(R.string.home_subtitle));
    }

    private void updateHiddenButtonLabel(MaterialButton hiddenPostsButton) {
        hiddenPostsButton.setText(showingHiddenPosts
                ? R.string.menu_feed
                : R.string.menu_hidden);
    }

    private void openLogin() {
        startActivity(new Intent(requireContext(), LoginActivity.class));
        requireActivity().finish();
    }

    private void toast(int messageId) {
        Toast.makeText(requireContext(), messageId, Toast.LENGTH_SHORT).show();
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
