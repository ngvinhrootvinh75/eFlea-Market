package com.example.efleamarket;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.example.efleamarket.activities.PostProductActivity;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.efleamarket.activities.ProductDetailActivity;
import com.example.efleamarket.activities.ProfileActivity;
import com.example.efleamarket.adapters.CategoryAdapter;
import com.example.efleamarket.adapters.ProductAdapter;
import com.example.efleamarket.models.Category;
import com.example.efleamarket.models.Product;
import com.example.efleamarket.utils.DataSeeder;
import com.example.efleamarket.viewmodels.HomeViewModel;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView rvCategories, rvProducts;
    private ProgressBar pbLoading;
    private TextView tvEmptyProducts, tvSectionTitle, btnViewAllCategories;

    private CategoryAdapter categoryAdapter;
    private ProductAdapter productAdapter;

    private final List<Category> categoryList = new ArrayList<>();
    private final List<Product> productList = new ArrayList<>();

    private HomeViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home);

        applySystemBarPadding();

        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        initViews();
        setupRecyclerViews();
        observeViewModel();
        setupEvents();

        viewModel.loadIfNeeded();

        // Bơm dữ liệu mẫu (chỉ chạy nếu database trống)
        DataSeeder.seedProducts(this);
    }
    @Override
    protected void onRestart() {
        super.onRestart();
        // Quay về từ màn hình khác (VD: vừa đăng tin) -> tải lại danh sách
        viewModel.refresh();
    }

    /** Chừa khoảng trống cho thanh trạng thái (trên) và thanh điều hướng hệ thống (dưới). */
    private void applySystemBarPadding() {
        View root = ((ViewGroup) findViewById(android.R.id.content)).getChildAt(0);
        final int left = root.getPaddingLeft();
        final int top = root.getPaddingTop();
        final int right = root.getPaddingRight();
        final int bottom = root.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(left + bars.left, top + bars.top, right + bars.right, bottom + bars.bottom);
            return insets;
        });
    }

    private void initViews() {
        rvCategories = findViewById(R.id.rvCategories);
        rvProducts = findViewById(R.id.rvProducts);
        pbLoading = findViewById(R.id.pbLoading);
        tvEmptyProducts = findViewById(R.id.tvEmptyProducts);
        tvSectionTitle = findViewById(R.id.tvSectionTitle);
        btnViewAllCategories = findViewById(R.id.btnViewAllCategories);
    }

    private void setupRecyclerViews() {
        categoryAdapter = new CategoryAdapter(this, categoryList, category ->
                viewModel.selectCategory(category));
        rvCategories.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvCategories.setAdapter(categoryAdapter);

        productAdapter = new ProductAdapter(this, productList, product -> {
            Intent intent = new Intent(this, ProductDetailActivity.class);
            intent.putExtra(ProductDetailActivity.EXTRA_PRODUCT_ID, product.getId());
            startActivity(intent);
        });
        rvProducts.setLayoutManager(new GridLayoutManager(this, 2));
        rvProducts.setAdapter(productAdapter);
    }

    private void observeViewModel() {
        viewModel.getCategories().observe(this, list -> {
            categoryList.clear();
            if (list != null) categoryList.addAll(list);
            categoryAdapter.notifyDataSetChanged();
        });

        viewModel.getProducts().observe(this, list -> {
            productList.clear();
            if (list != null) productList.addAll(list);
            productAdapter.notifyDataSetChanged();
            updateEmptyState();
        });

        viewModel.getLoading().observe(this, isLoading -> {
            pbLoading.setVisibility(Boolean.TRUE.equals(isLoading) ? View.VISIBLE : View.GONE);
            updateEmptyState();
        });

        viewModel.getSectionTitle().observe(this, tvSectionTitle::setText);

        viewModel.getError().observe(this, message -> {
            if (message == null) return;
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            viewModel.clearError(); // tránh hiện lại khi xoay màn hình
        });
    }

    private void updateEmptyState() {
        boolean isLoading = Boolean.TRUE.equals(viewModel.getLoading().getValue());
        tvEmptyProducts.setVisibility(!isLoading && productList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void setupEvents() {
        btnViewAllCategories.setOnClickListener(v -> viewModel.showAll());

        findViewById(R.id.navHome).setOnClickListener(v -> viewModel.showAll());

        findViewById(R.id.navFavorites).setOnClickListener(v ->
                Toast.makeText(this, "Chức năng Yêu thích đang phát triển", Toast.LENGTH_SHORT).show());

        findViewById(R.id.navPost).setOnClickListener(v ->
                startActivity(new Intent(this, PostProductActivity.class)));

        findViewById(R.id.navMessages).setOnClickListener(v ->
                Toast.makeText(this, "Chức năng Tin nhắn đang phát triển", Toast.LENGTH_SHORT).show());

        findViewById(R.id.navProfile).setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, ProfileActivity.class)));
    }
}