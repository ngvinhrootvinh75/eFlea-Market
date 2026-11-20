package com.example.efleamarket.repositories;

import androidx.annotation.Nullable;

import com.example.efleamarket.models.Category;
import com.example.efleamarket.models.Product;
import com.example.efleamarket.models.Seller;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Nơi DUY NHẤT trong phần sản phẩm được gọi Firestore.
 * Activity/ViewModel không import FirebaseFirestore.
 */
public class ProductRepository {

    public interface Callback<T> {
        void onSuccess(T data);
        void onError(String message);
    }

    private static ProductRepository instance;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private ProductRepository() {}

    public static synchronized ProductRepository getInstance() {
        if (instance == null) instance = new ProductRepository();
        return instance;
    }

    /** Danh mục, sắp theo sortOrder. */
    public void getCategories(Callback<List<Category>> cb) {
        db.collection("categories")
                .orderBy("sortOrder")
                .get()
                .addOnSuccessListener(snapshots -> {
                    List<Category> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        Category c = doc.toObject(Category.class);
                        c.setId(doc.getId());
                        list.add(c);
                    }
                    cb.onSuccess(list);
                })
                .addOnFailureListener(e -> cb.onError(e.getMessage()));
    }

    /** Sản phẩm đang bán, lọc theo danh mục nếu categoryId khác null. */
    public void getSellingProducts(@Nullable String categoryId, Callback<List<Product>> cb) {
        Query query = db.collection("products")
                .whereEqualTo("status", Product.STATUS_SELLING);
        if (categoryId != null && !categoryId.isEmpty()) {
            query = query.whereEqualTo("categoryId", categoryId);
        }
        query.get()
                .addOnSuccessListener(snapshots -> {
                    List<Product> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        Product p = doc.toObject(Product.class);
                        p.setId(doc.getId());
                        list.add(p);
                    }
                    cb.onSuccess(list);
                })
                .addOnFailureListener(e -> cb.onError(e.getMessage()));
    }

    /** Một sản phẩm theo id. */
    public void getProductById(String productId, Callback<Product> cb) {
        db.collection("products").document(productId).get()
                .addOnSuccessListener(doc -> {
                    Product p = doc.exists() ? doc.toObject(Product.class) : null;
                    if (p == null) {
                        cb.onError("Sản phẩm không còn tồn tại");
                        return;
                    }
                    p.setId(doc.getId());
                    cb.onSuccess(p);
                })
                .addOnFailureListener(e -> cb.onError(e.getMessage()));
    }

    /** Thông tin người bán - CHỈ ĐỌC collection users của B. Không có thì trả null. */
    public void getSeller(String uid, Callback<Seller> cb) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        cb.onSuccess(null);
                        return;
                    }
                    Number avg = (Number) doc.get("ratingAvg");
                    Number count = (Number) doc.get("ratingCount");
                    cb.onSuccess(new Seller(
                            uid,
                            doc.getString("fullName"),
                            doc.getString("avatarUrl"),
                            avg != null ? avg.doubleValue() : 0,
                            count != null ? count.intValue() : 0));
                })
                .addOnFailureListener(e -> cb.onError(e.getMessage()));
    }
}