package com.example.efleamarket.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.efleamarket.R;
import com.example.efleamarket.utils.AvatarUtils;
import com.facebook.login.LoginManager;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Màn hình hồ sơ cá nhân.
 *
 * - Mở từ Home (nút "Cá nhân") → xem hồ sơ của chính mình, có nút Chỉnh sửa + Đăng xuất.
 * - Mở từ chỗ khác kèm EXTRA_UID của người khác → chỉ xem, ẩn 2 nút trên.
 *   Ví dụ: intent.putExtra(ProfileActivity.EXTRA_UID, product.getSellerId());
 *
 * Mọi thông tin chỉ để xem; dữ liệu được lắng nghe realtime nên khi sửa hồ sơ,
 * có đánh giá mới hay bán thêm sản phẩm, màn hình tự cập nhật.
 */
public class ProfileActivity extends AppCompatActivity {

    public static final String EXTRA_UID = "uid";

    /** Đổi thành true nếu muốn người khác cũng thấy SĐT / địa chỉ. */
    private static final boolean SHOW_CONTACT_TO_OTHERS = false;

    private ShapeableImageView ivAvatar;
    private TextView tvFullName, tvUsername, tvBio, tvRating, tvReviewCount, tvJoinedDate, tvSoldCount;
    private TextView tvEmail, tvPhone, tvAddress, tvGender, tvDob;
    private View btnEdit, btnLogout;

    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private String targetUid;
    private boolean isOwner;

    private ListenerRegistration userListener;
    private ListenerRegistration privateListener;
    private ListenerRegistration soldListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile);

        ivAvatar = findViewById(R.id.ivAvatar);
        tvFullName = findViewById(R.id.tvFullName);
        tvUsername = findViewById(R.id.tvUsername);
        tvBio = findViewById(R.id.tvBio);
        tvRating = findViewById(R.id.tvRating);
        tvReviewCount = findViewById(R.id.tvReviewCount);
        tvJoinedDate = findViewById(R.id.tvJoinedDate);
        tvSoldCount = findViewById(R.id.tvCompletedOrders);
        tvEmail = findViewById(R.id.tvEmail);
        tvPhone = findViewById(R.id.tvPhone);
        tvAddress = findViewById(R.id.tvAddress);
        tvGender = findViewById(R.id.tvGender);
        tvDob = findViewById(R.id.tvDob);
        btnEdit = findViewById(R.id.btnEdit);
        btnLogout = findViewById(R.id.btnLogout);

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            goToLogin();
            return;
        }

        // Không truyền uid → xem hồ sơ của chính mình
        String extraUid = getIntent().getStringExtra(EXTRA_UID);
        targetUid = (extraUid != null && !extraUid.isEmpty()) ? extraUid : currentUser.getUid();
        isOwner = targetUid.equals(currentUser.getUid());

        ivAvatar.setImageResource(R.drawable.ic_person);
        tvSoldCount.setText("0"); // mặc định 0 khi chưa bán sản phẩm nào

        if (isOwner) {
            btnEdit.setVisibility(View.VISIBLE);
            btnLogout.setVisibility(View.VISIBLE);
            btnEdit.setOnClickListener(v -> openEditProfile());
            btnLogout.setOnClickListener(v -> logout());
        } else {
            // Người khác xem: không có Chỉnh sửa / Đăng xuất
            btnEdit.setVisibility(View.GONE);
            btnLogout.setVisibility(View.GONE);
            if (!SHOW_CONTACT_TO_OTHERS) {
                tvEmail.setText("Đã ẩn");
                tvPhone.setText("Đã ẩn");
                tvAddress.setText("Đã ẩn");
                tvGender.setText("Đã ẩn");
                tvDob.setText("Đã ẩn");
            }
        }

        // Chuông thông báo (giống Home)
        findViewById(R.id.btnNotification).setOnClickListener(v ->
                Toast.makeText(this, "Chức năng Thông báo đang phát triển", Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (targetUid == null) return;
        listenUserProfile();
        listenSoldCount();
        if (isOwner || SHOW_CONTACT_TO_OTHERS) {
            listenPrivateInfo();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (userListener != null) { userListener.remove(); userListener = null; }
        if (privateListener != null) { privateListener.remove(); privateListener = null; }
        if (soldListener != null) { soldListener.remove(); soldListener = null; }
    }

    // ---------------------------------------------------------------------
    // Thông tin công khai: tên, bio, avatar, đánh giá, ngày tham gia
    // ---------------------------------------------------------------------
    private void listenUserProfile() {
        userListener = db.collection("users").document(targetUid)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Lỗi tải hồ sơ: " + error.getMessage(),
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                    if (snapshot == null || !snapshot.exists()) {
                        if (isOwner) {
                            createDefaultProfile();   // listener sẽ tự chạy lại khi tạo xong
                        } else {
                            tvFullName.setText("Người dùng không tồn tại");
                        }
                        return;
                    }
                    showPublicInfo(snapshot);
                    if (isOwner) syncProviderAvatar(snapshot);
                });
    }

    private void showPublicInfo(DocumentSnapshot doc) {
        String fullName = doc.getString("fullName");
        tvFullName.setText(isEmpty(fullName) ? "Người dùng" : fullName);

        String username = doc.getString("username");
        if (isEmpty(username)) {
            tvUsername.setVisibility(View.GONE);
        } else {
            tvUsername.setVisibility(View.VISIBLE);
            tvUsername.setText(username.startsWith("@") ? username : "@" + username);
        }

        String bio = doc.getString("bio");
        tvBio.setText(isEmpty(bio) ? "Chưa có giới thiệu" : bio);

        // Avatar: (chủ hồ sơ) ảnh Google/Facebook → ảnh đã lưu → ảnh mặc định
        String avatarUrl = isOwner ? AvatarUtils.getProviderPhotoUrl(currentUser) : null;
        if (isEmpty(avatarUrl)) avatarUrl = doc.getString("avatarUrl");
        loadAvatar(avatarUrl);

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
            String email = currentUser.getEmail() != null ? currentUser.getEmail() : doc.getString("email");
            tvEmail.setText(isEmpty(email) ? "Chưa cập nhật" : email);
        } else if (SHOW_CONTACT_TO_OTHERS) {
            String email = doc.getString("email");
            tvEmail.setText(isEmpty(email) ? "Chưa cập nhật" : email);
        }
    }

    private void loadAvatar(String url) {
        if (isEmpty(url)) {
            ivAvatar.setImageResource(R.drawable.ic_person);
            return;
        }
        Glide.with(this)
                .load(url)
                .placeholder(R.drawable.ic_person)
                .error(R.drawable.ic_person)
                .into(ivAvatar);
    }

    /** Tạo hồ sơ mặc định cho chính chủ khi đăng nhập lần đầu. */
    private void createDefaultProfile() {
        Map<String, Object> data = new HashMap<>();
        data.put("uid", targetUid);
        data.put("fullName", !isEmpty(currentUser.getDisplayName())
                ? currentUser.getDisplayName()
                : (!isEmpty(currentUser.getEmail()) ? currentUser.getEmail() : "Người dùng"));
        data.put("email", currentUser.getEmail() != null ? currentUser.getEmail() : "");
        String photo = AvatarUtils.getProviderPhotoUrl(currentUser);
        data.put("avatarUrl", photo != null ? photo : "");
        data.put("bio", "");
        data.put("ratingAvg", 0.0);
        data.put("ratingCount", 0);
        data.put("role", "user");       // bắt buộc theo Firestore Rules
        data.put("status", "active");   // bắt buộc theo Firestore Rules
        data.put("createdAt", FieldValue.serverTimestamp());

        db.collection("users").document(targetUid)
                .set(data, SetOptions.merge())
                .addOnFailureListener(e -> Toast.makeText(this,
                        "Không tạo được hồ sơ: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    /**
     * Lưu ảnh Google/Facebook của chính chủ vào Firestore để người khác xem hồ sơ cũng thấy.
     * Cập nhật lại mỗi khi ảnh đổi (ví dụ đổi ảnh trên Google/Facebook, hoặc token Facebook mới).
     */
    private void syncProviderAvatar(DocumentSnapshot doc) {
        String providerUrl = AvatarUtils.getProviderPhotoUrl(currentUser);
        if (providerUrl != null && !providerUrl.equals(doc.getString("avatarUrl"))) {
            db.collection("users").document(targetUid).update("avatarUrl", providerUrl);
        }
    }

    // ---------------------------------------------------------------------
    // Số sản phẩm đã bán (realtime)
    // ---------------------------------------------------------------------
    private void listenSoldCount() {
        soldListener = db.collection("products")
                .whereEqualTo("sellerId", targetUid)
                .whereEqualTo("status", "sold")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) {
                        tvSoldCount.setText("0");
                        return;
                    }
                    tvSoldCount.setText(String.valueOf(snapshots.size()));
                });
    }

    // ---------------------------------------------------------------------
    // Thông tin riêng: SĐT, địa chỉ (realtime)
    // ---------------------------------------------------------------------
    private void listenPrivateInfo() {
        privateListener = db.collection("users").document(targetUid)
                .collection("private").document("info")
                .addSnapshotListener((doc, error) -> {
                    if (error != null) {
                        if (isOwner) {
                            Toast.makeText(this, "Lỗi tải thông tin riêng: " + error.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                        return;
                    }
                    if (doc == null || !doc.exists()) {
                        tvPhone.setText("Chưa cập nhật");
                        tvAddress.setText("Chưa cập nhật");
                        tvGender.setText("Chưa cập nhật");
                        tvDob.setText("Chưa cập nhật");
                        return;
                    }

                    String phone = doc.getString("phone");
                    tvPhone.setText(isEmpty(phone) ? "Chưa cập nhật" : phone);

                    @SuppressWarnings("unchecked")
                    Map<String, Object> address = (Map<String, Object>) doc.get("address");
                    String addressText = address == null ? "" : join(
                            address.get("street"),
                            address.get("ward"),
                            address.get("city"));
                    tvAddress.setText(addressText.isEmpty() ? "Chưa cập nhật" : addressText);

                    String gender = doc.getString("gender");
                    tvGender.setText(isEmpty(gender) ? "Chưa cập nhật" : gender);

                    // Ngày sinh lưu dạng Timestamp hoặc chuỗi "dd/MM/yyyy" đều hiển thị được
                    Object dob = doc.get("dob");
                    if (dob instanceof Timestamp) {
                        tvDob.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                .format(((Timestamp) dob).toDate()));
                    } else if (dob instanceof String && !isEmpty((String) dob)) {
                        tvDob.setText((String) dob);
                    } else {
                        tvDob.setText("Chưa cập nhật");
                    }
                });
    }

    // ---------------------------------------------------------------------
    // Nút bấm
    // ---------------------------------------------------------------------
    private void openEditProfile() {
        // Lưu xong quay về đây, các listener realtime sẽ tự cập nhật màn hình
        startActivity(new Intent(this, EditProfileActivity.class));
    }

    private void logout() {
        FirebaseAuth.getInstance().signOut();
        LoginManager.getInstance().logOut(); // Facebook
        // Nếu đăng nhập Google bằng GoogleSignInClient, nên signOut thêm để lần sau được chọn lại tài khoản:
        // GoogleSignIn.getClient(this, GoogleSignInOptions.DEFAULT_SIGN_IN).signOut();
        Toast.makeText(this, "Đã đăng xuất", Toast.LENGTH_SHORT).show();
        goToLogin();
    }

    private void goToLogin() {
        Intent intent = new Intent(this, Login.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // ---------------------------------------------------------------------
    // Tiện ích
    // ---------------------------------------------------------------------
    private static boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }

    private String join(Object... parts) {
        StringBuilder sb = new StringBuilder();
        for (Object p : parts) {
            if (p == null || p.toString().trim().isEmpty()) continue;
            if (sb.length() > 0) sb.append(", ");
            sb.append(p);
        }
        return sb.toString();
    }
}
