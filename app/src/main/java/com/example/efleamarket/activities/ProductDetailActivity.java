package com.example.efleamarket.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
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
import com.google.firebase.auth.FirebaseAuth;

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
        EdgeToEdge.enable(this);
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
        applySystemBarInsets();
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

    private void applySystemBarInsets() {
        View root = ((ViewGroup) findViewById(android.R.id.content)).getChildAt(0);
        View btnBack = findViewById(R.id.btnBack);
        View btnFavorite = findViewById(R.id.btnFavorite);

        final int backTop = ((ViewGroup.MarginLayoutParams) btnBack.getLayoutParams()).topMargin;
        final int favTop = ((ViewGroup.MarginLayoutParams) btnFavorite.getLayoutParams()).topMargin;
        final int contentBottom = layoutContent.getPaddingBottom();
        final int buyerBottom = layoutBuyerActions.getPaddingBottom();
        final int ownerBottom = layoutOwnerActions.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, 0, bars.right, 0);

            setTopMargin(btnBack, backTop + bars.top);
            setTopMargin(btnFavorite, favTop + bars.top);

            setBottomPadding(layoutContent, contentBottom + bars.bottom);
            setBottomPadding(layoutBuyerActions, buyerBottom + bars.bottom);
            setBottomPadding(layoutOwnerActions, ownerBottom + bars.bottom);
            return insets;
        });

        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(false);
    }

    private static void setTopMargin(View view, int top) {
        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
        lp.topMargin = top;
        view.setLayoutParams(lp);
    }

    private static void setBottomPadding(View view, int bottom) {
        view.setPadding(view.getPaddingLeft(), view.getPaddingTop(), view.getPaddingRight(), bottom);
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
            if (product == null) return;
            ChatActivity.start(this, product.getId(), product.getSellerId());
        });

        findViewById(R.id.btnCreateDeal).setOnClickListener(v -> {
            String myUid = FirebaseAuth.getInstance().getUid();
            if (product == null || myUid == null) return;
            CreateTransactionActivity.start(this, product.getId(), myUid, product.getSellerId());
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

        imageUrls.clear();
        if (product.getImages() != null) imageUrls.addAll(product.getImages());
        if (imageUrls.isEmpty() && product.getCoverImage() != null) imageUrls.add(product.getCoverImage());
        imageAdapter.notifyDataSetChanged();
        updateCounter(0);

        String status = product.getStatus();
        boolean selling = status == null || Product.STATUS_SELLING.equals(status);
        if (selling) {
            tvStatus.setVisibility(View.GONE);
        } else {
            tvStatus.setVisibility(View.VISIBLE);
            tvStatus.setText(statusLabel(status));
        }

        tvSellerName.setText(orDefault(product.getSellerName(), "Người bán"));
        tvSellerRating.setText("Chưa có đánh giá");

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
