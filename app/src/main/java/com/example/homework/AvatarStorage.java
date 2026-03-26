package com.example.homework;

import android.content.Context;
import android.net.Uri;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class AvatarStorage {

    private final Context context;

    public AvatarStorage(Context context) {
        this.context = context.getApplicationContext();
    }

    public String importAvatar(Uri sourceUri, String oldAvatarPath) {
        File avatarDirectory = new File(context.getFilesDir(), "avatars");
        if (!avatarDirectory.exists() && !avatarDirectory.mkdirs()) {
            return "";
        }

        File targetFile = new File(
                avatarDirectory,
                "profile_avatar_" + System.currentTimeMillis() + "." + resolveExtension(sourceUri)
        );

        try (InputStream inputStream = context.getContentResolver().openInputStream(sourceUri);
             FileOutputStream outputStream = new FileOutputStream(targetFile)) {

            if (inputStream == null) {
                return "";
            }

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            outputStream.flush();
            deleteManagedAvatar(oldAvatarPath);
            return targetFile.getAbsolutePath();
        } catch (IOException exception) {
            if (targetFile.exists()) {
                targetFile.delete();
            }
            return "";
        }
    }

    public String normalizeStoredPath(String storedValue) {
        File avatarFile = resolveExistingFile(storedValue);
        return avatarFile == null ? "" : avatarFile.getAbsolutePath();
    }

    public void loadInto(ImageView targetView, String avatarPath) {
        File avatarFile = resolveExistingFile(avatarPath);
        if (avatarFile == null) {
            showPlaceholder(targetView);
            return;
        }

        targetView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        Glide.with(targetView)
                .load(avatarFile)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_gallery)
                .centerCrop()
                .into(targetView);
    }

    public String getDisplayName(String avatarPath) {
        File avatarFile = resolveExistingFile(avatarPath);
        return avatarFile == null ? "" : avatarFile.getName();
    }

    private void showPlaceholder(ImageView targetView) {
        Glide.with(targetView).clear(targetView);
        targetView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        targetView.setImageResource(android.R.drawable.ic_menu_gallery);
    }

    private void deleteManagedAvatar(String avatarPath) {
        File avatarFile = resolveExistingFile(avatarPath);
        if (avatarFile == null) {
            return;
        }

        String internalRoot = context.getFilesDir().getAbsolutePath();
        if (avatarFile.getAbsolutePath().startsWith(internalRoot)) {
            avatarFile.delete();
        }
    }

    private File resolveExistingFile(String avatarPath) {
        if (avatarPath == null || avatarPath.isEmpty()) {
            return null;
        }

        File directFile = new File(avatarPath);
        if (directFile.exists()) {
            return directFile;
        }

        Uri fileUri = Uri.parse(avatarPath);
        if ("file".equalsIgnoreCase(fileUri.getScheme()) && fileUri.getPath() != null) {
            File uriFile = new File(fileUri.getPath());
            if (uriFile.exists()) {
                return uriFile;
            }
        }

        return null;
    }

    private String resolveExtension(Uri sourceUri) {
        String mimeType = context.getContentResolver().getType(sourceUri);
        String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
        return (extension == null || extension.isEmpty()) ? "jpg" : extension;
    }
}
