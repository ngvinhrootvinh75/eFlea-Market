package com.example.efleamarket.utils;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import com.example.efleamarket.R;
import com.example.efleamarket.repositories.NotificationRepository;
import com.google.firebase.firestore.ListenerRegistration;

public final class BadgeHelper {
    private BadgeHelper() {}

    public static void bind(AppCompatActivity activity) {
        TextView messages = activity.findViewById(R.id.tvBadgeMessages);
        TextView bell = activity.findViewById(R.id.tvBadgeBell);
        NotificationRepository repo = NotificationRepository.getInstance();

        activity.getLifecycle().addObserver(new DefaultLifecycleObserver() {
            private ListenerRegistration msgReg, bellReg;

            @Override public void onStart(@NonNull LifecycleOwner owner) {
                if (messages != null) msgReg = repo.listenUnreadMessages(callback(messages));
                if (bell != null) bellReg = repo.listenUnreadNotifications(callback(bell));
            }

            @Override public void onStop(@NonNull LifecycleOwner owner) {
                if (msgReg != null) { msgReg.remove(); msgReg = null; }
                if (bellReg != null) { bellReg.remove(); bellReg = null; }
            }
        });
    }

    private static NotificationRepository.Callback<Integer> callback(TextView badge) {
        return new NotificationRepository.Callback<Integer>() {
            @Override public void onSuccess(Integer n) { show(badge, n == null ? 0 : n); }
            @Override public void onError(String message) { show(badge, 0); }
        };
    }

    private static void show(TextView badge, int n) {
        badge.setVisibility(n > 0 ? View.VISIBLE : View.GONE);
        badge.setText(n > 99 ? "99+" : String.valueOf(n));
    }
}