package com.example.efleamarket.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.efleamarket.models.Product;
import com.example.efleamarket.models.Seller;
import com.example.efleamarket.repositories.ProductRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ProductDetailViewModel extends ViewModel {

    private final ProductRepository repo = ProductRepository.getInstance();

    private final MutableLiveData<Product> product = new MutableLiveData<>();
    private final MutableLiveData<Seller> seller = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    private String loadedId;

    public LiveData<Product> getProduct() { return product; }
    public LiveData<Seller> getSeller() { return seller; }
    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<String> getError() { return error; }

    public void load(String productId) {
        if (productId.equals(loadedId)) return; // đã tải rồi (ví dụ xoay màn hình)
        loadedId = productId;
        loading.setValue(true);

        repo.getProductById(productId, new ProductRepository.Callback<Product>() {
            @Override
            public void onSuccess(Product data) {
                loading.setValue(false);
                product.setValue(data);
                String sellerId = data.getSellerId();
                if (sellerId != null && !sellerId.isEmpty()) loadSeller(sellerId);
            }

            @Override
            public void onError(String message) {
                loading.setValue(false);
                error.setValue(message);
            }
        });
    }

    /** Người đang đăng nhập có phải chủ tin đăng không. */
    public boolean isOwner(Product p) {
        FirebaseUser me = FirebaseAuth.getInstance().getCurrentUser();
        return me != null && p != null && me.getUid().equals(p.getSellerId());
    }

    private void loadSeller(String uid) {
        repo.getSeller(uid, new ProductRepository.Callback<Seller>() {
            @Override public void onSuccess(Seller data) { seller.setValue(data); }
            @Override public void onError(String message) { /* Không có thì dùng sellerName trong product */ }
        });
    }
}