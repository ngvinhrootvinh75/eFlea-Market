package com.example.efleamarket.repositories;

import com.example.efleamarket.models.Chat;
import com.example.efleamarket.models.MeetingPlace;
import com.example.efleamarket.models.Message;
import com.example.efleamarket.models.Product;
import com.example.efleamarket.models.ShippingAddress;
import com.example.efleamarket.models.TrackingEvent;
import com.example.efleamarket.models.Transaction;
import com.example.efleamarket.utils.ChatFormat;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
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
    private static final String COL_TRACKING = "tracking";
    private static final String COL_PRODUCTS = "products";
    private static final String COL_USERS = "users";
    public static final double MAX_PRICE = 10_000_000_000d;
    public static final int MAX_NOTE = 300;
    public static final int MAX_PLACE_NAME = 100;
    public static final int MAX_SHIP_NAME = 60;
    public static final int MAX_ADDRESS = 200;
    public static final int MAX_CARRIER = 30;
    public static final int MAX_TRACKING = 200;
    private static final String PHONE_REGEX = "^0[0-9]{9,10}$";
    private static final String CODE_REGEX = "^[A-Za-z0-9-]{0,40}$";

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

    public ListenerRegistration listenTracking(String transactionId, Callback<List<TrackingEvent>> cb) {
        return db.collection(COL_TRANSACTIONS).document(transactionId).collection(COL_TRACKING)
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) { if (!signedOut()) cb.onError(e.getMessage()); return; }
                    List<TrackingEvent> list = new ArrayList<>();
                    if (snap != null) {
                        for (DocumentSnapshot d : snap.getDocuments()) {
                            TrackingEvent ev = d.toObject(TrackingEvent.class, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE);
                            if (ev != null) list.add(ev);
                        }
                    }
                    cb.onSuccess(list);
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

        String text = offerText("Đề nghị giao dịch: ", price, note);
        commit(chat, uid, text, batch -> batch.set(txRef(chat), tx), cb);
    }

    public void reOffer(Chat chat, Transaction t, double price, String rawNote, Callback<Void> cb) {
        String uid = myUid();
        String err = checkChat(chat, uid);
        if (err != null) { cb.onError(err); return; }
        if (t == null || !t.canReOffer(uid)) { cb.onError("Không thể đề nghị lại lúc này"); return; }
        if (!validPrice(price)) { cb.onError("Giá đề nghị không hợp lệ"); return; }
        String note = cleanNote(rawNote);

        Map<String, Object> upd = base(uid);
        upd.put("status", Transaction.STATUS_OFFERED);
        upd.put("offeredPrice", price);
        upd.put("note", note);
        upd.put("offeredBy", uid);

        String text = offerText("Đề nghị giá mới: ", price, note);
        commit(chat, uid, text, batch -> batch.update(txRef(chat), upd), cb);
    }

    public void accept(Chat chat, Transaction t, Callback<Void> cb) {
        String uid = myUid();
        String err = checkChat(chat, uid);
        if (err != null) { cb.onError(err); return; }
        if (t == null || !t.canRespond(uid)) { cb.onError("Bạn không thể chấp nhận đề nghị này"); return; }
        changeStatus(chat, uid, Transaction.STATUS_ACCEPTED,
                "Đã chấp nhận đề nghị " + ChatFormat.price(t.getOfferedPrice()), cb);
    }

    public void reject(Chat chat, Transaction t, Callback<Void> cb) {
        String uid = myUid();
        String err = checkChat(chat, uid);
        if (err != null) { cb.onError(err); return; }
        if (t == null || !t.canRespond(uid)) { cb.onError("Bạn không thể từ chối đề nghị này"); return; }
        changeStatus(chat, uid, Transaction.STATUS_REJECTED,
                "Đã từ chối đề nghị " + ChatFormat.price(t.getOfferedPrice()), cb);
    }

    public void cancel(Chat chat, Transaction t, Callback<Void> cb) {
        String uid = myUid();
        String err = checkChat(chat, uid);
        if (err != null) { cb.onError(err); return; }
        if (t == null || !t.canCancel(uid)) { cb.onError("Không thể hủy giao dịch lúc này"); return; }
        String text = Transaction.STATUS_OFFERED.equals(t.getStatus())
                ? "Đã rút lại đề nghị " + ChatFormat.price(t.getOfferedPrice())
                : "Đã hủy giao dịch " + ChatFormat.price(t.getOfferedPrice());
        changeStatus(chat, uid, Transaction.STATUS_CANCELLED, text, cb);
    }

    public void scheduleMeeting(Chat chat, Transaction t, MeetingPlace place, Date time, Callback<Void> cb) {
        String uid = myUid();
        String err = checkChat(chat, uid);
        if (err != null) { cb.onError(err); return; }
        if (t == null || !t.canSchedule(uid)) { cb.onError("Chỉ hẹn gặp được sau khi đã chấp nhận giá"); return; }
        if (place == null || place.getName() == null || place.getName().trim().isEmpty()) {
            cb.onError("Vui lòng đặt tên điểm hẹn");
            return;
        }
        if (time == null || time.getTime() <= System.currentTimeMillis()) {
            cb.onError("Giờ hẹn phải ở tương lai");
            return;
        }

        String name = cut(place.getName().trim(), MAX_PLACE_NAME);
        MeetingPlace clean = new MeetingPlace(name, place.getLat(), place.getLng(), place.getPointId());
        boolean reschedule = Transaction.STATUS_SCHEDULED.equals(t.getStatus());

        Map<String, Object> upd = base(uid);
        upd.put("status", Transaction.STATUS_SCHEDULED);
        upd.put("deliveryMethod", Transaction.DELIVERY_MEETUP);
        upd.put("meetingPlace", clean.toMap());
        upd.put("meetingTime", new Timestamp(time));

        String text = (reschedule ? "Đổi điểm hẹn: " : "Hẹn gặp tại ") + name
                + " lúc " + ChatFormat.dateTime(time);
        commit(chat, uid, text, batch -> batch.update(txRef(chat), upd), cb);
    }

    public void requestShipping(Chat chat, Transaction t, ShippingAddress a, Callback<Void> cb) {
        String uid = myUid();
        String err = checkChat(chat, uid);
        if (err != null) { cb.onError(err); return; }
        if (t == null || !t.canRequestShipping(uid)) { cb.onError("Chỉ người mua chọn giao tận nơi sau khi đã chấp nhận giá"); return; }

        String name = a == null || a.getName() == null ? "" : a.getName().trim();
        String phone = a == null || a.getPhone() == null ? "" : a.getPhone().replaceAll("[^0-9]", "");
        String address = a == null || a.getAddress() == null ? "" : a.getAddress().trim();
        if (name.isEmpty()) { cb.onError("Tài khoản chưa có tên, hãy cập nhật trong Hồ sơ"); return; }
        if (!phone.matches(PHONE_REGEX)) { cb.onError("Số điện thoại không hợp lệ"); return; }
        if (address.length() < 5 || (a.getLat() == 0 && a.getLng() == 0)) { cb.onError("Vui lòng chọn địa chỉ trên bản đồ"); return; }

        ShippingAddress clean = new ShippingAddress(cut(name, MAX_SHIP_NAME), phone,
                cut(address, MAX_ADDRESS), a.getLat(), a.getLng());

        Map<String, Object> upd = base(uid);
        upd.put("status", Transaction.STATUS_SHIPPING);
        upd.put("deliveryMethod", Transaction.DELIVERY_SHIPPING);
        upd.put("shippingAddress", clean.toMap());
        upd.put("shipStep", Transaction.SHIP_WAITING);
        upd.put("shipRequestedAt", FieldValue.serverTimestamp());

        String text = "Người mua chọn giao tận nơi tới: " + clean.getAddress();
        commit(chat, uid, text, batch -> batch.update(txRef(chat), upd), cb);
    }

    public void submitTracking(Chat chat, Transaction t, String rawCarrier, String rawCode, Callback<Void> cb) {
        String uid = myUid();
        String err = checkChat(chat, uid);
        if (err != null) { cb.onError(err); return; }
        if (t == null || !t.canEnterTracking(uid)) { cb.onError("Chỉ người bán xác nhận đã gửi hàng"); return; }

        String carrier = rawCarrier == null ? "" : rawCarrier.trim();
        String code = rawCode == null ? "" : rawCode.trim().toUpperCase();
        if (carrier.isEmpty()) { cb.onError("Vui lòng chọn đơn vị vận chuyển"); return; }
        if (!code.matches(CODE_REGEX)) { cb.onError("Mã vận đơn chỉ gồm chữ, số hoặc dấu gạch"); return; }
        carrier = cut(carrier, MAX_CARRIER);

        Map<String, Object> upd = base(uid);
        upd.put("shipStep", Transaction.SHIP_SHIPPED);
        upd.put("carrier", carrier);
        upd.put("trackingCode", code);
        upd.put("shippedAt", FieldValue.serverTimestamp());

        String text = "Đã gửi hàng qua " + carrier + (code.isEmpty() ? "" : "\nMã vận đơn: " + code);
        commit(chat, uid, text, batch -> batch.update(txRef(chat), upd), cb);
    }

    public void addTracking(Chat chat, Transaction t, String rawText, Callback<Void> cb) {
        String uid = myUid();
        String err = checkChat(chat, uid);
        if (err != null) { cb.onError(err); return; }
        if (t == null || !t.canAddTracking(uid)) { cb.onError("Chỉ người bán cập nhật hành trình sau khi đã gửi hàng"); return; }
        String text = rawText == null ? "" : rawText.trim();
        if (text.isEmpty()) { cb.onError("Vui lòng nhập vị trí hoặc trạng thái đơn"); return; }
        String clean = cut(text, MAX_TRACKING);

        DocumentReference ref = txRef(chat).collection(COL_TRACKING).document();
        Map<String, Object> ev = new HashMap<>();
        ev.put("text", clean);
        ev.put("by", uid);
        ev.put("createdAt", FieldValue.serverTimestamp());

        commit(chat, uid, "Hành trình đơn hàng: " + clean, batch -> batch.set(ref, ev), cb);
    }

    public void markDelivering(Chat chat, Transaction t, Callback<Void> cb) {
        String uid = myUid();
        String err = checkChat(chat, uid);
        if (err != null) { cb.onError(err); return; }
        if (t == null || !t.canMarkDelivering(uid)) { cb.onError("Không thể cập nhật lúc này"); return; }

        Map<String, Object> upd = base(uid);
        upd.put("shipStep", Transaction.SHIP_DELIVERING);
        upd.put("deliveringAt", FieldValue.serverTimestamp());

        commit(chat, uid, "Đơn hàng đang được giao", batch -> batch.update(txRef(chat), upd), cb);
    }

    public void confirmReceived(Chat chat, Transaction t, Callback<Void> cb) {
        String uid = myUid();
        String err = checkChat(chat, uid);
        if (err != null) { cb.onError(err); return; }
        if (t == null || !t.canConfirmReceived(uid)) { cb.onError("Chỉ người mua xác nhận đã nhận hàng"); return; }

        Map<String, Object> upd = base(uid);
        upd.put("status", Transaction.STATUS_COMPLETED);
        upd.put("shipStep", Transaction.SHIP_RECEIVED);
        upd.put("receivedAt", FieldValue.serverTimestamp());

        DocumentReference productRef = db.collection(COL_PRODUCTS).document(t.getProductId());
        commit(chat, uid, "Đã nhận được hàng. Giao dịch hoàn tất", batch -> {
            batch.update(txRef(chat), upd);
            batch.update(productRef, "status", Product.STATUS_SOLD);
        }, cb);
    }

    public void loadMyContact(Callback<String[]> cb) {
        String uid = myUid();
        if (uid == null) { cb.onError("Bạn chưa đăng nhập"); return; }
        db.collection(COL_USERS).document(uid).get()
                .addOnSuccessListener(doc -> {
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    String name = firstNonEmpty(doc.getString("fullName"), doc.getString("name"),
                            doc.getString("username"), user == null ? null : user.getDisplayName());
                    String phone = doc.getString("phone");
                    phone = phone == null ? "" : phone.replaceAll("[^0-9+]", "");
                    if (phone.startsWith("+84")) phone = "0" + phone.substring(3);
                    cb.onSuccess(new String[]{name, phone});
                })
                .addOnFailureListener(e -> cb.onError(e.getMessage()));
    }

    private void changeStatus(Chat chat, String uid, String status, String text, Callback<Void> cb) {
        Map<String, Object> upd = base(uid);
        upd.put("status", status);
        commit(chat, uid, text, batch -> batch.update(txRef(chat), upd), cb);
    }

    private Map<String, Object> base(String uid) {
        Map<String, Object> upd = new HashMap<>();
        upd.put("lastActionBy", uid);
        upd.put("updatedAt", FieldValue.serverTimestamp());
        return upd;
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
        DocumentReference notifRef = null;
        Map<String, Object> notif = null;
        if (other != null && !other.isEmpty()) {
            chatUpd.put("unread." + other, FieldValue.increment(1));
            String product = chat.getProductTitle() == null ? "" : chat.getProductTitle();
            notifRef = db.collection(COL_USERS).document(other).collection("notifications").document();
            notif = new HashMap<>();
            notif.put("type", "transaction");
            notif.put("title", cut("Giao dịch: " + product, 120));
            notif.put("body", cut(text, 300));
            notif.put("chatId", chat.getId());
            notif.put("fromUid", uid);
            notif.put("read", false);
            notif.put("createdAt", FieldValue.serverTimestamp());
        }

        WriteBatch batch = db.batch();
        step.apply(batch);
        batch.set(msgRef, msg);
        batch.update(chatRef, chatUpd);
        if (notifRef != null) batch.set(notifRef, notif);
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
        return cut(rawNote == null ? "" : rawNote.trim(), MAX_NOTE);
    }

    private static String cut(String s, int max) {
        return s.length() > max ? s.substring(0, max) : s;
    }

    private static String firstNonEmpty(String... values) {
        for (String v : values) if (v != null && !v.trim().isEmpty()) return v.trim();
        return "";
    }

    private static String offerText(String prefix, double price, String note) {
        return prefix + ChatFormat.price(price) + (note.isEmpty() ? "" : "\nGhi chú: " + note);
    }
}