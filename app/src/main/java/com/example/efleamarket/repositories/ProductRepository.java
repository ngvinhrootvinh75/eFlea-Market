package com.example.efleamarket.repositories;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;

import com.example.efleamarket.models.Category;
import com.example.efleamarket.models.Product;
import com.example.efleamarket.models.Seller;
import com.example.efleamarket.utils.CloudinaryHelper;
import com.example.efleamarket.utils.ImageCompressor;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Nơi DUY NHẤT trong phần sản phẩm được gọi Firestore / Cloudinary.
 * Activity/ViewModel không import FirebaseFirestore.
 */
public class ProductRepository {

    public interface Callback<T> {
        void onSuccess(T data);
        void onError(String message);
    }

    /** Callback có thêm báo tiến độ (dùng khi đăng tin). */
    public interface ProgressCallback<T> extends Callback<T> {
        void onProgress(String message);
    }

    private static ProductRepository instance;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // Đa luồng: 2 luồng nền để xử lý ảnh, Handler để quay về luồng chính
    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

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

    /**
     * Đăng tin mới: nén ảnh (luồng nền) → tải lên Cloudinary → lưu Firestore.
     * Trả về id của sản phẩm vừa tạo.
     */
    public void createProduct(Context context, List<Uri> imageUris, Product product,
                              ProgressCallback<String> cb) {
        Context app = context.getApplicationContext();
        List<Uri> sources = new ArrayList<>(imageUris);
        cb.onProgress("Đang xử lý ảnh...");

        // BƯỚC 1 - LUỒNG NỀN: nén ảnh để không làm đơ giao diện
        executor.execute(() -> {
            List<Uri> compressed = new ArrayList<>();
            try {
                for (Uri uri : sources) {
                    compressed.add(ImageCompressor.compress(app, uri));
                }
                // Xong thì quay về LUỒNG CHÍNH để tiếp tục
                mainHandler.post(() -> uploadAndSave(app, compressed, product, cb));
            } catch (Exception e) {
                deleteTempFiles(compressed);
                mainHandler.post(() -> cb.onError("Không xử lý được ảnh: " + e.getMessage()));
            }
        });
    }

    private void uploadAndSave(Context app, List<Uri> files, Product product,
                               ProgressCallback<String> cb) {
        cb.onProgress("Đang tải ảnh 0/" + files.size() + "...");

        // BƯỚC 2: tải ảnh lên Cloudinary (SDK tự chạy nền)
        CloudinaryHelper.uploadImages(app, files, new CloudinaryHelper.MultiUploadListener() {
            @Override
            public void onProgress(int done, int total) {
                cb.onProgress("Đang tải ảnh " + done + "/" + total + "...");
            }

            @Override
            public void onSuccess(List<String> urls) {
                deleteTempFiles(files);
                product.setImages(urls);
                cb.onProgress("Đang lưu tin đăng...");

                // BƯỚC 3: lưu Firestore
                db.collection("products").add(product)
                        .addOnSuccessListener(ref -> cb.onSuccess(ref.getId()))
                        .addOnFailureListener(e -> cb.onError(e.getMessage()));
            }

            @Override
            public void onError(String message) {
                deleteTempFiles(files);
                cb.onError(message);
            }
        });
    }

    private static void deleteTempFiles(List<Uri> files) {
        for (Uri uri : files) {
            if (uri != null && uri.getPath() != null) {
                //noinspection ResultOfMethodCallIgnored
                new File(uri.getPath()).delete();
            }
        }
    }
}