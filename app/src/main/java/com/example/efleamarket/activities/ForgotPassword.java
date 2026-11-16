
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

public class ForgotPassword extends AppCompatActivity {

    private EditText etResetEmail;
    private Button btnSendResetEmail;
    private TextView tvBackToLogin;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.forgot_password);

        // Khởi tạo Firebase Authentication
        mAuth = FirebaseAuth.getInstance();

        // Ánh xạ các thành phần giao diện

        etResetEmail = findViewById(R.id.etResetEmail);
        btnSendResetEmail = findViewById(R.id.btnSendResetEmail);
        tvBackToLogin = findViewById(R.id.tvBackToLogin);
        // Điền sẵn email nếu được truyền từ Login
        String email = getIntent().getStringExtra("email");

        if (email != null && !email.isEmpty()) {
            etResetEmail.setText(email);
        }
        // Quay lại màn hình đăng nhập
        tvBackToLogin.setOnClickListener(v -> finish());

        // Xử lý gửi email đặt lại mật khẩu
        btnSendResetEmail.setOnClickListener(v -> sendResetEmail());
    }

    // Xử lý chức năng gửi email đặt lại mật khẩu
    private void sendResetEmail() {

        // Lấy email người dùng nhập
        String email = etResetEmail.getText().toString().trim();

        // Kiểm tra email trống
        if (email.isEmpty()) {
            etResetEmail.setError("Vui lòng nhập email");
            etResetEmail.requestFocus();
            return;
        }

        // Kiểm tra định dạng email
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etResetEmail.setError("Email không hợp lệ");
            etResetEmail.requestFocus();
            return;
        }

        // Thiết lập ngôn ngữ email là tiếng Việt
        mAuth.setLanguageCode("vi");

        // Khóa nút trong lúc gửi email
        btnSendResetEmail.setEnabled(false);
        btnSendResetEmail.setText("ĐANG GỬI...");

        // Gửi email thông qua Firebase Authentication
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(this, task -> {

                    // Mở lại nút sau khi xử lý
                    btnSendResetEmail.setEnabled(true);
                    btnSendResetEmail.setText(
                            "GỬI LIÊN KẾT ĐẶT LẠI MẬT KHẨU"
                    );

                    if (task.isSuccessful()) {

                        Toast.makeText(
                                ForgotPassword.this,
                                "Nếu email thuộc tài khoản đã đăng ký, "
                                        + "hướng dẫn đặt lại mật khẩu "
                                        + "sẽ được gửi đến hộp thư của bạn.",
                                Toast.LENGTH_LONG
                        ).show();

                    } else {

                        Toast.makeText(
                                ForgotPassword.this,
                                "Không thể gửi email. "
                                        + "Vui lòng kiểm tra kết nối và thử lại.",
                                Toast.LENGTH_LONG
                        ).show();

                    }
                });
    }
}
