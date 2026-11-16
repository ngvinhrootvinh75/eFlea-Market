package com.example.efleamarket.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.CancellationSignal;
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
import androidx.core.content.ContextCompat;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialCancellationException;
import androidx.credentials.exceptions.GetCredentialException;

import com.example.efleamarket.MainActivity;
import com.example.efleamarket.R;
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
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Login extends AppCompatActivity {
    private static final String TAG = "GOOGLE_LOGIN";

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private MaterialButton btnGoogleLogin, btnFacebookLogin;
    private TextView tvForgotPassword, tvGoRegister;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private CredentialManager credentialManager;
    private CallbackManager callbackManager;
    private GoogleSignInClient googleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;
    private boolean socialLoginInProgress = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        credentialManager = CredentialManager.create(this);

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
        btnLogin.setText("ĐANG ĐĂNG NHẬP...");

        // 2. Kiểm tra xem người dùng nhập Email hay SĐT
        if (Patterns.EMAIL_ADDRESS.matcher(accountInput).matches()) {
            // Trường hợp nhập EMAIL -> Đăng nhập trực tiếp
            signInFirebase(accountInput, password);
        } else if (Patterns.PHONE.matcher(accountInput).matches()) {
            // Trường hợp nhập SỐ ĐIỆN THOẠI -> Tìm Email tương ứng trên Firestore
            db.collection("users")
                    .whereEqualTo("phone", accountInput)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && !task.getResult().isEmpty()) {
                            // Tìm thấy tài khoản -> Lấy email ra để đăng nhập
                            String emailFound = task.getResult().getDocuments().get(0).getString("email");
                            signInFirebase(emailFound, password);
                        } else {
                            btnLogin.setEnabled(true);
                            btnLogin.setText("ĐĂNG NHẬP");
                            etEmail.setError("Số điện thoại này chưa được đăng ký!");
                            etEmail.requestFocus();
                        }
                    });
        } else {
            // Định dạng không phải Email cũng không phải SĐT
            btnLogin.setEnabled(true);
            btnLogin.setText("ĐĂNG NHẬP");
            etEmail.setError("Định dạng Email hoặc Số điện thoại không đúng");
            etEmail.requestFocus();
        }
    }

    // Hàm phụ để tái sử dụng việc gọi auth.signInWithEmailAndPassword
    private void signInFirebase(String email, String password) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    btnLogin.setEnabled(true);
                    btnLogin.setText("ĐĂNG NHẬP");
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
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
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
        AuthCredential credential = FacebookAuthProvider.getCredential(token);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    btnFacebookLogin.setEnabled(true);
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Đăng nhập Facebook thành công!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, MainActivity.class));
                        finish();
                    } else {
                        Toast.makeText(this, "Đăng nhập thất bại: " + (task.getException() != null ? task.getException().getMessage() : "Lỗi"), Toast.LENGTH_LONG).show();
                    }
                });
    }
}