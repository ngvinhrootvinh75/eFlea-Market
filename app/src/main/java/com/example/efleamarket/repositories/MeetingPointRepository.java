package com.example.efleamarket.repositories;

import com.example.efleamarket.models.MeetingPoint;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MeetingPointRepository {

    public interface Callback<T> {
        void onSuccess(T data);
        void onError(String message);
    }

    private static final String COL = "meeting_points";
    public static final String SEED_UID = "zAnu6QpIpqRRk1Y1COlQs7LLfsp2";

    private static final Object[][] SEED = {
            {"nvh-thanh-nien", "Nhà Văn hóa Thanh niên", "4 Phạm Ngọc Thạch, Bến Nghé", "Quận 1", 10.7797, 106.6990},
            {"cv-tao-dan", "Công viên Tao Đàn (cổng Trương Định)", "Trương Định, Bến Thành", "Quận 1", 10.7745, 106.6927},
            {"pho-di-bo-nguyen-hue", "Phố đi bộ Nguyễn Huệ", "Nguyễn Huệ, Bến Nghé", "Quận 1", 10.7743, 106.7034},
            {"cv-le-van-tam", "Công viên Lê Văn Tám", "Hai Bà Trưng, Đa Kao", "Quận 1", 10.7889, 106.6950},
            {"dh-bach-khoa", "Trường ĐH Bách khoa (cổng chính)", "268 Lý Thường Kiệt", "Quận 10", 10.7724, 106.6579},
            {"cv-hoang-van-thu", "Công viên Hoàng Văn Thụ", "Hoàng Văn Thụ", "Tân Bình", 10.8006, 106.6655},
            {"cv-gia-dinh", "Công viên Gia Định", "Hoàng Minh Giám", "Gò Vấp", 10.8139, 106.6780},
            {"aeon-tan-phu", "AEON Mall Tân Phú (sảnh chính)", "30 Bờ Bao Tân Thắng, Sơn Kỳ", "Tân Phú", 10.8017, 106.6177},
            {"nvh-sinh-vien", "Nhà Văn hóa Sinh viên ĐHQG", "Làng Đại học, Đông Hòa", "Thủ Đức", 10.8753, 106.8006}
    };

    private static MeetingPointRepository instance;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private MeetingPointRepository() {}

    public static synchronized MeetingPointRepository getInstance() {
        if (instance == null) instance = new MeetingPointRepository();
        return instance;
    }

    public void getActivePoints(Callback<List<MeetingPoint>> cb) {
        db.collection(COL).whereEqualTo("active", true).get()
                .addOnSuccessListener(snap -> {
                    List<MeetingPoint> list = snap.toObjects(MeetingPoint.class);
                    if (list.isEmpty() && SEED_UID.equals(FirebaseAuth.getInstance().getUid())) {
                        seed(cb);
                        return;
                    }
                    cb.onSuccess(list);
                })
                .addOnFailureListener(e -> cb.onError(e.getMessage()));
    }

    private void seed(Callback<List<MeetingPoint>> cb) {
        WriteBatch batch = db.batch();
        for (Object[] p : SEED) {
            Map<String, Object> m = new HashMap<>();
            m.put("name", p[1]);
            m.put("address", p[2]);
            m.put("district", p[3]);
            m.put("lat", p[4]);
            m.put("lng", p[5]);
            m.put("note", "Nơi công cộng, đông người qua lại");
            m.put("active", true);
            batch.set(db.collection(COL).document((String) p[0]), m);
        }
        batch.commit()
                .addOnSuccessListener(v -> db.collection(COL).whereEqualTo("active", true).get()
                        .addOnSuccessListener(s -> cb.onSuccess(s.toObjects(MeetingPoint.class)))
                        .addOnFailureListener(e -> cb.onError(e.getMessage())))
                .addOnFailureListener(e -> cb.onError(e.getMessage()));
    }
}