package com.example.efleamarket.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.efleamarket.MainActivity;
import com.example.efleamarket.R;
import com.example.efleamarket.utils.LoadingDialog;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Collections;

public class Login extends AppCompatActivity {
    private static final String TAG = "GOOGLE_LOGIN";

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private MaterialButton btnGoogleLogin, btnFacebookLogin;
    private TextView tvForgotPassword, tvGoRegister;
    private LoadingDialog loadingDialog;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private CallbackManager callbackManager;
    private GoogleSignInClient googleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        loadingDialog = new LoadingDialog(this);

        // Khởi tạo Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
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
                                firebaseAuthWithGoogle(account.getIdToken());
                            }
                        } catch (ApiException e) {
                            Toast.makeText(Login.this, "Đăng nhập Google thất bại (Mã lỗi: " + e.getStatusCode() + ")", Toast.LENGTH_LONG).show();
                        }
                    }
                }
        );

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnGoogleLogin = findViewById(R.id.btnGoogleLogin);
        btnFacebookLogin = findViewById(R.id.btnFacebookLogin);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        tvGoRegister = findViewById(R.id.tvGoRegister);

        callbackManager = CallbackManager.Factory.create();
        LoginManager.getInstance().registerCallback(callbackManager,
                new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        firebaseAuthWithFacebook(loginResult.getAccessToken().getToken());
                    }

                    @Override
                    public void onCancel() {
                        btnFacebookLogin.setEnabled(true);
                        Toast.makeText(Login.this, "Đã hủy đăng nhập Facebook",
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(FacebookException exception) {
                        btnFacebookLogin.setEnabled(true);
                        Log.e(TAG, "Facebook sign-in error", exception);
                        Toast.makeText(Login.this, "Đăng nhập Facebook thất bại",
                                Toast.LENGTH_LONG).show();
                    }
                });

        btnLogin.setOnClickListener(v -> loginWithEmail());
        btnGoogleLogin.setOnClickListener(v -> signInWithGoogle());
        btnFacebookLogin.setOnClickListener(v -> {
            btnFacebookLogin.setEnabled(false);
            LoginManager.getInstance().logInWithReadPermissions(
                    Login.this, Collections.singletonList("email"));
        });
        tvForgotPassword.setOnClickListener(v ->
                startActivity(new Intent(Login.this, ForgotPassword.class)));
        tvGoRegister.setOnClickListener(v ->
                startActivity(new Intent(Login.this, RegisterActivity.class)));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    private void loginWithEmail() {
        String accountInput = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();

        // 1. Kiểm tra rỗng
        if (accountInput.isEmpty()) {
            etEmail.setError("Vui lòng nhập Email hoặc Số điện thoại");
            etEmail.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            etPassword.setError("Vui lòng nhập mật khẩu");
            etPassword.requestFocus();
            return;
        }

        btnLogin.setEnabled(false);
        loadingDialog.show();

        // 2. Kiểm tra xem người dùng nhập Email hay SĐT
        if (Patterns.EMAIL_ADDRESS.matcher(accountInput).matches()) {
            // Trường hợp nhập EMAIL -> Đăng nhập trực tiếp
            signInFirebase(accountInput, password);
        } else if (Patterns.PHONE.matcher(accountInput).matches()) {
            // Trường hợp nhập SỐ ĐIỆN THOẠI -> Tra email trong phoneIndex/{sđt}
            // (users/ chỉ đọc được khi đã đăng nhập, còn phoneIndex cho phép tra từng số)
            String phoneKey = normalizePhone(accountInput);
            db.collection("phoneIndex").document(phoneKey)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (!task.isSuccessful()) {
                            // Lỗi mạng / rules -> báo riêng, không nhầm thành "chưa đăng ký"
                            btnLogin.setEnabled(true);
                            loadingDialog.dismiss();
                            Log.e(TAG, "phoneIndex lookup failed", task.getException());
                            Toast.makeText(this, "Lỗi kết nối, vui lòng thử lại", Toast.LENGTH_LONG).show();
                            return;
                        }
                        String emailFound = task.getResult().exists()
                                ? task.getResult().getString("email") : null;
                        if (emailFound == null || emailFound.isEmpty()) {
                            btnLogin.setEnabled(true);
                            loadingDialog.dismiss();
                            etEmail.setError("Số điện thoại này chưa được đăng ký!");
                            etEmail.requestFocus();
                            return;
                        }
                        // Tìm thấy tài khoản -> đăng nhập bằng email tương ứng
                        signInFirebase(emailFound, password);
                    });
        } else {
            // Định dạng không phải Email cũng không phải SĐT
            btnLogin.setEnabled(true);
            loadingDialog.dismiss();
            etEmail.setError("Định dạng Email hoặc Số điện thoại không đúng");
            etEmail.requestFocus();
        }
    }

    // Hàm phụ để tái sử dụng việc gọi auth.signInWithEmailAndPassword
    private void signInFirebase(String email, String password) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    btnLogin.setEnabled(true);
                    loadingDialog.dismiss();
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, MainActivity.class));
                        finish();
                    } else {
                        Toast.makeText(this, "Đăng nhập thất bại: " +
                                        (task.getException() != null ? task.getException().getMessage() : "Sai thông tin đăng nhập"),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void signInWithGoogle() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        googleSignInClient.signOut().addOnCompleteListener(this, task -> {
            googleSignInLauncher.launch(signInIntent);
        });
    }

    private void firebaseAuthWithGoogle(String idToken) {
        loadingDialog.show();
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    loadingDialog.dismiss();
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Đăng nhập Google thành công!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, MainActivity.class));
                        finish();
                    } else {
                        Toast.makeText(this, "Đăng nhập Firebase bằng Google thất bại: " + (task.getException() != null ? task.getException().getMessage() : "Lỗi"), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void firebaseAuthWithFacebook(String token) {
        loadingDialog.show();
        AuthCredential credential = FacebookAuthProvider.getCredential(token);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    btnFacebookLogin.setEnabled(true);
                    loadingDialog.dismiss();
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Đăng nhập Facebook thành công!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, MainActivity.class));
                        finish();
                    } else {
                        Toast.makeText(this, "Đăng nhập thất bại: " + (task.getException() != null ? task.getException().getMessage() : "Lỗi"), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private static String normalizePhone(String raw) {
        String p = raw.replaceAll("[\\s.-]", "");
        if (p.startsWith("+84")) p = "0" + p.substring(3);
        return p;
    }
}
