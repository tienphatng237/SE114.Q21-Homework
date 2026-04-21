package com.example.homework;

import java.util.UUID;

public class PostItem {

    private final String id;
    private final String authorName;
    private final String authorAvatarPath;
    private final String dateLabel;
    private final String content;
    private final long createdAtMillis;

    public PostItem(String authorName, String authorAvatarPath, String dateLabel, String content) {
        this(UUID.randomUUID().toString(), authorName, authorAvatarPath, dateLabel, content, System.currentTimeMillis());
    }

    public PostItem(
            String authorName,
            String authorAvatarPath,
            String dateLabel,
            String content,
            long createdAtMillis
    ) {
        this(UUID.randomUUID().toString(), authorName, authorAvatarPath, dateLabel, content, createdAtMillis);
    }

    public PostItem(
            String id,
            String authorName,
            String authorAvatarPath,
            String dateLabel,
            String content,
            long createdAtMillis
    ) {
        this.id = id;
        this.authorName = authorName;
        this.authorAvatarPath = authorAvatarPath;
        this.dateLabel = dateLabel;
        this.content = content;
        this.createdAtMillis = createdAtMillis;
    }

    public String getId() {
        return id;
    }

    public String getAuthorName() {
        return authorName;
    }

    public String getAuthorAvatarPath() {
        return authorAvatarPath;
    }

    public String getDateLabel() {
        return dateLabel;
    }

    public String getContent() {
        return content;
    }

    public long getCreatedAtMillis() {
        return createdAtMillis;
    }
}
