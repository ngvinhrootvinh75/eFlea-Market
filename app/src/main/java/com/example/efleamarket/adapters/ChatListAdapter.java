package com.example.efleamarket.adapters;

import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.efleamarket.R;
import com.example.efleamarket.models.Chat;
import com.example.efleamarket.utils.ChatFormat;
import com.example.efleamarket.utils.CloudinaryHelper;

import java.util.Objects;

public class ChatListAdapter extends ListAdapter<Chat, ChatListAdapter.VH> {
    public interface OnChatClick { void onClick(Chat chat); }
    public interface OnChatLongClick { void onLongClick(Chat chat); }

    private final String myUid;
    private final OnChatClick listener;
    private final OnChatLongClick longListener;

    public ChatListAdapter(String myUid, OnChatClick listener, OnChatLongClick longListener) {
        super(DIFF);
        this.myUid = myUid;
        this.listener = listener;
        this.longListener = longListener;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
        return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_chat, p, false));
    }

    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        Chat c = getItem(pos);
        h.tvName.setText(c.getOtherName(myUid));
        h.tvProduct.setText(c.getProductTitle());
        String prefix = myUid.equals(c.getLastSenderId()) ? "Bạn: " : "";
        h.tvLast.setText(prefix + c.getLastMessage());
        h.tvTime.setText(ChatFormat.time(c.getUpdatedAt()));

        int unread = c.getUnreadFor(myUid);
        boolean hasUnread = unread > 0;
        h.tvLast.setTypeface(null, hasUnread ? Typeface.BOLD : Typeface.NORMAL);
        h.tvLast.setTextColor(hasUnread ? 0xFF0F172A : 0xFF6B7280);
        h.tvTime.setTypeface(null, hasUnread ? Typeface.BOLD : Typeface.NORMAL);
        h.tvTime.setTextColor(hasUnread ? 0xFF0F766E : 0xFF9CA3AF);
        h.badgeUnread.setVisibility(hasUnread ? View.VISIBLE : View.GONE);
        if (hasUnread) h.tvUnread.setText(unread > 99 ? "99+" : String.valueOf(unread));

        String avatar = c.getOtherAvatar(myUid);
        Glide.with(h.itemView)
                .load(avatar == null || avatar.isEmpty() ? null : CloudinaryHelper.thumbnail(avatar, 150))
                .placeholder(R.drawable.ic_person).error(R.drawable.ic_person)
                .circleCrop().into(h.ivAvatar);
        Glide.with(h.itemView)
                .load(CloudinaryHelper.thumbnail(c.getProductImage(), 150))
                .placeholder(R.drawable.ic_shopping_cart).error(R.drawable.ic_shopping_cart)
                .centerCrop().into(h.ivProduct);

        h.itemView.setOnClickListener(v -> listener.onClick(c));
        h.itemView.setOnLongClickListener(v -> {
            longListener.onLongClick(c);
            return true;
        });
    }

    static class VH extends RecyclerView.ViewHolder {
        final ImageView ivAvatar, ivProduct;
        final TextView tvName, tvProduct, tvLast, tvTime, tvUnread;
        final View badgeUnread;
        VH(View v) {
            super(v);
            ivAvatar = v.findViewById(R.id.ivAvatar);
            ivProduct = v.findViewById(R.id.ivProduct);
            tvName = v.findViewById(R.id.tvName);
            tvProduct = v.findViewById(R.id.tvProduct);
            tvLast = v.findViewById(R.id.tvLastMessage);
            tvTime = v.findViewById(R.id.tvTime);
            badgeUnread = v.findViewById(R.id.badgeUnread);
            tvUnread = v.findViewById(R.id.tvUnread);
        }
    }

    private static final DiffUtil.ItemCallback<Chat> DIFF = new DiffUtil.ItemCallback<Chat>() {
        @Override public boolean areItemsTheSame(@NonNull Chat a, @NonNull Chat b) {
            return Objects.equals(a.getId(), b.getId());
        }
        @Override public boolean areContentsTheSame(@NonNull Chat a, @NonNull Chat b) {
            return Objects.equals(a.getLastMessage(), b.getLastMessage())
                    && Objects.equals(a.getUpdatedAt(), b.getUpdatedAt())
                    && Objects.equals(a.getUnread(), b.getUnread())
                    && Objects.equals(a.getBuyerName(), b.getBuyerName())
                    && Objects.equals(a.getSellerName(), b.getSellerName());
        }
    };
}