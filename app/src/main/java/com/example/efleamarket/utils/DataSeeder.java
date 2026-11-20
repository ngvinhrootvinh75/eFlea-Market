package com.example.efleamarket.utils;

import android.content.Context;
import android.widget.Toast;

import com.example.efleamarket.models.Product;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class DataSeeder {

    public static void seedProducts(Context context) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseAuth auth = FirebaseAuth.getInstance();

        String currentUid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "admin_test";
        String currentName = auth.getCurrentUser() != null && auth.getCurrentUser().getDisplayName() != null
                ? auth.getCurrentUser().getDisplayName() : "Sinh viên Bách Khoa";

        // Kiểm tra nếu collection products chưa có món nào thì mới tạo
        db.collection("products").limit(1).get().addOnSuccessListener(snapshot -> {
            if (snapshot.isEmpty()) {
                List<Product> list = new ArrayList<>();

                list.add(new Product(
                        "Laptop Dell Inspiron 15 Core i5 8GB",
                        5500000,
                        "laptop",
                        "Đã qua sử dụng (90%)",
                        "Máy dùng mượt mà, pin 3 tiếng, phù hợp sinh viên học IT, văn phòng.",
                        "Quận 10, TP.HCM",
                        "https://images.unsplash.com/photo-1496181133206-80ce9b88a853?w=500",
                        currentUid, currentName, "selling"
                ));

                list.add(new Product(
                        "iPhone 11 64GB Quốc Tế Pin 85%",
                        4200000,
                        "dien-thoai",
                        "Đã qua sử dụng",
                        "Máy nguyên zin chưa sửa chữa, có kèm cáp sạc và ốp lưng.",
                        "TP. Thủ Đức, TP.HCM",
                        "https://images.unsplash.com/photo-1510557880182-3d4d3cba35a5?w=500",
                        currentUid, "Nguyễn Văn Nam", "selling"
                ));

                list.add(new Product(
                        "Tai nghe Bluetooth Sony WH-CH520",
                        850000,
                        "phu-kien",
                        "Như mới (98%)",
                        "Tai nghe âm thanh cực hay, pin trâu 50h, đầy đủ hộp sạc.",
                        "Bình Thạnh, TP.HCM",
                        "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=500",
                        currentUid, "Lê Hoàng Long", "selling"
                ));

                list.add(new Product(
                        "Combo Giáo trình Giải Tích 1, 2 và Đại Số",
                        90000,
                        "sach",
                        "Như mới",
                        "Sách đại cương không ghi chú bẩn, pass lại nửa giá cho khóa dưới.",
                        "Quận 5, TP.HCM",
                        "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=500",
                        currentUid, "Trần Thị Mai", "selling"
                ));

                list.add(new Product(
                        "Bàn học gấp gọn sinh viên chân sắt",
                        150000,
                        "noi-that",
                        "Còn tốt",
                        "Bàn gỗ MDF chân sắt chắc chắn, có khe để iPad/điện thoại học online.",
                        "Quận 7, TP.HCM",
                        "https://images.unsplash.com/photo-1518455027359-f3f8164ba6bd?w=500",
                        currentUid, currentName, "selling"
                ));

                for (Product p : list) {
                    db.collection("products").add(p);
                }

                Toast.makeText(context, "Đã bơm thành công 5 sản phẩm mẫu lên Firestore!", Toast.LENGTH_LONG).show();
            }
        });
    }
}