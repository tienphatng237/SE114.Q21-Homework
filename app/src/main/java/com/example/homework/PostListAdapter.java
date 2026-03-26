package com.example.homework;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class PostListAdapter extends BaseAdapter {

    public interface OnDeleteClickListener {
        void onDeleteClicked(int position);
    }

    private final LayoutInflater layoutInflater;
    private final AvatarStorage avatarStorage;
    private final OnDeleteClickListener onDeleteClickListener;
    private final List<PostItem> posts = new ArrayList<>();

    public PostListAdapter(
            LayoutInflater layoutInflater,
            AvatarStorage avatarStorage,
            OnDeleteClickListener onDeleteClickListener
    ) {
        this.layoutInflater = layoutInflater;
        this.avatarStorage = avatarStorage;
        this.onDeleteClickListener = onDeleteClickListener;
    }

    public void submitPosts(List<PostItem> newPosts) {
        posts.clear();
        posts.addAll(newPosts);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return posts.size();
    }

    @Override
    public PostItem getItem(int position) {
        return posts.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = layoutInflater.inflate(R.layout.item_post, parent, false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        PostItem post = getItem(position);
        avatarStorage.loadInto(
                holder.avatarImageView,
                avatarStorage.normalizeStoredPath(post.getAuthorAvatarPath())
        );
        holder.nameTextView.setText(post.getAuthorName());
        holder.dateTextView.setText(post.getDateLabel());
        holder.contentTextView.setText(post.getContent());
        holder.deleteButton.setOnClickListener(view -> onDeleteClickListener.onDeleteClicked(position));
        return convertView;
    }

    private static class ViewHolder {
        private final ImageView avatarImageView;
        private final TextView nameTextView;
        private final TextView dateTextView;
        private final TextView contentTextView;
        private final TextView deleteButton;

        ViewHolder(View itemView) {
            avatarImageView = itemView.findViewById(R.id.image_post_avatar);
            nameTextView = itemView.findViewById(R.id.text_post_author);
            dateTextView = itemView.findViewById(R.id.text_post_date);
            contentTextView = itemView.findViewById(R.id.text_post_content);
            deleteButton = itemView.findViewById(R.id.button_delete_post);
        }
    }
}
