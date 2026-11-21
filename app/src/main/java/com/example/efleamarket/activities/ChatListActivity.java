package com.example.efleamarket.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.TooltipCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.efleamarket.MainActivity;
import com.example.efleamarket.R;
import com.example.efleamarket.adapters.ChatListAdapter;
import com.example.efleamarket.models.Chat;
import com.example.efleamarket.viewmodels.ChatListViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;

public class ChatListActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat_list);

        applySystemBarPadding();

        String myUid = FirebaseAuth.getInstance().getUid();
        if (myUid == null) { finish(); return; }

        setupBottomNav();

        findViewById(R.id.btnNotification).setOnClickListener(v ->
                Toast.makeText(this, "Chức năng Thông báo đang phát triển", Toast.LENGTH_SHORT).show());

        View fabAssistant = findViewById(R.id.fabAssistant);
        fabAssistant.setOnClickListener(v ->
                Toast.makeText(this, "Trợ lý eFlea đang phát triển", Toast.LENGTH_SHORT).show());
        TooltipCompat.setTooltipText(fabAssistant, "Trợ lý eFlea");

        RecyclerView rv = findViewById(R.id.rvChats);
        TextView tvEmpty = findViewById(R.id.tvEmpty);
        ProgressBar pb = findViewById(R.id.pbLoading);

        ChatListViewModel vm = new ViewModelProvider(this).get(ChatListViewModel.class);

        ChatListAdapter adapter = new ChatListAdapter(myUid,
                chat -> ChatActivity.openChat(this, chat.getId()),
                chat -> confirmDelete(vm, chat, myUid));
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        vm.start();

        vm.getLoading().observe(this, l ->
                pb.setVisibility(Boolean.TRUE.equals(l) ? View.VISIBLE : View.GONE));
        vm.getChats().observe(this, list -> {
            adapter.submitList(list);
            boolean loading = Boolean.TRUE.equals(vm.getLoading().getValue());
            tvEmpty.setVisibility(!loading && (list == null || list.isEmpty()) ? View.VISIBLE : View.GONE);
        });
        vm.getError().observe(this, msg -> {
            if (msg == null) return;
            android.util.Log.e("ChatList", msg);
            vm.clearError();
        });
    }

    private void confirmDelete(ChatListViewModel vm, Chat chat, String myUid) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Xóa cuộc trò chuyện?")
                .setMessage("Cuộc trò chuyện với " + chat.getOtherName(myUid)
                        + " về \"" + chat.getProductTitle() + "\" sẽ bị xóa ở phía bạn. "
                        + "Người kia vẫn xem được. Nếu có tin nhắn mới, cuộc trò chuyện sẽ hiện lại.")
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Xóa", (d, w) -> vm.deleteChat(chat))
                .show();
    }

    private void applySystemBarPadding() {
        View root = ((ViewGroup) findViewById(android.R.id.content)).getChildAt(0);
        View banner = findViewById(R.id.viewTopBanner);
        View header = findViewById(R.id.layoutHeader);

        final int left = root.getPaddingLeft();
        final int right = root.getPaddingRight();
        final int bottom = root.getPaddingBottom();
        final int bannerHeight = banner.getLayoutParams().height;
        final int headerTop = header.getPaddingTop();

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(left + bars.left, 0, right + bars.right, bottom + bars.bottom);

            ViewGroup.LayoutParams lp = banner.getLayoutParams();
            lp.height = bannerHeight + bars.top;
            banner.setLayoutParams(lp);

            header.setPadding(header.getPaddingLeft(), headerTop + bars.top,
                    header.getPaddingRight(), header.getPaddingBottom());
            return insets;
        });

        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(true);
    }

    private void setupBottomNav() {
        findViewById(R.id.navHome).setOnClickListener(v -> {
            Intent i = new Intent(this, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(i);
            finish();
        });

        findViewById(R.id.navFavorites).setOnClickListener(v ->
                Toast.makeText(this, "Chức năng Yêu thích đang phát triển", Toast.LENGTH_SHORT).show());

        findViewById(R.id.navPost).setOnClickListener(v ->
                Toast.makeText(this, "Chức năng Đăng tin bán đang phát triển", Toast.LENGTH_SHORT).show());

        findViewById(R.id.navMessages).setOnClickListener(v -> { });

        findViewById(R.id.navProfile).setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class)));
    }
}