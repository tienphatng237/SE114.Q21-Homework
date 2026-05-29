package com.example.homework;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class HomeFragment extends Fragment {

    private static final int MAX_FRIEND_SUGGESTIONS = 5;

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
    private LinearLayout friendSuggestionsContainer;
    private TextView friendSuggestionsEmptyText;
    private MaterialButton syncContactsButton;
    private ActivityResultLauncher<String> contactsPermissionLauncher;
    private final Set<String> dismissedSuggestionEmails = new HashSet<>();
    private PostSortMode sortMode = PostSortMode.DATE_DESC;
    private boolean showingHiddenPosts = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        contactsPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                this::handleContactsPermissionResult
        );
    }

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
        syncContactsButton = view.findViewById(R.id.button_sync_contacts);
        friendSuggestionsEmptyText = view.findViewById(R.id.text_friend_suggestions_empty);
        friendSuggestionsContainer = view.findViewById(R.id.list_friend_suggestions);
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
        syncContactsButton.setOnClickListener(view1 -> onSyncContactsClicked());

        updateTopBarSubtitle(topAppBar);
        updateHiddenButtonLabel(hiddenPostsButton);
        loadPosts();
        refreshFriendSuggestions();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getView() != null) {
            refreshFriendSuggestions();
        }
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

    private void onSyncContactsClicked() {
        if (hasContactsPermission()) {
            refreshFriendSuggestions();
            return;
        }

        contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS);
    }

    private void handleContactsPermissionResult(boolean granted) {
        if (!isAdded()) {
            return;
        }

        if (granted) {
            refreshFriendSuggestions();
            return;
        }

        renderPermissionRequiredState();
        toast(R.string.friend_suggestions_permission_denied);
    }

    private void refreshFriendSuggestions() {
        if (!hasContactsPermission()) {
            renderPermissionRequiredState();
            return;
        }

        try {
            List<UserProfile> suggestions = findContactBasedSuggestions();
            renderFriendSuggestions(suggestions);
        } catch (SecurityException ignored) {
            renderPermissionRequiredState();
            toast(R.string.friend_suggestions_load_failed);
        }
    }

    private List<UserProfile> findContactBasedSuggestions() {
        Set<String> contactEmails = loadContactEmails();
        List<UserProfile> suggestions = new ArrayList<>();
        if (contactEmails.isEmpty()) {
            return suggestions;
        }

        String currentEmail = normalizeEmail(userPreferences.getCurrentEmail());
        for (UserProfile user : userPreferences.getRegisteredUsers()) {
            String candidateEmail = normalizeEmail(user.getEmail());
            if (candidateEmail.isEmpty() || candidateEmail.equals(currentEmail)) {
                continue;
            }
            if (dismissedSuggestionEmails.contains(candidateEmail)) {
                continue;
            }
            if (contactEmails.contains(candidateEmail)) {
                suggestions.add(user);
            }
        }

        suggestions.sort(
                Comparator.comparing(UserProfile::getName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(UserProfile::getEmail, String.CASE_INSENSITIVE_ORDER)
        );

        if (suggestions.size() <= MAX_FRIEND_SUGGESTIONS) {
            return suggestions;
        }
        return new ArrayList<>(suggestions.subList(0, MAX_FRIEND_SUGGESTIONS));
    }

    private Set<String> loadContactEmails() {
        Set<String> emails = new HashSet<>();
        String[] projection = new String[]{ContactsContract.CommonDataKinds.Email.ADDRESS};

        try (Cursor cursor = requireContext().getContentResolver().query(
                ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                projection,
                null,
                null,
                null
        )) {
            if (cursor == null) {
                return emails;
            }

            int emailColumnIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS);
            while (cursor.moveToNext()) {
                String rawEmail = emailColumnIndex >= 0 ? cursor.getString(emailColumnIndex) : null;
                String normalizedEmail = normalizeEmail(rawEmail);
                if (!normalizedEmail.isEmpty()) {
                    emails.add(normalizedEmail);
                }
            }
        }

        return emails;
    }

    private void renderPermissionRequiredState() {
        friendSuggestionsContainer.removeAllViews();
        friendSuggestionsEmptyText.setVisibility(View.VISIBLE);
        friendSuggestionsEmptyText.setText(R.string.friend_suggestions_permission_required);
        syncContactsButton.setText(R.string.friend_suggestions_allow);
    }

    private void renderFriendSuggestions(List<UserProfile> suggestions) {
        friendSuggestionsContainer.removeAllViews();
        syncContactsButton.setText(R.string.friend_suggestions_sync);

        if (suggestions.isEmpty()) {
            friendSuggestionsEmptyText.setVisibility(View.VISIBLE);
            friendSuggestionsEmptyText.setText(R.string.friend_suggestions_empty);
            return;
        }

        friendSuggestionsEmptyText.setVisibility(View.GONE);
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (UserProfile suggestion : suggestions) {
            View row = inflater.inflate(R.layout.item_friend_suggestion, friendSuggestionsContainer, false);
            TextView nameTextView = row.findViewById(R.id.text_suggestion_name);
            TextView emailTextView = row.findViewById(R.id.text_suggestion_email);
            MaterialButton addFriendButton = row.findViewById(R.id.button_add_friend);

            String displayName = suggestion.getName().trim().isEmpty()
                    ? getString(R.string.default_profile_name)
                    : suggestion.getName().trim();

            nameTextView.setText(displayName);
            emailTextView.setText(suggestion.getEmail());
            addFriendButton.setOnClickListener(view -> {
                dismissedSuggestionEmails.add(normalizeEmail(suggestion.getEmail()));
                pulseView(view);
                toast(getString(R.string.friend_suggestions_request_sent, displayName));
                refreshFriendSuggestions();
            });

            friendSuggestionsContainer.addView(row);
        }
    }

    private boolean hasContactsPermission() {
        return ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED;
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            return "";
        }
        return email.trim().toLowerCase(Locale.US);
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

    private void toast(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
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
