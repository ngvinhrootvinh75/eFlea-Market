package com.example.efleamarket.activities;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
import com.example.efleamarket.models.MeetingPlace;
import com.example.efleamarket.models.ShippingAddress;
import com.example.efleamarket.models.TrackingEvent;
import com.example.efleamarket.models.Transaction;
import com.example.efleamarket.utils.ChatFormat;
import com.example.efleamarket.utils.CloudinaryHelper;
import com.example.efleamarket.viewmodels.CreateTransactionViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Date;
import java.util.List;

public class CreateTransactionActivity extends AppCompatActivity {
    public static final String EXTRA_PRODUCT_ID = "productId";
    public static final String EXTRA_BUYER_ID = "buyerId";
    public static final String EXTRA_SELLER_ID = "sellerId";
    private static final String STATE_REOFFER = "reofferMode";
    private static final String STATE_SHIP_FORM = "shipForm";
    private static final String STATE_ADDR = "pickedAddress";
    private static final String STATE_LAT = "pickedLat";
    private static final String STATE_LNG = "pickedLng";
    private static final String[] CARRIERS = {
            "GHN", "GHTK", "Viettel Post", "J&T Express", "SPX Express", "VNPost", "Ahamove", "Tự giao", "Khác"
    };

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
    private TextView tvMeetingName, tvMeetingTime, tvMeetingCountdown, tvShipAddress, tvShipTracking;
    private TextView tvShipFormName, tvJourneyTitle;
    private EditText etPrice, etNote, etShipFormPhone;
    private MaterialButton btnSubmit, btnAccept, btnReject, btnCancelTx, btnReOffer;
    private MaterialButton btnSchedule, btnShip, btnChangeMeeting, btnDirections;
    private MaterialButton btnEnterTracking, btnAddTracking, btnMarkDelivering, btnReceived;
    private MaterialButton btnPickAddress, btnShipFormSubmit, btnShipFormCancel;
    private View cardForm, cardExisting, layoutSubmit, layoutRespond, layoutDelivery;
    private View cardMeeting, cardShipping, cardShipForm, pbLoading;
    private LinearLayout layoutTimeline, layoutJourney;
    private ActivityResultLauncher<Intent> pickMeeting, pickAddress;
    private String myUid;
    private boolean reofferMode, shipFormOpen;
    private String pickedAddress;
    private double pickedLat, pickedLng;
    private String accountName = "";

    private final Handler ticker = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            updateCountdown();
            ticker.postDelayed(this, 30_000L);
        }
    };

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

        if (savedInstanceState != null) {
            reofferMode = savedInstanceState.getBoolean(STATE_REOFFER);
            shipFormOpen = savedInstanceState.getBoolean(STATE_SHIP_FORM);
            pickedAddress = savedInstanceState.getString(STATE_ADDR);
            pickedLat = savedInstanceState.getDouble(STATE_LAT);
            pickedLng = savedInstanceState.getDouble(STATE_LNG);
        }

        bindViews();
        updatePickedAddress();

        vm = new ViewModelProvider(this).get(CreateTransactionViewModel.class);

        pickMeeting = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() != RESULT_OK || result.getData() == null) return;
            Intent d = result.getData();
            MeetingPlace place = new MeetingPlace(
                    d.getStringExtra(MeetingPickerActivity.EXTRA_NAME),
                    d.getDoubleExtra(MeetingPickerActivity.EXTRA_LAT, 0),
                    d.getDoubleExtra(MeetingPickerActivity.EXTRA_LNG, 0),
                    d.getStringExtra(MeetingPickerActivity.EXTRA_POINT_ID));
            Date time = new Date(d.getLongExtra(MeetingPickerActivity.EXTRA_TIME, 0));
            vm.scheduleMeeting(place, time);
        });

        pickAddress = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() != RESULT_OK || result.getData() == null) return;
            Intent d = result.getData();
            pickedAddress = d.getStringExtra(MeetingPickerActivity.EXTRA_NAME);
            pickedLat = d.getDoubleExtra(MeetingPickerActivity.EXTRA_LAT, 0);
            pickedLng = d.getDoubleExtra(MeetingPickerActivity.EXTRA_LNG, 0);
            updatePickedAddress();
        });

        vm.start(productId, buyerId);
        if (shipFormOpen) vm.loadMyContact();

        observe();
        setupEvents();
    }

    @Override
    protected void onResume() {
        super.onResume();
        ticker.post(tick);
    }

    @Override
    protected void onPause() {
        super.onPause();
        ticker.removeCallbacks(tick);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_REOFFER, reofferMode);
        outState.putBoolean(STATE_SHIP_FORM, shipFormOpen);
        outState.putString(STATE_ADDR, pickedAddress);
        outState.putDouble(STATE_LAT, pickedLat);
        outState.putDouble(STATE_LNG, pickedLng);
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
        tvMeetingName = findViewById(R.id.tvMeetingName);
        tvMeetingTime = findViewById(R.id.tvMeetingTime);
        tvMeetingCountdown = findViewById(R.id.tvMeetingCountdown);
        tvShipAddress = findViewById(R.id.tvShipAddress);
        tvShipTracking = findViewById(R.id.tvShipTracking);
        tvShipFormName = findViewById(R.id.tvShipFormName);
        tvJourneyTitle = findViewById(R.id.tvJourneyTitle);
        etPrice = findViewById(R.id.etPrice);
        etNote = findViewById(R.id.etNote);
        etShipFormPhone = findViewById(R.id.etShipFormPhone);
        btnSubmit = findViewById(R.id.btnSubmit);
        btnAccept = findViewById(R.id.btnAccept);
        btnReject = findViewById(R.id.btnReject);
        btnCancelTx = findViewById(R.id.btnCancelTx);
        btnReOffer = findViewById(R.id.btnReOffer);
        btnSchedule = findViewById(R.id.btnSchedule);
        btnShip = findViewById(R.id.btnShip);
        btnChangeMeeting = findViewById(R.id.btnChangeMeeting);
        btnDirections = findViewById(R.id.btnDirections);
        btnEnterTracking = findViewById(R.id.btnEnterTracking);
        btnAddTracking = findViewById(R.id.btnAddTracking);
        btnMarkDelivering = findViewById(R.id.btnMarkDelivering);
        btnReceived = findViewById(R.id.btnReceived);
        btnPickAddress = findViewById(R.id.btnPickAddress);
        btnShipFormSubmit = findViewById(R.id.btnShipFormSubmit);
        btnShipFormCancel = findViewById(R.id.btnShipFormCancel);
        cardForm = findViewById(R.id.cardForm);
        cardExisting = findViewById(R.id.cardExisting);
        layoutSubmit = findViewById(R.id.layoutSubmit);
        layoutRespond = findViewById(R.id.layoutRespond);
        layoutDelivery = findViewById(R.id.layoutDelivery);
        cardMeeting = findViewById(R.id.cardMeeting);
        cardShipping = findViewById(R.id.cardShipping);
        cardShipForm = findViewById(R.id.cardShipForm);
        layoutTimeline = findViewById(R.id.layoutTimeline);
        layoutJourney = findViewById(R.id.layoutJourney);
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
        vm.getTracking().observe(this, list -> bindJourney());

        vm.getMyContact().observe(this, contact -> {
            if (contact == null) return;
            accountName = contact[0];
            tvShipFormName.setText("Người nhận: " + (accountName.isEmpty() ? "(chưa có tên trong hồ sơ)" : accountName));
            if (text(etShipFormPhone).isEmpty() && !contact[1].isEmpty()) etShipFormPhone.setText(contact[1]);
        });

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
        if (t == null || !t.canRequestShipping(myUid)) shipFormOpen = false;

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

        boolean accepted = Transaction.STATUS_ACCEPTED.equals(t.getStatus());
        layoutDelivery.setVisibility(accepted && t.canSchedule(myUid) && !shipFormOpen ? View.VISIBLE : View.GONE);
        btnShip.setVisibility(t.canRequestShipping(myUid) ? View.VISIBLE : View.GONE);
        cardShipForm.setVisibility(shipFormOpen ? View.VISIBLE : View.GONE);

        boolean hasMeeting = t.hasMeeting();
        cardMeeting.setVisibility(hasMeeting ? View.VISIBLE : View.GONE);
        if (hasMeeting) {
            tvMeetingName.setText(t.getMeetingPlace().getName());
            tvMeetingTime.setText(" " + ChatFormat.dateTime(t.getMeetingTime()));
            btnChangeMeeting.setVisibility(t.canSchedule(myUid) ? View.VISIBLE : View.GONE);
        }
        updateCountdown();

        boolean shipping = t.isShippingFlow();
        cardShipping.setVisibility(shipping ? View.VISIBLE : View.GONE);
        if (shipping) bindShipping(t);

        boolean canCancel = t.canCancel(myUid) && !shipFormOpen;
        btnCancelTx.setVisibility(canCancel ? View.VISIBLE : View.GONE);
        btnCancelTx.setText(Transaction.STATUS_OFFERED.equals(t.getStatus()) ? "RÚT LẠI ĐỀ NGHỊ" : "HỦY GIAO DỊCH");

        btnReOffer.setVisibility(t.canReOffer(myUid) && !reofferMode ? View.VISIBLE : View.GONE);
    }

    private void bindShipping(Transaction t) {
        ShippingAddress a = t.getShippingAddress();
        tvShipAddress.setText(a == null ? ""
                : a.getName() + " · " + a.getPhone() + "\n" + a.getAddress()
                  + (t.isSeller(myUid) ? "\n(Chạm để mở bản đồ)" : ""));

        boolean hasCarrier = t.getCarrier() != null && !t.getCarrier().isEmpty();
        tvShipTracking.setVisibility(hasCarrier ? View.VISIBLE : View.GONE);
        if (hasCarrier) {
            tvShipTracking.setText("Đơn vị: " + t.getCarrier()
                    + (t.hasTrackingCode() ? " · Mã vận đơn: " + t.getTrackingCode() : ""));
        }

        bindTimeline(t);
        bindJourney();

        btnEnterTracking.setVisibility(t.canEnterTracking(myUid) ? View.VISIBLE : View.GONE);
        btnAddTracking.setVisibility(t.canAddTracking(myUid) ? View.VISIBLE : View.GONE);
        btnMarkDelivering.setVisibility(t.canMarkDelivering(myUid) ? View.VISIBLE : View.GONE);
        btnReceived.setVisibility(t.canConfirmReceived(myUid) ? View.VISIBLE : View.GONE);
    }

    private void bindTimeline(Transaction t) {
        layoutTimeline.removeAllViews();
        String[] labels = {"Người mua đặt giao tận nơi", "Người bán đã gửi hàng", "Đang giao hàng", "Đã nhận hàng"};
        Date[] times = {t.getShipRequestedAt(), t.getShippedAt(), t.getDeliveringAt(), t.getReceivedAt()};
        int reached = t.getShipStepIndex();
        for (int i = 0; i < labels.length; i++) {
            boolean done = i <= reached;
            TextView row = new TextView(this);
            String time = done && times[i] != null ? "  ·  " + ChatFormat.dateTime(times[i]) : "";
            row.setText((done ? "●  " : "○  ") + labels[i] + time);
            row.setTextColor(done ? 0xFF0F766E : 0xFF94A3B8);
            row.setTextSize(14);
            row.setPadding(0, (int) dp(4), 0, (int) dp(4));
            layoutTimeline.addView(row);
        }
    }

    private void bindJourney() {
        if (layoutJourney == null) return;
        layoutJourney.removeAllViews();
        List<TrackingEvent> list = vm == null ? null : vm.getTracking().getValue();
        boolean has = list != null && !list.isEmpty();
        tvJourneyTitle.setVisibility(has ? View.VISIBLE : View.GONE);
        if (!has) return;
        for (int i = list.size() - 1; i >= 0; i--) {
            TrackingEvent e = list.get(i);
            boolean latest = i == list.size() - 1;
            TextView row = new TextView(this);
            row.setText((latest ? "" : "•  ") + ChatFormat.dateTime(e.getCreatedAt()) + "\n     " + e.getText());
            row.setTextColor(latest ? 0xFF0F766E : 0xFF475569);
            row.setTypeface(null, latest ? Typeface.BOLD : Typeface.NORMAL);
            row.setTextSize(13);
            row.setPadding(0, (int) dp(4), 0, (int) dp(4));
            layoutJourney.addView(row);
        }
    }

    private void updateCountdown() {
        if (vm == null || tvMeetingCountdown == null) return;
        Transaction t = vm.getTransaction().getValue();
        if (t != null && t.hasMeeting()) {
            tvMeetingCountdown.setText("⏳ " + ChatFormat.countdown(t.getMeetingTime()));
        }
    }

    private void updatePickedAddress() {
        if (btnPickAddress == null) return;
        boolean has = pickedAddress != null && !pickedAddress.isEmpty();
        btnPickAddress.setText(has ? "" + pickedAddress + "\n(Chạm để đổi)" : "Chọn địa chỉ trên bản đồ");
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
                if (shipFormOpen) return "Kiểm tra số điện thoại và chọn địa chỉ nhận hàng trên bản đồ.";
                return t.isBuyer(myUid)
                        ? "Hai bên đã thống nhất giá. Chọn HẸN GẶP để giao nhận trực tiếp, hoặc GIAO TẬN NƠI nếu ở xa để người bán gửi hàng qua đơn vị vận chuyển."
                        : "Hai bên đã thống nhất giá. Chọn HẸN GẶP để giao nhận trực tiếp. Nếu người mua ở xa, họ sẽ chọn GIAO TẬN NƠI và nhập địa chỉ.";
            case Transaction.STATUS_REJECTED:
                return (byMe ? "Bạn đã từ chối đề nghị này." : "Người kia đã từ chối đề nghị này.")
                        + " Bạn có thể đề nghị giá mới.";
            case Transaction.STATUS_CANCELLED:
                return (byMe ? "Bạn đã hủy giao dịch này." : "Người kia đã hủy giao dịch này.")
                        + " Bạn có thể đề nghị giá mới.";
            case Transaction.STATUS_SCHEDULED:
                return "Hai bên đã hẹn gặp. Kiểm tra kỹ sản phẩm trước khi thanh toán.";
            case Transaction.STATUS_SHIPPING:
                return shippingHint(t);
            case Transaction.STATUS_COMPLETED:
                return t.isShippingFlow()
                        ? "Người mua đã nhận hàng. Giao dịch hoàn tất, tin đăng đã chuyển sang Đã bán."
                        : "Giao dịch đã hoàn tất.";
            default:
                return "";
        }
    }

    private String shippingHint(Transaction t) {
        boolean seller = t.isSeller(myUid);
        String step = t.getShipStep() == null ? "" : t.getShipStep();
        switch (step) {
            case Transaction.SHIP_WAITING:
                return seller
                        ? "Người mua chọn giao tận nơi. Đóng gói, gửi hàng tới địa chỉ bên dưới rồi bấm XÁC NHẬN ĐÃ GỬI HÀNG."
                        : "Đang chờ người bán gửi hàng.";
            case Transaction.SHIP_SHIPPED:
            case Transaction.SHIP_DELIVERING:
                return seller
                        ? "Bấm CẬP NHẬT HÀNH TRÌNH mỗi khi đơn tới chặng mới để người mua theo dõi."
                        : "Theo dõi hành trình đơn bên dưới. Nhận được hàng thì bấm ĐÃ NHẬN ĐƯỢC HÀNG.";
            default:
                return "";
        }
    }

    private void setBusy(boolean busy) {
        Transaction t = vm.getTransaction().getValue();
        btnSubmit.setEnabled(!busy);
        btnSubmit.setText(busy ? "ĐANG GỬI…" : (t == null ? "GỬI ĐỀ NGHỊ" : "GỬI ĐỀ NGHỊ MỚI"));
        MaterialButton[] buttons = {btnAccept, btnReject, btnCancelTx, btnReOffer, btnSchedule, btnShip,
                btnChangeMeeting, btnEnterTracking, btnAddTracking, btnMarkDelivering, btnReceived,
                btnShipFormSubmit};
        for (MaterialButton b : buttons) b.setEnabled(!busy);
    }

    private void openMeetingPicker() {
        Transaction t = vm.getTransaction().getValue();
        if (t == null) return;
        MeetingPlace place = t.hasMeeting() ? t.getMeetingPlace() : null;
        Date time = t.hasMeeting() ? t.getMeetingTime() : null;
        pickMeeting.launch(MeetingPickerActivity.intent(this, place, time));
    }

    private void openDirections() {
        Transaction t = vm.getTransaction().getValue();
        if (t == null || !t.hasMeeting()) return;
        MeetingPlace p = t.getMeetingPlace();
        openUrl("https://www.google.com/maps/dir/?api=1&destination=" + p.getLat() + "," + p.getLng());
    }

    private void openShipAddressMap() {
        Transaction t = vm.getTransaction().getValue();
        if (t == null || t.getShippingAddress() == null || !t.isSeller(myUid)) return;
        ShippingAddress a = t.getShippingAddress();
        openUrl("https://www.google.com/maps/search/?api=1&query=" + a.getLat() + "," + a.getLng());
    }

    private void submitShipForm() {
        if (pickedAddress == null || pickedAddress.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn địa chỉ trên bản đồ", Toast.LENGTH_SHORT).show();
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle("Xác nhận giao tận nơi?")
                .setMessage("Người nhận: " + accountName + "\nSĐT: " + text(etShipFormPhone) + "\nĐịa chỉ: " + pickedAddress)
                .setNegativeButton("Sửa lại", null)
                .setPositiveButton("Xác nhận", (d, w) -> vm.requestShipping(
                        new ShippingAddress(accountName, text(etShipFormPhone), pickedAddress, pickedLat, pickedLng)))
                .show();
    }

    private void showTrackingDialog() {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_tracking, null);
        AutoCompleteTextView actvCarrier = v.findViewById(R.id.actvCarrier);
        EditText etCode = v.findViewById(R.id.etTrackingCode);
        actvCarrier.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, CARRIERS));
        new MaterialAlertDialogBuilder(this)
                .setTitle("Xác nhận đã gửi hàng")
                .setView(v)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Xác nhận", (d, w) -> vm.submitTracking(text(actvCarrier), text(etCode)))
                .show();
    }

    private void showAddTrackingDialog() {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_tracking_update, null);
        EditText et = v.findViewById(R.id.etTrackingText);
        new MaterialAlertDialogBuilder(this)
                .setTitle("Cập nhật hành trình")
                .setView(v)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Cập nhật", (d, w) -> vm.addTracking(text(et)))
                .show();
    }

    private void openUrl(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Không tìm thấy ứng dụng để mở", Toast.LENGTH_SHORT).show();
        }
    }

    private static String text(EditText e) {
        return e.getText() == null ? "" : e.getText().toString().trim();
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

        btnSubmit.setOnClickListener(v -> vm.submit(text(etPrice), text(etNote)));

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

        btnSchedule.setOnClickListener(v -> openMeetingPicker());
        btnChangeMeeting.setOnClickListener(v -> openMeetingPicker());
        btnDirections.setOnClickListener(v -> openDirections());

        btnShip.setOnClickListener(v -> {
            shipFormOpen = true;
            vm.loadMyContact();
            render();
        });
        btnShipFormCancel.setOnClickListener(v -> {
            shipFormOpen = false;
            render();
        });
        btnPickAddress.setOnClickListener(v ->
                pickAddress.launch(MeetingPickerActivity.intentAddress(this, pickedAddress, pickedLat, pickedLng)));
        btnShipFormSubmit.setOnClickListener(v -> submitShipForm());
        tvShipAddress.setOnClickListener(v -> openShipAddressMap());

        btnEnterTracking.setOnClickListener(v -> showTrackingDialog());
        btnAddTracking.setOnClickListener(v -> showAddTrackingDialog());

        btnMarkDelivering.setOnClickListener(v -> new MaterialAlertDialogBuilder(this)
                .setTitle("Đơn đang được giao?")
                .setMessage("Xác nhận đơn đang trên đường giao tới người mua.")
                .setNegativeButton("Không", null)
                .setPositiveButton("Xác nhận", (d, w) -> vm.markDelivering())
                .show());

        btnReceived.setOnClickListener(v -> new MaterialAlertDialogBuilder(this)
                .setTitle("Đã nhận được hàng?")
                .setMessage("Chỉ xác nhận khi bạn đã nhận và kiểm tra hàng. Giao dịch sẽ hoàn tất và tin đăng chuyển sang Đã bán.")
                .setNegativeButton("Chưa", null)
                .setPositiveButton("Đã nhận", (d, w) -> vm.confirmReceived())
                .show());
    }

    private float dp(int value) {
        return value * getResources().getDisplayMetrics().density;
    }
}