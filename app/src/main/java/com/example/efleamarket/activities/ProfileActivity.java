package com.example.efleamarket.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.efleamarket.R;
import com.facebook.login.LoginManager;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.Locale;

public class ProfileActivity extends AppCompatActivity {

    public static final String EXTRA_UID = "uid";

    private ShapeableImageView ivAvatar;
    private TextView tvFullName, tvBio, tvRating, tvReviewCount, tvJoinedDate;
    private TextView tvEmail, tvPhone, tvAddress;
    private View btnEdit, btnLogout;

    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private String targetUid;
    private boolean isOwner;
    private ListenerRegistration publicListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile);

        ivAvatar = findViewById(R.id.ivAvatar);
        tvFullName = findViewById(R.id.tvFullName);
        tvBio = findViewById(R.id.tvBio);
        tvRating = findViewById(R.id.tvRating);
        tvReviewCount = findViewById(R.id.tvReviewCount);
        tvJoinedDate = findViewById(R.id.tvJoinedDate);
        tvEmail = findViewById(R.id.tvEmail);
        tvPhone = findViewById(R.id.tvPhone);
        tvAddress = findViewById(R.id.tvAddress);
        btnEdit = findViewById(R.id.btnEdit);
        btnLogout = findViewById(R.id.btnLogout);

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            goToLogin();
            return;
        }

        String extraUid = getIntent().getStringExtra(EXTRA_UID);
        targetUid = (extraUid != null && !extraUid.isEmpty())
                ? extraUid
                : currentUser.getUid();
        isOwner = targetUid.equals(currentUser.getUid());

        if (isOwner) {
            btnEdit.setOnClickListener(v ->
                    Toast.makeText(this, "Chức năng chỉnh sửa hồ sơ đang cập nhật", Toast.LENGTH_SHORT).show());
            btnLogout.setOnClickListener(v -> logout());
        } else {
            // Người khác xem: ẩn nút
            btnEdit.setVisibility(View.GONE);
            btnLogout.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        publicListener = db.collection("users").document(targetUid)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Lỗi tải hồ sơ: " + error.getMessage(),
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                    if (snapshot == null || !snapshot.exists()) {
                        Toast.makeText(this, "Không tìm thấy hồ sơ",
                                Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                    showPublicInfo(snapshot);
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isOwner) {
            loadPrivateInfo();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (publicListener != null) {
            publicListener.remove();
            publicListener = null;
        }
    }

    private void showPublicInfo(DocumentSnapshot doc) {
        tvFullName.setText(doc.getString("fullName"));
        tvBio.setText(doc.getString("bio"));

        String avatarUrl = doc.getString("avatarUrl");
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            Glide.with(this)
                    .load(avatarUrl)
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .into(ivAvatar);
        } else {
            ivAvatar.setImageResource(R.drawable.ic_person);
        }

        Number rating = (Number) doc.get("ratingAvg");
        Number count = (Number) doc.get("ratingCount");
        double avg = rating != null ? rating.doubleValue() : 0;
        long total = count != null ? count.longValue() : 0;
        tvRating.setText(String.format(Locale.getDefault(), "★ %.1f", avg));
        tvReviewCount.setText(total + " đánh giá");

        Timestamp createdAt = doc.getTimestamp("createdAt");
        if (createdAt != null) {
            tvJoinedDate.setText(new SimpleDateFormat("MM/yyyy", Locale.getDefault())
                    .format(createdAt.toDate()));
        }

        if (isOwner) {
            tvEmail.setText(doc.getString("email"));
        }
    }

    private void loadPrivateInfo() {
        db.collection("users").document(targetUid)
                .collection("private").document("info")
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    tvPhone.setText(doc.getString("phone"));

                    @SuppressWarnings("unchecked")
                    Map<String, Object> address =
                            (Map<String, Object>) doc.get("address");
                    if (address != null) {
                        tvAddress.setText(join(
                                address.get("street"),
                                address.get("ward"),
                                address.get("district"),
                                address.get("city")));
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi tải thông tin riêng: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }

    private void logout() {
        FirebaseAuth.getInstance().signOut();
        LoginManager.getInstance().logOut();
        Toast.makeText(this, "Đã đăng xuất", Toast.LENGTH_SHORT).show();
        goToLogin();
    }

    private void goToLogin() {
        Intent intent = new Intent(this, Login.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private String join(Object... parts) {
        StringBuilder sb = new StringBuilder();
        for (Object p : parts) {
            if (p == null || p.toString().isEmpty()) continue;
            if (sb.length() > 0) sb.append(", ");
            sb.append(p);
        }
        return sb.toString();
    }
}
