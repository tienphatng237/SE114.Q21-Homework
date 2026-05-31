package com.example.homework;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private EditText nameEditText;
    private EditText emailEditText;
    private EditText passwordEditText;
    private EditText confirmPasswordEditText;
    private UserPreferences userPreferences;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        userPreferences = new UserPreferences(this);
        apiService = ApiClient.getService();
        nameEditText = findViewById(R.id.edit_name);
        emailEditText = findViewById(R.id.edit_email);
        passwordEditText = findViewById(R.id.edit_password);
        confirmPasswordEditText = findViewById(R.id.edit_confirm_password);
        PasswordVisibilityHelper.attach(passwordEditText);
        PasswordVisibilityHelper.attach(confirmPasswordEditText);
        Button createButton = findViewById(R.id.button_create);
        TextView backTextView = findViewById(R.id.text_back);
        TextView loginTextView = findViewById(R.id.text_login);

        createButton.setOnClickListener(view -> register());
        backTextView.setOnClickListener(view -> openLogin(null));
        loginTextView.setOnClickListener(view -> openLogin(null));
    }

    private void register() {
        String name = nameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString();
        String confirmPassword = confirmPasswordEditText.getText().toString();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            toast(R.string.message_fill_all_fields);
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError(getString(R.string.message_invalid_email));
            emailEditText.requestFocus();
            return;
        }

        if (userPreferences.isEmailRegistered(email)) {
            emailEditText.setError(getString(R.string.message_email_already_registered));
            emailEditText.requestFocus();
            return;
        }

        int passwordError = validatePassword(password);
        if (passwordError != -1) {
            passwordEditText.setError(getString(passwordError));
            passwordEditText.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            confirmPasswordEditText.setError(getString(R.string.message_password_mismatch));
            confirmPasswordEditText.requestFocus();
            return;
        }

        UserProfile newProfile = new UserProfile(name, email, password, "", "", "");
        if (!userPreferences.register(newProfile)) {
            emailEditText.setError(getString(R.string.message_email_already_registered));
            emailEditText.requestFocus();
            return;
        }

        syncRegisterToApi(name, email, password);
        toast(R.string.message_register_success);
        openLogin(email);
    }

    private void syncRegisterToApi(String name, String email, String password) {
        String generatedPhone = buildGeneratedPhone(email);
        apiService.register(new ApiRegisterRequest(name, email, generatedPhone, password))
                .enqueue(new Callback<ApiRegisterResponse>() {
                    @Override
                    public void onResponse(Call<ApiRegisterResponse> call, Response<ApiRegisterResponse> response) {
                        ApiRegisterResponse body = response.body();
                        if (!response.isSuccessful() || body == null || body.getUser() == null) {
                            return;
                        }

                        ApiUser apiUser = body.getUser();
                        if (apiUser.getId() > 0) {
                            userPreferences.saveApiUserId(email, apiUser.getId());
                        }
                        if (!apiUser.getPhone().isEmpty()) {
                            userPreferences.saveApiPhone(email, apiUser.getPhone());
                        } else {
                            userPreferences.saveApiPhone(email, generatedPhone);
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiRegisterResponse> call, Throwable throwable) {
                        // Tạo tài khoản local vẫn là luồng chính của app.
                    }
                });
    }

    private String buildGeneratedPhone(String email) {
        String digits = String.format(
                Locale.US,
                "%010d",
                Math.abs((email + System.currentTimeMillis()).hashCode())
        );
        return digits.substring(0, 10);
    }

    private void openLogin(String email) {
        Intent intent = new Intent(this, LoginActivity.class);
        if (email != null && !email.isEmpty()) {
            intent.putExtra(UserPreferences.EXTRA_EMAIL, email);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private int validatePassword(String password) {
        // Mật khẩu phải đủ dài để tránh kiểu đặt quá ngắn, dễ đoán.
        if (password.length() < 8) {
            return R.string.message_password_short;
        }

        boolean hasUppercase = false;
        boolean hasLowercase = false;
        boolean hasDigit = false;
        boolean hasSpecialCharacter = false;

        for (char character : password.toCharArray()) {
            // Không cho khoảng trắng để tránh lỗi nhập liệu và giảm độ rõ ràng của mật khẩu.
            if (Character.isWhitespace(character)) {
                return R.string.message_password_no_spaces;
            }

            // Kiểm tra lần lượt từng nhóm ký tự cần có trong mật khẩu mạnh.
            if (Character.isUpperCase(character)) {
                hasUppercase = true;
            } else if (Character.isLowerCase(character)) {
                hasLowercase = true;
            } else if (Character.isDigit(character)) {
                hasDigit = true;
            } else {
                hasSpecialCharacter = true;
            }
        }

        // Bắt buộc có đủ chữ hoa, chữ thường, số và ký tự đặc biệt.
        if (!hasUppercase) {
            return R.string.message_password_need_uppercase;
        }
        if (!hasLowercase) {
            return R.string.message_password_need_lowercase;
        }
        if (!hasDigit) {
            return R.string.message_password_need_digit;
        }
        if (!hasSpecialCharacter) {
            return R.string.message_password_need_special;
        }

        return -1;
    }

    private void toast(int messageId) {
        Toast.makeText(this, messageId, Toast.LENGTH_SHORT).show();
    }
}
