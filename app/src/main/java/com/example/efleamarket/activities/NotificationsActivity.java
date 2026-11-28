package com.example.efleamarket.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.efleamarket.R;
import com.example.efleamarket.adapters.NotificationAdapter;
import com.example.efleamarket.models.AppNotification;
import com.example.efleamarket.repositories.NotificationRepository;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;

public class NotificationsActivity extends AppCompatActivity {
    private final NotificationRepository repo = NotificationRepository.getInstance();
    private ListenerRegistration reg;
    private NotificationAdapter adapter;
    private TextView tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.notifications);
        applyInsets();

        tvEmpty = findViewById(R.id.tvEmpty);
        RecyclerView rv = findViewById(R.id.rvNotifications);
        adapter = new NotificationAdapter(this::openNotification);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.tvReadAll).setOnClickListener(v -> repo.markAllRead(
                new NotificationRepository.Callback<Void>() {
                    @Override public void onSuccess(Void data) { }
                    @Override public void onError(String message) {
                        Toast.makeText(NotificationsActivity.this, message, Toast.LENGTH_SHORT).show();
                    }
                }));
    }

    @Override
    protected void onStart() {
        super.onStart();
        reg = repo.listenNotifications(new NotificationRepository.Callback<List<AppNotification>>() {
            @Override public void onSuccess(List<AppNotification> list) {
                adapter.submitList(list);
                tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
            }
            @Override public void onError(String message) { }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (reg != null) { reg.remove(); reg = null; }
    }

    private void openNotification(AppNotification n) {
        if (!n.isRead()) repo.markRead(n.getId());
        if (n.getChatId() != null && !n.getChatId().isEmpty()) ChatActivity.openChat(this, n.getChatId());
    }

    private void applyInsets() {
        View root = findViewById(R.id.rootNotifications);
        View header = findViewById(R.id.layoutHeader);
        final int headerTop = header.getPaddingTop();
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, 0, bars.right, bars.bottom);
            header.setPadding(header.getPaddingLeft(), headerTop + bars.top,
                    header.getPaddingRight(), header.getPaddingBottom());
            return insets;
        });
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(true);
    }
}