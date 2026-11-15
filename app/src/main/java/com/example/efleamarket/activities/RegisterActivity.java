package com.example.efleamarket.activities;

import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.efleamarket.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private EditText etFullName, etEmail;
    private EditText etPassword, etConfirmPassword;
    private Button btnRegister;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword =
                findViewById(R.id.etConfirmPassword);

        btnRegister = findViewById(R.id.btnRegister);
        TextView tvGoLogin = findViewById(R.id.tvGoLogin);

        btnRegister.setOnClickListener(v -> registerUser());

        // Register được mở từ LoginActivity
        tvGoLogin.setOnClickListener(v -> finish());
    }

    private void registerUser() {

        String name = etFullName.getText()
                .toString().trim();
        String email = etEmail.getText()
                .toString().trim();
        String password = etPassword.getText()
                .toString();
        String confirm = etConfirmPassword.getText()
                .toString();

        if (name.isEmpty()) {
            etFullName.setError("Vui lòng nhập họ tên");
            etFullName.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Email không hợp lệ");
            etEmail.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            etPassword.setError("Vui lòng nhập mật khẩu");
            etPassword.requestFocus();
            return;
        }

        if (password.length() < 8) {
            etPassword.setError("Mật khẩu phải có ít nhất 8 ký tự");
            etPassword.requestFocus();
            return;
        }

        boolean hasUppercase = !password.equals(password.toUpperCase());
        boolean hasLowercase = !password.equals(password.toLowerCase());
        boolean hasDigit = password.matches(".*\\d.*");
        boolean hasSpecialChar = password.matches(
                ".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*"
        );

        if (!hasUppercase || !hasLowercase || !hasDigit || !hasSpecialChar) {
            etPassword.setError(
                    "Mật khẩu phải chứa chữ thường, chữ in hoa, số và ký tự đặc biệt"
            );
            etPassword.requestFocus();
            return;
        }

        if (confirm.isEmpty()) {
            etConfirmPassword.setError("Vui lòng xác nhận mật khẩu");
            etConfirmPassword.requestFocus();
            return;
        }

        if (!password.equals(confirm)) {
            etConfirmPassword.setError(
                    "Mật khẩu không trùng khớp");
            etConfirmPassword.requestFocus();
            return;
        }

        btnRegister.setEnabled(false);
        btnRegister.setText("Đang đăng ký...");

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {

                    if (!task.isSuccessful()) {
                        resetButton();
                        Toast.makeText(this,
                                "Đăng ký thất bại: "
                                        + (task.getException() != null
                                        ? task.getException().getMessage()
                                        : "Lỗi không xác định"),
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    FirebaseUser user = auth.getCurrentUser();

                    if (user == null) {
                        resetButton();
                        Toast.makeText(this,
                                "Không lấy được tài khoản",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    saveProfile(user, name, email);
                });
    }

    private void saveProfile(
            FirebaseUser user,
            String name,
            String email) {

        Map<String, Object> profile = new HashMap<>();
        profile.put("uid", user.getUid());
        profile.put("name", name);
        profile.put("email", email);
        profile.put("role", "user");
        profile.put("status", "active");
        profile.put("ratingAvg", 0.0);
        profile.put("createdAt",
                FieldValue.serverTimestamp());

        db.collection("users")
                .document(user.getUid())
                .set(profile)
                .addOnSuccessListener(unused -> {
                    resetButton();

                    auth.signOut();

                    Toast.makeText(this,
                            "Đăng ký thành công!",
                            Toast.LENGTH_LONG).show();

                    setResult(RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e -> {
                    resetButton();

                    Toast.makeText(this,
                            "Đã tạo tài khoản nhưng lỗi lưu hồ sơ: "
                                    + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void resetButton() {
        btnRegister.setEnabled(true);
        btnRegister.setText("ĐĂNG KÝ");
    }
}
