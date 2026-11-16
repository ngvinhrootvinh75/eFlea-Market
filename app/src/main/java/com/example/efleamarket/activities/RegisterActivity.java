package com.example.efleamarket.activities;

import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.efleamarket.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private EditText etFullName, etEmail, etPhone;
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
        etPhone = findViewById(R.id.etPhone);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);

        btnRegister = findViewById(R.id.btnRegister);
        TextView tvGoLogin = findViewById(R.id.tvGoLogin);

        btnRegister.setOnClickListener(v -> registerUser());
        tvGoLogin.setOnClickListener(v -> finish());
    }

    private void registerUser() {

        String name = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString();
        String confirm = etConfirmPassword.getText().toString();

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

        if (phone.isEmpty()) {
            etPhone.setError("Vui lòng nhập số điện thoại");
            etPhone.requestFocus();
            return;
        }

        if (!Patterns.PHONE.matcher(phone).matches()) {
            etPhone.setError("Số điện thoại không hợp lệ");
            etPhone.requestFocus();
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
                ".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*");

        if (!hasUppercase || !hasLowercase || !hasDigit || !hasSpecialChar) {
            etPassword.setError(
                    "Mật khẩu phải chứa chữ thường, chữ in hoa, số và ký tự đặc biệt");
            etPassword.requestFocus();
            return;
        }

        if (confirm.isEmpty()) {
            etConfirmPassword.setError("Vui lòng xác nhận mật khẩu");
            etConfirmPassword.requestFocus();
            return;
        }

        if (!password.equals(confirm)) {
            etConfirmPassword.setError("Mật khẩu không trùng khớp");
            etConfirmPassword.requestFocus();
            return;
        }

        btnRegister.setEnabled(false);
        btnRegister.setText("Đang đăng ký...");

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, regTask -> {
                    if (!regTask.isSuccessful()) {
                        resetButton();
                        String msg = regTask.getException() != null
                                ? regTask.getException().getMessage()
                                : "Lỗi không xác định";
                        Toast.makeText(this,
                                "Đăng ký thất bại: " + msg,
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

                    createUserDocs(user, name, phone);
                });
    }

    private void createUserDocs(FirebaseUser user, String fullName, String phone) {

        DocumentReference userRef = db.collection("users").document(user.getUid());
        DocumentReference privateRef = userRef.collection("private").document("info");

        // Document công khai: đúng 14 trường theo rules
        Map<String, Object> publicData = new HashMap<>();
        publicData.put("uid", user.getUid());
        publicData.put("fullName", fullName);
        publicData.put("email", user.getEmail() != null ? user.getEmail() : "");
        publicData.put("avatarUrl", "");
        publicData.put("bio", "");
        publicData.put("loginProvider", "password");
        publicData.put("location", null);
        publicData.put("geohash", "");
        publicData.put("ratingAvg", 0);
        publicData.put("ratingCount", 0);
        publicData.put("role", "user");
        publicData.put("status", "active");
        publicData.put("createdAt", FieldValue.serverTimestamp());
        publicData.put("updatedAt", FieldValue.serverTimestamp());

        // Document riêng tư: chỉ chủ tài khoản đọc được
        Map<String, Object> address = new HashMap<>();
        address.put("street", "");
        address.put("ward", "");
        address.put("district", "");
        address.put("city", "");

        Map<String, Object> privateData = new HashMap<>();
        privateData.put("phone", phone);
        privateData.put("address", address);

        userRef.set(publicData)
                .addOnSuccessListener(u -> {
                    Log.d("FS_DEBUG", "users/{uid} OK");

                    privateRef.set(privateData)
                            .addOnSuccessListener(u2 -> {
                                Log.d("FS_DEBUG", "private/info OK");
                                resetButton();
                                auth.signOut();
                                Toast.makeText(this,
                                        "Đăng ký thành công!",
                                        Toast.LENGTH_LONG).show();
                                setResult(RESULT_OK);
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                Log.e("FS_DEBUG", "private/info FAIL", e);
                                showSaveError("private/info", e);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("FS_DEBUG", "users/{uid} FAIL", e);
                    showSaveError("users/{uid}", e);
                });
    }

    private void showSaveError(String where, Exception e) {
        resetButton();
        Toast.makeText(this,
                "Tạo tài khoản được nhưng lỗi lưu " + where + ": " + e.getMessage(),
                Toast.LENGTH_LONG).show();
    }

    private void resetButton() {
        btnRegister.setEnabled(true);
        btnRegister.setText("ĐĂNG KÝ");
    }
}