package com.example.efleamarket.utils;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tải ảnh lên Cloudinary (unsigned upload), trả về URL để lưu vào Firestore.
 * Không chứa API secret nên có thể commit lên Git an toàn.
 * Theo MVVM: chỉ được gọi từ tầng Repository, KHÔNG gọi trực tiếp trong Activity.
 */
public class CloudinaryHelper {

    private static final String CLOUD_NAME = "ie3ffytz";
    private static final String UPLOAD_PRESET = "eflea_unsigned";

    private static boolean initialized = false;
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface UploadListener {
        void onSuccess(String url);
        void onError(String message);
    }

    public interface MultiUploadListener {
        void onProgress(int done, int total);
        void onSuccess(List<String> urls);
        void onError(String message);
    }

    /** Khởi tạo 1 lần duy nhất, gọi tự động trong uploadImage(). */
    public static synchronized void init(Context context) {
        if (initialized) return;
        Map<String, Object> config = new HashMap<>();
        config.put("cloud_name", CLOUD_NAME);
        config.put("secure", true);
        try {
            MediaManager.init(context.getApplicationContext(), config);
        } catch (IllegalStateException ignored) {
            // Đã init trước đó
        }
        initialized = true;
    }

    /** Tải 1 ảnh. Kết quả trả về trên main thread. */
    public static void uploadImage(Context context, Uri uri, UploadListener listener) {
        init(context);
        MediaManager.get().upload(uri)
                .unsigned(UPLOAD_PRESET)
                .callback(new UploadCallback() {
                    @Override public void onStart(String requestId) { }

                    @Override public void onProgress(String requestId, long bytes, long totalBytes) { }

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        Object url = resultData.get("secure_url");
                        mainHandler.post(() -> {
                            if (url != null) listener.onSuccess(url.toString());
                            else listener.onError("Không nhận được URL ảnh");
                        });
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        mainHandler.post(() -> listener.onError(error.getDescription()));
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {
                        // Mất mạng: huỷ thay vì để SDK tự thử lại ngầm
                        MediaManager.get().cancelRequest(requestId);
                        mainHandler.post(() -> listener.onError("Mạng không ổn định, vui lòng thử lại"));
                    }
                })
                .dispatch();
    }

    /** Tải nhiều ảnh lần lượt, giữ đúng thứ tự (ảnh đầu tiên = ảnh bìa). */
    public static void uploadImages(Context context, List<Uri> uris, MultiUploadListener listener) {
        uploadNext(context, uris, 0, new ArrayList<>(), listener);
    }

    private static void uploadNext(Context context, List<Uri> uris, int index,
                                   List<String> urls, MultiUploadListener listener) {
        if (index >= uris.size()) {
            listener.onSuccess(urls);
            return;
        }
        uploadImage(context, uris.get(index), new UploadListener() {
            @Override
            public void onSuccess(String url) {
                urls.add(url);
                listener.onProgress(index + 1, uris.size());
                uploadNext(context, uris, index + 1, urls, listener);
            }

            @Override
            public void onError(String message) {
                listener.onError("Ảnh " + (index + 1) + ": " + message);
            }
        });
    }

    /** Tạo link ảnh nhỏ cho danh sách: Cloudinary tự resize + nén. */
    public static String thumbnail(String url, int width) {
        if (url == null || !url.contains("res.cloudinary.com") || !url.contains("/upload/")) return url;
        return url.replace("/upload/", "/upload/w_" + width + ",c_limit,q_auto,f_auto/");
    }
}