package com.example.homework;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainBottomNavView {

    public static final class TabSpec {
        private final int iconResId;
        private final int labelResId;
        private final int contentDescriptionResId;

        public TabSpec(@DrawableRes int iconResId, @StringRes int labelResId) {
            this(iconResId, labelResId, labelResId);
        }

        public TabSpec(
                @DrawableRes int iconResId,
                @StringRes int labelResId,
                @StringRes int contentDescriptionResId
        ) {
            this.iconResId = iconResId;
            this.labelResId = labelResId;
            this.contentDescriptionResId = contentDescriptionResId;
        }
    }

    private static final long INDICATOR_DURATION_MS = 280L;
    private static final long ITEM_FEEDBACK_DURATION_MS = 220L;
    private static final TimeInterpolator INDICATOR_INTERPOLATOR = new FastOutSlowInInterpolator();

    private final AppCompatActivity activity;
    private final FrameLayout navHost;
    private final View navIndicator;
    private final LinearLayout navContainer;
    private final LayoutInflater layoutInflater;
    private final List<TabSpec> tabSpecs = new ArrayList<>();
    private final List<TabHolder> tabHolders = new ArrayList<>();
    private final List<View.OnClickListener> tabClickListeners = new ArrayList<>();
    private int selectedIndex = 0;

    public MainBottomNavView(AppCompatActivity activity) {
        this(activity, defaultTabs());
    }

    public MainBottomNavView(AppCompatActivity activity, List<TabSpec> tabs) {
        this.activity = activity;
        this.layoutInflater = LayoutInflater.from(activity);
        navHost = activity.findViewById(R.id.nav_host);
        navIndicator = activity.findViewById(R.id.nav_indicator);
        navContainer = activity.findViewById(R.id.nav_container);
        setTabs(tabs);
    }

    public void setTabs(List<TabSpec> tabs) {
        tabSpecs.clear();
        tabSpecs.addAll(tabs);
        tabHolders.clear();
        tabClickListeners.clear();
        navContainer.removeAllViews();

        for (int index = 0; index < tabSpecs.size(); index++) {
            TabSpec spec = tabSpecs.get(index);
            View tabView = layoutInflater.inflate(R.layout.item_bottom_nav_tab, navContainer, false);
            LinearLayout tabRoot = tabView.findViewById(R.id.tab_root);
            ImageView iconView = tabView.findViewById(R.id.icon_tab);
            TextView labelView = tabView.findViewById(R.id.label_tab);

            iconView.setImageResource(spec.iconResId);
            labelView.setText(spec.labelResId);
            tabView.setContentDescription(activity.getString(spec.contentDescriptionResId));
            tabRoot.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
            navContainer.addView(tabRoot);
            tabHolders.add(new TabHolder(tabRoot, iconView, labelView));
            tabClickListeners.add(null);
        }

        selectedIndex = clampSelectedIndex(selectedIndex);
        applySelection(false);
    }

    public void bindTab(int index, View.OnClickListener listener) {
        if (!isValidIndex(index)) {
            return;
        }

        tabClickListeners.set(index, listener);
        tabHolders.get(index).root.setOnClickListener(view -> {
            selectTab(index, true);
            View.OnClickListener boundListener = tabClickListeners.get(index);
            if (boundListener != null) {
                boundListener.onClick(view);
            }
        });
    }

    public void bindHome(View.OnClickListener listener) {
        bindTab(0, listener);
    }

    public void bindProfile(View.OnClickListener listener) {
        bindTab(1, listener);
    }

    public void showHomeSelected() {
        selectTab(0, false);
    }

    public void showProfileSelected() {
        selectTab(1, false);
    }

    public void selectTab(int index) {
        selectTab(index, true);
    }

    public void selectTab(int index, boolean animateIndicator) {
        if (!isValidIndex(index)) {
            return;
        }

        selectedIndex = index;
        applySelection(animateIndicator);
    }

    private void applySelection(boolean animateIndicator) {
        if (tabHolders.isEmpty()) {
            return;
        }

        for (int index = 0; index < tabHolders.size(); index++) {
            boolean selected = index == selectedIndex;
            updateItem(tabHolders.get(index), selected);
        }
        updateIndicator(animateIndicator);
    }

    private void updateItem(TabHolder holder, boolean selected) {
        int color = ContextCompat.getColor(
                activity,
                selected ? R.color.text_primary : R.color.hint_text
        );
        float alpha = selected ? 1f : 0.72f;
        float iconScale = selected ? 1.08f : 0.94f;
        float labelScale = selected ? 1.02f : 0.96f;

        holder.root.setEnabled(!selected);
        holder.icon.setColorFilter(color);
        holder.label.setTextColor(color);
        holder.label.setTypeface(Typeface.SANS_SERIF, selected ? Typeface.BOLD : Typeface.NORMAL);
        holder.root.animate()
                .alpha(alpha)
                .setDuration(ITEM_FEEDBACK_DURATION_MS)
                .setInterpolator(INDICATOR_INTERPOLATOR)
                .start();
        holder.icon.animate()
                .scaleX(iconScale)
                .scaleY(iconScale)
                .setDuration(ITEM_FEEDBACK_DURATION_MS)
                .setInterpolator(INDICATOR_INTERPOLATOR)
                .start();
        holder.label.animate()
                .scaleX(labelScale)
                .scaleY(labelScale)
                .setDuration(ITEM_FEEDBACK_DURATION_MS)
                .setInterpolator(INDICATOR_INTERPOLATOR)
                .start();
    }

    private void updateIndicator(boolean animateIndicator) {
        navHost.post(() -> {
            if (tabHolders.isEmpty()) {
                return;
            }

            int availableWidth = navContainer.getWidth();
            if (availableWidth <= 0) {
                return;
            }

            int segmentWidth = availableWidth / tabHolders.size();
            int inset = dpToPx(12);
            int indicatorWidth = Math.max(0, segmentWidth - inset * 2);

            ViewGroup.LayoutParams params = navIndicator.getLayoutParams();
            params.width = indicatorWidth;
            navIndicator.setLayoutParams(params);

            float targetX = (segmentWidth * selectedIndex) + inset;
            if (!animateIndicator) {
                navIndicator.animate().cancel();
                navIndicator.clearAnimation();
                navIndicator.setTranslationX(targetX);
                navIndicator.setScaleX(1f);
                navIndicator.setScaleY(1f);
                navIndicator.setAlpha(1f);
                return;
            }

            float currentX = navIndicator.getTranslationX();
            float distance = Math.abs(targetX - currentX);
            float stretchScale = 1f + Math.min(0.22f, distance / Math.max(1f, segmentWidth * 2f));

            AnimatorSet indicatorSet = new AnimatorSet();
            ObjectAnimator moveAnimator = ObjectAnimator.ofFloat(navIndicator, View.TRANSLATION_X, currentX, targetX);
            moveAnimator.setDuration(INDICATOR_DURATION_MS);
            moveAnimator.setInterpolator(INDICATOR_INTERPOLATOR);

            ObjectAnimator stretchXAnimator = ObjectAnimator.ofFloat(
                    navIndicator,
                    View.SCALE_X,
                    1f,
                    stretchScale,
                    1f
            );
            stretchXAnimator.setDuration(INDICATOR_DURATION_MS);
            stretchXAnimator.setInterpolator(INDICATOR_INTERPOLATOR);

            ObjectAnimator squashYAnimator = ObjectAnimator.ofFloat(
                    navIndicator,
                    View.SCALE_Y,
                    1f,
                    0.94f,
                    1f
            );
            squashYAnimator.setDuration(INDICATOR_DURATION_MS);
            squashYAnimator.setInterpolator(INDICATOR_INTERPOLATOR);

            ObjectAnimator alphaAnimator = ObjectAnimator.ofFloat(
                    navIndicator,
                    View.ALPHA,
                    0.9f,
                    1f
            );
            alphaAnimator.setDuration(INDICATOR_DURATION_MS);
            alphaAnimator.setInterpolator(INDICATOR_INTERPOLATOR);

            indicatorSet.playTogether(moveAnimator, stretchXAnimator, squashYAnimator, alphaAnimator);
            indicatorSet.start();
        });
    }

    private boolean isValidIndex(int index) {
        return index >= 0 && index < tabHolders.size();
    }

    private int clampSelectedIndex(int index) {
        if (tabHolders.isEmpty()) {
            return 0;
        }
        return Math.max(0, Math.min(index, tabHolders.size() - 1));
    }

    private int dpToPx(int dp) {
        float density = activity.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private static List<TabSpec> defaultTabs() {
        return Arrays.asList(
                new TabSpec(R.drawable.ic_nav_home, R.string.home, R.string.home_nav_description),
                new TabSpec(R.drawable.ic_nav_profile, R.string.profile, R.string.profile_nav_description)
        );
    }

    private static final class TabHolder {
        private final LinearLayout root;
        private final ImageView icon;
        private final TextView label;

        private TabHolder(LinearLayout root, ImageView icon, TextView label) {
            this.root = root;
            this.icon = icon;
            this.label = label;
        }
    }
}
