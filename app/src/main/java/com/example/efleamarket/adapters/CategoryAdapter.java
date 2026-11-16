package com.example.efleamarket.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.efleamarket.R;
import com.example.efleamarket.models.Category;

import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CatViewHolder> {

    public interface OnCategorySelectListener {
        void onSelect(Category category);
    }

    private final Context mContext;
    private final List<Category> mList;
    private final OnCategorySelectListener mListener;

    public CategoryAdapter(Context context, List<Category> list, OnCategorySelectListener listener) {
        this.mContext = context;
        this.mList = list;
        this.mListener = listener;
    }

    @NonNull
    @Override
    public CatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(mContext).inflate(R.layout.item_category, parent, false);
        return new CatViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull CatViewHolder holder, int position) {
        Category item = mList.get(position);
        holder.tvName.setText(item.getName());

        // Dùng icon ic_shopping_cart có sẵn trong project
        holder.ivIcon.setImageResource(R.drawable.ic_shopping_cart);

        holder.itemView.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onSelect(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mList != null ? mList.size() : 0;
    }

    // Đổi thành public static class để tránh cảnh báo visibility
    public static class CatViewHolder extends RecyclerView.ViewHolder {
        public final ImageView ivIcon;
        public final TextView tvName;

        public CatViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.ivCategoryIcon);
            tvName = itemView.findViewById(R.id.tvCategoryName);
        }
    }
}