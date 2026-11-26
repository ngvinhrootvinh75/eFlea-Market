package com.example.efleamarket.repositories;

import com.example.efleamarket.models.Chat;
import com.example.efleamarket.models.Message;
import com.example.efleamarket.models.Transaction;
import com.example.efleamarket.utils.ChatFormat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.WriteBatch;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class TransactionRepository {

    public interface Callback<T> {
        void onSuccess(T data);
        void onError(String message);
    }

    private interface BatchStep {
        void apply(WriteBatch batch);
    }

    private static final String COL_TRANSACTIONS = "transactions";
    private static final String COL_CHATS = "chats";
    private static final String COL_MESSAGES = "messages";
    public static final double MAX_PRICE = 10_000_000_000d;
    public static final int MAX_NOTE = 300;

    private static TransactionRepository instance;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private TransactionRepository() {}

    public static synchronized TransactionRepository getInstance() {
        if (instance == null) instance = new TransactionRepository();
        return instance;
    }

    private String myUid() { return FirebaseAuth.getInstance().getUid(); }

    private boolean signedOut() { return FirebaseAuth.getInstance().getCurrentUser() == null; }

    public ListenerRegistration listenTransaction(String transactionId, Callback<Transaction> cb) {
        return db.collection(COL_TRANSACTIONS).document(transactionId).addSnapshotListener((snap, e) -> {
            if (e != null) { if (!signedOut()) cb.onError(e.getMessage()); return; }
            if (snap != null && snap.exists()) {
                cb.onSuccess(snap.toObject(Transaction.class, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE));
            } else {
                cb.onSuccess(null);
            }
        });
    }

    public void createOffer(Chat chat, double price, String rawNote, Callback<Void> cb) {
        String uid = myUid();
        String err = checkChat(chat, uid);
        if (err != null) { cb.onError(err); return; }
        if (!validPrice(price)) { cb.onError("Giá đề nghị không hợp lệ"); return; }
        String note = cleanNote(rawNote);

        Map<String, Object> tx = new HashMap<>();
        tx.put("chatId", chat.getId());
        tx.put("productId", chat.getProductId());
        tx.put("productTitle", chat.getProductTitle() == null ? "" : chat.getProductTitle());
        tx.put("productImage", chat.getProductImage() == null ? "" : chat.getProductImage());
        tx.put("listPrice", chat.getProductPrice());
        tx.put("offeredPrice", price);
        tx.put("note", note);
        tx.put("buyerId", chat.getBuyerId());
        tx.put("sellerId", chat.getSellerId());
        tx.put("participants", Arrays.asList(chat.getBuyerId(), chat.getSellerId()));
        tx.put("offeredBy", uid);
        tx.put("lastActionBy", uid);
        tx.put("status", Transaction.STATUS_OFFERED);
        tx.put("createdAt", FieldValue.serverTimestamp());
        tx.put("updatedAt", FieldValue.serverTimestamp());

        String text = offerText(" Đề nghị giao dịch: ", price, note);
        commit(chat, uid, text, batch -> batch.set(txRef(chat), tx), cb);
    }

    public void reOffer(Chat chat, Transaction t, double price, String rawNote, Callback<Void> cb) {
        String uid = myUid();
        String err = checkChat(chat, uid);
        if (err != null) { cb.onError(err); return; }
        if (t == null || !t.canReOffer(uid)) { cb.onError("Không thể đề nghị lại lúc này"); return; }
        if (!validPrice(price)) { cb.onError("Giá đề nghị không hợp lệ"); return; }
        String note = cleanNote(rawNote);

        Map<String, Object> upd = new HashMap<>();
        upd.put("status", Transaction.STATUS_OFFERED);
        upd.put("offeredPrice", price);
        upd.put("note", note);
        upd.put("offeredBy", uid);
        upd.put("lastActionBy", uid);
        upd.put("updatedAt", FieldValue.serverTimestamp());

        String text = offerText(" Đề nghị giá mới: ", price, note);
        commit(chat, uid, text, batch -> batch.update(txRef(chat), upd), cb);
    }

    public void accept(Chat chat, Transaction t, Callback<Void> cb) {
        String uid = myUid();
        String err = checkChat(chat, uid);
        if (err != null) { cb.onError(err); return; }
        if (t == null || !t.canRespond(uid)) { cb.onError("Bạn không thể chấp nhận đề nghị này"); return; }
        changeStatus(chat, uid, Transaction.STATUS_ACCEPTED,
                " Đã chấp nhận đề nghị " + ChatFormat.price(t.getOfferedPrice()), cb);
    }

    public void reject(Chat chat, Transaction t, Callback<Void> cb) {
        String uid = myUid();
        String err = checkChat(chat, uid);
        if (err != null) { cb.onError(err); return; }
        if (t == null || !t.canRespond(uid)) { cb.onError("Bạn không thể từ chối đề nghị này"); return; }
        changeStatus(chat, uid, Transaction.STATUS_REJECTED,
                " Đã từ chối đề nghị " + ChatFormat.price(t.getOfferedPrice()), cb);
    }

    public void cancel(Chat chat, Transaction t, Callback<Void> cb) {
        String uid = myUid();
        String err = checkChat(chat, uid);
        if (err != null) { cb.onError(err); return; }
        if (t == null || !t.canCancel(uid)) { cb.onError("Không thể hủy giao dịch lúc này"); return; }
        String text = Transaction.STATUS_OFFERED.equals(t.getStatus())
                ? "↩️ Đã rút lại đề nghị " + ChatFormat.price(t.getOfferedPrice())
                : " Đã hủy giao dịch " + ChatFormat.price(t.getOfferedPrice());
        changeStatus(chat, uid, Transaction.STATUS_CANCELLED, text, cb);
    }

    private void changeStatus(Chat chat, String uid, String status, String text, Callback<Void> cb) {
        Map<String, Object> upd = new HashMap<>();
        upd.put("status", status);
        upd.put("lastActionBy", uid);
        upd.put("updatedAt", FieldValue.serverTimestamp());
        commit(chat, uid, text, batch -> batch.update(txRef(chat), upd), cb);
    }

    private void commit(Chat chat, String uid, String text, BatchStep step, Callback<Void> cb) {
        DocumentReference chatRef = db.collection(COL_CHATS).document(chat.getId());
        DocumentReference msgRef = chatRef.collection(COL_MESSAGES).document();

        Map<String, Object> msg = new HashMap<>();
        msg.put("senderId", uid);
        msg.put("text", text);
        msg.put("type", Message.TYPE_TEXT);
        msg.put("createdAt", FieldValue.serverTimestamp());

        Map<String, Object> chatUpd = new HashMap<>();
        chatUpd.put("lastMessage", text.length() > 100 ? text.substring(0, 100) + "…" : text);
        chatUpd.put("lastSenderId", uid);
        chatUpd.put("updatedAt", FieldValue.serverTimestamp());
        String other = chat.getOtherId(uid);
        if (other != null && !other.isEmpty()) {
            chatUpd.put("unread." + other, FieldValue.increment(1));
        }

        WriteBatch batch = db.batch();
        step.apply(batch);
        batch.set(msgRef, msg);
        batch.update(chatRef, chatUpd);
        batch.commit()
                .addOnSuccessListener(v -> cb.onSuccess(null))
                .addOnFailureListener(e -> cb.onError(e.getMessage()));
    }

    private DocumentReference txRef(Chat chat) {
        return db.collection(COL_TRANSACTIONS).document(chat.getId());
    }

    private static String checkChat(Chat chat, String uid) {
        if (uid == null) return "Bạn chưa đăng nhập";
        if (chat == null || chat.getId() == null) return "Cuộc trò chuyện chưa sẵn sàng";
        if (chat.getParticipants() == null || !chat.getParticipants().contains(uid)) {
            return "Bạn không thuộc cuộc trò chuyện này";
        }
        return null;
    }

    private static boolean validPrice(double price) {
        return price > 0 && price <= MAX_PRICE;
    }

    private static String cleanNote(String rawNote) {
        String note = rawNote == null ? "" : rawNote.trim();
        return note.length() > MAX_NOTE ? note.substring(0, MAX_NOTE) : note;
    }

    private static String offerText(String prefix, double price, String note) {
        return prefix + ChatFormat.price(price) + (note.isEmpty() ? "" : "\nGhi chú: " + note);
    }
}