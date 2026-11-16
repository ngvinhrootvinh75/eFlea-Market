package com.example.efleamarket.adapters;

import android.content.Context;
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

import java.text.DecimalFormat;
import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProdViewHolder> {

    public interface OnProductClickListener {
        void onClick(Product product);
    }

    private final Context mContext;
    private final List<Product> mProductList;
    private final OnProductClickListener mListener;
    private final DecimalFormat mPriceFormatter = new DecimalFormat("#,### đ");

    public ProductAdapter(Context context, List<Product> productList, OnProductClickListener listener) {
        this.mContext = context;
        this.mProductList = productList;
        this.mListener = listener;
    }

    @NonNull
    @Override
    public ProdViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.item_product, parent, false);
        return new ProdViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProdViewHolder holder, int position) {
        Product prod = mProductList.get(position);

        holder.tvTitle.setText(prod.getTitle());
        holder.tvPrice.setText(mPriceFormatter.format(prod.getPrice()));
        holder.tvLocation.setText(prod.getLocation() != null ? prod.getLocation() : "Toàn quốc");
        holder.tvBadge.setText(prod.getCondition() != null ? prod.getCondition() : "Đã qua sử dụng");

        // Tải ảnh từ URL mạng bằng Glide
        Glide.with(mContext)
                .load(prod.getImageUrl())
                .placeholder(R.drawable.ic_shopping_cart)
                .error(R.drawable.ic_shopping_cart)
                .into(holder.ivThumb);

        holder.itemView.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onClick(prod);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mProductList != null ? mProductList.size() : 0;
    }

    static class ProdViewHolder extends RecyclerView.ViewHolder {
        final ImageView ivThumb;
        final TextView tvBadge, tvTitle, tvPrice, tvLocation;

        ProdViewHolder(@NonNull View itemView) {
            super(itemView);
            ivThumb = itemView.findViewById(R.id.ivProductImage);
            tvBadge = itemView.findViewById(R.id.tvConditionBadge);
            tvTitle = itemView.findViewById(R.id.tvProductTitle);
            tvPrice = itemView.findViewById(R.id.tvProductPrice);
            tvLocation = itemView.findViewById(R.id.tvProductLocation);
        }
    }
}