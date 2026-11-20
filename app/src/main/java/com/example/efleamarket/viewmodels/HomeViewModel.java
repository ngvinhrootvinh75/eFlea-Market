package com.example.efleamarket.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.efleamarket.models.Category;
import com.example.efleamarket.models.Product;
import com.example.efleamarket.repositories.ProductRepository;

import java.util.ArrayList;
import java.util.List;

public class HomeViewModel extends ViewModel {

    private static final String TITLE_ALL = "GỢI Ý HÔM NAY";

    private final ProductRepository repo = ProductRepository.getInstance();

    private final MutableLiveData<List<Category>> categories = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Product>> products = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> sectionTitle = new MutableLiveData<>(TITLE_ALL);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    private boolean loaded = false;

    public LiveData<List<Category>> getCategories() { return categories; }
    public LiveData<List<Product>> getProducts() { return products; }
    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<String> getSectionTitle() { return sectionTitle; }
    public LiveData<String> getError() { return error; }

    /** Chỉ tải lần đầu; xoay màn hình sẽ không tải lại. */
    public void loadIfNeeded() {
        if (loaded) return;
        loaded = true;
        loadCategories();
        showAll();
    }

    public void showAll() {
        sectionTitle.setValue(TITLE_ALL);
        loadProducts(null);
    }

    public void selectCategory(Category category) {
        sectionTitle.setValue("DANH MỤC: " + category.getName().toUpperCase());
        loadProducts(category.getId());
    }

    public void clearError() { error.setValue(null); }

    private void loadCategories() {
        repo.getCategories(new ProductRepository.Callback<List<Category>>() {
            @Override public void onSuccess(List<Category> data) { categories.setValue(data); }
            @Override public void onError(String message) { error.setValue("Lỗi tải danh mục: " + message); }
        });
    }

    private void loadProducts(String categoryId) {
        loading.setValue(true);
        repo.getSellingProducts(categoryId, new ProductRepository.Callback<List<Product>>() {
            @Override
            public void onSuccess(List<Product> data) {
                loading.setValue(false);
                products.setValue(data);
            }

            @Override
            public void onError(String message) {
                loading.setValue(false);
                error.setValue("Lỗi tải sản phẩm: " + message);
            }
        });
    }
}