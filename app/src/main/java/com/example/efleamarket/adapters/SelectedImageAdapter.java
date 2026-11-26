package com.example.efleamarket.adapters;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.efleamarket.R;

import java.util.List;

/** Hiển thị các ảnh đã chọn ở màn hình Đăng tin (có nút xoá). */
public class SelectedImageAdapter extends RecyclerView.Adapter<SelectedImageAdapter.VH> {

    public interface OnRemoveListener {
        void onRemove(int position);
    }

    private final List<Uri> items;
    private final OnRemoveListener listener;

    public SelectedImageAdapter(List<Uri> items, OnRemoveListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_selected_image, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Glide.with(holder.ivImage).load(items.get(position)).centerCrop().into(holder.ivImage);
        holder.tvCover.setVisibility(position == 0 ? View.VISIBLE : View.GONE);
        holder.btnRemove.setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos != RecyclerView.NO_POSITION && listener != null) listener.onRemove(pos);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final ImageView ivImage;
        final TextView tvCover, btnRemove;

        VH(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.ivImage);
            tvCover = itemView.findViewById(R.id.tvCover);
            btnRemove = itemView.findViewById(R.id.btnRemove);
        }
    }
}