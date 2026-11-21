package com.example.efleamarket.repositories;

import android.content.Context;
import android.net.Uri;

import com.example.efleamarket.utils.CloudinaryHelper;


public class UserRepository {

    public interface Callback<T> {
        void onSuccess(T data);
        void onError(String message);
    }

    private static UserRepository instance;

    private UserRepository() {}

    public static synchronized UserRepository getInstance() {
        if (instance == null) instance = new UserRepository();
        return instance;
    }

    public void uploadAvatar(Context context, Uri uri, Callback<String> cb) {
        CloudinaryHelper.uploadImage(context, uri, CloudinaryHelper.PRESET_AVATAR,
                new CloudinaryHelper.UploadListener() {
                    @Override
                    public void onSuccess(String url) {
                        cb.onSuccess(url);
                    }

                    @Override
                    public void onError(String message) {
                        cb.onError(message);
                    }
                });
    }
}