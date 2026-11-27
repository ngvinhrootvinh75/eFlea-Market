package com.example.efleamarket.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.efleamarket.MainActivity;
import com.example.efleamarket.R;
import com.example.efleamarket.adapters.ProductAdapter;
import com.example.efleamarket.models.Chat;
import com.example.efleamarket.models.Product;
import com.example.efleamarket.repositories.ChatRepository;
import com.example.efleamarket.utils.AvatarUtils;
import com.example.efleamarket.utils.ChatFormat;
import com.example.efleamarket.utils.CloudinaryHelper;
import com.facebook.login.LoginManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    public static final String EXTRA_UID = "uid";

    private static final boolean SHOW_CONTACT_TO_OTHERS = false;

    private ShapeableImageView ivAvatar;
    private TextView tvFullName, tvUsername, tvBio, tvRating, tvReviewCount, tvJoinedDate, tvSoldCount;
    private TextView tvEmail, tvPhone, tvAddress, tvGender, tvDob;
    private View btnEdit, btnLogout, btnMessage;

    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private String targetUid;
    private boolean isOwner;

    private ListenerRegistration userListener;
    private ListenerRegistration privateListener;
    private ListenerRegistration soldListener;
    private final List<Product> sellerProducts = new ArrayList<>();
    private ProductAdapter sellerProductAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile);
        applySystemBarPadding();

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
        btnMessage = findViewById(R.id.btnMessage);

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            goToLogin();
            return;
        }

        String extraUid = getIntent().getStringExtra(EXTRA_UID);
        targetUid = (extraUid != null && !extraUid.isEmpty()) ? extraUid : currentUser.getUid();
        isOwner = targetUid.equals(currentUser.getUid());

        ivAvatar.setImageResource(R.drawable.ic_person);
        tvSoldCount.setText("0");

        if (isOwner) {
            btnEdit.setVisibility(View.VISIBLE);
            btnLogout.setVisibility(View.VISIBLE);
            btnMessage.setVisibility(View.GONE);
            btnEdit.setOnClickListener(v -> openEditProfile());
            btnLogout.setOnClickListener(v -> logout());
            View cardMyProducts = findViewById(R.id.cardMyProducts);
            cardMyProducts.setVisibility(View.VISIBLE);
            cardMyProducts.setOnClickListener(v ->
                    startActivity(new Intent(this, MyProductsActivity.class)));
        } else {
            btnEdit.setVisibility(View.GONE);
            btnLogout.setVisibility(View.GONE);
            btnMessage.setVisibility(View.VISIBLE);
            btnMessage.setOnClickListener(v -> openChatWithTarget());
            if (!SHOW_CONTACT_TO_OTHERS) {
                findViewById(R.id.tvContactTitle).setVisibility(View.GONE);
                findViewById(R.id.cardContact).setVisibility(View.GONE);
                findViewById(R.id.tvAddressTitle).setVisibility(View.GONE);
                findViewById(R.id.cardAddress).setVisibility(View.GONE);
            }
            setupSellerProducts();
        }

        findViewById(R.id.btnNotification).setOnClickListener(v ->
                Toast.makeText(this, "Chức năng Thông báo đang phát triển", Toast.LENGTH_SHORT).show());

        setupBottomNav();
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

    private void listenUserProfile() {
        userListener = db.collection("users").document(targetUid)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) return;
                    if (snapshot == null || !snapshot.exists()) {
                        if (isOwner) {
                            createDefaultProfile();
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

        String avatarUrl = doc.getString("avatarUrl");
        if (isEmpty(avatarUrl) && isOwner) avatarUrl = AvatarUtils.getProviderPhotoUrl(currentUser);
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
                .load(CloudinaryHelper.thumbnail(url, 300))
                .placeholder(R.drawable.ic_person)
                .error(R.drawable.ic_person)
                .into(ivAvatar);
    }

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
        data.put("role", "user");
        data.put("status", "active");
        data.put("createdAt", FieldValue.serverTimestamp());

        db.collection("users").document(targetUid)
                .set(data, SetOptions.merge())
                .addOnFailureListener(e -> Toast.makeText(this,
                        "Không tạo được hồ sơ: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void syncProviderAvatar(DocumentSnapshot doc) {
        String current = doc.getString("avatarUrl");
        boolean isCustom = !isEmpty(current) && current.contains("res.cloudinary.com");
        if (isCustom) return;

        String providerUrl = AvatarUtils.getProviderPhotoUrl(currentUser);
        if (providerUrl != null && !providerUrl.equals(current)) {
            db.collection("users").document(targetUid).update("avatarUrl", providerUrl);
        }
    }

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

    private void listenPrivateInfo() {
        privateListener = db.collection("users").document(targetUid)
                .collection("private").document("info")
                .addSnapshotListener((doc, error) -> {
                    if (error != null) return;
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

    private void applySystemBarPadding() {
        View root = ((ViewGroup) findViewById(android.R.id.content)).getChildAt(0);
        View banner = findViewById(R.id.viewTopBanner);
        View header = findViewById(R.id.layoutHeader);

        final int left = root.getPaddingLeft();
        final int right = root.getPaddingRight();
        final int bottom = root.getPaddingBottom();
        final int bannerHeight = banner.getLayoutParams().height;
        final int headerTop = header.getPaddingTop();

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(left + bars.left, 0, right + bars.right, bottom + bars.bottom);

            ViewGroup.LayoutParams lp = banner.getLayoutParams();
            lp.height = bannerHeight + bars.top;
            banner.setLayoutParams(lp);

            header.setPadding(header.getPaddingLeft(), headerTop + bars.top,
                    header.getPaddingRight(), header.getPaddingBottom());
            return insets;
        });

        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(true);
    }

    private void setupBottomNav() {
        findViewById(R.id.navHome).setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        findViewById(R.id.navFavorites).setOnClickListener(v ->
                Toast.makeText(this, "Chức năng Yêu thích đang phát triển", Toast.LENGTH_SHORT).show());

        findViewById(R.id.navPost).setOnClickListener(v ->
                startActivity(new Intent(this, PostProductActivity.class)));

        findViewById(R.id.navMessages).setOnClickListener(v ->
                startActivity(new Intent(this, ChatListActivity.class)));

        // MỚI: đang xem hồ sơ người khác thì bấm "Cá nhân" mở hồ sơ của mình
        findViewById(R.id.navProfile).setOnClickListener(v -> {
            if (!isOwner) startActivity(new Intent(this, ProfileActivity.class));
        });
    }

    private void openEditProfile() {
        startActivity(new Intent(this, EditProfileActivity.class));
    }

    private void logout() {
        FirebaseAuth.getInstance().signOut();
        LoginManager.getInstance().logOut();
        goToLogin();
    }

    private void goToLogin() {
        Intent intent = new Intent(this, Login.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }


    private void setupSellerProducts() {
        findViewById(R.id.layoutSellerProducts).setVisibility(View.VISIBLE);

        RecyclerView rv = findViewById(R.id.rvSellerProducts);
        sellerProductAdapter = new ProductAdapter(this, sellerProducts, product -> {
            Intent i = new Intent(this, ProductDetailActivity.class);
            i.putExtra(ProductDetailActivity.EXTRA_PRODUCT_ID, product.getId());
            startActivity(i);
        });
        rv.setLayoutManager(new GridLayoutManager(this, 2));
        rv.setAdapter(sellerProductAdapter);

        loadSellerProducts();
    }

    private void loadSellerProducts() {
        ProgressBar pb = findViewById(R.id.pbSellerProducts);
        TextView tvEmpty = findViewById(R.id.tvNoProducts);
        TextView tvTitle = findViewById(R.id.tvSellingTitle);
        pb.setVisibility(View.VISIBLE);

        ChatRepository.getInstance().getSellingProductsOf(targetUid,
                new ChatRepository.Callback<List<Product>>() {
                    @Override
                    public void onSuccess(List<Product> data) {
                        if (isFinishing() || isDestroyed()) return;
                        pb.setVisibility(View.GONE);
                        sellerProducts.clear();
                        sellerProducts.addAll(data);
                        sellerProductAdapter.notifyDataSetChanged();
                        tvTitle.setText("Đang bán (" + data.size() + ")");
                        tvEmpty.setVisibility(data.isEmpty() ? View.VISIBLE : View.GONE);
                    }

                    @Override
                    public void onError(String message) {
                        if (isFinishing() || isDestroyed()) return;
                        pb.setVisibility(View.GONE);
                        tvEmpty.setText("Không tải được sản phẩm");
                        tvEmpty.setVisibility(View.VISIBLE);
                    }
                });
    }


    private void openChatWithTarget() {
        btnMessage.setEnabled(false);
        ChatRepository repo = ChatRepository.getInstance();
        repo.getChatsWith(targetUid, new ChatRepository.Callback<List<Chat>>() {
            @Override
            public void onSuccess(List<Chat> chats) {
                if (isFinishing() || isDestroyed()) return;
                if (chats.size() == 1) {
                    btnMessage.setEnabled(true);
                    ChatActivity.openChat(ProfileActivity.this, chats.get(0).getId());
                } else if (chats.size() > 1) {
                    btnMessage.setEnabled(true);
                    pickExistingChat(chats);
                } else {
                    pickProductToChat(repo);
                }
            }

            @Override
            public void onError(String message) {
                btnMessage.setEnabled(true);
                Toast.makeText(ProfileActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void pickExistingChat(List<Chat> chats) {
        String[] items = new String[chats.size()];
        for (int i = 0; i < chats.size(); i++) {
            Chat c = chats.get(i);
            items[i] = c.getProductTitle() + " · " + ChatFormat.price(c.getProductPrice());
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle("Chọn cuộc trò chuyện")
                .setItems(items, (d, which) ->
                        ChatActivity.openChat(this, chats.get(which).getId()))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void pickProductToChat(ChatRepository repo) {
        repo.getSellingProductsOf(targetUid, new ChatRepository.Callback<List<Product>>() {
            @Override
            public void onSuccess(List<Product> products) {
                btnMessage.setEnabled(true);
                if (isFinishing() || isDestroyed()) return;

                if (products.isEmpty()) {
                    Toast.makeText(ProfileActivity.this,
                            "Người này chưa có sản phẩm đang bán", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (products.size() == 1) {
                    Product p = products.get(0);
                    ChatActivity.start(ProfileActivity.this, p.getId(), p.getSellerId());
                    return;
                }

                String[] items = new String[products.size()];
                for (int i = 0; i < products.size(); i++) {
                    Product p = products.get(i);
                    items[i] = p.getTitle() + " · " + ChatFormat.price(p.getPrice());
                }
                new MaterialAlertDialogBuilder(ProfileActivity.this)
                        .setTitle("Bạn muốn hỏi về sản phẩm nào?")
                        .setItems(items, (d, which) -> {
                            Product p = products.get(which);
                            ChatActivity.start(ProfileActivity.this, p.getId(), p.getSellerId());
                        })
                        .setNegativeButton("Hủy", null)
                        .show();
            }

            @Override
            public void onError(String message) {
                btnMessage.setEnabled(true);
                Toast.makeText(ProfileActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

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