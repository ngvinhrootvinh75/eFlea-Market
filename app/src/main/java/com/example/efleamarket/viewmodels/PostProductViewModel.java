package com.example.efleamarket.viewmodels;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.efleamarket.models.Category;
import com.example.efleamarket.models.Product;
import com.example.efleamarket.models.Seller;
import com.example.efleamarket.repositories.ProductRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class PostProductViewModel extends AndroidViewModel {

    public static final int MAX_IMAGES = 5;
    public static final List<String> CONDITIONS =
            Arrays.asList("Mới", "Như mới", "Tốt", "Đã qua sử dụng", "Cũ nhiều");

    private final ProductRepository repo = ProductRepository.getInstance();

    private final MutableLiveData<List<Uri>> images = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Category>> categories = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> submitting = new MutableLiveData<>(false);
    private final MutableLiveData<String> progressText = new MutableLiveData<>("");
    private final MutableLiveData<String> message = new MutableLiveData<>();
    private final MutableLiveData<String> createdProductId = new MutableLiveData<>();

    private int selectedCategoryIndex = -1;
    private String sellerName;

    public PostProductViewModel(@NonNull Application application) {
        super(application);
        loadCategories();
        loadSellerName();
    }

    public LiveData<List<Uri>> getImages() { return images; }
    public LiveData<List<Category>> getCategories() { return categories; }
    public LiveData<Boolean> getSubmitting() { return submitting; }
    public LiveData<String> getProgressText() { return progressText; }
    public LiveData<String> getMessage() { return message; }
    public LiveData<String> getCreatedProductId() { return createdProductId; }

    public void clearMessage() { message.setValue(null); }

    public void setSelectedCategoryIndex(int index) { selectedCategoryIndex = index; }

    public boolean isSubmitting() { return Boolean.TRUE.equals(submitting.getValue()); }

    // ===== Ảnh =====

    public int remainingSlots() { return MAX_IMAGES - currentImages().size(); }

    public void addImages(List<Uri> uris) {
        List<Uri> list = new ArrayList<>(currentImages());
        boolean overflow = false;
        for (Uri u : uris) {
            if (list.contains(u)) continue;
            if (list.size() >= MAX_IMAGES) { overflow = true; break; }
            list.add(u);
        }
        images.setValue(list);
        if (overflow) message.setValue("Chỉ được chọn tối đa " + MAX_IMAGES + " ảnh");
    }

    public void removeImage(int position) {
        List<Uri> list = new ArrayList<>(currentImages());
        if (position >= 0 && position < list.size()) {
            list.remove(position);
            images.setValue(list);
        }
    }

    private List<Uri> currentImages() {
        List<Uri> list = images.getValue();
        return list != null ? list : new ArrayList<>();
    }

    // ===== Đăng tin =====

    public void submit(String title, String priceText, String condition,
                       String description, String location) {
        if (isSubmitting()) return;

        title = title == null ? "" : title.trim();
        description = description == null ? "" : description.trim();
        location = location == null ? "" : location.trim();
        String digits = priceText == null ? "" : priceText.replaceAll("[^0-9]", "");

        // Kiểm tra dữ liệu nhập
        String error = null;
        List<Category> cats = categories.getValue();
        if (currentImages().isEmpty()) error = "Vui lòng chọn ít nhất 1 ảnh";
        else if (title.length() < 5) error = "Tên sản phẩm cần ít nhất 5 ký tự";
        else if (digits.isEmpty() || digits.length() > 11 || Long.parseLong(digits) <= 0) error = "Giá bán không hợp lệ";
        else if (cats == null || selectedCategoryIndex < 0 || selectedCategoryIndex >= cats.size()) error = "Vui lòng chọn danh mục";
        else if (!CONDITIONS.contains(condition)) error = "Vui lòng chọn tình trạng";

        if (error != null) {
            message.setValue(error);
            return;
        }

        FirebaseUser me = FirebaseAuth.getInstance().getCurrentUser();
        if (me == null) {
            message.setValue("Bạn cần đăng nhập để đăng tin");
            return;
        }

        Product p = new Product();
        p.setTitle(title);
        p.setPrice(Long.parseLong(digits));
        p.setCategoryId(cats.get(selectedCategoryIndex).getId());
        p.setCondition(condition);
        p.setDescription(description);
        p.setLocation(location);
        p.setKeywords(buildKeywords(title));
        p.setSellerId(me.getUid());
        p.setSellerName(resolveSellerName(me));
        p.setStatus(Product.STATUS_SELLING); // Tạm thời; khi có Admin sẽ đổi sang PENDING

        submitting.setValue(true);
        repo.createProduct(getApplication(), currentImages(), p,
                new ProductRepository.ProgressCallback<String>() {
                    @Override
                    public void onProgress(String text) {
                        progressText.setValue(text);
                    }

                    @Override
                    public void onSuccess(String productId) {
                        submitting.setValue(false);
                        createdProductId.setValue(productId);
                    }

                    @Override
                    public void onError(String msg) {
                        submitting.setValue(false);
                        message.setValue("Đăng tin thất bại: " + msg);
                    }
                });
    }

    // ===== Hỗ trợ =====

    private void loadCategories() {
        repo.getCategories(new ProductRepository.Callback<List<Category>>() {
            @Override public void onSuccess(List<Category> data) { categories.setValue(data); }
            @Override public void onError(String msg) { message.setValue("Lỗi tải danh mục: " + msg); }
        });
    }

    private void loadSellerName() {
        FirebaseUser me = FirebaseAuth.getInstance().getCurrentUser();
        if (me == null) return;
        repo.getSeller(me.getUid(), new ProductRepository.Callback<Seller>() {
            @Override
            public void onSuccess(Seller s) {
                if (s != null && s.getFullName() != null && !s.getFullName().isEmpty()) {
                    sellerName = s.getFullName();
                }
            }
            @Override public void onError(String msg) { /* dùng tên dự phòng */ }
        });
    }

    private String resolveSellerName(FirebaseUser me) {
        if (sellerName != null) return sellerName;
        if (me.getDisplayName() != null && !me.getDisplayName().isEmpty()) return me.getDisplayName();
        if (me.getEmail() != null) return me.getEmail();
        return "Người bán";
    }

    /** Tách tên sản phẩm thành từ khoá (chữ thường) để tìm kiếm sau này. */
    private static List<String> buildKeywords(String title) {
        Set<String> set = new LinkedHashSet<>();
        for (String w : title.toLowerCase(new Locale("vi")).split("\\s+")) {
            String word = w.replaceAll("[^\\p{L}\\p{N}]", "");
            if (word.length() >= 2) set.add(word);
        }
        return new ArrayList<>(set);
    }
}