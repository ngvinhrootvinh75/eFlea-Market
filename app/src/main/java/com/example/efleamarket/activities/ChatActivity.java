package com.example.efleamarket.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.efleamarket.R;
import com.example.efleamarket.adapters.MessageAdapter;
import com.example.efleamarket.models.Chat;
import com.example.efleamarket.models.Transaction;
import com.example.efleamarket.utils.ChatFormat;
import com.example.efleamarket.utils.CloudinaryHelper;
import com.example.efleamarket.viewmodels.ChatViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;

public class ChatActivity extends AppCompatActivity {
    public static final String EXTRA_PRODUCT_ID = "productId";
    public static final String EXTRA_SELLER_ID = "sellerId";
    public static final String EXTRA_CHAT_ID = "chatId";

    public static void start(Context ctx, String productId, String sellerId) {
        ctx.startActivity(new Intent(ctx, ChatActivity.class)
                .putExtra(EXTRA_PRODUCT_ID, productId)
                .putExtra(EXTRA_SELLER_ID, sellerId));
    }

    public static void openChat(Context ctx, String chatId) {
        ctx.startActivity(new Intent(ctx, ChatActivity.class).putExtra(EXTRA_CHAT_ID, chatId));
    }

    private ChatViewModel vm;
    private MessageAdapter adapter;
    private RecyclerView rvMessages;
    private EditText etMessage;
    private TextView tvName, tvProductTitle, tvProductPrice;
    private ImageView ivAvatar, ivProduct;
    private View btnAttach, layoutUploading;
    private MaterialCardView cardTxBanner;
    private TextView tvTxBanner, tvTxBannerAction;
    private MaterialButton btnCreateTransaction;
    private ActivityResultLauncher<PickVisualMediaRequest> pickMedia;
    private String myUid;
    private Chat currentChat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat);
        applyInsets();

        myUid = FirebaseAuth.getInstance().getUid();
        if (myUid == null) { finish(); return; }

        bindViews();

        pickMedia = registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
            if (uri != null) vm.sendMedia(this, uri);
        });

        adapter = new MessageAdapter(myUid, m ->
                MediaViewerActivity.open(this, m.getMediaUrl(), m.getType()));
        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        rvMessages.setLayoutManager(lm);
        rvMessages.setAdapter(adapter);

        vm = new ViewModelProvider(this).get(ChatViewModel.class);
        String chatId = getIntent().getStringExtra(EXTRA_CHAT_ID);
        String productId = getIntent().getStringExtra(EXTRA_PRODUCT_ID);
        if (chatId != null) vm.openExisting(chatId);
        else if (productId != null) vm.openForProduct(productId);
        else { finish(); return; }

        observe();
        setupEvents();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (vm != null) vm.setVisible(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (vm != null) vm.setVisible(false);
    }

    private void bindViews() {
        tvName = findViewById(R.id.tvName);
        ivAvatar = findViewById(R.id.ivAvatar);
        ivProduct = findViewById(R.id.ivProduct);
        tvProductTitle = findViewById(R.id.tvProductTitle);
        tvProductPrice = findViewById(R.id.tvProductPrice);
        rvMessages = findViewById(R.id.rvMessages);
        etMessage = findViewById(R.id.etMessage);
        btnAttach = findViewById(R.id.btnAttach);
        layoutUploading = findViewById(R.id.layoutUploading);
        cardTxBanner = findViewById(R.id.cardTxBanner);
        tvTxBanner = findViewById(R.id.tvTxBanner);
        tvTxBannerAction = findViewById(R.id.tvTxBannerAction);
        btnCreateTransaction = findViewById(R.id.btnCreateTransaction);
    }

    private void applyInsets() {
        View root = findViewById(R.id.rootChat);
        View header = findViewById(R.id.layoutHeader);
        final int headerTop = header.getPaddingTop();

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, 0, bars.right, Math.max(bars.bottom, ime.bottom));
            header.setPadding(header.getPaddingLeft(), headerTop + bars.top,
                    header.getPaddingRight(), header.getPaddingBottom());
            return insets;
        });

        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(true);
    }

    private void observe() {
        vm.getChat().observe(this, this::bindHeader);

        vm.getMessages().observe(this, list -> adapter.submitList(list, () -> {
            if (list != null && !list.isEmpty()) rvMessages.scrollToPosition(list.size() - 1);
        }));

        vm.getTransaction().observe(this, this::bindTransaction);

        vm.getUploading().observe(this, up -> {
            boolean u = Boolean.TRUE.equals(up);
            layoutUploading.setVisibility(u ? View.VISIBLE : View.GONE);
            btnAttach.setEnabled(!u);
            btnAttach.setAlpha(u ? 0.4f : 1f);
        });

        vm.getError().observe(this, msg -> {
            if (msg == null) return;
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            vm.clearError();
        });

        vm.getCloseScreen().observe(this, close -> { if (Boolean.TRUE.equals(close)) finish(); });
    }

    private void setupEvents() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        findViewById(R.id.btnSend).setOnClickListener(v -> {
            String text = etMessage.getText().toString().trim();
            if (text.isEmpty()) return;
            vm.sendMessage(text);
            etMessage.setText("");
        });

        btnAttach.setOnClickListener(v ->
                pickMedia.launch(new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageAndVideo.INSTANCE)
                        .build()));

        findViewById(R.id.cardProduct).setOnClickListener(v -> {
            if (currentChat == null) return;
            Intent i = new Intent(this, ProductDetailActivity.class);
            i.putExtra(ProductDetailActivity.EXTRA_PRODUCT_ID, currentChat.getProductId());
            startActivity(i);
        });

        findViewById(R.id.layoutOther).setOnClickListener(v -> {
            if (currentChat == null) return;
            Intent i = new Intent(this, ProfileActivity.class);
            i.putExtra(ProfileActivity.EXTRA_UID, currentChat.getOtherId(myUid));
            startActivity(i);
        });

        btnCreateTransaction.setOnClickListener(v -> openTransaction());
        cardTxBanner.setOnClickListener(v -> openTransaction());
    }

    private void openTransaction() {
        if (currentChat == null) return;
        CreateTransactionActivity.start(this,
                currentChat.getProductId(), currentChat.getBuyerId(), currentChat.getSellerId());
    }

    private void bindHeader(Chat c) {
        if (c == null || isFinishing() || isDestroyed()) return;
        currentChat = c;
        tvName.setText(c.getOtherName(myUid));
        tvProductTitle.setText(c.getProductTitle());
        tvProductPrice.setText(ChatFormat.price(c.getProductPrice()));

        String avatar = c.getOtherAvatar(myUid);
        Glide.with(this)
                .load(avatar == null || avatar.isEmpty() ? null : CloudinaryHelper.thumbnail(avatar, 150))
                .placeholder(R.drawable.ic_person).error(R.drawable.ic_person)
                .circleCrop().into(ivAvatar);
        Glide.with(this)
                .load(CloudinaryHelper.thumbnail(c.getProductImage(), 150))
                .placeholder(R.drawable.ic_shopping_cart).error(R.drawable.ic_shopping_cart)
                .centerCrop().into(ivProduct);
    }

    private void bindTransaction(Transaction t) {
        if (t == null) {
            cardTxBanner.setVisibility(View.GONE);
            btnCreateTransaction.setText("Tạo giao dịch");
            return;
        }
        btnCreateTransaction.setText("Xem giao dịch");
        cardTxBanner.setVisibility(View.VISIBLE);
        cardTxBanner.setCardBackgroundColor(t.getStatusBgColor());
        tvTxBanner.setTextColor(t.getStatusTextColor());
        tvTxBannerAction.setTextColor(t.getStatusTextColor());
        if (t.hasMeeting()) {
            tvTxBanner.setText(" " + ChatFormat.dateTime(t.getMeetingTime()) + " · " + t.getMeetingPlace().getName());
        } else if (Transaction.STATUS_SHIPPING.equals(t.getStatus())) {
            tvTxBanner.setText(" " + t.getShipStepLabel()
                    + (t.hasTrackingCode() ? " · " + t.getCarrier() + " " + t.getTrackingCode() : ""));
        } else {
            tvTxBanner.setText("Giao dịch: " + t.getStatusLabel() + " · " + ChatFormat.price(t.getOfferedPrice()));
        }
    }
}