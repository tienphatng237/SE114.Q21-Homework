package com.example.homework;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private EditText emailEditText;
    private EditText passwordEditText;
    private UserPreferences userPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        userPreferences = new UserPreferences(this);
        if (userPreferences.isLoggedIn() && userPreferences.hasRegisteredUser()) {
            openHome();
            return;
        }

        emailEditText = findViewById(R.id.edit_email);
        passwordEditText = findViewById(R.id.edit_password);
        Button signInButton = findViewById(R.id.button_sign_in);
        TextView registerTextView = findViewById(R.id.text_register);
        TextView forgotPasswordTextView = findViewById(R.id.text_forgot_password);

        String prefilledEmail = getIntent().getStringExtra(UserPreferences.EXTRA_EMAIL);
        if (prefilledEmail != null) {
            emailEditText.setText(prefilledEmail);
        }

        signInButton.setOnClickListener(view -> signIn());
        registerTextView.setOnClickListener(view ->
                startActivity(new Intent(this, RegisterActivity.class)));
        forgotPasswordTextView.setOnClickListener(view -> showPasswordHint());
    }

    private void signIn() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString();

        if (email.isEmpty() || password.isEmpty()) {
            toast(R.string.message_fill_all_fields);
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError(getString(R.string.message_invalid_email));
            emailEditText.requestFocus();
            return;
        }

        if (!userPreferences.hasRegisteredUser()) {
            toast(R.string.message_no_account);
            startActivity(new Intent(this, RegisterActivity.class));
            return;
        }

        if (!userPreferences.validateCredentials(email, password)) {
            toast(R.string.message_invalid_login);
            return;
        }

        userPreferences.setLoggedIn(true);
        toast(R.string.message_login_success);
        openHome();
    }

    private void showPasswordHint() {
        String email = emailEditText.getText().toString().trim();
        if (email.isEmpty()) {
            toast(R.string.message_enter_email_for_password);
            return;
        }

        String password = userPreferences.findPasswordByEmail(email);
        if (password == null || password.isEmpty()) {
            toast(R.string.message_password_not_found);
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.forgot_password)
                .setMessage(getString(R.string.message_password_demo, password))
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void openHome() {
        Intent intent = new Intent(this, PostActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void toast(int messageId) {
        Toast.makeText(this, messageId, Toast.LENGTH_SHORT).show();
    }
}
