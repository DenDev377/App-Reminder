package com.kejaksaan.reminder;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.kejaksaan.reminder.adapter.ReminderLogAdapter;
import com.kejaksaan.reminder.model.PerkaraReminderLogGroup;
import com.kejaksaan.reminder.model.ReminderLog;
import com.kejaksaan.reminder.model.ReminderLogSummary;
import com.kejaksaan.reminder.util.SessionManager;
import com.kejaksaan.reminder.viewmodel.ReminderLogViewModel;

import java.util.List;

/**
 * Activity untuk menampilkan Menu "Log Reminders" (Riwayat / Audit Trail Reminder SLA).
 * Menyediakan filter status, pencarian serbaguna, filter tahap perkara, ringkasan metrik KPI,
 * serta dukungan pagination dan refresh.
 */
public class ReminderLogListActivity extends AppCompatActivity {

    private ImageView btnBack;
    private ImageView btnRefresh;

    // Summary Metric KPI Cards
    private MaterialCardView cardSummaryTotal;
    private MaterialCardView cardSummarySelesai;
    private MaterialCardView cardSummaryAktif;
    private MaterialCardView cardSummaryOverdue;
    private TextView tvSummaryTotal;
    private TextView tvSummarySelesai;
    private TextView tvSummaryAktif;
    private TextView tvSummaryOverdue;

    // Search Box
    private EditText etSearch;
    private ImageView btnClearSearch;

    // Filter Chips
    private TextView chipSemua;
    private TextView chipAktif;
    private TextView chipOverdue;
    private TextView chipSelesai;

    // List & States
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView rvReminderLogs;
    private ProgressBar progressBarBottom;
    private LinearLayout layoutEmptyState;
    private TextView tvEmptyTitle;
    private TextView tvEmptyMessage;
    private MaterialButton btnResetFilter;
    private LinearLayout layoutErrorState;
    private TextView tvErrorMessage;
    private MaterialButton btnRetry;

    private ReminderLogAdapter adapter;
    private ReminderLogViewModel viewModel;

    private String currentSelectedStatus = "all";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_reminder_log_list);

        initViews();
        setupWindowInsets();
        setupRecyclerView();
        setupSearch();
        setupFilterChips();
        setupSummaryCards();
        setupSwipeRefresh();
        setupViewModel();

        // Tampilkan animasi mengambang SwipeRefreshLayout saat memuat awal
        if (swipeRefresh != null) {
            swipeRefresh.post(() -> swipeRefresh.setRefreshing(true));
        }

        // Load page 1 default
        loadData();
    }

    private void initViews() {
        btnBack            = findViewById(R.id.btnBack);
        btnRefresh         = findViewById(R.id.btnRefresh);

        cardSummaryTotal   = findViewById(R.id.cardSummaryTotal);
        cardSummarySelesai = findViewById(R.id.cardSummarySelesai);
        cardSummaryAktif   = findViewById(R.id.cardSummaryAktif);
        cardSummaryOverdue = findViewById(R.id.cardSummaryOverdue);

        tvSummaryTotal     = findViewById(R.id.tvSummaryTotal);
        tvSummarySelesai   = findViewById(R.id.tvSummarySelesai);
        tvSummaryAktif     = findViewById(R.id.tvSummaryAktif);
        tvSummaryOverdue   = findViewById(R.id.tvSummaryOverdue);

        etSearch           = findViewById(R.id.etSearch);
        btnClearSearch     = findViewById(R.id.btnClearSearch);

        chipSemua          = findViewById(R.id.chipSemua);
        chipAktif          = findViewById(R.id.chipAktif);
        chipOverdue        = findViewById(R.id.chipOverdue);
        chipSelesai        = findViewById(R.id.chipSelesai);

        swipeRefresh       = findViewById(R.id.swipeRefresh);
        rvReminderLogs     = findViewById(R.id.rvReminderLogs);
        progressBarBottom  = findViewById(R.id.progressBarBottom);

        layoutEmptyState   = findViewById(R.id.layoutEmptyState);
        tvEmptyTitle       = findViewById(R.id.tvEmptyTitle);
        tvEmptyMessage     = findViewById(R.id.tvEmptyMessage);
        btnResetFilter     = findViewById(R.id.btnResetFilter);

        layoutErrorState   = findViewById(R.id.layoutErrorState);
        tvErrorMessage     = findViewById(R.id.tvErrorMessage);
        btnRetry           = findViewById(R.id.btnRetry);

        btnBack.setOnClickListener(v -> finish());
        btnRefresh.setOnClickListener(v -> {
            if (swipeRefresh != null) {
                swipeRefresh.setRefreshing(true);
            }
            viewModel.refresh();
        });
        btnRetry.setOnClickListener(v -> loadData());
        btnResetFilter.setOnClickListener(v -> resetAllFilters());
    }

    private void setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.headerLayout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), systemBars.top, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });

        ViewCompat.setOnApplyWindowInsetsListener(rvReminderLogs, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), systemBars.bottom + 16);
            return insets;
        });
    }

    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        rvReminderLogs.setLayoutManager(layoutManager);

        adapter = new ReminderLogAdapter(this);
        adapter.setOnItemClickListener(group -> {
            if (group != null && group.getPerkaraId() > 0) {
                Intent intent = new Intent(ReminderLogListActivity.this, DetailPerkaraActivity.class);
                intent.putExtra(DetailPerkaraActivity.EXTRA_PERKARA_ID, group.getPerkaraId());
                if (group.getNomorPerkara() != null && !group.getNomorPerkara().isEmpty()) {
                    intent.putExtra(DetailPerkaraActivity.EXTRA_NOMOR_PERKARA, group.getNomorPerkara());
                }
                startActivity(intent);
            } else {
                Toast.makeText(ReminderLogListActivity.this, "Data perkara tidak tersedia", Toast.LENGTH_SHORT).show();
            }
        });
        rvReminderLogs.setAdapter(adapter);

        // Infinite scroll pagination listener
        rvReminderLogs.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy > 0) {
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 2
                            && firstVisibleItemPosition >= 0) {
                        viewModel.loadNextPage();
                    }
                }
            }
        });
    }

    private void setupSearch() {
        btnClearSearch.setOnClickListener(v -> {
            etSearch.setText("");
            hideKeyboard();
            loadData();
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                btnClearSearch.setVisibility(s != null && s.length() > 0 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboard();
                loadData();
                return true;
            }
            return false;
        });
    }

    private void setupFilterChips() {
        chipSemua.setOnClickListener(v -> selectStatusFilter("all", chipSemua));
        chipAktif.setOnClickListener(v -> selectStatusFilter("aktif", chipAktif));
        chipOverdue.setOnClickListener(v -> selectStatusFilter("overdue", chipOverdue));
        chipSelesai.setOnClickListener(v -> selectStatusFilter("selesai", chipSelesai));
    }

    private void setupSummaryCards() {
        cardSummaryTotal.setOnClickListener(v -> selectStatusFilter("all", chipSemua));
        cardSummarySelesai.setOnClickListener(v -> selectStatusFilter("selesai", chipSelesai));
        cardSummaryAktif.setOnClickListener(v -> selectStatusFilter("aktif", chipAktif));
        cardSummaryOverdue.setOnClickListener(v -> selectStatusFilter("overdue", chipOverdue));
    }

    private void selectStatusFilter(String status, TextView activeChip) {
        if (currentSelectedStatus.equalsIgnoreCase(status)) {
            return;
        }

        currentSelectedStatus = status;

        resetChipStyle(chipSemua);
        resetChipStyle(chipAktif);
        resetChipStyle(chipOverdue);
        resetChipStyle(chipSelesai);

        activeChip.setBackgroundResource(R.drawable.bg_filter_chip_active);
        activeChip.setTextColor(Color.WHITE);

        rvReminderLogs.scrollToPosition(0);
        loadData();
    }

    private void resetChipStyle(TextView chip) {
        chip.setBackgroundResource(R.drawable.bg_filter_chip_inactive);
        chip.setTextColor(Color.parseColor("#64748B"));
    }

    private void resetAllFilters() {
        currentSelectedStatus = "all";
        etSearch.setText("");

        resetChipStyle(chipSemua);
        resetChipStyle(chipAktif);
        resetChipStyle(chipOverdue);
        resetChipStyle(chipSelesai);

        chipSemua.setBackgroundResource(R.drawable.bg_filter_chip_active);
        chipSemua.setTextColor(Color.WHITE);

        loadData();
    }

    private void setupSwipeRefresh() {
        swipeRefresh.setColorSchemeColors(Color.parseColor("#00897B"), Color.parseColor("#DC2626"), Color.parseColor("#0284C7"));
        swipeRefresh.setOnRefreshListener(this::loadData);
    }

    private void loadData() {
        if (swipeRefresh != null) {
            swipeRefresh.setRefreshing(true);
        }
        String query = etSearch.getText().toString().trim();
        viewModel.loadFirstPage(currentSelectedStatus, query);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(ReminderLogViewModel.class);

        // Bind Summary Metric KPI
        viewModel.getSummaryResult().observe(this, this::bindSummaryData);

        // Bind Perkara Groups Result (1 card per perkara)
        viewModel.getPerkaraGroupsResult().observe(this, result -> {
            if (result == null) return;

            if (result.isLoading()) {
                if (swipeRefresh != null) {
                    swipeRefresh.setRefreshing(true);
                }
                layoutEmptyState.setVisibility(View.GONE);
                layoutErrorState.setVisibility(View.GONE);
            } else if (result.isSuccess()) {
                if (swipeRefresh != null) {
                    swipeRefresh.setRefreshing(false);
                }
                layoutErrorState.setVisibility(View.GONE);

                List<PerkaraReminderLogGroup> list = result.getData();
                if (list == null || list.isEmpty()) {
                    adapter.setGroups(null);
                    layoutEmptyState.setVisibility(View.VISIBLE);
                    rvReminderLogs.setVisibility(View.GONE);

                    String currentSearch = etSearch.getText().toString().trim();
                    if (!currentSearch.isEmpty()) {
                        tvEmptyTitle.setText("Tidak Ada Hasil");
                        tvEmptyMessage.setText("Tidak ada log reminder perkara yang cocok dengan pencarian \"" + currentSearch + "\".");
                    } else if (!"all".equalsIgnoreCase(currentSelectedStatus)) {
                        tvEmptyTitle.setText("Data Tidak Ditemukan");
                        tvEmptyMessage.setText("Tidak ada perkara untuk filter status yang dipilih.");
                    } else {
                        tvEmptyTitle.setText("Belum Ada Log Reminder");
                        tvEmptyMessage.setText("Daftar audit trail batas waktu SLA perkara belum tersedia di sistem.");
                    }
                } else {
                    layoutEmptyState.setVisibility(View.GONE);
                    rvReminderLogs.setVisibility(View.VISIBLE);
                    adapter.setGroups(list);
                }
            } else if (result.isError()) {
                if (swipeRefresh != null) {
                    swipeRefresh.setRefreshing(false);
                }

                String errorMsg = result.getMessage() != null ? result.getMessage() : "Gagal memuat log reminder";

                // Tangani sesi kedaluwarsa 401
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
                    rvReminderLogs.setVisibility(View.GONE);
                    layoutEmptyState.setVisibility(View.GONE);
                } else {
                    Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Bottom pagination indicator
        viewModel.getIsPaginating().observe(this, isPaginating -> {
            if (isPaginating != null) {
                progressBarBottom.setVisibility(isPaginating ? View.VISIBLE : View.GONE);
            }
        });

        // Pagination error notification
        viewModel.getPaginationError().observe(this, errorMsg -> {
            if (errorMsg != null && !errorMsg.isEmpty()) {
                Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void bindSummaryData(ReminderLogSummary summary) {
        if (summary != null) {
            tvSummaryTotal.setText(String.valueOf(summary.getTotalLogs()));
            tvSummarySelesai.setText(String.valueOf(summary.getTotalSelesai()));
            tvSummaryAktif.setText(String.valueOf(summary.getTotalAktif()));
            tvSummaryOverdue.setText(String.valueOf(summary.getTotalOverdue()));
        }
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }
}
