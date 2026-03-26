package com.example.homework;

public class PostItem {

    private final String authorName;
    private final String authorAvatarPath;
    private final String dateLabel;
    private final String content;

    public PostItem(String authorName, String authorAvatarPath, String dateLabel, String content) {
        this.authorName = authorName;
        this.authorAvatarPath = authorAvatarPath;
        this.dateLabel = dateLabel;
        this.content = content;
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
}
