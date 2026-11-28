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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ChatListAdapter extends ListAdapter<ChatListAdapter.Group, ChatListAdapter.VH> {
    public interface OnGroupClick { void onClick(Group group); }
    public interface OnGroupLongClick { void onLongClick(Group group); }

    public static class Group {
        public final String otherId;
        public final List<Chat> chats;

        Group(String otherId, List<Chat> chats) {
            this.otherId = otherId;
            this.chats = chats;
        }

        public Chat latest() { return chats.get(0); }

        public int unreadFor(String uid) {
            int sum = 0;
            for (Chat c : chats) sum += c.getUnreadFor(uid);
            return sum;
        }

        String signature(String uid) {
            StringBuilder sb = new StringBuilder();
            for (Chat c : chats) {
                sb.append(c.getId()).append('|').append(c.getLastMessage()).append('|')
                        .append(c.getUpdatedAt()).append('|').append(c.getUnreadFor(uid)).append(';');
            }
            return sb.toString();
        }
    }

    public static List<Group> group(List<Chat> chats, String myUid) {
        Map<String, List<Chat>> map = new LinkedHashMap<>();
        if (chats != null) {
            for (Chat c : chats) {
                String other = c.getOtherId(myUid);
                if (other == null) continue;
                List<Chat> list = map.get(other);
                if (list == null) { list = new ArrayList<>(); map.put(other, list); }
                list.add(c);
            }
        }
        List<Group> result = new ArrayList<>();
        for (Map.Entry<String, List<Chat>> e : map.entrySet()) result.add(new Group(e.getKey(), e.getValue()));
        return result;
    }

    private final String myUid;
    private final OnGroupClick listener;
    private final OnGroupLongClick longListener;

    public ChatListAdapter(String myUid, OnGroupClick listener, OnGroupLongClick longListener) {
        super(new DiffUtil.ItemCallback<Group>() {
            @Override public boolean areItemsTheSame(@NonNull Group a, @NonNull Group b) {
                return Objects.equals(a.otherId, b.otherId);
            }
            @Override public boolean areContentsTheSame(@NonNull Group a, @NonNull Group b) {
                return a.signature(myUid).equals(b.signature(myUid))
                        && Objects.equals(a.latest().getOtherName(myUid), b.latest().getOtherName(myUid))
                        && Objects.equals(a.latest().getOtherAvatar(myUid), b.latest().getOtherAvatar(myUid));
            }
        });
        this.myUid = myUid;
        this.listener = listener;
        this.longListener = longListener;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
        return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_chat, p, false));
    }

    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        Group g = getItem(pos);
        Chat c = g.latest();
        int more = g.chats.size() - 1;

        h.tvName.setText(c.getOtherName(myUid));
        h.tvProduct.setText(c.getProductTitle() + (more > 0 ? " và " + more + " sản phẩm khác" : ""));
        String prefix = myUid.equals(c.getLastSenderId()) ? "Bạn: " : "";
        h.tvLast.setText(prefix + c.getLastMessage());
        h.tvTime.setText(ChatFormat.time(c.getUpdatedAt()));

        int unread = g.unreadFor(myUid);
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

        h.itemView.setOnClickListener(v -> listener.onClick(g));
        h.itemView.setOnLongClickListener(v -> {
            longListener.onLongClick(g);
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
}