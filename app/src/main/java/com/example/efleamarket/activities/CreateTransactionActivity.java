package com.example.efleamarket.activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.efleamarket.R;
import com.example.efleamarket.models.Chat;
import com.example.efleamarket.models.Transaction;
import com.example.efleamarket.utils.ChatFormat;
import com.example.efleamarket.utils.CloudinaryHelper;
import com.example.efleamarket.viewmodels.CreateTransactionViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;

public class CreateTransactionActivity extends AppCompatActivity {
    public static final String EXTRA_PRODUCT_ID = "productId";
    public static final String EXTRA_BUYER_ID = "buyerId";
    public static final String EXTRA_SELLER_ID = "sellerId";
    private static final String STATE_REOFFER = "reofferMode";

    public static void start(Context ctx, String productId, String buyerId, String sellerId) {
        ctx.startActivity(new Intent(ctx, CreateTransactionActivity.class)
                .putExtra(EXTRA_PRODUCT_ID, productId)
                .putExtra(EXTRA_BUYER_ID, buyerId)
                .putExtra(EXTRA_SELLER_ID, sellerId));
    }

    private CreateTransactionViewModel vm;
    private ImageView ivProduct;
    private TextView tvHeaderTitle, tvProductTitle, tvListPrice, tvPricePreview, tvFormTitle;
    private TextView tvExistingStatus, tvExistingPrice, tvExistingOfferedBy, tvExistingNote, tvExistingHint;
    private EditText etPrice, etNote;
    private MaterialButton btnSubmit, btnAccept, btnReject, btnCancelTx, btnReOffer;
    private View cardForm, cardExisting, layoutSubmit, layoutRespond, pbLoading;
    private String myUid;
    private boolean reofferMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.create_transaction);
        applyInsets();

        myUid = FirebaseAuth.getInstance().getUid();
        String productId = getIntent().getStringExtra(EXTRA_PRODUCT_ID);
        String buyerId = getIntent().getStringExtra(EXTRA_BUYER_ID);
        if (myUid == null || productId == null || buyerId == null) { finish(); return; }

        if (savedInstanceState != null) reofferMode = savedInstanceState.getBoolean(STATE_REOFFER);

        bindViews();

        vm = new ViewModelProvider(this).get(CreateTransactionViewModel.class);
        vm.start(productId, buyerId);

        observe();
        setupEvents();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_REOFFER, reofferMode);
    }

    private void bindViews() {
        ivProduct = findViewById(R.id.ivProduct);
        tvHeaderTitle = findViewById(R.id.tvHeaderTitle);
        tvProductTitle = findViewById(R.id.tvProductTitle);
        tvListPrice = findViewById(R.id.tvListPrice);
        tvPricePreview = findViewById(R.id.tvPricePreview);
        tvFormTitle = findViewById(R.id.tvFormTitle);
        tvExistingStatus = findViewById(R.id.tvExistingStatus);
        tvExistingPrice = findViewById(R.id.tvExistingPrice);
        tvExistingOfferedBy = findViewById(R.id.tvExistingOfferedBy);
        tvExistingNote = findViewById(R.id.tvExistingNote);
        tvExistingHint = findViewById(R.id.tvExistingHint);
        etPrice = findViewById(R.id.etPrice);
        etNote = findViewById(R.id.etNote);
        btnSubmit = findViewById(R.id.btnSubmit);
        btnAccept = findViewById(R.id.btnAccept);
        btnReject = findViewById(R.id.btnReject);
        btnCancelTx = findViewById(R.id.btnCancelTx);
        btnReOffer = findViewById(R.id.btnReOffer);
        cardForm = findViewById(R.id.cardForm);
        cardExisting = findViewById(R.id.cardExisting);
        layoutSubmit = findViewById(R.id.layoutSubmit);
        layoutRespond = findViewById(R.id.layoutRespond);
        pbLoading = findViewById(R.id.pbLoading);
    }

    private void applyInsets() {
        View root = findViewById(R.id.rootCreateTx);
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
        vm.getChat().observe(this, this::bindProduct);

        vm.getLoading().observe(this, l -> {
            pbLoading.setVisibility(Boolean.TRUE.equals(l) ? View.VISIBLE : View.GONE);
            render();
        });

        vm.getTransaction().observe(this, t -> render());

        vm.getSubmitting().observe(this, s -> setBusy(Boolean.TRUE.equals(s)));

        vm.getError().observe(this, msg -> {
            if (msg == null) return;
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            vm.clearError();
        });

        vm.getInfo().observe(this, msg -> {
            if (msg == null) return;
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            vm.clearInfo();
        });

        vm.getDone().observe(this, d -> {
            if (!Boolean.TRUE.equals(d)) return;
            Toast.makeText(this, "Đã gửi đề nghị giao dịch", Toast.LENGTH_SHORT).show();
            finish();
        });

        vm.getCloseScreen().observe(this, close -> {
            if (Boolean.TRUE.equals(close)) finish();
        });
    }

    private void render() {
        if (Boolean.TRUE.equals(vm.getLoading().getValue())) {
            cardForm.setVisibility(View.GONE);
            cardExisting.setVisibility(View.GONE);
            layoutSubmit.setVisibility(View.GONE);
            return;
        }

        Transaction t = vm.getTransaction().getValue();
        if (t != null && !t.canReOffer(myUid)) reofferMode = false;

        boolean showForm = t == null || reofferMode;
        cardForm.setVisibility(showForm ? View.VISIBLE : View.GONE);
        layoutSubmit.setVisibility(showForm ? View.VISIBLE : View.GONE);
        cardExisting.setVisibility(t != null ? View.VISIBLE : View.GONE);

        tvHeaderTitle.setText(t == null ? "Tạo giao dịch" : "Giao dịch");
        tvFormTitle.setText(t == null ? "Giá đề nghị" : "Giá đề nghị mới");
        setBusy(Boolean.TRUE.equals(vm.getSubmitting().getValue()));

        if (t != null) bindExisting(t);
    }

    private void bindProduct(Chat c) {
        if (c == null || isFinishing() || isDestroyed()) return;
        tvProductTitle.setText(c.getProductTitle());
        tvListPrice.setText("Giá niêm yết: " + ChatFormat.price(c.getProductPrice()));
        Glide.with(this)
                .load(CloudinaryHelper.thumbnail(c.getProductImage(), 200))
                .placeholder(R.drawable.ic_shopping_cart)
                .error(R.drawable.ic_shopping_cart)
                .centerCrop()
                .into(ivProduct);
    }

    private void bindExisting(Transaction t) {
        GradientDrawable chip = new GradientDrawable();
        chip.setCornerRadius(dp(8));
        chip.setColor(t.getStatusBgColor());
        tvExistingStatus.setBackground(chip);
        tvExistingStatus.setTextColor(t.getStatusTextColor());
        tvExistingStatus.setText(t.getStatusLabel());

        tvExistingPrice.setText(ChatFormat.price(t.getOfferedPrice()));
        tvExistingOfferedBy.setText(t.isOfferedBy(myUid) ? "Bạn đã đề nghị giá này" : "Người kia đã đề nghị giá này");

        String note = t.getNote();
        boolean hasNote = note != null && !note.trim().isEmpty();
        tvExistingNote.setVisibility(hasNote ? View.VISIBLE : View.GONE);
        if (hasNote) tvExistingNote.setText("Ghi chú: " + note);

        tvExistingHint.setText(hintFor(t));

        layoutRespond.setVisibility(t.canRespond(myUid) ? View.VISIBLE : View.GONE);

        boolean canCancel = t.canCancel(myUid);
        btnCancelTx.setVisibility(canCancel ? View.VISIBLE : View.GONE);
        btnCancelTx.setText(Transaction.STATUS_OFFERED.equals(t.getStatus()) ? "RÚT LẠI ĐỀ NGHỊ" : "HỦY GIAO DỊCH");

        btnReOffer.setVisibility(t.canReOffer(myUid) && !reofferMode ? View.VISIBLE : View.GONE);
    }

    private String hintFor(Transaction t) {
        boolean byMe = myUid != null && myUid.equals(t.getLastActionBy());
        String status = t.getStatus() == null ? "" : t.getStatus();
        switch (status) {
            case Transaction.STATUS_OFFERED:
                return t.canRespond(myUid)
                        ? "Người kia đề nghị mức giá này. Bạn chấp nhận hay từ chối?"
                        : "Đang chờ người kia phản hồi.";
            case Transaction.STATUS_ACCEPTED:
                return "Hai bên đã thống nhất giá. Bước tiếp theo: hẹn địa điểm và thời gian giao nhận.";
            case Transaction.STATUS_REJECTED:
                return (byMe ? "Bạn đã từ chối đề nghị này." : "Người kia đã từ chối đề nghị này.")
                        + " Bạn có thể đề nghị giá mới.";
            case Transaction.STATUS_CANCELLED:
                return (byMe ? "Bạn đã hủy giao dịch này." : "Người kia đã hủy giao dịch này.")
                        + " Bạn có thể đề nghị giá mới.";
            case Transaction.STATUS_SCHEDULED:
                return "Hai bên đã hẹn gặp để giao nhận.";
            case Transaction.STATUS_COMPLETED:
                return "Giao dịch đã hoàn tất.";
            default:
                return "";
        }
    }

    private void setBusy(boolean busy) {
        Transaction t = vm.getTransaction().getValue();
        btnSubmit.setEnabled(!busy);
        btnSubmit.setText(busy ? "ĐANG GỬI…" : (t == null ? "GỬI ĐỀ NGHỊ" : "GỬI ĐỀ NGHỊ MỚI"));
        btnAccept.setEnabled(!busy);
        btnReject.setEnabled(!busy);
        btnCancelTx.setEnabled(!busy);
        btnReOffer.setEnabled(!busy);
    }

    private void setupEvents() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        findViewById(R.id.btnUseListPrice).setOnClickListener(v -> {
            Chat c = vm.getChat().getValue();
            if (c == null) return;
            String value = String.valueOf((long) c.getProductPrice());
            etPrice.setText(value);
            etPrice.setSelection(value.length());
        });

        etPrice.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override public void afterTextChanged(Editable s) {
                double price = CreateTransactionViewModel.parsePrice(s.toString());
                tvPricePreview.setText(price > 0 ? ChatFormat.price(price) : "");
            }
        });

        btnSubmit.setOnClickListener(v ->
                vm.submit(etPrice.getText() == null ? "" : etPrice.getText().toString(),
                        etNote.getText() == null ? "" : etNote.getText().toString()));

        btnAccept.setOnClickListener(v -> {
            Transaction t = vm.getTransaction().getValue();
            if (t == null) return;
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Chấp nhận đề nghị?")
                    .setMessage("Bạn đồng ý giao dịch với giá " + ChatFormat.price(t.getOfferedPrice()) + ".")
                    .setNegativeButton("Không", null)
                    .setPositiveButton("Chấp nhận", (d, w) -> vm.accept())
                    .show();
        });

        btnReject.setOnClickListener(v -> new MaterialAlertDialogBuilder(this)
                .setTitle("Từ chối đề nghị?")
                .setMessage("Người kia sẽ nhận được thông báo trong cuộc trò chuyện và có thể đề nghị giá khác.")
                .setNegativeButton("Không", null)
                .setPositiveButton("Từ chối", (d, w) -> vm.reject())
                .show());

        btnCancelTx.setOnClickListener(v -> {
            Transaction t = vm.getTransaction().getValue();
            if (t == null) return;
            boolean offered = Transaction.STATUS_OFFERED.equals(t.getStatus());
            new MaterialAlertDialogBuilder(this)
                    .setTitle(offered ? "Rút lại đề nghị?" : "Hủy giao dịch?")
                    .setMessage(offered
                            ? "Đề nghị " + ChatFormat.price(t.getOfferedPrice()) + " sẽ bị hủy."
                            : "Giao dịch đã thống nhất sẽ bị hủy. Người kia sẽ nhận được thông báo.")
                    .setNegativeButton("Không", null)
                    .setPositiveButton(offered ? "Rút lại" : "Hủy giao dịch", (d, w) -> vm.cancel())
                    .show();
        });

        btnReOffer.setOnClickListener(v -> {
            reofferMode = true;
            etPrice.setText("");
            etNote.setText("");
            render();
            etPrice.requestFocus();
        });
    }

    private float dp(int value) {
        return value * getResources().getDisplayMetrics().density;
    }
}