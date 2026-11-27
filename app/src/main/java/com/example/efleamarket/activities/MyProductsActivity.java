package com.example.efleamarket.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.efleamarket.R;
import com.example.efleamarket.adapters.MyProductAdapter;
import com.example.efleamarket.models.Product;
import com.example.efleamarket.viewmodels.MyProductsViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

public class MyProductsActivity extends AppCompatActivity {

    private final List<Product> productList = new ArrayList<>();
    private MyProductAdapter adapter;
    private MyProductsViewModel viewModel;

    private View pbLoading, layoutEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.my_products);

        viewModel = new ViewModelProvider(this).get(MyProductsViewModel.class);

        pbLoading = findViewById(R.id.pbLoading);
        layoutEmpty = findViewById(R.id.layoutEmpty);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnPostNow).setOnClickListener(v ->
                startActivity(new Intent(this, PostProductActivity.class)));

        setupList();
        observeViewModel();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Tải lại mỗi khi quay về màn hình này (VD: vừa sửa / đăng tin mới)
        viewModel.load();
    }

    private void setupList() {
        RecyclerView rv = findViewById(R.id.rvMyProducts);
        adapter = new MyProductAdapter(productList, new MyProductAdapter.Listener() {
            @Override
            public void onOpen(Product p) {
                Intent intent = new Intent(MyProductsActivity.this, ProductDetailActivity.class);
                intent.putExtra(ProductDetailActivity.EXTRA_PRODUCT_ID, p.getId());
                startActivity(intent);
            }

            @Override
            public void onEdit(Product p) {
                // Đợt 2 sẽ mở form sửa tin
                Toast.makeText(MyProductsActivity.this,
                        "Chức năng Sửa tin đang phát triển", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onToggleSold(Product p) {
                viewModel.toggleSold(p);
            }

            @Override
            public void onDelete(Product p) {
                new MaterialAlertDialogBuilder(MyProductsActivity.this)
                        .setTitle("Xoá tin đăng?")
                        .setMessage("Tin \"" + p.getTitle() + "\" sẽ bị xoá vĩnh viễn.")
                        .setNegativeButton("Huỷ", null)
                        .setPositiveButton("Xoá", (d, w) -> viewModel.delete(p))
                        .show();
            }
        });
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);
    }

    private void observeViewModel() {
        viewModel.getProducts().observe(this, list -> {
            productList.clear();
            if (list != null) productList.addAll(list);
            adapter.notifyDataSetChanged();
            updateEmptyState();
        });

        viewModel.getLoading().observe(this, isLoading -> {
            pbLoading.setVisibility(Boolean.TRUE.equals(isLoading) ? View.VISIBLE : View.GONE);
            updateEmptyState();
        });

        viewModel.getMessage().observe(this, msg -> {
            if (msg == null) return;
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            viewModel.clearMessage();
        });
    }

    private void updateEmptyState() {
        boolean isLoading = Boolean.TRUE.equals(viewModel.getLoading().getValue());
        layoutEmpty.setVisibility(!isLoading && productList.isEmpty() ? View.VISIBLE : View.GONE);
    }
}