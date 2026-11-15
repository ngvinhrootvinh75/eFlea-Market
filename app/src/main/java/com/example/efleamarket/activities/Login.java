package com.example.efleamarket.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.efleamarket.R;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;

import java.util.Collections;

public class Login extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private MaterialButton btnGoogleLogin, btnFacebookLogin;
    private TextView tvForgotPassword, tvGoRegister;

    // Google Sign-In
    private GoogleSignInClient googleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    // Facebook Login
    private CallbackManager callbackManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        // Ánh xạ các thành phần giao diện
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnGoogleLogin = findViewById(R.id.btnGoogleLogin);
        btnFacebookLogin = findViewById(R.id.btnFacebookLogin);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        tvGoRegister = findViewById(R.id.tvGoRegister);

        // Khởi tạo Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                        try {
                            GoogleSignInAccount account = task.getResult(ApiException.class);
                            if (account != null) {
                                Toast.makeText(Login.this, "Đăng nhập Google thành công: " + account.getEmail(), Toast.LENGTH_LONG).show();
                            }
                        } catch (ApiException e) {
                            Toast.makeText(Login.this, "Đăng nhập Google thất bại (Mã lỗi: " + e.getStatusCode() + ")", Toast.LENGTH_LONG).show();
                        }
                    }
                }
        );

        // Khởi tạo Facebook Login
        callbackManager = CallbackManager.Factory.create();
        LoginManager.getInstance().registerCallback(callbackManager,
                new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        Toast.makeText(Login.this, "Đăng nhập Facebook thành công!", Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onCancel() {
                        Toast.makeText(Login.this, "Đăng nhập Facebook đã hủy", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(FacebookException exception) {
                        Toast.makeText(Login.this, "Đăng nhập Facebook lỗi: " + exception.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });

        // Xử lý sự kiện đăng nhập thông thường
        btnLogin.setOnClickListener(v -> login());

        // Xử lý sự kiện đăng nhập Google
        btnGoogleLogin.setOnClickListener(v -> {
            // To ensure fresh account selection:
            googleSignInClient.signOut().addOnCompleteListener(this, task -> {
                Intent intent = googleSignInClient.getSignInIntent();
                googleSignInLauncher.launch(intent);
            });
        });

        // Xử lý sự kiện đăng nhập Facebook
        btnFacebookLogin.setOnClickListener(v -> {
            LoginManager.getInstance().logInWithReadPermissions(Login.this, Collections.singletonList("email"));
        });

        // Xử lý sự kiện quên mật khẩu
        tvForgotPassword.setOnClickListener(v ->
                Toast.makeText(
                        Login.this,
                        "Chức năng quên mật khẩu sẽ được bổ sung sau",
                        Toast.LENGTH_SHORT
                ).show()
        );

        // Xử lý sự kiện chuyển sang đăng ký
        tvGoRegister.setOnClickListener(v ->
                Toast.makeText(
                        Login.this,
                        "Màn hình đăng ký sẽ được tạo tiếp theo",
                        Toast.LENGTH_SHORT
                ).show()
        );
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Chuyển kết quả Facebook Login về callbackManager
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    // Kiểm tra thông tin đăng nhập
    private void login() {

        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Kiểm tra email trống
        if (email.isEmpty()) {
            etEmail.setError("Vui lòng nhập email");
            etEmail.requestFocus();
            return;
        }

        // Kiểm tra định dạng email
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Email không hợp lệ");
            etEmail.requestFocus();
            return;
        }

        // Kiểm tra mật khẩu trống
        if (password.isEmpty()) {
            etPassword.setError("Vui lòng nhập mật khẩu");
            etPassword.requestFocus();
            return;
        }

        // Kiểm tra độ dài mật khẩu
        if (password.length() < 6) {
            etPassword.setError("Mật khẩu phải có ít nhất 6 ký tự");
            etPassword.requestFocus();
            return;
        }

        // Tạm thời chưa kết nối Firebase
        Toast.makeText(
                Login.this,
                "Thông tin hợp lệ! Chờ kết nối Firebase để đăng nhập.",
                Toast.LENGTH_LONG
        ).show();
    }
}
