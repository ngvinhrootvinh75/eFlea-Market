package com.example.efleamarket.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

/**
 * Nén + thu nhỏ ảnh trước khi tải lên (cạnh dài tối đa 1600px, JPEG 85%).
 * PHẢI gọi ở LUỒNG NỀN vì giải mã ảnh tốn thời gian.
 */
public final class ImageCompressor {

    private static final int MAX_SIZE = 1600;
    private static final int JPEG_QUALITY = 85;

    private ImageCompressor() {}

    /** Trả về Uri của file JPG tạm trong thư mục cache. */
    public static Uri compress(Context context, Uri source) throws IOException {
        ContentResolver cr = context.getContentResolver();

        // 1. Chỉ đọc kích thước ảnh (chưa giải mã cả ảnh để tiết kiệm RAM)
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream in = cr.openInputStream(source)) {
            BitmapFactory.decodeStream(in, null, bounds);
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw new IOException("Không đọc được ảnh");
        }

        // 2. Giải mã ảnh ở kích thước đã giảm sơ bộ
        int sample = 1;
        int longest = Math.max(bounds.outWidth, bounds.outHeight);
        while (longest / (sample * 2) >= MAX_SIZE) sample *= 2;

        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = sample;
        Bitmap bitmap;
        try (InputStream in = cr.openInputStream(source)) {
            bitmap = BitmapFactory.decodeStream(in, null, opts);
        }
        if (bitmap == null) throw new IOException("Định dạng ảnh không được hỗ trợ");

        // 3. Thu nhỏ chính xác + xoay đúng chiều theo EXIF
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        float scale = Math.min(1f, (float) MAX_SIZE / Math.max(w, h));
        int rotation = readRotation(cr, source);
        if (scale < 1f || rotation != 0) {
            Matrix m = new Matrix();
            if (scale < 1f) m.postScale(scale, scale);
            if (rotation != 0) m.postRotate(rotation);
            Bitmap transformed = Bitmap.createBitmap(bitmap, 0, 0, w, h, m, true);
            if (transformed != bitmap) bitmap.recycle();
            bitmap = transformed;
        }

        // 4. Ghi ra file JPG tạm
        File out = new File(context.getCacheDir(), "upload_" + UUID.randomUUID() + ".jpg");
        try (FileOutputStream fos = new FileOutputStream(out)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, fos);
        } finally {
            bitmap.recycle();
        }
        return Uri.fromFile(out);
    }

    private static int readRotation(ContentResolver cr, Uri source) {
        try (InputStream in = cr.openInputStream(source)) {
            if (in == null) return 0;
            ExifInterface exif = new ExifInterface(in);
            switch (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                case ExifInterface.ORIENTATION_ROTATE_90: return 90;
                case ExifInterface.ORIENTATION_ROTATE_180: return 180;
                case ExifInterface.ORIENTATION_ROTATE_270: return 270;
                default: return 0;
            }
        } catch (IOException e) {
            return 0;
        }
    }
}