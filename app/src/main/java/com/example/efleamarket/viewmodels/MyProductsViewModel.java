package com.example.efleamarket.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.efleamarket.models.Product;
import com.example.efleamarket.repositories.ProductRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class MyProductsViewModel extends ViewModel {

    private final ProductRepository repo = ProductRepository.getInstance();

    private final MutableLiveData<List<Product>> products = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> message = new MutableLiveData<>();

    public LiveData<List<Product>> getProducts() { return products; }
    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<String> getMessage() { return message; }

    public void clearMessage() { message.setValue(null); }

    public void load() {
        FirebaseUser me = FirebaseAuth.getInstance().getCurrentUser();
        if (me == null) {
            message.setValue("Bạn cần đăng nhập");
            return;
        }
        loading.setValue(true);
        repo.getProductsBySeller(me.getUid(), new ProductRepository.Callback<List<Product>>() {
            @Override
            public void onSuccess(List<Product> data) {
                loading.setValue(false);
                products.setValue(data);
            }

            @Override
            public void onError(String msg) {
                loading.setValue(false);
                message.setValue("Lỗi tải tin đăng: " + msg);
            }
        });
    }

    /** Đang bán <-> Đã bán. */
    public void toggleSold(Product p) {
        boolean isSold = Product.STATUS_SOLD.equals(p.getStatus());
        String newStatus = isSold ? Product.STATUS_SELLING : Product.STATUS_SOLD;
        repo.updateStatus(p.getId(), newStatus, new ProductRepository.Callback<Void>() {
            @Override
            public void onSuccess(Void v) {
                message.setValue(isSold ? "Đã mở bán lại" : "Đã đánh dấu đã bán");
                load();
            }

            @Override
            public void onError(String msg) {
                message.setValue("Không cập nhật được: " + msg);
            }
        });
    }

    public void delete(Product p) {
        repo.deleteProduct(p.getId(), new ProductRepository.Callback<Void>() {
            @Override
            public void onSuccess(Void v) {
                message.setValue("Đã xoá tin đăng");
                load();
            }

            @Override
            public void onError(String msg) {
                message.setValue("Không xoá được: " + msg);
            }
        });
    }
}