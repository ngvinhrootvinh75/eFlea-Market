package com.example.efleamarket.adapters;

import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.efleamarket.R;
import com.example.efleamarket.models.AppNotification;
import com.example.efleamarket.utils.ChatFormat;

import java.util.Objects;

public class NotificationAdapter extends ListAdapter<AppNotification, NotificationAdapter.VH> {
    public interface OnClick { void onClick(AppNotification n); }

    private final OnClick listener;

    public NotificationAdapter(OnClick listener) {
        super(new DiffUtil.ItemCallback<AppNotification>() {
            @Override public boolean areItemsTheSame(@NonNull AppNotification a, @NonNull AppNotification b) {
                return Objects.equals(a.getId(), b.getId());
            }
            @Override public boolean areContentsTheSame(@NonNull AppNotification a, @NonNull AppNotification b) {
                return a.isRead() == b.isRead()
                        && Objects.equals(a.getBody(), b.getBody())
                        && Objects.equals(a.getCreatedAt(), b.getCreatedAt());
            }
        });
        this.listener = listener;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
        return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_notification, p, false));
    }

    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        AppNotification n = getItem(pos);
        boolean unread = !n.isRead();
        h.tvTitle.setText(n.getTitle());
        h.tvBody.setText(n.getBody());
        h.tvBody.setTypeface(null, unread ? Typeface.BOLD : Typeface.NORMAL);
        h.tvTime.setText(ChatFormat.dateTime(n.getCreatedAt()));
        h.dot.setVisibility(unread ? View.VISIBLE : View.INVISIBLE);
        h.itemView.setBackgroundColor(unread ? 0xFFECF7F4 : 0x00000000);
        h.itemView.setOnClickListener(v -> listener.onClick(n));
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tvTitle, tvBody, tvTime;
        final View dot;
        VH(View v) {
            super(v);
            tvTitle = v.findViewById(R.id.tvTitle);
            tvBody = v.findViewById(R.id.tvBody);
            tvTime = v.findViewById(R.id.tvTime);
            dot = v.findViewById(R.id.dotUnread);
        }
    }
}