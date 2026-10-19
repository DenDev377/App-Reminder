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
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.kejaksaan.reminder.adapter.ReminderAdapter;
import com.kejaksaan.reminder.model.Reminder;
import com.kejaksaan.reminder.util.SessionManager;
import com.kejaksaan.reminder.viewmodel.ReminderViewModel;

import java.util.List;

public class ReminderListActivity extends AppCompatActivity {

    private RecyclerView rvReminders;
    private ProgressBar progressBarMain;
    private ProgressBar progressBarBottom;
    private LinearLayout layoutEmptyState;
    private TextView tvEmptyMessage;
    private LinearLayout layoutErrorState;
    private TextView tvErrorMessage;
    private MaterialButton btnRetry;

    private TextView chipSemua;
    private TextView chipMendesak;
    private TextView chipOverdue;
    private TextView chipHMin3;
    private TextView chipAman;

    private ReminderAdapter adapter;
    private ReminderViewModel viewModel;

    private String currentSelectedFilter = null; // null = semua

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_reminder_list);

        initViews();
        setupWindowInsets();
        setupRecyclerView();
        setupFilterChips();
        setupViewModel();

        // Load first page (Semua)
        viewModel.loadFirstPage(null);
    }

    private void initViews() {
        rvReminders        = findViewById(R.id.rvReminders);
        progressBarMain    = findViewById(R.id.progressBarMain);
        progressBarBottom  = findViewById(R.id.progressBarBottom);
        layoutEmptyState   = findViewById(R.id.layoutEmptyState);
        tvEmptyMessage     = findViewById(R.id.tvEmptyMessage);
        layoutErrorState   = findViewById(R.id.layoutErrorState);
        tvErrorMessage     = findViewById(R.id.tvErrorMessage);
        btnRetry           = findViewById(R.id.btnRetry);

        chipSemua    = findViewById(R.id.chipSemua);
        chipMendesak = findViewById(R.id.chipMendesak);
        chipOverdue  = findViewById(R.id.chipOverdue);
        chipHMin3    = findViewById(R.id.chipHMin3);
        chipAman     = findViewById(R.id.chipAman);

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        ImageView btnRefresh = findViewById(R.id.btnRefresh);
        btnRefresh.setOnClickListener(v -> viewModel.refresh());

        View btnLogReminders = findViewById(R.id.btnLogReminders);
        if (btnLogReminders != null) {
            btnLogReminders.setOnClickListener(v -> {
                Intent intent = new Intent(this, ReminderLogListActivity.class);
                startActivity(intent);
            });
        }

        btnRetry.setOnClickListener(v -> viewModel.loadFirstPage(currentSelectedFilter));
    }

    private void setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.headerLayout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), systemBars.top, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rvReminders), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), systemBars.bottom + 8);
            return insets;
        });
    }

    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        rvReminders.setLayoutManager(layoutManager);
        adapter = new ReminderAdapter(this);
        rvReminders.setAdapter(adapter);

        // Infinite scroll / pagination listener
        rvReminders.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy > 0) { // scrolling down
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

    private void setupFilterChips() {
        chipSemua.setOnClickListener(v -> selectFilter(null, chipSemua));
        chipMendesak.setOnClickListener(v -> selectFilter("semua_mendesak", chipMendesak));
        chipOverdue.setOnClickListener(v -> selectFilter("Overdue", chipOverdue));
        chipHMin3.setOnClickListener(v -> selectFilter("H-3", chipHMin3));
        chipAman.setOnClickListener(v -> selectFilter("Aman", chipAman));
    }

    private void selectFilter(String filterValue, TextView activeChip) {
        if (currentSelectedFilter == null && filterValue == null) {
            // Already on 'Semua', ignore redundant tap
            return;
        }
        if (currentSelectedFilter != null && currentSelectedFilter.equals(filterValue)) {
            return;
        }

        currentSelectedFilter = filterValue;

        // Reset all chip visuals to inactive
        resetChipStyle(chipSemua);
        resetChipStyle(chipMendesak);
        resetChipStyle(chipOverdue);
        resetChipStyle(chipHMin3);
        resetChipStyle(chipAman);

        // Set active chip visual
        activeChip.setBackgroundResource(R.drawable.bg_filter_chip_active);
        activeChip.setTextColor(Color.WHITE);

        // Scroll RecyclerView to top and reload page 1
        rvReminders.scrollToPosition(0);
        viewModel.loadFirstPage(filterValue);
    }

    private void resetChipStyle(TextView chip) {
        chip.setBackgroundResource(R.drawable.bg_filter_chip_inactive);
        chip.setTextColor(Color.parseColor("#64748B"));
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(ReminderViewModel.class);

        viewModel.getRemindersResult().observe(this, result -> {
            if (result == null) return;

            if (result.isLoading()) {
                progressBarMain.setVisibility(View.VISIBLE);
                layoutEmptyState.setVisibility(View.GONE);
                layoutErrorState.setVisibility(View.GONE);
            } else if (result.isSuccess()) {
                progressBarMain.setVisibility(View.GONE);
                layoutErrorState.setVisibility(View.GONE);

                List<Reminder> list = result.getData();
                if (list == null || list.isEmpty()) {
                    layoutEmptyState.setVisibility(View.VISIBLE);
                    rvReminders.setVisibility(View.GONE);
                    tvEmptyMessage.setText("Tidak ada reminder aktif" + (currentSelectedFilter != null ? " untuk filter ini" : ""));
                } else {
                    layoutEmptyState.setVisibility(View.GONE);
                    rvReminders.setVisibility(View.VISIBLE);
                    adapter.setReminderList(list);
                }
            } else if (result.isError()) {
                progressBarMain.setVisibility(View.GONE);
                String errorMsg = result.getMessage() != null ? result.getMessage() : "Terjadi kesalahan";

                // Tangani 401 Unauthorized
                if (errorMsg.contains("401") || errorMsg.contains("UNAUTHORIZED")) {
                    Toast.makeText(this, "Sesi telah berakhir. Silakan login kembali.", Toast.LENGTH_LONG).show();
                    new SessionManager(this).clearSession();
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                    return;
                }

                layoutEmptyState.setVisibility(View.GONE);
                rvReminders.setVisibility(View.GONE);
                layoutErrorState.setVisibility(View.VISIBLE);
                tvErrorMessage.setText(errorMsg);
            }
        });

        // Observe pagination bottom spinner
        viewModel.getIsPaginating().observe(this, isPaginating -> {
            progressBarBottom.setVisibility(Boolean.TRUE.equals(isPaginating) ? View.VISIBLE : View.GONE);
        });

        // Observe pagination error (Toast without clearing list)
        viewModel.getPaginationError().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
