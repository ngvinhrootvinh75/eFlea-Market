package com.example.efleamarket.activities;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.efleamarket.R;
import com.example.efleamarket.models.MeetingPlace;
import com.example.efleamarket.models.MeetingPoint;
import com.example.efleamarket.repositories.MeetingPointRepository;
import com.example.efleamarket.utils.ChatFormat;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.util.MapTileIndex;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MeetingPickerActivity extends AppCompatActivity {
    public static final String EXTRA_NAME = "name";
    public static final String EXTRA_LAT = "lat";
    public static final String EXTRA_LNG = "lng";
    public static final String EXTRA_POINT_ID = "pointId";
    public static final String EXTRA_TIME = "time";
    public static final String EXTRA_MODE = "mode";
    public static final String MODE_ADDRESS = "address";

    private static final GeoPoint DEFAULT_CENTER = new GeoPoint(10.7769, 106.7009);
    private static final String USER_AGENT = "eFleaMarket/1.0 (+https://github.com/ngvinhrootvinh75/eFlea-Market)";

    private static final OnlineTileSourceBase ESRI_STREET = new OnlineTileSourceBase(
            "EsriWorldStreet", 1, 19, 256, "",
            new String[]{"https://server.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer/tile/"}) {
        @Override
        public String getTileURLString(long index) {
            return getBaseUrl() + MapTileIndex.getZoom(index) + "/"
                    + MapTileIndex.getY(index) + "/" + MapTileIndex.getX(index);
        }
    };

    public static Intent intent(Context ctx, MeetingPlace place, Date time) {
        Intent i = new Intent(ctx, MeetingPickerActivity.class);
        if (place != null) {
            i.putExtra(EXTRA_NAME, place.getName());
            i.putExtra(EXTRA_LAT, place.getLat());
            i.putExtra(EXTRA_LNG, place.getLng());
            i.putExtra(EXTRA_POINT_ID, place.getPointId());
        }
        if (time != null) i.putExtra(EXTRA_TIME, time.getTime());
        return i;
    }

    public static Intent intentAddress(Context ctx, String address, double lat, double lng) {
        Intent i = new Intent(ctx, MeetingPickerActivity.class).putExtra(EXTRA_MODE, MODE_ADDRESS);
        if (address != null && !(lat == 0 && lng == 0)) {
            i.putExtra(EXTRA_NAME, address);
            i.putExtra(EXTRA_LAT, lat);
            i.putExtra(EXTRA_LNG, lng);
        }
        return i;
    }

    private boolean addressMode;
    private MapView mapView;
    private IMapController controller;
    private Marker marker;
    private MyLocationNewOverlay myLocation;
    private GeoPoint selected;
    private String pointId = "";
    private long selectedTime;
    private EditText etName, etSearch;
    private TextView tvTime;
    private View tvHintCard;
    private ChipGroup chipPoints;
    private ActivityResultLauncher<String[]> locationPermission;
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE));
        Configuration.getInstance().setUserAgentValue(USER_AGENT);
        setContentView(R.layout.meeting_picker);
        applyInsets();

        etName = findViewById(R.id.etPlaceName);
        etSearch = findViewById(R.id.etSearch);
        tvTime = findViewById(R.id.tvTime);
        tvHintCard = findViewById(R.id.tvHintCard);
        chipPoints = findViewById(R.id.chipPoints);
        mapView = findViewById(R.id.mapView);

        Intent in = getIntent();
        addressMode = MODE_ADDRESS.equals(in.getStringExtra(EXTRA_MODE));
        applyMode();

        if (in.hasExtra(EXTRA_LAT) && in.hasExtra(EXTRA_LNG)) {
            selected = new GeoPoint(in.getDoubleExtra(EXTRA_LAT, 0), in.getDoubleExtra(EXTRA_LNG, 0));
            String name = in.getStringExtra(EXTRA_NAME);
            if (name != null) etName.setText(name);
            String pid = in.getStringExtra(EXTRA_POINT_ID);
            pointId = pid == null ? "" : pid;
        }
        long t = in.getLongExtra(EXTRA_TIME, 0);
        if (t > System.currentTimeMillis()) setTime(t);

        setupMap();
        if (!addressMode) loadMeetingPoints();

        locationPermission = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                    if (hasLocationPermission()) showMyLocation(true);
                    else Toast.makeText(this, "Cần quyền vị trí để hiện vị trí của bạn", Toast.LENGTH_SHORT).show();
                });

        TextInputLayout tilSearch = findViewById(R.id.tilSearch);
        tilSearch.setEndIconOnClickListener(v -> search());
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) { search(); return true; }
            return false;
        });

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnMyLocation).setOnClickListener(v -> {
            if (hasLocationPermission()) showMyLocation(true);
            else locationPermission.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION});
        });
        findViewById(R.id.cardTime).setOnClickListener(v -> pickDate());
        findViewById(R.id.btnConfirm).setOnClickListener(v -> confirm());
    }

    private void applyMode() {
        if (!addressMode) return;
        ((TextView) findViewById(R.id.tvPickerTitle)).setText("Địa chỉ nhận hàng");
        ((TextView) findViewById(R.id.tvHint)).setText("Tìm địa chỉ hoặc chạm vào bản đồ để chọn nơi nhận hàng");
        ((TextInputLayout) findViewById(R.id.tilPlaceName)).setHint("Địa chỉ nhận hàng");
        ((MaterialButton) findViewById(R.id.btnConfirm)).setText("XÁC NHẬN ĐỊA CHỈ");
        findViewById(R.id.cardTime).setVisibility(View.GONE);
        findViewById(R.id.tvPointsTitle).setVisibility(View.GONE);
        findViewById(R.id.scrollPoints).setVisibility(View.GONE);
    }

    private void applyInsets() {
        View root = findViewById(R.id.rootPicker);
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

    private void setupMap() {
        mapView.setTileSource(ESRI_STREET);
        mapView.setMultiTouchControls(true);
        mapView.setTilesScaledToDpi(true);
        mapView.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT);
        mapView.setMinZoomLevel(4.0);
        mapView.setMaxZoomLevel(19.0);

        controller = mapView.getController();
        controller.setZoom(selected != null ? 17.0 : 12.5);
        controller.setCenter(selected != null ? selected : DEFAULT_CENTER);

        MapEventsOverlay events = new MapEventsOverlay(new MapEventsReceiver() {
            @Override public boolean singleTapConfirmedHelper(GeoPoint p) {
                pointId = "";
                placeMarker(p, true);
                return true;
            }
            @Override public boolean longPressHelper(GeoPoint p) { return false; }
        });
        mapView.getOverlays().add(0, events);

        if (selected != null) placeMarker(selected, false);
        if (hasLocationPermission()) showMyLocation(false);
    }

    private void search() {
        String q = etSearch.getText() == null ? "" : etSearch.getText().toString().trim();
        if (q.isEmpty()) return;
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
        Toast.makeText(this, "Đang tìm…", Toast.LENGTH_SHORT).show();
        io.execute(() -> {
            List<Address> found = null;
            try {
                if (Geocoder.isPresent()) {
                    found = new Geocoder(this, new Locale("vi", "VN"))
                            .getFromLocationName(q, 6, 8.0, 102.0, 23.5, 110.0);
                }
            } catch (Exception ignored) { }
            final List<Address> result = found;
            runOnUiThread(() -> showResults(result));
        });
    }

    private void showResults(List<Address> result) {
        if (isFinishing() || isDestroyed()) return;
        if (result == null || result.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy, hãy ghi rõ hơn (ví dụ: tên đường + quận)", Toast.LENGTH_LONG).show();
            return;
        }
        if (result.size() == 1) { selectAddress(result.get(0)); return; }
        String[] items = new String[result.size()];
        for (int i = 0; i < result.size(); i++) items[i] = label(result.get(i));
        new MaterialAlertDialogBuilder(this)
                .setTitle("Chọn kết quả")
                .setItems(items, (d, which) -> selectAddress(result.get(which)))
                .show();
    }

    private void selectAddress(Address a) {
        GeoPoint g = new GeoPoint(a.getLatitude(), a.getLongitude());
        pointId = "";
        placeMarker(g, false);
        controller.animateTo(g, 17.0, 600L);
        String l = label(a);
        etName.setText(l.length() > 200 ? l.substring(0, 200) : l);
    }

    private static String label(Address a) {
        String line = a.getAddressLine(0);
        if (line != null && !line.isEmpty()) return line;
        return a.getFeatureName() == null ? "" : a.getFeatureName();
    }

    private void loadMeetingPoints() {
        MeetingPointRepository.getInstance().getActivePoints(new MeetingPointRepository.Callback<List<MeetingPoint>>() {
            @Override public void onSuccess(List<MeetingPoint> points) {
                if (isFinishing() || isDestroyed() || points.isEmpty()) return;
                Drawable icon = ContextCompat.getDrawable(MeetingPickerActivity.this, R.drawable.ic_meeting_point);
                for (MeetingPoint p : points) {
                    Marker m = new Marker(mapView);
                    m.setPosition(new GeoPoint(p.getLat(), p.getLng()));
                    m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                    m.setTitle(p.getName());
                    if (icon != null) m.setIcon(icon);
                    m.setOnMarkerClickListener((mk, mv) -> { selectPoint(p); return true; });
                    mapView.getOverlays().add(1, m);

                    Chip chip = new Chip(MeetingPickerActivity.this);
                    chip.setText(p.getName());
                    chip.setOnClickListener(v -> selectPoint(p));
                    chipPoints.addView(chip);
                }
                findViewById(R.id.tvPointsTitle).setVisibility(View.VISIBLE);
                findViewById(R.id.scrollPoints).setVisibility(View.VISIBLE);
                mapView.invalidate();
            }
            @Override public void onError(String message) { }
        });
    }

    private void selectPoint(MeetingPoint p) {
        GeoPoint g = new GeoPoint(p.getLat(), p.getLng());
        pointId = p.getId() == null ? "" : p.getId();
        etName.setText(p.getName());
        placeMarker(g, false);
        controller.animateTo(g, 17.0, 600L);
        String detail = p.getAddress() == null ? "" : p.getAddress() + ", " + p.getDistrict();
        Toast.makeText(this, "Đã chọn: " + p.getName() + (detail.isEmpty() ? "" : "\n" + detail), Toast.LENGTH_SHORT).show();
    }

    private void placeMarker(GeoPoint p, boolean lookupName) {
        selected = p;
        if (marker == null) {
            marker = new Marker(mapView);
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            marker.setTitle(addressMode ? "Địa chỉ nhận hàng" : "Điểm hẹn");
            marker.setOnMarkerClickListener((m, mv) -> true);
            mapView.getOverlays().add(marker);
        }
        marker.setPosition(p);
        mapView.invalidate();
        tvHintCard.setVisibility(View.GONE);
        if (lookupName) reverseGeocode(p);
    }

    @SuppressWarnings("deprecation")
    private void reverseGeocode(GeoPoint p) {
        io.execute(() -> {
            String found = null;
            try {
                if (Geocoder.isPresent()) {
                    List<Address> list = new Geocoder(this, new Locale("vi", "VN"))
                            .getFromLocation(p.getLatitude(), p.getLongitude(), 1);
                    if (list != null && !list.isEmpty()) found = label(list.get(0));
                }
            } catch (Exception ignored) { }
            final String result = found;
            runOnUiThread(() -> {
                if (result == null || result.isEmpty() || isFinishing() || isDestroyed()) return;
                etName.setText(result.length() > 200 ? result.substring(0, 200) : result);
            });
        });
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void showMyLocation(boolean moveCamera) {
        if (!hasLocationPermission()) return;
        if (myLocation == null) {
            myLocation = new MyLocationNewOverlay(new GpsMyLocationProvider(this), mapView);
            mapView.getOverlays().add(myLocation);
        }
        myLocation.enableMyLocation();
        if (!moveCamera) return;
        GeoPoint now = myLocation.getMyLocation();
        if (now != null) {
            controller.animateTo(now, 17.0, 800L);
            if (addressMode) placeMarker(now, true);
        } else {
            Toast.makeText(this, "Đang lấy vị trí, hãy bật GPS", Toast.LENGTH_SHORT).show();
            myLocation.runOnFirstFix(() -> runOnUiThread(() -> {
                GeoPoint fix = myLocation.getMyLocation();
                if (fix == null || isFinishing()) return;
                controller.animateTo(fix, 17.0, 800L);
                if (addressMode) placeMarker(fix, true);
            }));
        }
    }

    private void pickDate() {
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Chọn ngày hẹn")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .setCalendarConstraints(new CalendarConstraints.Builder()
                        .setValidator(DateValidatorPointForward.now())
                        .build())
                .build();
        datePicker.addOnPositiveButtonClickListener(selection -> {
            Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            utc.setTimeInMillis(selection);
            pickTime(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH));
        });
        datePicker.show(getSupportFragmentManager(), "date");
    }

    private void pickTime(int year, int month, int day) {
        MaterialTimePicker timePicker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(15)
                .setMinute(0)
                .setTitleText("Chọn giờ hẹn")
                .build();
        timePicker.addOnPositiveButtonClickListener(v -> {
            Calendar c = Calendar.getInstance();
            c.set(year, month, day, timePicker.getHour(), timePicker.getMinute(), 0);
            c.set(Calendar.MILLISECOND, 0);
            if (c.getTimeInMillis() <= System.currentTimeMillis()) {
                Toast.makeText(this, "Giờ hẹn phải ở tương lai", Toast.LENGTH_SHORT).show();
                return;
            }
            setTime(c.getTimeInMillis());
        });
        timePicker.show(getSupportFragmentManager(), "time");
    }

    private void setTime(long millis) {
        selectedTime = millis;
        tvTime.setText(ChatFormat.dateTime(new Date(millis)));
        tvTime.setTextColor(0xFF0F172A);
    }

    private void confirm() {
        String name = etName.getText() == null ? "" : etName.getText().toString().trim();
        if (selected == null) {
            Toast.makeText(this, addressMode ? "Hãy tìm hoặc chạm bản đồ để chọn địa chỉ" : "Chạm vào bản đồ để ghim điểm hẹn", Toast.LENGTH_SHORT).show();
            return;
        }
        if (name.isEmpty()) {
            Toast.makeText(this, addressMode ? "Vui lòng nhập địa chỉ" : "Vui lòng đặt tên điểm hẹn", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!addressMode && selectedTime <= System.currentTimeMillis()) {
            Toast.makeText(this, "Vui lòng chọn ngày giờ hẹn", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent data = new Intent()
                .putExtra(EXTRA_NAME, name)
                .putExtra(EXTRA_LAT, selected.getLatitude())
                .putExtra(EXTRA_LNG, selected.getLongitude())
                .putExtra(EXTRA_POINT_ID, pointId);
        if (!addressMode) data.putExtra(EXTRA_TIME, selectedTime);
        setResult(RESULT_OK, data);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
        if (myLocation != null && hasLocationPermission()) myLocation.enableMyLocation();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
        if (myLocation != null) myLocation.disableMyLocation();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        io.shutdown();
    }
}