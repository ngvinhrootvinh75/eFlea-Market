package com.example.efleamarket.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.efleamarket.R;
import com.example.efleamarket.adapters.SelectedImageAdapter;
import com.example.efleamarket.models.Category;
import com.example.efleamarket.viewmodels.PostProductViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class PostProductActivity extends AppCompatActivity {

    private TextInputEditText etTitle, etPrice, etLocation, etDescription;
    private MaterialAutoCompleteTextView actCategory, actCondition;
    private TextView tvImageCount, tvProgress;
    private View layoutProgress;
    private MaterialButton btnSubmit;

    private final List<Uri> imageList = new ArrayList<>();
    private SelectedImageAdapter imageAdapter;

    private PostProductViewModel viewModel;

    // Trình chọn ảnh của hệ thống - KHÔNG cần xin quyền đọc bộ nhớ
    private final ActivityResultLauncher<PickVisualMediaRequest> pickImages =
            registerForActivityResult(
                    new ActivityResultContracts.PickMultipleVisualMedia(PostProductViewModel.MAX_IMAGES),
                    uris -> {
                        if (uris != null && !uris.isEmpty()) viewModel.addImages(uris);
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.post_product);

        viewModel = new ViewModelProvider(this).get(PostProductViewModel.class);

        bindViews();
        setupImageList();
        setupDropdowns();
        setupEvents();
        observeViewModel();
    }

    private void bindViews() {
        etTitle = findViewById(R.id.etTitle);
        etPrice = findViewById(R.id.etPrice);
        etLocation = findViewById(R.id.etLocation);
        etDescription = findViewById(R.id.etDescription);
        actCategory = findViewById(R.id.actCategory);
        actCondition = findViewById(R.id.actCondition);
        tvImageCount = findViewById(R.id.tvImageCount);
        tvProgress = findViewById(R.id.tvProgress);
        layoutProgress = findViewById(R.id.layoutProgress);
        btnSubmit = findViewById(R.id.btnSubmit);
    }

    private void setupImageList() {
        RecyclerView rv = findViewById(R.id.rvSelectedImages);
        imageAdapter = new SelectedImageAdapter(imageList, position -> viewModel.removeImage(position));
        rv.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rv.setAdapter(imageAdapter);
    }

    private void setupDropdowns() {
        actCondition.setSimpleItems(PostProductViewModel.CONDITIONS.toArray(new String[0]));
        actCategory.setOnItemClickListener((parent, view, position, id) ->
                viewModel.setSelectedCategoryIndex(position));
    }

    private void setupEvents() {
        findViewById(R.id.btnBack).setOnClickListener(v -> tryLeave());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                tryLeave();
            }
        });

        findViewById(R.id.btnAddImage).setOnClickListener(v -> {
            if (viewModel.remainingSlots() <= 0) {
                Toast.makeText(this, "Đã đủ " + PostProductViewModel.MAX_IMAGES + " ảnh", Toast.LENGTH_SHORT).show();
                return;
            }
            pickImages.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        });

        btnSubmit.setOnClickListener(v -> viewModel.submit(
                textOf(etTitle),
                textOf(etPrice),
                actCondition.getText() != null ? actCondition.getText().toString() : "",
                textOf(etDescription),
                textOf(etLocation)));
    }

    private void observeViewModel() {
        viewModel.getImages().observe(this, list -> {
            imageList.clear();
            if (list != null) imageList.addAll(list);
            imageAdapter.notifyDataSetChanged();
            tvImageCount.setText(imageList.size() + "/" + PostProductViewModel.MAX_IMAGES);
        });

        viewModel.getCategories().observe(this, list -> {
            if (list == null) return;
            String[] names = new String[list.size()];
            for (int i = 0; i < list.size(); i++) {
                Category c = list.get(i);
                names[i] = c.getName();
            }
            actCategory.setSimpleItems(names);
        });

        viewModel.getSubmitting().observe(this, isSubmitting -> {
            boolean busy = Boolean.TRUE.equals(isSubmitting);
            layoutProgress.setVisibility(busy ? View.VISIBLE : View.GONE);
            btnSubmit.setEnabled(!busy);
        });

        viewModel.getProgressText().observe(this, tvProgress::setText);

        viewModel.getMessage().observe(this, msg -> {
            if (msg == null) return;
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            viewModel.clearMessage();
        });

        viewModel.getCreatedProductId().observe(this, productId -> {
            if (productId == null) return;
            Toast.makeText(this, "Đăng tin thành công!", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, ProductDetailActivity.class);
            intent.putExtra(ProductDetailActivity.EXTRA_PRODUCT_ID, productId);
            startActivity(intent);
            finish();
        });
    }

    private void tryLeave() {
        if (viewModel.isSubmitting()) {
            Toast.makeText(this, "Đang đăng tin, vui lòng chờ...", Toast.LENGTH_SHORT).show();
            return;
        }
        finish();
    }

    private static String textOf(TextInputEditText et) {
        return et.getText() != null ? et.getText().toString() : "";
    }
}