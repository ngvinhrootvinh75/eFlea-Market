package com.example.efleamarket.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.efleamarket.R;
import com.example.efleamarket.models.Product;
import com.example.efleamarket.models.Seller;
import com.example.efleamarket.viewmodels.ProductDetailViewModel;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProductDetailActivity extends AppCompatActivity {

    public static final String EXTRA_PRODUCT_ID = "productId";

    private RecyclerView rvImages;
    private TextView tvImageCounter, tvPrice, tvTitle, tvStatus, tvCondition, tvLocation,
            tvPostedDate, tvDescription, tvSellerName, tvSellerRating;
    private ImageView ivSellerAvatar;
    private View layoutContent, layoutBuyerActions, layoutOwnerActions, cardSeller;
    private ProgressBar pbLoading;

    private final List<String> imageUrls = new ArrayList<>();
    private ImagePagerAdapter imageAdapter;

    private ProductDetailViewModel viewModel;
    private Product product;

    private final DecimalFormat priceFormatter = new DecimalFormat("#,### đ");
    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.product_detail);

        String productId = getIntent().getStringExtra(EXTRA_PRODUCT_ID);
        if (productId == null || productId.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy sản phẩm", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        viewModel = new ViewModelProvider(this).get(ProductDetailViewModel.class);

        bindViews();
        setupImagePager();
        setupEvents();
        observeViewModel();

        viewModel.load(productId);
    }

    private void bindViews() {
        rvImages = findViewById(R.id.rvImages);
        tvImageCounter = findViewById(R.id.tvImageCounter);
        tvPrice = findViewById(R.id.tvPrice);
        tvTitle = findViewById(R.id.tvTitle);
        tvStatus = findViewById(R.id.tvStatus);
        tvCondition = findViewById(R.id.tvCondition);
        tvLocation = findViewById(R.id.tvLocation);
        tvPostedDate = findViewById(R.id.tvPostedDate);
        tvDescription = findViewById(R.id.tvDescription);
        tvSellerName = findViewById(R.id.tvSellerName);
        tvSellerRating = findViewById(R.id.tvSellerRating);
        ivSellerAvatar = findViewById(R.id.ivSellerAvatar);
        layoutContent = findViewById(R.id.layoutContent);
        layoutBuyerActions = findViewById(R.id.layoutBuyerActions);
        layoutOwnerActions = findViewById(R.id.layoutOwnerActions);
        cardSeller = findViewById(R.id.cardSeller);
        pbLoading = findViewById(R.id.pbLoading);
    }

    private void observeViewModel() {
        viewModel.getLoading().observe(this, isLoading -> {
            boolean loading = Boolean.TRUE.equals(isLoading);
            pbLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
            if (loading) layoutContent.setVisibility(View.INVISIBLE);
        });

        viewModel.getProduct().observe(this, p -> {
            if (p == null) return;
            product = p;
            layoutContent.setVisibility(View.VISIBLE);
            showProduct();
        });

        viewModel.getSeller().observe(this, this::showSeller);

        viewModel.getError().observe(this, message -> {
            if (message == null) return;
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            finish();
        });
    }

    // Ảnh vuốt ngang từng tấm một
    private void setupImagePager() {
        imageAdapter = new ImagePagerAdapter(imageUrls);
        LinearLayoutManager lm = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        rvImages.setLayoutManager(lm);
        rvImages.setAdapter(imageAdapter);

        PagerSnapHelper snapHelper = new PagerSnapHelper();
        snapHelper.attachToRecyclerView(rvImages);

        rvImages.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView rv, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    View snap = snapHelper.findSnapView(lm);
                    if (snap != null) updateCounter(lm.getPosition(snap));
                }
            }
        });
    }

    private void updateCounter(int position) {
        if (imageUrls.size() <= 1) {
            tvImageCounter.setVisibility(View.GONE);
            return;
        }
        tvImageCounter.setVisibility(View.VISIBLE);
        tvImageCounter.setText((position + 1) + "/" + imageUrls.size());
    }

    private void setupEvents() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        findViewById(R.id.btnFavorite).setOnClickListener(v ->
                Toast.makeText(this, "Chức năng Yêu thích đang phát triển", Toast.LENGTH_SHORT).show());

        findViewById(R.id.btnChat).setOnClickListener(v -> {
            // TODO (tuần 4 - ghép với B): mở màn hình chat
            // Intent i = new Intent(this, ChatActivity.class);
            // i.putExtra("productId", product.getId());
            // i.putExtra("sellerId", product.getSellerId());
            // startActivity(i);
            Toast.makeText(this, "Chức năng Nhắn tin đang phát triển", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.btnCreateDeal).setOnClickListener(v -> {
            // TODO (tuần 4 - ghép với B): mở màn hình tạo giao dịch
            // truyền productId, buyerId (người đang đăng nhập), sellerId
            Toast.makeText(this, "Chức năng Giao dịch đang phát triển", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.btnEditProduct).setOnClickListener(v ->
                Toast.makeText(this, "Chức năng Sửa tin đang phát triển", Toast.LENGTH_SHORT).show());

        cardSeller.setOnClickListener(v -> openSellerProfile());
    }

    private void showProduct() {
        tvTitle.setText(product.getTitle());
        tvPrice.setText(priceFormatter.format(product.getPrice()));
        tvCondition.setText(orDefault(product.getCondition(), "Đã qua sử dụng"));
        tvLocation.setText("📍 " + orDefault(product.getLocation(), "Toàn quốc"));
        tvPostedDate.setText(product.getCreatedAt() != null
                ? "Đăng ngày " + dateFormatter.format(product.getCreatedAt()) : "");
        tvDescription.setText(orDefault(product.getDescription(), "Người bán chưa thêm mô tả."));

        // Ảnh
        imageUrls.clear();
        if (product.getImages() != null) imageUrls.addAll(product.getImages());
        if (imageUrls.isEmpty() && product.getCoverImage() != null) imageUrls.add(product.getCoverImage());
        imageAdapter.notifyDataSetChanged();
        updateCounter(0);

        // Trạng thái tin đăng
        String status = product.getStatus();
        boolean selling = status == null || Product.STATUS_SELLING.equals(status);
        if (selling) {
            tvStatus.setVisibility(View.GONE);
        } else {
            tvStatus.setVisibility(View.VISIBLE);
            tvStatus.setText(statusLabel(status));
        }

        // Người bán mặc định (khi chưa đọc được users)
        tvSellerName.setText(orDefault(product.getSellerName(), "Người bán"));
        tvSellerRating.setText("Chưa có đánh giá");

        // Chủ tin thấy nút "Sửa", người khác thấy "Nhắn tin / Tạo giao dịch"
        boolean isOwner = viewModel.isOwner(product);
        layoutOwnerActions.setVisibility(isOwner ? View.VISIBLE : View.GONE);
        layoutBuyerActions.setVisibility(!isOwner && selling ? View.VISIBLE : View.GONE);
    }

    private void showSeller(Seller seller) {
        if (seller == null || isFinishing() || isDestroyed()) return;

        if (seller.getFullName() != null && !seller.getFullName().isEmpty()) {
            tvSellerName.setText(seller.getFullName());
        }

        String avatarUrl = seller.getAvatarUrl();
        Glide.with(this)
                .load(avatarUrl != null && !avatarUrl.isEmpty() ? avatarUrl : null)
                .placeholder(R.drawable.ic_person)
                .error(R.drawable.ic_person)
                .circleCrop()
                .into(ivSellerAvatar);

        if (seller.getRatingCount() > 0) {
            tvSellerRating.setText(String.format(Locale.getDefault(),
                    "★ %.1f  (%d đánh giá)", seller.getRatingAvg(), seller.getRatingCount()));
        }
    }

    private void openSellerProfile() {
        if (product == null || product.getSellerId() == null || product.getSellerId().isEmpty()) return;
        Intent intent = new Intent(this, ProfileActivity.class);
        intent.putExtra(ProfileActivity.EXTRA_UID, product.getSellerId());
        startActivity(intent);
    }

    private String statusLabel(String status) {
        switch (status) {
            case Product.STATUS_SOLD: return "Đã bán";
            case Product.STATUS_PENDING: return "Đang chờ duyệt";
            case Product.STATUS_HIDDEN: return "Tin đã bị ẩn";
            default: return status;
        }
    }

    private static String orDefault(String value, String fallback) {
        return value != null && !value.trim().isEmpty() ? value : fallback;
    }

    // ===== Adapter ảnh vuốt ngang =====
    private static class ImagePagerAdapter extends RecyclerView.Adapter<ImagePagerAdapter.VH> {
        private final List<String> urls;

        ImagePagerAdapter(List<String> urls) {
            this.urls = urls;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ImageView iv = new ImageView(parent.getContext());
            iv.setLayoutParams(new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            iv.setBackgroundColor(0xFFF3F4F6);
            return new VH(iv);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            String url = urls.isEmpty() ? null : urls.get(position);
            Glide.with(holder.image)
                    .load(url)
                    .placeholder(R.drawable.ic_shopping_cart)
                    .error(R.drawable.ic_shopping_cart)
                    .into(holder.image);
        }

        @Override
        public int getItemCount() {
            return Math.max(1, urls.size());
        }

        static class VH extends RecyclerView.ViewHolder {
            final ImageView image;

            VH(ImageView v) {
                super(v);
                image = v;
            }
        }
    }
}