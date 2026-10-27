package com.kejaksaan.reminder;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
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
import androidx.core.widget.NestedScrollView;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.kejaksaan.reminder.adapter.PerkaraTimelineAdapter;
import com.kejaksaan.reminder.model.AvailableActionsResponse;
import com.kejaksaan.reminder.model.GenericResponse;
import com.kejaksaan.reminder.model.Perkara;
import com.kejaksaan.reminder.model.PerkaraTimeline;
import com.kejaksaan.reminder.model.PerkaraTimelineResponse;
import com.kejaksaan.reminder.model.WorkflowAction;
import com.kejaksaan.reminder.util.SessionManager;
import com.kejaksaan.reminder.viewmodel.TimelineViewModel;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DetailPerkaraActivity extends AppCompatActivity {

    public static final String EXTRA_PERKARA_ID = "EXTRA_PERKARA_ID";
    public static final String EXTRA_NOMOR_PERKARA = "EXTRA_NOMOR_PERKARA";

    private int perkaraId = -1;
    private String nomorPerkaraInitial = null;

    // Header & Summary Views
    private ImageView btnBack;
    private ImageView btnRefresh;
    private TextView tvNomorPerkara;
    private TextView tvTahapSaatIni;
    private TextView tvStatusSaatIni;
    private LinearLayout layoutPenyidik;
    private TextView tvPenyidik;

    // Section Aksi Perkara Views
    private TextView tvActionCountBadge;
    private ProgressBar pbActionsLoading;
    private LinearLayout layoutActionCardsContainer;
    private MaterialCardView cardEmptyAction;
    private TextView tvEmptyActionTitle;
    private TextView tvEmptyActionDesc;

    // Scroll & Timeline Views
    private SwipeRefreshLayout swipeRefresh;
    private NestedScrollView nestedScrollView;
    private RecyclerView rvPerkaraTimeline;
    private ProgressBar progressBar;
    private LinearLayout layoutEmptyState;
    private LinearLayout layoutErrorState;
    private TextView tvErrorMessage;
    private MaterialButton btnRetry;

    private PerkaraTimelineAdapter adapter;
    private TimelineViewModel viewModel;
    private AlertDialog activeActionDialog = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_detail_perkara);

        extractIntentData(getIntent());

        // Verifikasi sesi autentikasi pengguna
        SessionManager sessionManager = new SessionManager(this);
        if (!sessionManager.isLoggedIn()) {
            Toast.makeText(this, "Silakan login terlebih dahulu untuk mengakses data perkara.", Toast.LENGTH_LONG).show();
            Intent loginIntent = new Intent(this, MainActivity.class);
            if (perkaraId > 0) {
                loginIntent.putExtra("pending_perkara_id", perkaraId);
            }
            if (nomorPerkaraInitial != null) {
                loginIntent.putExtra("pending_nomor_perkara", nomorPerkaraInitial);
            }
            loginIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(loginIntent);
            finish();
            return;
        }

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
        loadAllData();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        extractIntentData(intent);
        if (perkaraId > 0) {
            if (tvNomorPerkara != null) {
                if (nomorPerkaraInitial != null && !nomorPerkaraInitial.isEmpty()) {
                    tvNomorPerkara.setText(nomorPerkaraInitial);
                } else {
                    tvNomorPerkara.setText("Perkara #" + perkaraId);
                }
            }
            if (swipeRefresh != null) {
                swipeRefresh.setRefreshing(true);
            }
            loadAllData();
        }
    }

    private void extractIntentData(Intent intent) {
        if (intent == null) return;

        perkaraId = intent.getIntExtra(EXTRA_PERKARA_ID, -1);
        nomorPerkaraInitial = intent.getStringExtra(EXTRA_NOMOR_PERKARA);

        // Jika dibuka dari FCM (data payload dikirim sebagai String di extras)
        if (perkaraId <= 0) {
            String strPerkaraId = intent.getStringExtra("perkara_id");
            if (strPerkaraId != null && !strPerkaraId.trim().isEmpty()) {
                try {
                    perkaraId = Integer.parseInt(strPerkaraId.trim());
                } catch (NumberFormatException ignored) {}
            }
        }

        if (nomorPerkaraInitial == null || nomorPerkaraInitial.isEmpty()) {
            nomorPerkaraInitial = intent.getStringExtra("nomor_perkara");
        }
    }

    private void initViews() {
        btnBack            = findViewById(R.id.btnBack);
        btnRefresh         = findViewById(R.id.btnRefresh);
        tvNomorPerkara     = findViewById(R.id.tvNomorPerkara);
        tvTahapSaatIni     = findViewById(R.id.tvTahapSaatIni);
        tvStatusSaatIni    = findViewById(R.id.tvStatusSaatIni);
        layoutPenyidik     = findViewById(R.id.layoutPenyidik);
        tvPenyidik         = findViewById(R.id.tvPenyidik);

        // Section Aksi Perkara Views
        tvActionCountBadge         = findViewById(R.id.tvActionCountBadge);
        pbActionsLoading           = findViewById(R.id.pbActionsLoading);
        layoutActionCardsContainer = findViewById(R.id.layoutActionCardsContainer);
        cardEmptyAction            = findViewById(R.id.cardEmptyAction);
        tvEmptyActionTitle         = findViewById(R.id.tvEmptyActionTitle);
        tvEmptyActionDesc          = findViewById(R.id.tvEmptyActionDesc);

        // Timeline Views
        swipeRefresh       = findViewById(R.id.swipeRefresh);
        nestedScrollView   = findViewById(R.id.nestedScrollView);
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

        if (btnRefresh != null) {
            btnRefresh.setOnClickListener(v -> {
                btnRefresh.animate()
                        .rotationBy(360f)
                        .setDuration(600)
                        .withEndAction(() -> btnRefresh.setRotation(0f))
                        .start();
                if (swipeRefresh != null) {
                    swipeRefresh.setRefreshing(true);
                }
                loadAllData();
            });
        }

        btnRetry.setOnClickListener(v -> {
            if (swipeRefresh != null) {
                swipeRefresh.setRefreshing(true);
            }
            loadAllData();
        });
    }

    private void setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.headerLayout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), systemBars.top, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });

        if (nestedScrollView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(nestedScrollView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), systemBars.bottom + 16);
                return insets;
            });
        }
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
        swipeRefresh.setOnRefreshListener(this::loadAllData);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(TimelineViewModel.class);

        // 1. Observer Riwayat Timeline Perkara
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
                if (swipeRefresh != null) {
                    swipeRefresh.setRefreshing(false);
                }
                if (progressBar != null) {
                    progressBar.setVisibility(View.GONE);
                }

                String errorMsg = result.getMessage() != null ? result.getMessage() : "Gagal memuat riwayat perkara";

                if (errorMsg.contains("UNAUTHORIZED:401")) {
                    handleUnauthorized();
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

        // 2. Observer Aksi Alur Perkara yang Tersedia (GET /api/perkara/{id}/available-actions)
        viewModel.getAvailableActionsResult().observe(this, result -> {
            if (result == null) return;

            if (result.isLoading()) {
                pbActionsLoading.setVisibility(View.VISIBLE);
                cardEmptyAction.setVisibility(View.GONE);
            } else if (result.isSuccess()) {
                pbActionsLoading.setVisibility(View.GONE);
                AvailableActionsResponse body = result.getData();

                if (body != null && body.getData() != null && !body.getData().isEmpty() && body.isCanExecute()) {
                    List<WorkflowAction> actions = body.getData();
                    tvActionCountBadge.setVisibility(View.VISIBLE);
                    tvActionCountBadge.setText(actions.size() + " Aksi Tersedia");
                    populateActionCards(actions);
                } else {
                    tvActionCountBadge.setVisibility(View.GONE);
                    layoutActionCardsContainer.removeAllViews();
                    layoutActionCardsContainer.setVisibility(View.GONE);
                    cardEmptyAction.setVisibility(View.VISIBLE);

                    if (body != null && !body.isCanExecute()) {
                        tvEmptyActionTitle.setText("Tidak memiliki wewenang aksi pada perkara ini");
                        tvEmptyActionDesc.setText("Hanya Jaksa yang ditugaskan pada perkara ini yang dapat memproses tindakan alur.");
                    } else {
                        tvEmptyActionTitle.setText("Tidak ada tindakan yang perlu dilakukan pada tahap ini");
                        tvEmptyActionDesc.setText("Perkara sedang dalam proses atau menunggu tindak lanjut dari pihak penyidik.");
                    }
                }
            } else if (result.isError()) {
                pbActionsLoading.setVisibility(View.GONE);
                String errorMsg = result.getMessage() != null ? result.getMessage() : "";

                if (errorMsg.contains("UNAUTHORIZED:401")) {
                    handleUnauthorized();
                    return;
                }

                tvActionCountBadge.setVisibility(View.GONE);
                layoutActionCardsContainer.removeAllViews();
                layoutActionCardsContainer.setVisibility(View.GONE);
                cardEmptyAction.setVisibility(View.VISIBLE);

                if (errorMsg.contains("FORBIDDEN:403")) {
                    tvEmptyActionTitle.setText("Akses ditolak pada perkara ini");
                    tvEmptyActionDesc.setText("Anda tidak terdaftar sebagai Jaksa yang ditugaskan untuk mengeksekusi alur perkara ini.");
                } else {
                    tvEmptyActionTitle.setText("Aksi alur belum dapat dimuat");
                    tvEmptyActionDesc.setText("Tarik ke bawah atau tekan refresh untuk memuat ulang daftar tindakan.");
                }
            }
        });

        // 3. Observer Eksekusi Aksi Workflow (POST /api/perkara/{id}/actions)
        viewModel.getProcessActionResult().observe(this, result -> {
            if (result == null) return;

            if (result.isLoading()) {
                // Dialog sudah mengatur loading spinner dan mendisable tombol
            } else if (result.isSuccess()) {
                if (activeActionDialog != null && activeActionDialog.isShowing()) {
                    activeActionDialog.dismiss();
                    activeActionDialog = null;
                }

                GenericResponse body = result.getData();
                String msg = (body != null && body.getMessage() != null)
                        ? body.getMessage()
                        : "Aksi alur perkara berhasil diproses.";
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();

                // Muat ulang detail, available actions, dan timeline secara segar dari API
                if (swipeRefresh != null) {
                    swipeRefresh.setRefreshing(true);
                }
                loadAllData();

            } else if (result.isError()) {
                String errorMsg = result.getMessage() != null ? result.getMessage() : "Gagal memproses aksi perkara";

                if (activeActionDialog != null && activeActionDialog.isShowing()) {
                    ProgressBar pbDialogLoading = activeActionDialog.findViewById(R.id.pbDialogLoading);
                    MaterialButton btnDialogConfirm = activeActionDialog.findViewById(R.id.btnDialogConfirm);
                    MaterialButton btnDialogCancel = activeActionDialog.findViewById(R.id.btnDialogCancel);
                    TextView tvDialogError = activeActionDialog.findViewById(R.id.tvDialogError);

                    if (pbDialogLoading != null) pbDialogLoading.setVisibility(View.GONE);
                    if (btnDialogConfirm != null) {
                        btnDialogConfirm.setEnabled(true);
                        btnDialogConfirm.setText("Konfirmasi Tindakan");
                    }
                    if (btnDialogCancel != null) btnDialogCancel.setEnabled(true);

                    if (errorMsg.contains("UNAUTHORIZED:401")) {
                        activeActionDialog.dismiss();
                        activeActionDialog = null;
                        handleUnauthorized();
                        return;
                    }

                    if (errorMsg.contains("CONFLICT:409")) {
                        // Stale state / alur telah berubah
                        activeActionDialog.dismiss();
                        activeActionDialog = null;
                        Toast.makeText(this, "Tahap atau status perkara telah berubah. Memuat data terbaru...", Toast.LENGTH_LONG).show();
                        if (swipeRefresh != null) {
                            swipeRefresh.setRefreshing(true);
                        }
                        loadAllData();
                        return;
                    }

                    String cleanMsg = errorMsg;
                    if (cleanMsg.contains("FORBIDDEN:403:")) {
                        cleanMsg = cleanMsg.replace("FORBIDDEN:403:", "");
                    } else if (cleanMsg.contains("VALIDATION:422:")) {
                        cleanMsg = cleanMsg.replace("VALIDATION:422:", "");
                    }

                    if (tvDialogError != null) {
                        tvDialogError.setVisibility(View.VISIBLE);
                        tvDialogError.setText(cleanMsg);
                    } else {
                        Toast.makeText(this, cleanMsg, Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    /**
     * Mengisi kartu aksi secara dinamis berdasarkan respons backend.
     * Khusus P-24, kedua opsi (P-21 Berkas Lengkap dan P-19 Belum Lengkap) ditampilkan
     * berdampingan/berurutan secara eksplisit tanpa otomatisasi.
     */
    private void populateActionCards(List<WorkflowAction> actions) {
        layoutActionCardsContainer.removeAllViews();
        if (actions == null || actions.isEmpty()) {
            layoutActionCardsContainer.setVisibility(View.GONE);
            cardEmptyAction.setVisibility(View.VISIBLE);
            return;
        }

        layoutActionCardsContainer.setVisibility(View.VISIBLE);
        cardEmptyAction.setVisibility(View.GONE);

        for (WorkflowAction action : actions) {
            View cardView = LayoutInflater.from(this).inflate(R.layout.item_perkara_action_card, layoutActionCardsContainer, false);

            TextView tvActionBadge         = cardView.findViewById(R.id.tvActionBadge);
            View viewAccentDot             = cardView.findViewById(R.id.viewAccentDot);
            TextView tvActionTitle         = cardView.findViewById(R.id.tvActionTitle);
            TextView tvActionDescription   = cardView.findViewById(R.id.tvActionDescription);
            TextView tvActionImpact        = cardView.findViewById(R.id.tvActionImpact);
            MaterialButton btnExecuteAction = cardView.findViewById(R.id.btnExecuteAction);

            tvActionBadge.setText(action.getBadgeLabel());
            tvActionTitle.setText(action.getDisplayTitle());
            tvActionDescription.setText(action.getDisplayDescription());
            tvActionImpact.setText(action.getNextImpactSummary());
            btnExecuteAction.setText(action.getActionButtonText());

            try {
                int accentColor = Color.parseColor(action.getAccentColorHex());
                btnExecuteAction.setBackgroundTintList(ColorStateList.valueOf(accentColor));
                tvActionBadge.setTextColor(accentColor);
                if (viewAccentDot != null && viewAccentDot.getBackground() != null) {
                    viewAccentDot.getBackground().setTint(accentColor);
                }
            } catch (Exception ignored) {}

            btnExecuteAction.setOnClickListener(v -> showConfirmActionDialog(action));

            layoutActionCardsContainer.addView(cardView);
        }
    }

    /**
     * Menampilkan dialog konfirmasi tindakan alur perkara resmi.
     */
    private void showConfirmActionDialog(WorkflowAction action) {
        if (isFinishing() || isDestroyed()) return;

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_confirm_workflow_action, null);

        TextView tvDialogNomorPerkara = dialogView.findViewById(R.id.tvDialogNomorPerkara);
        TextView tvDialogActionTitle  = dialogView.findViewById(R.id.tvDialogActionTitle);
        TextView tvDialogActionImpact = dialogView.findViewById(R.id.tvDialogActionImpact);
        View layoutTanggalPicker      = dialogView.findViewById(R.id.layoutTanggalPicker);
        TextView tvDialogTanggal      = dialogView.findViewById(R.id.tvDialogTanggal);
        EditText etDialogCatatan      = dialogView.findViewById(R.id.etDialogCatatan);
        TextView tvDialogError        = dialogView.findViewById(R.id.tvDialogError);
        ProgressBar pbDialogLoading   = dialogView.findViewById(R.id.pbDialogLoading);
        MaterialButton btnDialogCancel  = dialogView.findViewById(R.id.btnDialogCancel);
        MaterialButton btnDialogConfirm = dialogView.findViewById(R.id.btnDialogConfirm);

        tvDialogNomorPerkara.setText("Nomor: " + tvNomorPerkara.getText().toString());
        tvDialogActionTitle.setText(action.getDisplayTitle());
        tvDialogActionImpact.setText(action.getNextImpactSummary());

        // Default tanggal hari ini (format standar yyyy-MM-dd)
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        tvDialogTanggal.setText(sdf.format(new Date()));

        layoutTanggalPicker.setOnClickListener(v -> showDatePicker(tvDialogTanggal));

        btnDialogConfirm.setText("Konfirmasi Tindakan");
        try {
            int accentColor = Color.parseColor(action.getAccentColorHex());
            btnDialogConfirm.setBackgroundTintList(ColorStateList.valueOf(accentColor));
        } catch (Exception ignored) {}

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        btnDialogCancel.setOnClickListener(v -> dialog.dismiss());

        btnDialogConfirm.setOnClickListener(v -> {
            String tanggal = tvDialogTanggal.getText().toString().trim();
            String catatan = etDialogCatatan.getText().toString().trim();

            btnDialogConfirm.setEnabled(false);
            btnDialogConfirm.setText("Memproses...");
            btnDialogCancel.setEnabled(false);
            pbDialogLoading.setVisibility(View.VISIBLE);
            tvDialogError.setVisibility(View.GONE);

            // Kirim action_id aktual backend (bukan label UI)
            viewModel.executeAction(perkaraId, action.getId(), tanggal, catatan.isEmpty() ? null : catatan);
        });

        activeActionDialog = dialog;
        dialog.show();
    }

    private void showDatePicker(TextView tvTanggal) {
        Calendar cal = Calendar.getInstance();
        try {
            String currentStr = tvTanggal.getText().toString();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date d = sdf.parse(currentStr);
            if (d != null) {
                cal.setTime(d);
            }
        } catch (Exception ignored) {}

        int year  = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int day   = cal.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dpd = new DatePickerDialog(this, (view, y, m, d) -> {
            String formatted = String.format(Locale.getDefault(), "%04d-%02d-%02d", y, (m + 1), d);
            tvTanggal.setText(formatted);
        }, year, month, day);
        dpd.show();
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

    private void loadAllData() {
        loadTimeline();
        loadAvailableActions();
    }

    private void loadTimeline() {
        if (viewModel != null) {
            viewModel.loadPerkaraTimeline(perkaraId);
        }
    }

    private void loadAvailableActions() {
        if (viewModel != null) {
            viewModel.loadAvailableActions(perkaraId);
        }
    }

    private void handleUnauthorized() {
        SessionManager sessionManager = new SessionManager(this);
        sessionManager.clearSession();
        Toast.makeText(this, "Sesi telah berakhir. Silakan login kembali.", Toast.LENGTH_LONG).show();
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (activeActionDialog != null && activeActionDialog.isShowing()) {
            activeActionDialog.dismiss();
            activeActionDialog = null;
        }
    }
}