package com.example.homework;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

public class MainShellActivity extends AppCompatActivity {

    public static final String EXTRA_START_TAB = "extra_start_tab";
    public static final int TAB_HOME = 0;
    public static final int TAB_PROFILE = 1;

    private static final String TAG_HOME = "home_fragment";
    private static final String TAG_PROFILE = "profile_fragment";

    private MainBottomNavView bottomNavView;
    private int currentTab = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_shell);

        bottomNavView = new MainBottomNavView(this);
        bottomNavView.bindTab(TAB_HOME, view -> showTab(TAB_HOME));
        bottomNavView.bindTab(TAB_PROFILE, view -> showTab(TAB_PROFILE));

        int initialTab = getIntent().getIntExtra(EXTRA_START_TAB, TAB_HOME);
        showTab(initialTab, false);
    }

    public void showHomeTab() {
        showTab(TAB_HOME);
    }

    public void showProfileTab() {
        showTab(TAB_PROFILE);
    }

    private void showTab(int tabIndex) {
        showTab(tabIndex, true);
    }

    private void showTab(int tabIndex, boolean animateIndicator) {
        if (tabIndex != TAB_HOME && tabIndex != TAB_PROFILE) {
            tabIndex = TAB_HOME;
        }

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();

        Fragment targetFragment = getOrCreateFragment(tabIndex);
        Fragment currentFragment = fragmentManager.findFragmentByTag(getFragmentTag(currentTab));

        if (currentTab == tabIndex && currentFragment != null) {
            if (animateIndicator) {
                bottomNavView.selectTab(tabIndex);
            } else {
                bottomNavView.selectTab(tabIndex, false);
            }
            return;
        }

        if (currentFragment != null) {
            applyContentTransition(transaction, tabIndex);
        }

        if (currentFragment == null) {
            transaction.add(R.id.content_container, targetFragment, getFragmentTag(tabIndex));
        } else if (currentFragment != targetFragment) {
            transaction.hide(currentFragment);
            if (targetFragment.isAdded()) {
                transaction.show(targetFragment);
            } else {
                transaction.add(R.id.content_container, targetFragment, getFragmentTag(tabIndex));
            }
        }

        transaction.setReorderingAllowed(true);
        transaction.commit();

        currentTab = tabIndex;
        if (animateIndicator) {
            bottomNavView.selectTab(tabIndex);
        } else {
            bottomNavView.selectTab(tabIndex, false);
        }
    }

    private void applyContentTransition(FragmentTransaction transaction, int targetTab) {
        boolean movingForward = targetTab > currentTab;
        if (movingForward) {
            transaction.setCustomAnimations(
                    R.anim.fragment_slide_in_right,
                    R.anim.fragment_slide_out_left,
                    R.anim.fragment_slide_in_left,
                    R.anim.fragment_slide_out_right
            );
            return;
        }

        transaction.setCustomAnimations(
                R.anim.fragment_slide_in_left,
                R.anim.fragment_slide_out_right,
                R.anim.fragment_slide_in_right,
                R.anim.fragment_slide_out_left
        );
    }

    @NonNull
    private Fragment getOrCreateFragment(int tabIndex) {
        String tag = getFragmentTag(tabIndex);
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(tag);
        if (fragment != null) {
            return fragment;
        }

        if (tabIndex == TAB_PROFILE) {
            return new ProfileFragment();
        }
        return new HomeFragment();
    }

    @NonNull
    private String getFragmentTag(int tabIndex) {
        return tabIndex == TAB_PROFILE ? TAG_PROFILE : TAG_HOME;
    }

    public static Intent createIntent(android.content.Context context, int startTab) {
        Intent intent = new Intent(context, MainShellActivity.class);
        intent.putExtra(EXTRA_START_TAB, startTab);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }
}
