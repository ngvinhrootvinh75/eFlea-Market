package com.example.efleamarket.repositories;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import com.example.efleamarket.models.Chat;
import com.example.efleamarket.models.Message;
import com.example.efleamarket.models.Product;
import com.example.efleamarket.utils.CloudinaryHelper;
import com.example.efleamarket.utils.MediaUtils;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatRepository {

    public interface Callback<T> {
        void onSuccess(T data);
        void onError(String message);
    }

    private static final String COL_CHATS = "chats";
    private static final String COL_MESSAGES = "messages";
    private static final int MAX_TEXT = 1000;
    private static final int MESSAGE_LIMIT = 100;

    private static ChatRepository instance;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());

    private ChatRepository() {}

    public static synchronized ChatRepository getInstance() {
        if (instance == null) instance = new ChatRepository();
        return instance;
    }

    private String myUid() { return FirebaseAuth.getInstance().getUid(); }

    public String currentUid() { return myUid(); }

    private boolean signedOut() { return FirebaseAuth.getInstance().getCurrentUser() == null; }


    public void getOrCreateChat(String productId, Callback<Chat> cb) {
        String uid = myUid();
        if (uid == null) { cb.onError("Bạn chưa đăng nhập"); return; }

        DocumentReference ref = db.collection(COL_CHATS).document(Chat.buildId(productId, uid));
        ref.get().addOnSuccessListener(snap -> {
            if (snap.exists()) {
                cb.onSuccess(snap.toObject(Chat.class, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE));
                return;
            }
            db.collection("products").document(productId).get().addOnSuccessListener(pDoc -> {
                Product p = pDoc.exists() ? pDoc.toObject(Product.class) : null;
                if (p == null || p.getSellerId() == null) { cb.onError("Sản phẩm không còn tồn tại"); return; }
                String sellerId = p.getSellerId();
                if (uid.equals(sellerId)) { cb.onError("Đây là sản phẩm của bạn"); return; }

                Task<DocumentSnapshot> bTask = db.collection("users").document(uid).get();
                Task<DocumentSnapshot> sTask = db.collection("users").document(sellerId).get();
                Tasks.whenAllSuccess(bTask, sTask).addOnSuccessListener(res -> {
                    DocumentSnapshot b = (DocumentSnapshot) res.get(0);
                    DocumentSnapshot s = (DocumentSnapshot) res.get(1);

                    Map<String, Object> data = new HashMap<>();
                    data.put("productId", productId);
                    data.put("productTitle", str(p.getTitle()));
                    data.put("productImage", str(p.getCoverImage()));
                    data.put("productPrice", p.getPrice());
                    data.put("buyerId", uid);
                    data.put("buyerName", str(b.getString("fullName")));
                    data.put("buyerAvatar", str(b.getString("avatarUrl")));
                    data.put("sellerId", sellerId);
                    data.put("sellerName", s.exists() ? str(s.getString("fullName")) : str(p.getSellerName()));
                    data.put("sellerAvatar", str(s.getString("avatarUrl")));
                    data.put("participants", Arrays.asList(uid, sellerId));
                    data.put("lastMessage", "");
                    data.put("lastSenderId", "");
                    Map<String, Object> unread = new HashMap<>();
                    unread.put(uid, 0);
                    unread.put(sellerId, 0);
                    data.put("unread", unread);
                    data.put("createdAt", FieldValue.serverTimestamp());
                    data.put("updatedAt", FieldValue.serverTimestamp());

                    ref.set(data)
                            .addOnSuccessListener(v -> ref.get()
                                    .addOnSuccessListener(d -> cb.onSuccess(d.toObject(Chat.class,
                                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)))
                                    .addOnFailureListener(e -> cb.onError(e.getMessage())))
                            .addOnFailureListener(e -> cb.onError(e.getMessage()));
                }).addOnFailureListener(e -> cb.onError(e.getMessage()));
            }).addOnFailureListener(e -> cb.onError(e.getMessage()));
        }).addOnFailureListener(e -> cb.onError(e.getMessage()));
    }


    public ListenerRegistration listenChat(String chatId, Callback<Chat> cb) {
        return db.collection(COL_CHATS).document(chatId).addSnapshotListener((snap, e) -> {
            if (e != null) { if (!signedOut()) cb.onError(e.getMessage()); return; }
            if (snap != null && snap.exists()) {
                cb.onSuccess(snap.toObject(Chat.class, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE));
            }
        });
    }

    public ListenerRegistration listenMyChats(Callback<List<Chat>> cb) {
        String uid = myUid();
        if (uid == null) { cb.onError("Bạn chưa đăng nhập"); return null; }
        return db.collection(COL_CHATS)
                .whereArrayContains("participants", uid)
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) { if (!signedOut()) cb.onError(e.getMessage()); return; }
                    List<Chat> list = new ArrayList<>();
                    if (snap != null) {
                        for (DocumentSnapshot d : snap.getDocuments()) {
                            Chat c = d.toObject(Chat.class, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE);
                            if (c == null) continue;
                            if (c.getLastMessage() == null || c.getLastMessage().isEmpty()) continue;
                            if (c.isHiddenFor(uid)) continue;
                            list.add(c);
                        }
                    }
                    cb.onSuccess(list);
                });
    }

    public ListenerRegistration listenMessages(String chatId, Callback<List<Message>> cb) {
        return db.collection(COL_CHATS).document(chatId).collection(COL_MESSAGES)
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .limitToLast(MESSAGE_LIMIT)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) { if (!signedOut()) cb.onError(e.getMessage()); return; }
                    List<Message> list = new ArrayList<>();
                    if (snap != null) {
                        for (DocumentSnapshot d : snap.getDocuments()) {
                            Message m = d.toObject(Message.class, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE);
                            if (m != null) list.add(m);
                        }
                    }
                    cb.onSuccess(list);
                });
    }


    public void sendMessage(Chat chat, String rawText, Callback<Void> cb) {
        String uid = myUid();
        if (uid == null) { cb.onError("Bạn chưa đăng nhập"); return; }
        if (chat == null || chat.getId() == null) { cb.onError("Cuộc trò chuyện chưa sẵn sàng"); return; }
        String text = rawText == null ? "" : rawText.trim();
        if (text.isEmpty()) return;
        if (text.length() > MAX_TEXT) text = text.substring(0, MAX_TEXT);

        Map<String, Object> msg = new HashMap<>();
        msg.put("senderId", uid);
        msg.put("text", text);
        msg.put("type", Message.TYPE_TEXT);
        msg.put("createdAt", FieldValue.serverTimestamp());

        String preview = text.length() > 100 ? text.substring(0, 100) + "…" : text;
        commitMessage(chat, uid, msg, preview, cb);
    }

    public void sendMedia(Context ctx, Chat chat, Uri uri, Callback<Void> cb) {
        String uid = myUid();
        if (uid == null) { cb.onError("Bạn chưa đăng nhập"); return; }
        if (chat == null || chat.getId() == null) { cb.onError("Cuộc trò chuyện chưa sẵn sàng"); return; }
        Context app = ctx.getApplicationContext();

        io.execute(() -> {
            try {
                boolean video = MediaUtils.isVideo(app, uri);
                Uri toUpload = uri;
                File temp = null;

                if (video) {
                    long size = MediaUtils.getSize(app, uri);
                    if (size > MediaUtils.MAX_VIDEO_BYTES) throw new Exception("Video tối đa 20 MB");
                    long dur = MediaUtils.getVideoDurationMs(app, uri);
                    if (dur > MediaUtils.MAX_VIDEO_MS) throw new Exception("Video tối đa 60 giây");
                } else {
                    temp = MediaUtils.compressImage(app, uri);
                    toUpload = Uri.fromFile(temp);
                }

                final Uri finalUri = toUpload;
                final File finalTemp = temp;
                main.post(() -> CloudinaryHelper.uploadMedia(app, finalUri, new CloudinaryHelper.UploadListener() {
                    @Override public void onSuccess(String url) {
                        if (finalTemp != null) finalTemp.delete();
                        Map<String, Object> msg = new HashMap<>();
                        msg.put("senderId", uid);
                        msg.put("text", "");
                        msg.put("type", video ? Message.TYPE_VIDEO : Message.TYPE_IMAGE);
                        msg.put("mediaUrl", url);
                        msg.put("createdAt", FieldValue.serverTimestamp());
                        commitMessage(chat, uid, msg, video ? "🎬 Video" : "📷 Hình ảnh", cb);
                    }

                    @Override public void onError(String message) {
                        if (finalTemp != null) finalTemp.delete();
                        cb.onError(message);
                    }
                }));
            } catch (Exception e) {
                String m = e.getMessage() != null ? e.getMessage() : "Không đọc được tệp";
                main.post(() -> cb.onError(m));
            }
        });
    }

    private void commitMessage(Chat chat, String uid, Map<String, Object> msg,
                               String preview, Callback<Void> cb) {
        DocumentReference chatRef = db.collection(COL_CHATS).document(chat.getId());
        DocumentReference msgRef = chatRef.collection(COL_MESSAGES).document();

        Map<String, Object> upd = new HashMap<>();
        upd.put("lastMessage", preview);
        upd.put("lastSenderId", uid);
        upd.put("updatedAt", FieldValue.serverTimestamp());
        String other = chat.getOtherId(uid);
        if (other != null && !other.isEmpty()) {
            upd.put("unread." + other, FieldValue.increment(1));
        }

        WriteBatch batch = db.batch();
        batch.set(msgRef, msg);
        batch.update(chatRef, upd);
        batch.commit()
                .addOnSuccessListener(v -> cb.onSuccess(null))
                .addOnFailureListener(e -> cb.onError(e.getMessage()));
    }

    public void markRead(Chat chat) {
        String uid = myUid();
        if (uid == null || chat == null || chat.getId() == null) return;
        if (chat.getUnreadFor(uid) <= 0) return;
        db.collection(COL_CHATS).document(chat.getId())
                .update(FieldPath.of("unread", uid), 0);
    }

    public void deleteChatForMe(Chat chat, Callback<Void> cb) {
        String uid = myUid();
        if (uid == null) { cb.onError("Bạn chưa đăng nhập"); return; }
        if (chat == null || chat.getId() == null) { cb.onError("Không tìm thấy cuộc trò chuyện"); return; }

        Map<String, Object> upd = new HashMap<>();
        upd.put("deletedAt." + uid, FieldValue.serverTimestamp());
        upd.put("unread." + uid, 0);

        db.collection(COL_CHATS).document(chat.getId())
                .update(upd)
                .addOnSuccessListener(v -> cb.onSuccess(null))
                .addOnFailureListener(e -> cb.onError(e.getMessage()));
    }


    public void getChatsWith(String otherUid, Callback<List<Chat>> cb) {
        String uid = myUid();
        if (uid == null) { cb.onError("Bạn chưa đăng nhập"); return; }
        db.collection(COL_CHATS)
                .whereArrayContains("participants", uid)
                .get()
                .addOnSuccessListener(snap -> {
                    List<Chat> list = new ArrayList<>();
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        Chat c = d.toObject(Chat.class, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE);
                        if (c != null && c.getParticipants() != null && c.getParticipants().contains(otherUid)) {
                            list.add(c);
                        }
                    }
                    list.sort((a, b) -> {
                        Date da = a.getUpdatedAt(), dbb = b.getUpdatedAt();
                        if (da == null && dbb == null) return 0;
                        if (da == null) return 1;
                        if (dbb == null) return -1;
                        return dbb.compareTo(da);
                    });
                    cb.onSuccess(list);
                })
                .addOnFailureListener(e -> cb.onError(e.getMessage()));
    }

    public void getSellingProductsOf(String sellerId, Callback<List<Product>> cb) {
        db.collection("products")
                .whereEqualTo("sellerId", sellerId)
                .whereEqualTo("status", Product.STATUS_SELLING)
                .get()
                .addOnSuccessListener(snap -> {
                    List<Product> list = new ArrayList<>();
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        Product p = d.toObject(Product.class);
                        if (p != null) list.add(p);
                    }
                    cb.onSuccess(list);
                })
                .addOnFailureListener(e -> cb.onError(e.getMessage()));
    }

    private static String str(String s) { return s == null ? "" : s; }
}