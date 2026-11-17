package com.example.efleamarket.utils;

import android.net.Uri;

import com.facebook.AccessToken;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;


public final class AvatarUtils {

    private AvatarUtils() {}

    public static String getProviderPhotoUrl(FirebaseUser user) {
        if (user == null) return null;

        for (UserInfo info : user.getProviderData()) {
            if ("google.com".equals(info.getProviderId())) {
                Uri photo = info.getPhotoUrl();
                if (photo != null) {
                    return photo.toString().replace("s96-c", "s400-c");
                }
            }
        }

        for (UserInfo info : user.getProviderData()) {
            if ("facebook.com".equals(info.getProviderId())) {
                if (info.getUid() == null || info.getUid().isEmpty()) continue;
                String url = "https://graph.facebook.com/" + info.getUid() + "/picture?type=large";
                AccessToken token = AccessToken.getCurrentAccessToken();
                if (token != null && !token.isExpired()) {
                    url += "&access_token=" + token.getToken();
                }
                return url;
            }
        }

        return null;
    }
}
