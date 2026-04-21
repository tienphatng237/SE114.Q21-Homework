package com.example.homework;

import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.MotionEvent;
import android.widget.EditText;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import android.graphics.drawable.Drawable;

public final class PasswordVisibilityHelper {

    private PasswordVisibilityHelper() {
    }

    public static void attach(EditText editText) {
        updateToggleIcon(editText, false);
        editText.setOnTouchListener((view, event) -> {
            if (event.getAction() != MotionEvent.ACTION_UP) {
                return false;
            }

            Drawable toggleDrawable = editText.getCompoundDrawablesRelative()[2];
            if (toggleDrawable == null) {
                return false;
            }

            int touchBoundary = editText.getWidth() - editText.getPaddingEnd() - toggleDrawable.getBounds().width();
            if (event.getX() < touchBoundary) {
                return false;
            }

            boolean shouldShowPassword = editText.getTransformationMethod() instanceof PasswordTransformationMethod;
            editText.setTransformationMethod(
                    shouldShowPassword
                            ? HideReturnsTransformationMethod.getInstance()
                            : PasswordTransformationMethod.getInstance()
            );
            updateToggleIcon(editText, shouldShowPassword);
            editText.setSelection(editText.getText().length());
            view.performClick();
            return true;
        });
    }

    private static void updateToggleIcon(EditText editText, boolean passwordVisible) {
        int iconResId = passwordVisible ? R.drawable.ic_visibility_off : R.drawable.ic_visibility;
        int tintColor = ContextCompat.getColor(
                editText.getContext(),
                passwordVisible ? R.color.primary_blue_dark : R.color.hint_text
        );

        Drawable drawable = AppCompatResources.getDrawable(editText.getContext(), iconResId);
        if (drawable != null) {
            Drawable wrappedDrawable = DrawableCompat.wrap(drawable.mutate());
            DrawableCompat.setTint(wrappedDrawable, tintColor);
            wrappedDrawable.setBounds(0, 0, wrappedDrawable.getIntrinsicWidth(), wrappedDrawable.getIntrinsicHeight());
            editText.setCompoundDrawablesRelative(null, null, wrappedDrawable, null);
            editText.setCompoundDrawablePadding(dpToPx(editText, 10));
        }
    }

    private static int dpToPx(EditText editText, int dp) {
        float density = editText.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
