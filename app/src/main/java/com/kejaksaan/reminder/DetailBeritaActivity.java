package com.kejaksaan.reminder;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.android.material.card.MaterialCardView;
import com.kejaksaan.reminder.model.Berita;

import java.util.Locale;

public class DetailBeritaActivity extends AppCompatActivity {

    public static final String EXTRA_BERITA = "extra_berita";

    private ImageView btnBack;
    private ImageView btnShare;
    private FrameLayout layoutImageHeader;
    private ImageView ivDetailImage;
    private TextView tvDetailViews;
    private TextView tvDetailJudul;
    private TextView tvDetailPenulis;
    private TextView tvDetailTanggal;
    private MaterialCardView cardRingkasan;
    private TextView tvDetailRingkasan;
    private TextView tvDetailKonten;

    private Berita berita;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_detail_berita);

        setupWindowInsets();
        initViews();
        loadIntentData();
    }

    private void setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.headerLayout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), systemBars.top, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        btnShare = findViewById(R.id.btnShare);
        layoutImageHeader = findViewById(R.id.layoutImageHeader);
        ivDetailImage = findViewById(R.id.ivDetailImage);
        tvDetailViews = findViewById(R.id.tvDetailViews);
        tvDetailJudul = findViewById(R.id.tvDetailJudul);
        tvDetailPenulis = findViewById(R.id.tvDetailPenulis);
        tvDetailTanggal = findViewById(R.id.tvDetailTanggal);
        cardRingkasan = findViewById(R.id.cardRingkasan);
        tvDetailRingkasan = findViewById(R.id.tvDetailRingkasan);
        tvDetailKonten = findViewById(R.id.tvDetailKonten);

        btnBack.setOnClickListener(v -> finish());
        btnShare.setOnClickListener(v -> shareBerita());
    }

    private void loadIntentData() {
        if (getIntent() != null && getIntent().hasExtra(EXTRA_BERITA)) {
            berita = (Berita) getIntent().getSerializableExtra(EXTRA_BERITA);
            if (berita != null) {
                bindBeritaData(berita);
                return;
            }
        }

        Toast.makeText(this, "Informasi berita tidak ditemukan", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void bindBeritaData(Berita item) {
        tvDetailJudul.setText(item.getJudul() != null ? item.getJudul() : "-");
        tvDetailPenulis.setText(item.getPenulis());
        tvDetailTanggal.setText(item.getFormattedTanggal());
        tvDetailViews.setText(String.format(Locale.getDefault(), "%d pembaca", item.getViewsCount()));

        // Tampilkan ringkasan jika tersedia
        if (item.getRingkasan() != null && !item.getRingkasan().trim().isEmpty()) {
            cardRingkasan.setVisibility(View.VISIBLE);
            tvDetailRingkasan.setText(item.getRingkasan());
        } else {
            cardRingkasan.setVisibility(View.GONE);
        }

        // Tampilkan konten lengkap
        if (item.getKonten() != null && !item.getKonten().trim().isEmpty()) {
            tvDetailKonten.setText(item.getKonten());
        } else if (item.getRingkasan() != null) {
            tvDetailKonten.setText(item.getRingkasan());
        } else {
            tvDetailKonten.setText("-");
        }

        // Tampilkan gambar berita
        String imageUrl = item.getResolvedImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            layoutImageHeader.setVisibility(View.VISIBLE);
            ivDetailImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
            Glide.with(this)
                    .load(imageUrl)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .placeholder(R.drawable.lambang_kejaksaan)
                    .error(R.drawable.lambang_kejaksaan)
                    .into(ivDetailImage);
        } else {
            // Jika tidak ada foto berita, gunakan banner lambang kejaksaan
            ivDetailImage.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            ivDetailImage.setImageResource(R.drawable.lambang_kejaksaan);
        }
    }

    private void shareBerita() {
        if (berita == null) return;

        String shareText = berita.getJudul() + "\n\n"
                + (berita.getRingkasan() != null ? berita.getRingkasan() + "\n\n" : "")
                + "Dipublikasikan oleh: " + berita.getPenulis() + " (" + berita.getFormattedTanggal() + ")\n"
                + "Aplikasi SIPETA Kejaksaan";

        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        sendIntent.putExtra(Intent.EXTRA_SUBJECT, berita.getJudul());
        sendIntent.setType("text/plain");

        Intent shareIntent = Intent.createChooser(sendIntent, "Bagikan Berita");
        startActivity(shareIntent);
    }
}

