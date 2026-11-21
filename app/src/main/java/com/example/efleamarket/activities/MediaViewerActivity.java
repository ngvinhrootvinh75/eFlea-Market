package com.example.efleamarket.activities;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.efleamarket.R;
import com.example.efleamarket.models.Message;
import com.example.efleamarket.utils.CloudinaryHelper;

public class MediaViewerActivity extends AppCompatActivity {
    private static final String EXTRA_URL = "url";
    private static final String EXTRA_TYPE = "type";

    public static void open(Context ctx, String url, String type) {
        if (url == null) return;
        ctx.startActivity(new Intent(ctx, MediaViewerActivity.class)
                .putExtra(EXTRA_URL, url)
                .putExtra(EXTRA_TYPE, type));
    }

    private VideoView videoView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.media_viewer);

        View root = findViewById(R.id.rootViewer);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(false);

        findViewById(R.id.btnClose).setOnClickListener(v -> finish());

        String url = getIntent().getStringExtra(EXTRA_URL);
        String type = getIntent().getStringExtra(EXTRA_TYPE);
        ImageView ivImage = findViewById(R.id.ivImage);
        ProgressBar pb = findViewById(R.id.pbLoading);
        videoView = findViewById(R.id.videoView);

        if (Message.TYPE_VIDEO.equals(type)) {
            ivImage.setVisibility(View.GONE);
            videoView.setVisibility(View.VISIBLE);
            MediaController mc = new MediaController(this);
            mc.setAnchorView(videoView);
            videoView.setMediaController(mc);
            videoView.setVideoURI(Uri.parse(url));
            videoView.setOnPreparedListener(mp -> { pb.setVisibility(View.GONE); videoView.start(); });
            videoView.setOnErrorListener((mp, what, extra) -> {
                pb.setVisibility(View.GONE);
                Toast.makeText(this, "Không phát được video", Toast.LENGTH_SHORT).show();
                return true;
            });
        } else {
            videoView.setVisibility(View.GONE);
            ivImage.setVisibility(View.VISIBLE);
            pb.setVisibility(View.GONE);
            Glide.with(this).load(CloudinaryHelper.thumbnail(url, 1600)).fitCenter().into(ivImage);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (videoView != null && videoView.isPlaying()) videoView.pause();
    }
}