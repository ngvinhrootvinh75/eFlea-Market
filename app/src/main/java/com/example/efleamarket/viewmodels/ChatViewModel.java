package com.example.efleamarket.viewmodels;

import android.content.Context;
import android.net.Uri;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.efleamarket.models.Chat;
import com.example.efleamarket.models.Message;
import com.example.efleamarket.repositories.ChatRepository;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import com.example.efleamarket.models.Transaction;
import com.example.efleamarket.repositories.TransactionRepository;

public class ChatViewModel extends ViewModel {
    private final ChatRepository repo = ChatRepository.getInstance();
    private final TransactionRepository txRepo = TransactionRepository.getInstance();
    private final MutableLiveData<Transaction> transaction = new MutableLiveData<>();
    private ListenerRegistration txReg;
    private final MutableLiveData<Chat> chat = new MutableLiveData<>();
    private final MutableLiveData<List<Message>> messages = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> closeScreen = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> uploading = new MutableLiveData<>(false);
    private final MutableLiveData<List<String>> suggestions = new MutableLiveData<>();

    private String chatId;
    private boolean started;
    private boolean visible;
    private List<Message> rawMessages = new ArrayList<>();
    private ListenerRegistration chatReg, msgReg;

    public LiveData<Chat> getChat() { return chat; }
    public LiveData<List<Message>> getMessages() { return messages; }
    public LiveData<String> getError() { return error; }
    public LiveData<Boolean> getCloseScreen() { return closeScreen; }
    public LiveData<Boolean> getUploading() { return uploading; }
    public LiveData<List<String>> getSuggestions() { return suggestions; }
    public LiveData<Transaction> getTransaction() { return transaction; }
    public void openExisting(String id) {
        if (started) return;
        started = true;
        attach(id);
    }

    public void openForProduct(String productId) {
        if (started) return;
        started = true;
        repo.getOrCreateChat(productId, new ChatRepository.Callback<Chat>() {
            @Override public void onSuccess(Chat c) { chat.setValue(c); attach(c.getId()); }
            @Override public void onError(String message) { error.setValue(message); closeScreen.setValue(true); }
        });
    }

    private void attach(String id) {
        chatId = id;
        chatReg = repo.listenChat(id, new ChatRepository.Callback<Chat>() {
            @Override public void onSuccess(Chat c) {
                chat.setValue(c);
                publishMessages();
                if (visible) repo.markRead(c);
            }
            @Override public void onError(String message) { error.setValue(message); }
        });
        msgReg = repo.listenMessages(id, new ChatRepository.Callback<List<Message>>() {
            @Override public void onSuccess(List<Message> list) {
                rawMessages = list;
                publishMessages();
            }
            @Override public void onError(String message) { error.setValue(message); }
        });
        txReg = txRepo.listenTransaction(id, new TransactionRepository.Callback<Transaction>() {
            @Override public void onSuccess(Transaction t) { transaction.setValue(t); }
            @Override public void onError(String message) { }
        });
    }

    private void publishMessages() {
        Chat c = chat.getValue();
        Date cutoff = c == null ? null : c.getDeletedAtFor(repo.currentUid());
        if (cutoff == null) { messages.setValue(rawMessages); return; }
        List<Message> out = new ArrayList<>();
        for (Message m : rawMessages) {
            if (m.getCreatedAt() == null || m.getCreatedAt().after(cutoff)) out.add(m);
        }
        messages.setValue(out);
    }

    public void setVisible(boolean v) {
        visible = v;
        if (v) repo.markRead(chat.getValue());
    }

    public void sendMessage(String text) {
        Chat c = chat.getValue();
        if (c == null) return;
        repo.sendMessage(c, text, new ChatRepository.Callback<Void>() {
            @Override public void onSuccess(Void v) {}
            @Override public void onError(String message) { error.setValue("Gửi thất bại: " + message); }
        });
    }

    public void sendMedia(Context ctx, Uri uri) {
        Chat c = chat.getValue();
        if (c == null || Boolean.TRUE.equals(uploading.getValue())) return;
        uploading.setValue(true);
        repo.sendMedia(ctx, c, uri, new ChatRepository.Callback<Void>() {
            @Override public void onSuccess(Void v) { uploading.setValue(false); }
            @Override public void onError(String message) {
                uploading.setValue(false);
                error.setValue("Gửi tệp thất bại: " + message);
            }
        });
    }

    public void clearError() { error.setValue(null); }

    @Override protected void onCleared() {
        if (chatReg != null) chatReg.remove();
        if (msgReg != null) msgReg.remove();
        if (txReg != null) txReg.remove();
    }
}