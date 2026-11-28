package com.example.efleamarket.repositories;

import com.example.efleamarket.models.AppNotification;
import com.example.efleamarket.models.Chat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;

public class NotificationRepository {

    public interface Callback<T> {
        void onSuccess(T data);
        void onError(String message);
    }

    private static NotificationRepository instance;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private NotificationRepository() {}

    public static synchronized NotificationRepository getInstance() {
        if (instance == null) instance = new NotificationRepository();
        return instance;
    }

    private String myUid() { return FirebaseAuth.getInstance().getUid(); }

    private boolean signedOut() { return FirebaseAuth.getInstance().getCurrentUser() == null; }

    private CollectionReference myNotifications(String uid) {
        return db.collection("users").document(uid).collection("notifications");
    }

    public ListenerRegistration listenUnreadMessages(Callback<Integer> cb) {
        String uid = myUid();
        if (uid == null) return null;
        return db.collection("chats").whereArrayContains("participants", uid)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) { if (!signedOut()) cb.onError(e.getMessage()); return; }
                    int total = 0;
                    if (snap != null) {
                        for (DocumentSnapshot d : snap.getDocuments()) {
                            Chat c = d.toObject(Chat.class);
                            if (c != null) total += c.getUnreadFor(uid);
                        }
                    }
                    cb.onSuccess(total);
                });
    }

    public ListenerRegistration listenUnreadNotifications(Callback<Integer> cb) {
        String uid = myUid();
        if (uid == null) return null;
        return myNotifications(uid).whereEqualTo("read", false)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) { if (!signedOut()) cb.onError(e.getMessage()); return; }
                    cb.onSuccess(snap == null ? 0 : snap.size());
                });
    }

    public ListenerRegistration listenNotifications(Callback<List<AppNotification>> cb) {
        String uid = myUid();
        if (uid == null) { cb.onError("Bạn chưa đăng nhập"); return null; }
        return myNotifications(uid).orderBy("createdAt", Query.Direction.DESCENDING).limit(50)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) { if (!signedOut()) cb.onError(e.getMessage()); return; }
                    List<AppNotification> list = new ArrayList<>();
                    if (snap != null) {
                        for (DocumentSnapshot d : snap.getDocuments()) {
                            AppNotification n = d.toObject(AppNotification.class, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE);
                            if (n != null) list.add(n);
                        }
                    }
                    cb.onSuccess(list);
                });
    }

    public void markRead(String notificationId) {
        String uid = myUid();
        if (uid == null || notificationId == null) return;
        myNotifications(uid).document(notificationId).update("read", true);
    }

    public void markAllRead(Callback<Void> cb) {
        String uid = myUid();
        if (uid == null) { cb.onError("Bạn chưa đăng nhập"); return; }
        myNotifications(uid).whereEqualTo("read", false).get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) { cb.onSuccess(null); return; }
                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot d : snap.getDocuments()) batch.update(d.getReference(), "read", true);
                    batch.commit()
                            .addOnSuccessListener(v -> cb.onSuccess(null))
                            .addOnFailureListener(err -> cb.onError(err.getMessage()));
                })
                .addOnFailureListener(e -> cb.onError(e.getMessage()));
    }
}