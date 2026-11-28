package com.example.efleamarket.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.efleamarket.models.Chat;
import com.example.efleamarket.models.MeetingPlace;
import com.example.efleamarket.models.ShippingAddress;
import com.example.efleamarket.models.Transaction;
import com.example.efleamarket.repositories.ChatRepository;
import com.example.efleamarket.repositories.TransactionRepository;
import com.google.firebase.firestore.ListenerRegistration;
import com.example.efleamarket.models.TrackingEvent;
import java.util.List;
import java.util.Date;

public class CreateTransactionViewModel extends ViewModel {
    private interface Action {
        void run(Chat c, Transaction t);
    }

    private final ChatRepository chatRepo = ChatRepository.getInstance();
    private final TransactionRepository txRepo = TransactionRepository.getInstance();

    private final MutableLiveData<Chat> chat = new MutableLiveData<>();
    private final MutableLiveData<Transaction> transaction = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(true);
    private final MutableLiveData<Boolean> submitting = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<String> info = new MutableLiveData<>();
    private final MutableLiveData<Boolean> done = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> closeScreen = new MutableLiveData<>(false);
    private final MutableLiveData<List<TrackingEvent>> tracking = new MutableLiveData<>();
    private final MutableLiveData<String[]> myContact = new MutableLiveData<>();
    private ListenerRegistration trackReg;
    private boolean started;
    private boolean chatLoaded, txLoaded;
    private ListenerRegistration chatReg, txReg;

    public LiveData<Chat> getChat() { return chat; }
    public LiveData<Transaction> getTransaction() { return transaction; }
    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<Boolean> getSubmitting() { return submitting; }
    public LiveData<String> getError() { return error; }
    public LiveData<String> getInfo() { return info; }
    public LiveData<Boolean> getDone() { return done; }
    public LiveData<Boolean> getCloseScreen() { return closeScreen; }
    public LiveData<List<TrackingEvent>> getTracking() { return tracking; }
    public LiveData<String[]> getMyContact() { return myContact; }
    public String myUid() { return chatRepo.currentUid(); }

    public void start(String productId, String buyerId) {
        if (started) return;
        started = true;

        String uid = myUid();
        if (uid != null && uid.equals(buyerId)) {
            chatRepo.getOrCreateChat(productId, new ChatRepository.Callback<Chat>() {
                @Override public void onSuccess(Chat c) {
                    chat.setValue(c);
                    attach(c.getId());
                }
                @Override public void onError(String message) {
                    error.setValue(message);
                    closeScreen.setValue(true);
                }
            });
        } else {
            attach(Chat.buildId(productId, buyerId));
        }
    }

    private void attach(String chatId) {
        chatReg = chatRepo.listenChat(chatId, new ChatRepository.Callback<Chat>() {
            @Override public void onSuccess(Chat c) {
                chat.setValue(c);
                chatLoaded = true;
                updateLoading();
            }
            @Override public void onError(String message) {
                chatLoaded = true;
                updateLoading();
                error.setValue(message);
            }
        });

        txReg = txRepo.listenTransaction(chatId, new TransactionRepository.Callback<Transaction>() {
            @Override public void onSuccess(Transaction t) {
                transaction.setValue(t);
                txLoaded = true;
                updateLoading();
            }
            @Override public void onError(String message) {
                txLoaded = true;
                updateLoading();
                error.setValue(message);
            }
        });

        trackReg = txRepo.listenTracking(chatId, new TransactionRepository.Callback<List<TrackingEvent>>() {
            @Override public void onSuccess(List<TrackingEvent> list) { tracking.setValue(list); }
            @Override public void onError(String message) { }
        });
    }

    private void updateLoading() {
        loading.setValue(!(chatLoaded && txLoaded));
    }

    public void submit(String priceText, String note) {
        if (Boolean.TRUE.equals(submitting.getValue())) return;

        Chat c = chat.getValue();
        if (c == null) { error.setValue("Cuộc trò chuyện chưa sẵn sàng"); return; }

        Transaction t = transaction.getValue();
        if (t != null && !t.canReOffer(myUid())) {
            error.setValue("Cuộc trò chuyện này đã có giao dịch đang diễn ra");
            return;
        }

        double price = parsePrice(priceText);
        if (price <= 0) { error.setValue("Vui lòng nhập giá đề nghị"); return; }
        if (price > TransactionRepository.MAX_PRICE) { error.setValue("Giá đề nghị quá lớn"); return; }

        submitting.setValue(true);
        TransactionRepository.Callback<Void> cb = new TransactionRepository.Callback<Void>() {
            @Override public void onSuccess(Void v) {
                submitting.setValue(false);
                done.setValue(true);
            }
            @Override public void onError(String message) {
                submitting.setValue(false);
                error.setValue("Gửi đề nghị thất bại: " + message);
            }
        };

        if (t == null) txRepo.createOffer(c, price, note, cb);
        else txRepo.reOffer(c, t, price, note, cb);
    }

    private void run(Action action) {
        Chat c = chat.getValue();
        Transaction t = transaction.getValue();
        if (c == null || t == null || Boolean.TRUE.equals(submitting.getValue())) return;
        submitting.setValue(true);
        action.run(c, t);
    }

    public void accept() {
        run((c, t) -> txRepo.accept(c, t, actionCallback("Đã chấp nhận đề nghị")));
    }

    public void reject() {
        run((c, t) -> txRepo.reject(c, t, actionCallback("Đã từ chối đề nghị")));
    }

    public void cancel() {
        run((c, t) -> {
            boolean offered = Transaction.STATUS_OFFERED.equals(t.getStatus());
            txRepo.cancel(c, t, actionCallback(offered ? "Đã rút lại đề nghị" : "Đã hủy giao dịch"));
        });
    }

    public void scheduleMeeting(MeetingPlace place, Date time) {
        run((c, t) -> {
            boolean reschedule = Transaction.STATUS_SCHEDULED.equals(t.getStatus());
            txRepo.scheduleMeeting(c, t, place, time, actionCallback(reschedule ? "Đã đổi điểm hẹn" : "Đã hẹn gặp"));
        });
    }

    public void requestShipping(ShippingAddress address) {
        run((c, t) -> txRepo.requestShipping(c, t, address, actionCallback("Đã chọn giao tận nơi")));
    }

    public void submitTracking(String carrier, String code) {
        run((c, t) -> txRepo.submitTracking(c, t, carrier, code, actionCallback("Đã cập nhật mã vận đơn")));
    }

    public void markDelivering() {
        run((c, t) -> txRepo.markDelivering(c, t, actionCallback("Đã cập nhật: đang giao hàng")));
    }

    public void confirmReceived() {
        run((c, t) -> txRepo.confirmReceived(c, t, actionCallback("Đã xác nhận nhận hàng")));
    }

    public void addTracking(String text) {
        run((c, t) -> txRepo.addTracking(c, t, text, actionCallback("Đã cập nhật hành trình")));
    }

    public void loadMyContact() {
        if (myContact.getValue() != null) return;
        txRepo.loadMyContact(new TransactionRepository.Callback<String[]>() {
            @Override public void onSuccess(String[] data) { myContact.setValue(data); }
            @Override public void onError(String message) { error.setValue(message); }
        });
    }

    private TransactionRepository.Callback<Void> actionCallback(String successMessage) {
        return new TransactionRepository.Callback<Void>() {
            @Override public void onSuccess(Void v) {
                submitting.setValue(false);
                info.setValue(successMessage);
            }
            @Override public void onError(String message) {
                submitting.setValue(false);
                error.setValue("Thao tác thất bại: " + message);
            }
        };
    }

    public static double parsePrice(String text) {
        if (text == null) return 0;
        String digits = text.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) return 0;
        if (digits.length() > 12) return TransactionRepository.MAX_PRICE + 1;
        return Double.parseDouble(digits);
    }

    public void clearError() { error.setValue(null); }

    public void clearInfo() { info.setValue(null); }

    @Override protected void onCleared() {
        if (chatReg != null) chatReg.remove();
        if (txReg != null) txReg.remove();
        if (trackReg != null) trackReg.remove();
    }
}