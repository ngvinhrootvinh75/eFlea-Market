package com.example.efleamarket.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public final class MediaUtils {
    private MediaUtils() {}

    public static final long MAX_VIDEO_BYTES = 20L * 1024 * 1024;
    public static final long MAX_VIDEO_MS = 60_000;
    private static final int MAX_IMAGE_SIDE = 1280;

    public static boolean isVideo(Context c, Uri uri) {
        String t = c.getContentResolver().getType(uri);
        return t != null && t.startsWith("video/");
    }

    public static long getSize(Context c, Uri uri) {
        try (Cursor cur = c.getContentResolver().query(uri,
                new String[]{OpenableColumns.SIZE}, null, null, null)) {
            if (cur != null && cur.moveToFirst() && !cur.isNull(0)) return cur.getLong(0);
        } catch (Exception ignored) { }
        return -1;
    }

    public static long getVideoDurationMs(Context c, Uri uri) {
        MediaMetadataRetriever r = new MediaMetadataRetriever();
        try {
            r.setDataSource(c, uri);
            String d = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            return d == null ? -1 : Long.parseLong(d);
        } catch (Exception e) {
            return -1;
        } finally {
            try { r.release(); } catch (Exception ignored) { }
        }
    }

    //Nén ảnh: cạnh dài tối đa 1280px, JPEG 80%, xoay đúng chiều. Trả về file tạm trong cache.
    public static File compressImage(Context c, Uri uri) throws IOException {
        ContentResolver cr = c.getContentResolver();

        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream in = cr.openInputStream(uri)) { BitmapFactory.decodeStream(in, null, bounds); }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw new IOException("Không đọc được ảnh");

        int sample = 1;
        while (bounds.outWidth / sample > MAX_IMAGE_SIDE * 2 || bounds.outHeight / sample > MAX_IMAGE_SIDE * 2) {
            sample *= 2;
        }
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = sample;
        Bitmap bmp;
        try (InputStream in = cr.openInputStream(uri)) { bmp = BitmapFactory.decodeStream(in, null, opts); }
        if (bmp == null) throw new IOException("Không đọc được ảnh");

        int rotation = 0;
        try (InputStream in = cr.openInputStream(uri)) {
            if (in != null) {
                int o = new ExifInterface(in).getAttributeInt(
                        ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                if (o == ExifInterface.ORIENTATION_ROTATE_90) rotation = 90;
                else if (o == ExifInterface.ORIENTATION_ROTATE_180) rotation = 180;
                else if (o == ExifInterface.ORIENTATION_ROTATE_270) rotation = 270;
            }
        } catch (Exception ignored) { }

        float scale = Math.min(1f, (float) MAX_IMAGE_SIDE / Math.max(bmp.getWidth(), bmp.getHeight()));
        if (scale < 1f || rotation != 0) {
            Matrix m = new Matrix();
            if (scale < 1f) m.postScale(scale, scale);
            if (rotation != 0) m.postRotate(rotation);
            Bitmap out = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), m, true);
            if (out != bmp) bmp.recycle();
            bmp = out;
        }

        File file = new File(c.getCacheDir(), "chat_" + System.currentTimeMillis() + ".jpg");
        try (FileOutputStream fos = new FileOutputStream(file)) {
            bmp.compress(Bitmap.CompressFormat.JPEG, 80, fos);
        } finally {
            bmp.recycle();
        }
        return file;
    }

    //Ảnh bìa của video Cloudinary: lấy khung hình đầu, rộng 500px.
    public static String videoThumbnail(String url) {
        if (url == null || !url.contains("res.cloudinary.com") || !url.contains("/video/upload/")) return url;
        String u = url.replace("/video/upload/", "/video/upload/so_0,w_500,c_limit/");
        int dot = u.lastIndexOf('.'), slash = u.lastIndexOf('/');
        if (dot > slash) u = u.substring(0, dot);
        return u + ".jpg";
    }
}