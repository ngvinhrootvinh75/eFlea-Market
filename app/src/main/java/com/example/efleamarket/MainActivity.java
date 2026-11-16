package com.example.efleamarket;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.efleamarket.adapters.CategoryAdapter;
import com.example.efleamarket.adapters.ProductAdapter;
import com.example.efleamarket.models.Category;
import com.example.efleamarket.models.Product;
import com.example.efleamarket.utils.DataSeeder;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

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

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home); // Dùng layout home.xml

        db = FirebaseFirestore.getInstance();

        // 1. Ánh xạ các View từ home.xml
        initViews();

        // 2. Cài đặt RecyclerView cho Danh mục và Sản phẩm
        setupRecyclerViews();

        // 3. Tải dữ liệu từ Firestore
        loadCategories();
        loadProducts(null); // null là tải tất cả sản phẩm ban đầu

        // 4. Lắng nghe các sự kiện bấm nút
        setupEvents();

        // 5. Bơm dữ liệu mẫu (chỉ chạy 1 lần nếu database trống)
        DataSeeder.seedProducts(this);
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
        // Danh mục cuộn ngang
        categoryAdapter = new CategoryAdapter(this, categoryList, category -> {
            // Khi bấm vào 1 danh mục -> Lọc sản phẩm theo danh mục đó
            tvSectionTitle.setText("DANH MỤC: " + category.getName().toUpperCase());
            loadProducts(category.getId());
        });
        rvCategories.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvCategories.setAdapter(categoryAdapter);

        // Sản phẩm dạng lưới 2 cột
        productAdapter = new ProductAdapter(this, productList, product -> {
            // Khi bấm vào 1 sản phẩm -> Tạm thời báo Toast (sau này sẽ mở màn hình chi tiết sản phẩm)
            Toast.makeText(this, "Chọn: " + product.getTitle(), Toast.LENGTH_SHORT).show();
        });
        rvProducts.setLayoutManager(new GridLayoutManager(this, 2));
        rvProducts.setAdapter(productAdapter);
    }

    // Tải 9 danh mục từ Firestore
    private void loadCategories() {
        db.collection("categories")
                .orderBy("sortOrder")
                .get()
                .addOnSuccessListener(snapshots -> {
                    categoryList.clear();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        Category cat = doc.toObject(Category.class);
                        cat.setId(doc.getId());
                        categoryList.add(cat);
                    }
                    categoryAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi tải danh mục: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // Tải danh sách sản phẩm (có hỗ trợ lọc theo categoryId)
    private void loadProducts(String categoryId) {
        pbLoading.setVisibility(View.VISIBLE);
        tvEmptyProducts.setVisibility(View.GONE);

        Query query = db.collection("products").whereEqualTo("status", "selling");

        // Nếu có chọn danh mục cụ thể thì lọc thêm điều kiện categoryId
        if (categoryId != null && !categoryId.isEmpty()) {
            query = query.whereEqualTo("categoryId", categoryId);
        }

        query.get()
                .addOnSuccessListener(snapshots -> {
                    pbLoading.setVisibility(View.GONE);
                    productList.clear();

                    for (QueryDocumentSnapshot doc : snapshots) {
                        Product p = doc.toObject(Product.class);
                        p.setId(doc.getId());
                        productList.add(p);
                    }

                    productAdapter.notifyDataSetChanged();

                    // Nếu không có sản phẩm nào
                    if (productList.isEmpty()) {
                        tvEmptyProducts.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    pbLoading.setVisibility(View.GONE);
                    Toast.makeText(this, "Lỗi tải sản phẩm: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void setupEvents() {
        // Nút "Tất cả" danh mục -> Hiển thị lại toàn bộ sản phẩm
        btnViewAllCategories.setOnClickListener(v -> {
            tvSectionTitle.setText("GỢI Ý HÔM NAY");
            loadProducts(null);
        });

        // Thanh điều hướng Bottom Navigation
        findViewById(R.id.navHome).setOnClickListener(v -> {
            tvSectionTitle.setText("GỢI Ý HÔM NAY");
            loadProducts(null);
        });

        findViewById(R.id.navFavorites).setOnClickListener(v ->
                Toast.makeText(this, "Chức năng Yêu thích đang phát triển", Toast.LENGTH_SHORT).show());

        findViewById(R.id.navPost).setOnClickListener(v ->
                Toast.makeText(this, "Chức năng Đăng tin bán đang phát triển", Toast.LENGTH_SHORT).show());

        findViewById(R.id.navMessages).setOnClickListener(v ->
                Toast.makeText(this, "Chức năng Tin nhắn đang phát triển", Toast.LENGTH_SHORT).show());

        findViewById(R.id.navProfile).setOnClickListener(v ->
                Toast.makeText(this, "Chức năng Cá nhân đang phát triển", Toast.LENGTH_SHORT).show());
    }
}