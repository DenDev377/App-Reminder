package com.kejaksaan.reminder;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.button.MaterialButton;
import com.kejaksaan.reminder.adapter.PerkaraTimelineAdapter;
import com.kejaksaan.reminder.model.Perkara;
import com.kejaksaan.reminder.model.PerkaraTimeline;
import com.kejaksaan.reminder.model.PerkaraTimelineResponse;
import com.kejaksaan.reminder.util.SessionManager;
import com.kejaksaan.reminder.viewmodel.TimelineViewModel;

import java.util.List;

public class DetailPerkaraActivity extends AppCompatActivity {

    public static final String EXTRA_PERKARA_ID = "EXTRA_PERKARA_ID";
    public static final String EXTRA_NOMOR_PERKARA = "EXTRA_NOMOR_PERKARA";

    private int perkaraId = -1;
    private String nomorPerkaraInitial = null;

    private ImageView btnBack;
    private TextView tvNomorPerkara;
    private TextView tvTahapSaatIni;
    private TextView tvStatusSaatIni;
    private LinearLayout layoutPenyidik;
    private TextView tvPenyidik;

    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView rvPerkaraTimeline;
    private ProgressBar progressBar;
    private LinearLayout layoutEmptyState;
    private LinearLayout layoutErrorState;
    private TextView tvErrorMessage;
    private MaterialButton btnRetry;

    private PerkaraTimelineAdapter adapter;
    private TimelineViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_detail_perkara);

        perkaraId = getIntent().getIntExtra(EXTRA_PERKARA_ID, -1);
        nomorPerkaraInitial = getIntent().getStringExtra(EXTRA_NOMOR_PERKARA);

        if (perkaraId <= 0) {
            Toast.makeText(this, "ID Perkara tidak valid", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupWindowInsets();
        setupRecyclerView();
        setupSwipeRefresh();
        setupViewModel();

        // Tampilkan animasi mengambang SwipeRefreshLayout saat memuat awal
        if (swipeRefresh != null) {
            swipeRefresh.post(() -> swipeRefresh.setRefreshing(true));
        }

        // Initial fetch
        loadTimeline();
    }

    private void initViews() {
        btnBack            = findViewById(R.id.btnBack);
        tvNomorPerkara     = findViewById(R.id.tvNomorPerkara);
        tvTahapSaatIni     = findViewById(R.id.tvTahapSaatIni);
        tvStatusSaatIni    = findViewById(R.id.tvStatusSaatIni);
        layoutPenyidik     = findViewById(R.id.layoutPenyidik);
        tvPenyidik         = findViewById(R.id.tvPenyidik);

        swipeRefresh       = findViewById(R.id.swipeRefresh);
        rvPerkaraTimeline  = findViewById(R.id.rvPerkaraTimeline);
        progressBar        = findViewById(R.id.progressBar);
        layoutEmptyState   = findViewById(R.id.layoutEmptyState);
        layoutErrorState   = findViewById(R.id.layoutErrorState);
        tvErrorMessage     = findViewById(R.id.tvErrorMessage);
        btnRetry           = findViewById(R.id.btnRetry);

        if (nomorPerkaraInitial != null && !nomorPerkaraInitial.isEmpty()) {
            tvNomorPerkara.setText(nomorPerkaraInitial);
        } else {
            tvNomorPerkara.setText("Perkara #" + perkaraId);
        }

        btnBack.setOnClickListener(v -> finish());

        ImageView btnRefresh = findViewById(R.id.btnRefresh);
        if (btnRefresh != null) {
            btnRefresh.setOnClickListener(v -> {
                if (swipeRefresh != null) {
                    swipeRefresh.setRefreshing(true);
                }
                loadTimeline();
            });
        }

        btnRetry.setOnClickListener(v -> {
            if (swipeRefresh != null) {
                swipeRefresh.setRefreshing(true);
            }
            loadTimeline();
        });
    }

    private void setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.headerLayout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), systemBars.top, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });

        ViewCompat.setOnApplyWindowInsetsListener(rvPerkaraTimeline, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), systemBars.bottom + 16);
            return insets;
        });
    }

    private void setupRecyclerView() {
        rvPerkaraTimeline.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PerkaraTimelineAdapter(this);
        rvPerkaraTimeline.setAdapter(adapter);
    }

    private void setupSwipeRefresh() {
        swipeRefresh.setColorSchemeColors(
                Color.parseColor("#00897B"),
                Color.parseColor("#DC2626"),
                Color.parseColor("#D97706")
        );
        swipeRefresh.setOnRefreshListener(this::loadTimeline);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(TimelineViewModel.class);

        viewModel.getPerkaraTimelineResult().observe(this, result -> {
            if (result == null) return;

            if (result.isLoading()) {
                if (swipeRefresh != null) {
                    swipeRefresh.setRefreshing(true);
                }
                if (progressBar != null) {
                    progressBar.setVisibility(View.GONE);
                }
                layoutEmptyState.setVisibility(View.GONE);
                layoutErrorState.setVisibility(View.GONE);
            } else if (result.isSuccess()) {
                if (swipeRefresh != null) {
                    swipeRefresh.setRefreshing(false);
                }
                if (progressBar != null) {
                    progressBar.setVisibility(View.GONE);
                }
                layoutErrorState.setVisibility(View.GONE);

                PerkaraTimelineResponse body = result.getData();
                if (body != null) {
                    // Update Case Summary Header
                    Perkara perkara = body.getPerkara();
                    if (perkara != null) {
                        bindCaseHeader(perkara);
                    }

                    // Update Timelines list
                    List<PerkaraTimeline> list = body.getData();
                    if (list == null || list.isEmpty()) {
                        adapter.setTimelines(null);
                        layoutEmptyState.setVisibility(View.VISIBLE);
                        rvPerkaraTimeline.setVisibility(View.GONE);
                    } else {
                        layoutEmptyState.setVisibility(View.GONE);
                        rvPerkaraTimeline.setVisibility(View.VISIBLE);
                        adapter.setTimelines(list);
                    }
                }
            } else if (result.isError()) {
                swipeRefresh.setRefreshing(false);
                progressBar.setVisibility(View.GONE);

                String errorMsg = result.getMessage() != null ? result.getMessage() : "Gagal memuat riwayat perkara";

                if (errorMsg.contains("UNAUTHORIZED:401")) {
                    SessionManager sessionManager = new SessionManager(this);
                    sessionManager.clearSession();
                    Toast.makeText(this, "Sesi telah berakhir. Silakan login kembali.", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                    return;
                }

                if (adapter.getItemCount() == 0) {
                    layoutErrorState.setVisibility(View.VISIBLE);
                    tvErrorMessage.setText(errorMsg);
                    rvPerkaraTimeline.setVisibility(View.GONE);
                    layoutEmptyState.setVisibility(View.GONE);
                } else {
                    Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void bindCaseHeader(Perkara perkara) {
        if (perkara.getNomorPerkara() != null && !perkara.getNomorPerkara().isEmpty()) {
            tvNomorPerkara.setText(perkara.getNomorPerkara());
        }

        if (perkara.getTahapSaatIni() != null && !perkara.getTahapSaatIni().isEmpty()) {
            tvTahapSaatIni.setVisibility(View.VISIBLE);
            tvTahapSaatIni.setText(perkara.getTahapSaatIni());
        } else {
            tvTahapSaatIni.setVisibility(View.GONE);
        }

        if (perkara.getStatusSaatIni() != null && !perkara.getStatusSaatIni().isEmpty()) {
            tvStatusSaatIni.setText(perkara.getStatusSaatIni());
        } else {
            tvStatusSaatIni.setText("-");
        }

        if (perkara.getPenyidik() != null && !perkara.getPenyidik().trim().isEmpty()) {
            layoutPenyidik.setVisibility(View.VISIBLE);
            tvPenyidik.setText(perkara.getPenyidik().trim());
        } else {
            layoutPenyidik.setVisibility(View.GONE);
        }
    }

    private void loadTimeline() {
        viewModel.loadPerkaraTimeline(perkaraId);
    }
}