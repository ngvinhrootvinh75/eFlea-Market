package com.example.efleamarket.adapters;

import android.graphics.drawable.ColorDrawable;
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
import com.example.efleamarket.models.Message;
import com.example.efleamarket.utils.ChatFormat;
import com.example.efleamarket.utils.CloudinaryHelper;
import com.example.efleamarket.utils.MediaUtils;

import java.util.Objects;

public class MessageAdapter extends ListAdapter<Message, MessageAdapter.VH> {
    public interface OnMediaClick { void onClick(Message message); }

    private static final int TYPE_SENT = 1, TYPE_RECEIVED = 2;
    private final String myUid;
    private final OnMediaClick mediaClick;

    public MessageAdapter(String myUid, OnMediaClick mediaClick) {
        super(DIFF);
        this.myUid = myUid;
        this.mediaClick = mediaClick;
    }

    @Override public int getItemViewType(int pos) {
        return myUid.equals(getItem(pos).getSenderId()) ? TYPE_SENT : TYPE_RECEIVED;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        int layout = type == TYPE_SENT ? R.layout.item_message_sent : R.layout.item_message_received;
        return new VH(LayoutInflater.from(parent.getContext()).inflate(layout, parent, false));
    }

    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        Message m = getItem(pos);

        boolean hasMedia = (m.isImage() || m.isVideo()) && m.getMediaUrl() != null;
        h.cardMedia.setVisibility(hasMedia ? View.VISIBLE : View.GONE);
        h.ivPlay.setVisibility(hasMedia && m.isVideo() ? View.VISIBLE : View.GONE);
        if (hasMedia) {
            String thumb = m.isVideo()
                    ? MediaUtils.videoThumbnail(m.getMediaUrl())
                    : CloudinaryHelper.thumbnail(m.getMediaUrl(), 500);
            Glide.with(h.ivMedia).load(thumb)
                    .placeholder(new ColorDrawable(0xFFE5E7EB))
                    .centerCrop()
                    .into(h.ivMedia);
            h.cardMedia.setOnClickListener(v -> mediaClick.onClick(m));
        } else {
            Glide.with(h.ivMedia).clear(h.ivMedia);
            h.cardMedia.setOnClickListener(null);
        }

        String text = m.getText();
        boolean hasText = text != null && !text.isEmpty();
        h.tvText.setVisibility(hasText ? View.VISIBLE : View.GONE);
        if (hasText) h.tvText.setText(text);

        h.tvTime.setText(ChatFormat.time(m.getCreatedAt()));
    }

    static class VH extends RecyclerView.ViewHolder {
        final View cardMedia;
        final ImageView ivMedia, ivPlay;
        final TextView tvText, tvTime;
        VH(View v) {
            super(v);
            cardMedia = v.findViewById(R.id.cardMedia);
            ivMedia = v.findViewById(R.id.ivMedia);
            ivPlay = v.findViewById(R.id.ivPlay);
            tvText = v.findViewById(R.id.tvText);
            tvTime = v.findViewById(R.id.tvTime);
        }
    }

    private static final DiffUtil.ItemCallback<Message> DIFF = new DiffUtil.ItemCallback<Message>() {
        @Override public boolean areItemsTheSame(@NonNull Message a, @NonNull Message b) {
            return Objects.equals(a.getId(), b.getId());
        }
        @Override public boolean areContentsTheSame(@NonNull Message a, @NonNull Message b) {
            return Objects.equals(a.getText(), b.getText())
                    && Objects.equals(a.getMediaUrl(), b.getMediaUrl())
                    && Objects.equals(a.getCreatedAt(), b.getCreatedAt());
        }
    };
}