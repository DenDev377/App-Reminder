package com.kejaksaan.reminder;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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

import android.content.SharedPreferences;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.android.material.card.MaterialCardView;
import com.kejaksaan.reminder.api.RetrofitClient;
import com.kejaksaan.reminder.model.Berita;
import com.kejaksaan.reminder.model.GenericResponse;
import com.kejaksaan.reminder.util.SessionManager;

import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

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
                // Tambahkan 1 ke views_count di backend secara fire-and-forget
                incrementViewCount(berita.getId());
                return;
            }
        }

        Toast.makeText(this, "Informasi berita tidak ditemukan", Toast.LENGTH_SHORT).show();
        finish();
    }

    /**
     * Memanggil endpoint POST /api/berita/{id}/view untuk menaikkan jumlah pembaca di backend.
     * Dibatasi hanya 1 kali penambahan pembaca per akun per berita (dicatat di SharedPreferences).
     * Bersifat fire-and-forget: kegagalan jaringan tidak mengganggu kenyamanan membaca berita.
     */
    private void incrementViewCount(int beritaId) {
        if (beritaId <= 0) return;

        // Ambil identitas akun yang sedang login
        SessionManager sessionManager = new SessionManager(this);
        int userId = sessionManager.fetchUserId();
        String userKey = userId > 0 ? String.valueOf(userId) : sessionManager.fetchUserEmail();
        if (userKey == null || userKey.trim().isEmpty()) {
            userKey = "anonymous";
        }

        // Cek apakah akun ini sudah pernah membuka/membaca berita ini sebelumnya
        SharedPreferences prefs = getSharedPreferences("SIPETA_BERITA_VIEWS", MODE_PRIVATE);
        String viewKey = "viewed_user_" + userKey + "_berita_" + beritaId;
        boolean alreadyViewed = prefs.getBoolean(viewKey, false);

        if (alreadyViewed) {
            Log.d("DetailBerita", "Berita #" + beritaId + " sudah pernah dibaca oleh user (" + userKey + "), tidak menambah jumlah pembaca.");
            return;
        }

        // Tandai sudah dibaca oleh akun ini agar tidak bertambah lagi walaupun diklik berulang kali
        prefs.edit().putBoolean(viewKey, true).apply();

        // Optimistic update pada tampilan lokal (+1 pembaca)
        if (berita != null) {
            int updatedViews = berita.getViewsCount() + 1;
            berita.setViewsCount(updatedViews);
            if (tvDetailViews != null) {
                tvDetailViews.setText(String.format(Locale.getDefault(), "%d pembaca", updatedViews));
            }
        }

        // Panggil endpoint backend
        RetrofitClient.getApiService(this)
                .incrementBeritaView(beritaId)
                .enqueue(new Callback<GenericResponse>() {
                    @Override
                    public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                        if (response.isSuccessful()) {
                            Log.d("DetailBerita", "Views berita #" + beritaId + " berhasil diinkremen di server.");
                        } else {
                            Log.w("DetailBerita", "Server mengembalikan HTTP " + response.code() + " saat increment view berita #" + beritaId);
                        }
                    }

                    @Override
                    public void onFailure(Call<GenericResponse> call, Throwable t) {
                        Log.w("DetailBerita", "Gagal menghubungi server untuk increment view: " + t.getMessage());
                    }
                });
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

        // Tampilkan gambar berita (Utuh & Presisi tanpa terpotong)
        String imageUrl = item.getResolvedImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            layoutImageHeader.setVisibility(View.VISIBLE);
            ivDetailImage.setAdjustViewBounds(true);
            ivDetailImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
            Glide.with(this)
                    .load(imageUrl)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .placeholder(R.drawable.lambang_kejaksaan)
                    .error(R.drawable.lambang_kejaksaan)
                    .fitCenter()
                    .into(ivDetailImage);
        } else {
            // Jika tidak ada foto berita, gunakan banner lambang kejaksaan
            ivDetailImage.setAdjustViewBounds(false);
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

