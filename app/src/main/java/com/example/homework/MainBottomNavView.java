package com.example.homework;

import android.graphics.Typeface;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class MainBottomNavView {

    private final AppCompatActivity activity;
    private final LinearLayout homeItem;
    private final LinearLayout profileItem;
    private final ImageView homeIcon;
    private final ImageView profileIcon;
    private final TextView homeLabel;
    private final TextView profileLabel;

    public MainBottomNavView(AppCompatActivity activity) {
        this.activity = activity;
        homeItem = activity.findViewById(R.id.nav_home);
        profileItem = activity.findViewById(R.id.nav_profile);
        homeIcon = activity.findViewById(R.id.icon_home);
        profileIcon = activity.findViewById(R.id.icon_profile);
        homeLabel = activity.findViewById(R.id.label_home);
        profileLabel = activity.findViewById(R.id.label_profile);
    }

    public void bindHome(View.OnClickListener listener) {
        homeItem.setOnClickListener(listener);
    }

    public void bindProfile(View.OnClickListener listener) {
        profileItem.setOnClickListener(listener);
    }

    public void showHomeSelected() {
        updateItem(homeItem, homeIcon, homeLabel, true);
        updateItem(profileItem, profileIcon, profileLabel, false);
    }

    public void showProfileSelected() {
        updateItem(homeItem, homeIcon, homeLabel, false);
        updateItem(profileItem, profileIcon, profileLabel, true);
    }

    private void updateItem(LinearLayout item, ImageView icon, TextView label, boolean selected) {
        int color = ContextCompat.getColor(
                activity,
                selected ? R.color.text_primary : R.color.hint_text
        );
        float alpha = selected ? 1f : 0.68f;

        item.setEnabled(!selected);
        item.setAlpha(alpha);
        icon.setColorFilter(color);
        label.setTextColor(color);
        label.setTypeface(Typeface.MONOSPACE, selected ? Typeface.BOLD : Typeface.NORMAL);
    }
}
