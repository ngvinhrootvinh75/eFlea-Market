package com.example.efleamarket.activities;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.efleamarket.MainActivity;
import com.example.efleamarket.R;
import com.example.efleamarket.utils.AccentInsensitiveAdapter;
import com.example.efleamarket.utils.AvatarUtils;
import com.example.efleamarket.utils.LoadingDialog;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.net.Uri;
import android.widget.ProgressBar;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import com.example.efleamarket.repositories.UserRepository;

public class EditProfileActivity extends AppCompatActivity {

    private static final SimpleDateFormat DOB_FORMAT =
            new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    private ShapeableImageView ivAvatar;
    private TextInputLayout tilFullName, tilUsername, tilBio, tilPhone, tilDob, tilCity, tilWard;
    private TextInputEditText etFullName, etUsername, etBio, etPhone, etEmail, etDob;
    private TextInputEditText etStreet;
    private MaterialAutoCompleteTextView actvCity, actvWard;
    private List<String> provinces;
    private Map<String, List<String>> wardsByProvince = new LinkedHashMap<>();
    private List<String> currentWards = Collections.emptyList();
    private AccentInsensitiveAdapter wardAdapter;
    private RadioGroup rgGender;
    private View btnSave, btnCancel;
    private LoadingDialog loadingDialog;
    private View btnChangeAvatar;
    private ProgressBar pbAvatar;
    private String newAvatarUrl = null;
    private boolean uploadingAvatar = false;
    private String currentAvatarUrl = null;

    private final ActivityResultLauncher<PickVisualMediaRequest> pickAvatar =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) uploadAvatar(uri);
            });

    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private DocumentReference userRef, privateRef;

    private String originalPhone = "";
    private String originalUsername = "";
    private Calendar selectedDob = null;
    private boolean dataLoaded = false;
    private boolean userDocExists = false;
    private String initialSnapshot = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit_profile);
        applySystemBarPadding();

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();
        userRef = db.collection("users").document(currentUser.getUid());
        privateRef = userRef.collection("private").document("info");
        loadingDialog = new LoadingDialog(this);

        bindViews();
        ivAvatar.setOnClickListener(v -> openAvatarPicker());
        btnChangeAvatar.setOnClickListener(v -> openAvatarPicker());
        btnCancel.setOnClickListener(v -> confirmExit());

        findViewById(R.id.btnNotification).setOnClickListener(v ->
                Toast.makeText(this, "Chức năng Thông báo đang phát triển", Toast.LENGTH_SHORT).show());
        setupBottomNav();
        btnSave.setOnClickListener(v -> validateAndSave());
        etDob.setOnClickListener(v -> showDatePicker());
        tilDob.setEndIconOnClickListener(v -> showDatePicker());

        setupProvinceDropdown();

        getOnBackPressedDispatcher().addCallback(this,
                new androidx.activity.OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        confirmExit();
                    }
                });

        loadData();
    }

    private void bindViews() {
        ivAvatar = findViewById(R.id.ivAvatar);
        tilFullName = findViewById(R.id.tilFullName);
        tilUsername = findViewById(R.id.tilUsername);
        tilBio = findViewById(R.id.tilBio);
        tilPhone = findViewById(R.id.tilPhone);
        tilDob = findViewById(R.id.tilDob);
        tilCity = findViewById(R.id.tilCity);
        tilWard = findViewById(R.id.tilWard);
        etFullName = findViewById(R.id.etFullName);
        etUsername = findViewById(R.id.etUsername);
        etBio = findViewById(R.id.etBio);
        etPhone = findViewById(R.id.etPhone);
        etEmail = findViewById(R.id.etEmail);
        etDob = findViewById(R.id.etDob);
        etStreet = findViewById(R.id.etStreet);
        actvCity = findViewById(R.id.actvCity);
        actvWard = findViewById(R.id.actvWard);
        rgGender = findViewById(R.id.rgGender);
        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);
        btnChangeAvatar = findViewById(R.id.btnChangeAvatar);
        pbAvatar = findViewById(R.id.pbAvatar);
    }

    private void loadData() {
        loadingDialog.show();
        etEmail.setText(currentUser.getEmail() != null ? currentUser.getEmail() : "");

        Tasks.whenAllSuccess(userRef.get(), privateRef.get())
                .addOnSuccessListener(this, results -> {
                    loadingDialog.dismiss();
                    DocumentSnapshot userDoc = (DocumentSnapshot) results.get(0);
                    DocumentSnapshot privateDoc = (DocumentSnapshot) results.get(1);
                    userDocExists = userDoc.exists();
                    fillPublic(userDoc);
                    fillPrivate(privateDoc, userDoc);
                    dataLoaded = true;
                    initialSnapshot = currentFormSnapshot();
                })
                .addOnFailureListener(this, e -> {
                    loadingDialog.dismiss();
                    Toast.makeText(this, "Lỗi tải hồ sơ: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    finish();
                });
    }

    private void fillPublic(DocumentSnapshot doc) {
        String fullName = doc.exists() ? doc.getString("fullName") : null;
        if (isEmpty(fullName)) fullName = currentUser.getDisplayName();
        etFullName.setText(nn(fullName));

        String username = doc.exists() ? doc.getString("username") : null;
        if (username != null && username.startsWith("@")) username = username.substring(1);
        originalUsername = nn(username);
        etUsername.setText(originalUsername);

        etBio.setText(doc.exists() ? nn(doc.getString("bio")) : "");

        String avatarUrl = doc.exists() ? doc.getString("avatarUrl") : null;
        if (isEmpty(avatarUrl)) avatarUrl = AvatarUtils.getProviderPhotoUrl(currentUser);
        currentAvatarUrl = avatarUrl;
        if (isEmpty(avatarUrl)) {
            ivAvatar.setImageResource(R.drawable.ic_person);
        } else {
            Glide.with(this).load(avatarUrl)
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .into(ivAvatar);
        }
    }

    private void fillPrivate(DocumentSnapshot doc, DocumentSnapshot userDoc) {
        String phone = doc.exists() ? doc.getString("phone") : null;
        if (isEmpty(phone) && userDoc.exists()) phone = userDoc.getString("phone");
        originalPhone = nn(phone);
        etPhone.setText(originalPhone);

        if (!doc.exists()) return;

        String gender = doc.getString("gender");
        if ("Nữ".equals(gender)) rgGender.check(R.id.rbFemale);
        else rgGender.check(R.id.rbMale);

        Object dob = doc.get("dob");
        if (dob instanceof Timestamp) {
            selectedDob = Calendar.getInstance();
            selectedDob.setTime(((Timestamp) dob).toDate());
        } else if (dob instanceof String && !isEmpty((String) dob)) {
            try {
                selectedDob = Calendar.getInstance();
                selectedDob.setTime(DOB_FORMAT.parse((String) dob));
            } catch (ParseException e) {
                selectedDob = null;
            }
        }
        if (selectedDob != null) etDob.setText(DOB_FORMAT.format(selectedDob.getTime()));

        @SuppressWarnings("unchecked")
        Map<String, Object> address = (Map<String, Object>) doc.get("address");
        if (address != null) {
            etStreet.setText(str(address.get("street")));
            String city = str(address.get("city"));
            actvCity.setText(city, false);
            onProvinceSelected(city, false);
            actvWard.setText(str(address.get("ward")), false);
        }
    }

    private void setupProvinceDropdown() {
        provinces = Arrays.asList(getResources().getStringArray(R.array.vn_provinces));
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                R.layout.item_province, provinces);
        actvCity.setAdapter(adapter);
        actvCity.setDropDownBackgroundResource(R.drawable.bg_dropdown_light);
        actvCity.setOnItemClickListener((parent, view, position, id) -> {
            tilCity.setError(null);
            onProvinceSelected(adapter.getItem(position), true);
        });

        loadWardsFromAssets();

        actvWard.setDropDownBackgroundResource(R.drawable.bg_dropdown_light);
        actvWard.setThreshold(1);
        actvWard.setOnClickListener(v -> showAllWards());
        actvWard.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) showAllWards(); });
        actvWard.setOnItemClickListener((parent, view, position, id) -> tilWard.setError(null));
    }

    private void loadWardsFromAssets() {
        try (InputStream is = getAssets().open("vn_wards.json")) {
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) != -1) bos.write(buf, 0, n);
            JSONArray arr = new JSONArray(new String(bos.toByteArray(), StandardCharsets.UTF_8));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject p = arr.getJSONObject(i);
                JSONArray w = p.getJSONArray("wards");
                List<String> list = new ArrayList<>(w.length());
                for (int j = 0; j < w.length(); j++) list.add(w.getString(j));
                wardsByProvince.put(p.getString("name"), list);
            }
        } catch (Exception e) {
            Toast.makeText(this, "Không tải được danh sách xã/phường", Toast.LENGTH_LONG).show();
        }
    }

    private void onProvinceSelected(String province, boolean clear) {
        List<String> list = wardsByProvince.get(province);
        currentWards = list != null ? list : Collections.emptyList();
        wardAdapter = new AccentInsensitiveAdapter(this, R.layout.item_province, currentWards);
        actvWard.setAdapter(wardAdapter);

        boolean hasWards = !currentWards.isEmpty();
        tilWard.setEnabled(hasWards);
        tilWard.setHelperText(hasWards
                ? currentWards.size() + " phường/xã - gõ tên để tìm nhanh"
                : "Chọn tỉnh / thành phố trước");
        if (clear) {
            actvWard.setText("", false);
            tilWard.setError(null);
        }
    }

    private void showAllWards() {
        if (wardAdapter == null || currentWards.isEmpty()) return;
        wardAdapter.getFilter().filter(null, count -> actvWard.showDropDown());
    }

    private void showDatePicker() {
        Calendar c = selectedDob != null ? (Calendar) selectedDob.clone() : Calendar.getInstance();
        if (selectedDob == null) c.set(2000, Calendar.JANUARY, 1);

        DatePickerDialog dialog = new DatePickerDialog(this, R.style.EFlea_DatePickerDialog, (view, year, month, day) -> {
            selectedDob = Calendar.getInstance();
            selectedDob.clear();
            selectedDob.set(year, month, day);
            etDob.setText(DOB_FORMAT.format(selectedDob.getTime()));
            tilDob.setError(null);
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));

        dialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        dialog.show();

        int teal = android.graphics.Color.parseColor("#0F766E");
        dialog.getButton(DatePickerDialog.BUTTON_POSITIVE).setTextColor(teal);
        dialog.getButton(DatePickerDialog.BUTTON_NEGATIVE).setTextColor(teal);
    }

    private void openAvatarPicker() {
        if (uploadingAvatar) return;
        pickAvatar.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build());
    }

    private void uploadAvatar(Uri uri) {
        Glide.with(this).load(uri).placeholder(R.drawable.ic_person).into(ivAvatar);
        uploadingAvatar = true;
        pbAvatar.setVisibility(View.VISIBLE);
        btnSave.setEnabled(false);

        UserRepository.getInstance().uploadAvatar(this, uri, new UserRepository.Callback<String>() {
            @Override
            public void onSuccess(String url) {
                if (isFinishing() || isDestroyed()) return;
                uploadingAvatar = false;
                newAvatarUrl = url;
                pbAvatar.setVisibility(View.GONE);
                btnSave.setEnabled(true);
                Toast.makeText(EditProfileActivity.this,
                        "Đã tải ảnh lên, bấm Lưu thay đổi để cập nhật", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String message) {
                if (isFinishing() || isDestroyed()) return;
                uploadingAvatar = false;
                pbAvatar.setVisibility(View.GONE);
                btnSave.setEnabled(true);
                Glide.with(EditProfileActivity.this)
                        .load(newAvatarUrl != null ? newAvatarUrl : currentAvatarUrl)
                        .placeholder(R.drawable.ic_person)
                        .error(R.drawable.ic_person)
                        .into(ivAvatar);
                Toast.makeText(EditProfileActivity.this,
                        "Tải ảnh thất bại: " + message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void validateAndSave() {
        if (!dataLoaded) return;
        if (uploadingAvatar) {
            Toast.makeText(this, "Ảnh đang tải lên, vui lòng đợi chút", Toast.LENGTH_SHORT).show();
            return;
        }
        tilFullName.setError(null);
        tilUsername.setError(null);
        tilPhone.setError(null);
        tilDob.setError(null);

        String fullName = text(etFullName);
        String username = text(etUsername).toLowerCase(Locale.ROOT);
        String phone = text(etPhone).replaceAll("[\\s.-]", "");
        if (phone.startsWith("+84")) phone = "0" + phone.substring(3);

        boolean ok = true;

        if (fullName.isEmpty()) {
            tilFullName.setError("Vui lòng nhập họ và tên");
            ok = false;
        } else if (fullName.length() < 2) {
            tilFullName.setError("Họ tên quá ngắn");
            ok = false;
        }

        if (!username.isEmpty() && !username.matches("^[a-z0-9._]{3,20}$")) {
            tilUsername.setError("Chỉ dùng chữ thường, số, dấu . hoặc _ (3–20 ký tự)");
            ok = false;
        }

        if (!phone.isEmpty() && !phone.matches("^0\\d{9}$")) {
            tilPhone.setError("Số điện thoại không hợp lệ");
            ok = false;
        }

        if (selectedDob != null) {
            Calendar minAge = Calendar.getInstance();
            minAge.add(Calendar.YEAR, -13);
            if (selectedDob.after(minAge)) {
                tilDob.setError("Bạn phải từ 13 tuổi trở lên");
                ok = false;
            }
        }

        tilCity.setError(null);
        String city = text(actvCity);
        if (!city.isEmpty() && !provinces.contains(city)) {
            tilCity.setError("Vui lòng chọn lại tỉnh/thành phố trong danh sách");
            ok = false;
        }

        tilWard.setError(null);
        String ward = text(actvWard);
        if (!ward.isEmpty() && !currentWards.contains(ward)) {
            tilWard.setError("Vui lòng chọn phường/xã trong danh sách");
            ok = false;
        }

        if (!ok) return;

        btnSave.setEnabled(false);
        loadingDialog.show();

        final String phoneFinal = phone;
        checkPhoneFree(phoneFinal,
                () -> checkUnique("username", username, originalUsername, tilUsername, "Tên người dùng đã có người dùng",
                        () -> writeData(fullName, username, phoneFinal)));
    }

    private void checkPhoneFree(String phone, Runnable next) {
        if (phone.isEmpty() || phone.equals(originalPhone)) {
            next.run();
            return;
        }
        db.collection("phoneIndex").document(phone).get()
                .addOnSuccessListener(this, snap -> {
                    if (snap.exists() && !currentUser.getUid().equals(snap.getString("uid"))) {
                        stopSaving();
                        tilPhone.setError("Số điện thoại đã được tài khoản khác sử dụng");
                        tilPhone.requestFocus();
                        return;
                    }
                    next.run();
                })
                .addOnFailureListener(this, e -> {
                    stopSaving();
                    Toast.makeText(this, "Lỗi kiểm tra dữ liệu: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void checkUnique(String field, String value, String original,
                             TextInputLayout til, String errorMsg, Runnable next) {
        if (value.isEmpty() || value.equals(original)) {
            next.run();
            return;
        }
        db.collection("users").whereEqualTo(field, value).limit(2).get()
                .addOnSuccessListener(this, snap -> {
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        if (!d.getId().equals(currentUser.getUid())) {
                            stopSaving();
                            til.setError(errorMsg);
                            til.requestFocus();
                            return;
                        }
                    }
                    next.run();
                })
                .addOnFailureListener(this, e -> {
                    stopSaving();
                    Toast.makeText(this, "Lỗi kiểm tra dữ liệu: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void writeData(String fullName, String username, String phone) {
        // Thông tin công khai
        Map<String, Object> pub = new HashMap<>();
        pub.put("uid", currentUser.getUid());
        pub.put("fullName", fullName);
        pub.put("username", username);
        pub.put("bio", text(etBio));
        pub.put("phone", phone);
        pub.put("updatedAt", FieldValue.serverTimestamp());

        if (!userDocExists) {
            String photo = AvatarUtils.getProviderPhotoUrl(currentUser);
            pub.put("email", currentUser.getEmail() != null ? currentUser.getEmail() : "");
            pub.put("avatarUrl", photo != null ? photo : "");
            pub.put("ratingAvg", 0.0);
            pub.put("ratingCount", 0);
            pub.put("role", "user");
            pub.put("status", "active");
            pub.put("createdAt", FieldValue.serverTimestamp());
        }
        if (newAvatarUrl != null) {
            pub.put("avatarUrl", newAvatarUrl);
        }
        Map<String, Object> address = new HashMap<>();
        address.put("street", text(etStreet));
        address.put("ward", text(actvWard));
        address.put("city", text(actvCity));
        address.put("district", FieldValue.delete());

        Map<String, Object> priv = new HashMap<>();
        priv.put("phone", phone);
        priv.put("gender", getSelectedGender());
        priv.put("dob", selectedDob != null ? new Timestamp(selectedDob.getTime()) : FieldValue.delete());
        priv.put("address", address);
        priv.put("updatedAt", FieldValue.serverTimestamp());

        WriteBatch batch = db.batch();
        batch.set(userRef, pub, SetOptions.merge());
        batch.set(privateRef, priv, SetOptions.merge());

        String email = currentUser.getEmail();
        if (email != null && !email.isEmpty()) {
            if (!originalPhone.isEmpty() && !originalPhone.equals(phone)) {
                batch.delete(db.collection("phoneIndex").document(originalPhone));
            }
            if (!phone.isEmpty()) {
                Map<String, Object> idx = new HashMap<>();
                idx.put("uid", currentUser.getUid());
                idx.put("email", email);
                batch.set(db.collection("phoneIndex").document(phone), idx);
            }
        }
        batch.commit()
                .addOnSuccessListener(this, unused -> {
                    loadingDialog.dismiss();
                    userDocExists = true;
                    Toast.makeText(this, "Đã lưu hồ sơ", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(this, e -> {
                    stopSaving();
                    Toast.makeText(this, "Lưu thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void stopSaving() {
        loadingDialog.dismiss();
        btnSave.setEnabled(true);
    }

    private String getSelectedGender() {
        int id = rgGender.getCheckedRadioButtonId();
        if (id == R.id.rbMale) return "Nam";
        if (id == R.id.rbFemale) return "Nữ";
        return "";
    }

    private void confirmExit() {
        leave(this::finish);
    }

    private void leave(Runnable go) {
        if (!dataLoaded || currentFormSnapshot().equals(initialSnapshot)) {
            go.run();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Bỏ thay đổi?")
                .setMessage("Những thay đổi bạn vừa nhập sẽ không được lưu.")
                .setNegativeButton("Tiếp tục sửa", null)
                .setPositiveButton("Bỏ thay đổi", (d, w) -> go.run())
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
        findViewById(R.id.navHome).setOnClickListener(v -> leave(() -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        }));

        findViewById(R.id.navProfile).setOnClickListener(v -> leave(this::finish));

        findViewById(R.id.navFavorites).setOnClickListener(v ->
                Toast.makeText(this, "Chức năng Yêu thích đang phát triển", Toast.LENGTH_SHORT).show());

        findViewById(R.id.navPost).setOnClickListener(v ->
                Toast.makeText(this, "Chức năng Đăng tin bán đang phát triển", Toast.LENGTH_SHORT).show());

        findViewById(R.id.navMessages).setOnClickListener(v -> leave(() -> {
            startActivity(new Intent(this, ChatListActivity.class));
            finish();
        }));
    }

    private String currentFormSnapshot() {
        return TextUtils.join("|", new String[]{
                text(etFullName), text(etUsername), text(etBio), text(etPhone),
                getSelectedGender(), text(etDob),
                text(etStreet), text(actvWard), text(actvCity),
                String.valueOf(newAvatarUrl)});
    }

    private static String text(TextView et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }

    private static boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String nn(String s) {
        return s == null ? "" : s;
    }

    private static String str(Object o) {
        return o == null ? "" : o.toString();
    }
}