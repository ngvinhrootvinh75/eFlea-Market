package com.example.efleamarket.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.efleamarket.R;
import com.example.efleamarket.models.Product;
import com.example.efleamarket.utils.CloudinaryHelper;
import com.google.android.material.button.MaterialButton;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/** Danh sách "Tin đăng của tôi" với các nút Sửa / Đã bán / Xoá. */
public class MyProductAdapter extends RecyclerView.Adapter<MyProductAdapter.VH> {

    public interface Listener {
        void onOpen(Product p);
        void onEdit(Product p);
        void onToggleSold(Product p);
        void onDelete(Product p);
    }

    private final List<Product> items;
    private final Listener listener;
    private final DecimalFormat priceFormatter = new DecimalFormat("#,### đ");
    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    public MyProductAdapter(List<Product> items, Listener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_my_product, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Product p = items.get(position);

        h.tvTitle.setText(p.getTitle());
        h.tvPrice.setText(priceFormatter.format(p.getPrice()));
        h.tvDate.setText(p.getCreatedAt() != null ? dateFormatter.format(p.getCreatedAt()) : "");

        Glide.with(h.ivThumb)
                .load(CloudinaryHelper.thumbnail(p.getCoverImage(), 300))
                .placeholder(R.drawable.ic_shopping_cart)
                .error(R.drawable.ic_shopping_cart)
                .centerCrop()
                .into(h.ivThumb);

        // Nhãn trạng thái
        String status = p.getStatus() != null ? p.getStatus() : Product.STATUS_SELLING;
        switch (status) {
            case Product.STATUS_SOLD:
                setBadge(h.tvStatus, "Đã bán", 0xFFF1F5F9, 0xFF64748B);
                break;
            case Product.STATUS_PENDING:
                setBadge(h.tvStatus, "Chờ duyệt", 0xFFFEF3C7, 0xFF92400E);
                break;
            case Product.STATUS_HIDDEN:
                setBadge(h.tvStatus, "Bị ẩn", 0xFFFEE2E2, 0xFFB91C1C);
                break;
            default:
                setBadge(h.tvStatus, "Đang bán", 0xFFE6F4F1, 0xFF0F766E);
        }

        // Chỉ cho đổi qua lại giữa Đang bán và Đã bán
        boolean canToggle = Product.STATUS_SELLING.equals(status) || Product.STATUS_SOLD.equals(status);
        h.btnToggleSold.setVisibility(canToggle ? View.VISIBLE : View.GONE);
        h.btnToggleSold.setText(Product.STATUS_SOLD.equals(status) ? "Bán lại" : "Đã bán");

        h.itemView.setOnClickListener(v -> listener.onOpen(p));
        h.btnEdit.setOnClickListener(v -> listener.onEdit(p));
        h.btnToggleSold.setOnClickListener(v -> listener.onToggleSold(p));
        h.btnDelete.setOnClickListener(v -> listener.onDelete(p));
    }

    private static void setBadge(TextView tv, String text, int bgColor, int textColor) {
        tv.setText(text);
        tv.setBackgroundColor(bgColor);
        tv.setTextColor(textColor);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final ImageView ivThumb;
        final TextView tvTitle, tvPrice, tvStatus, tvDate;
        final MaterialButton btnEdit, btnToggleSold, btnDelete;

        VH(@NonNull View v) {
            super(v);
            ivThumb = v.findViewById(R.id.ivThumb);
            tvTitle = v.findViewById(R.id.tvTitle);
            tvPrice = v.findViewById(R.id.tvPrice);
            tvStatus = v.findViewById(R.id.tvStatus);
            tvDate = v.findViewById(R.id.tvDate);
            btnEdit = v.findViewById(R.id.btnEdit);
            btnToggleSold = v.findViewById(R.id.btnToggleSold);
            btnDelete = v.findViewById(R.id.btnDelete);
        }
    }
}