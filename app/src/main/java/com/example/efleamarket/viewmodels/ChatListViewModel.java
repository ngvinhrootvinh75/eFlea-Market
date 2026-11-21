package com.example.efleamarket.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.efleamarket.models.Chat;
import com.example.efleamarket.repositories.ChatRepository;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;

public class ChatListViewModel extends ViewModel {
    private final ChatRepository repo = ChatRepository.getInstance();
    private final MutableLiveData<List<Chat>> chats = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(true);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private ListenerRegistration reg;

    public LiveData<List<Chat>> getChats() { return chats; }
    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<String> getError() { return error; }

    public void start() {
        if (reg != null) return;
        reg = repo.listenMyChats(new ChatRepository.Callback<List<Chat>>() {
            @Override public void onSuccess(List<Chat> data) { loading.setValue(false); chats.setValue(data); }
            @Override public void onError(String message) { loading.setValue(false); error.setValue(message); }
        });
    }

    public void clearError() { error.setValue(null); }
    public void deleteChat(Chat c) {
        repo.deleteChatForMe(c, new ChatRepository.Callback<Void>() {
            @Override public void onSuccess(Void v) { /* listener tự cập nhật danh sách */ }
            @Override public void onError(String message) { error.setValue("Xóa thất bại: " + message); }
        });
    }

    @Override protected void onCleared() { if (reg != null) reg.remove(); }
}