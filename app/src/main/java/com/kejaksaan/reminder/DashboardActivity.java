package com.kejaksaan.reminder;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.signature.ObjectKey;
import com.kejaksaan.reminder.adapter.BeritaDashboardAdapter;
import com.kejaksaan.reminder.adapter.ReminderAdapter;
import com.kejaksaan.reminder.model.Berita;
import com.kejaksaan.reminder.model.Reminder;
import com.kejaksaan.reminder.model.User;
import com.kejaksaan.reminder.util.FcmTokenManager;
import com.kejaksaan.reminder.util.NotificationHelper;
import com.kejaksaan.reminder.util.SessionManager;
import com.kejaksaan.reminder.viewmodel.AuthViewModel;
import com.kejaksaan.reminder.viewmodel.BeritaViewModel;
import com.kejaksaan.reminder.viewmodel.PerkaraViewModel;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DashboardActivity extends AppCompatActivity {

    private TextView tvUserName;
    private TextView tvUserRole;
    private ImageView imgProfileAvatar;
    private RecyclerView rvSlaReminders;
    private View layoutSlaReminders;
    private RecyclerView rvBeritaDashboard;
    private SwipeRefreshLayout swipeRefresh;
    private NestedScrollView nestedScrollView;

    private PerkaraViewModel perkaraViewModel;
    private BeritaViewModel beritaViewModel;
    private ReminderAdapter reminderAdapter;
    private BeritaDashboardAdapter beritaDashboardAdapter;

    private boolean isProfileRefreshing = false;
    private boolean isSlaRefreshing = false;
    private boolean isBeritaRefreshing = false;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                // Hasil izin POST_NOTIFICATIONS (Android 13+)
            });

    @Override
    protected void onResume() {
        super.onResume();
        SessionManager sessionManager = new SessionManager(this);
        if (!sessionManager.isLoggedIn()) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }
        if (perkaraViewModel != null) {
            perkaraViewModel.loadUserProfile();
        }
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Verifikasi sesi pengguna
        SessionManager sessionManager = new SessionManager(this);
        if (!sessionManager.isLoggedIn()) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dashboard);

        // Inisialisasi Notification Channel untuk notifikasi pengingat SIPETA
        NotificationHelper.createNotificationChannel(this);

        // Cek dan minta runtime permission POST_NOTIFICATIONS untuk Android 13+
        checkNotificationPermission();

        // Periksa dan tawarkan pembebasan penghemat baterai agar alarm offline tidak dibekukan sistem
        com.kejaksaan.reminder.util.BatteryOptimizationHelper.checkAndPromptBatteryOptimization(this);

        // Pastikan token FCM perangkat terdaftar/disinkronkan dengan backend
        FcmTokenManager.registerCurrentTokenToServer(this, null);

        // Top padding for status bar (applied to profile card to avoid pushing background)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.cardUserProfile), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), systemBars.top + 16, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });

        // Bottom padding for navigation bar background (so background reaches bottom)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.navBarBackground), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), systemBars.bottom + 12);
            return insets;
        });

        // Initialize Views
        tvUserName = findViewById(R.id.tvUserName);
        tvUserRole = findViewById(R.id.tvUserRole);
        imgProfileAvatar = findViewById(R.id.imgProfileAvatar);
        rvSlaReminders = findViewById(R.id.rvSlaReminders);
        layoutSlaReminders = findViewById(R.id.layoutSlaReminders);
        rvBeritaDashboard = findViewById(R.id.rvBeritaDashboard);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        nestedScrollView = findViewById(R.id.nestedScrollView);

        // Setup SwipeRefreshLayout
        if (swipeRefresh != null) {
            swipeRefresh.setColorSchemeColors(
                    Color.parseColor("#00897B"),
                    Color.parseColor("#DC2626"),
                    Color.parseColor("#D97706")
            );
            swipeRefresh.setOnRefreshListener(this::refreshDashboardData);
        }

        // Setup Header Refresh Button
        ImageView btnRefreshDashboard = findViewById(R.id.btnRefreshDashboard);
        if (btnRefreshDashboard != null) {
            btnRefreshDashboard.setOnClickListener(v -> {
                if (swipeRefresh != null) {
                    swipeRefresh.setRefreshing(true);
                }
                refreshDashboardData();
            });
        }

        // Bottom Nav Dashboard item: tap to scroll top & refresh
        View navDashboard = findViewById(R.id.navDashboard);
        if (navDashboard != null) {
            navDashboard.setOnClickListener(v -> {
                if (nestedScrollView != null) {
                    nestedScrollView.smoothScrollTo(0, 0);
                }
                if (swipeRefresh != null) {
                    swipeRefresh.setRefreshing(true);
                }
                refreshDashboardData();
            });
        }

        // Setup RecyclerView SLA Reminders (Horizontal Carousel)
        LinearLayoutManager slaLayoutManager =
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        rvSlaReminders.setLayoutManager(slaLayoutManager);
        rvSlaReminders.setNestedScrollingEnabled(false);
        reminderAdapter = new ReminderAdapter(this, true);
        rvSlaReminders.setAdapter(reminderAdapter);

        // Klik item reminder membuka DetailPerkaraActivity atau ReminderListActivity
        reminderAdapter.setOnReminderClickListener(reminder -> {
            if (reminder != null && reminder.getPerkara() != null && reminder.getPerkara().getId() > 0) {
                Intent intent = new Intent(DashboardActivity.this, DetailPerkaraActivity.class);
                intent.putExtra(DetailPerkaraActivity.EXTRA_PERKARA_ID, reminder.getPerkara().getId());
                intent.putExtra(DetailPerkaraActivity.EXTRA_NOMOR_PERKARA, reminder.getPerkara().getNomorPerkara());
                startActivity(intent);
            } else if (reminder != null && reminder.getPerkaraId() > 0) {
                Intent intent = new Intent(DashboardActivity.this, DetailPerkaraActivity.class);
                intent.putExtra(DetailPerkaraActivity.EXTRA_PERKARA_ID, reminder.getPerkaraId());
                startActivity(intent);
            } else {
                Intent intent = new Intent(DashboardActivity.this, ReminderListActivity.class);
                startActivity(intent);
            }
        });

        // Setup RecyclerView Berita Dashboard (Horizontal Carousel)
        if (rvBeritaDashboard != null) {
            LinearLayoutManager beritaLayoutManager =
                    new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
            rvBeritaDashboard.setLayoutManager(beritaLayoutManager);
            rvBeritaDashboard.setNestedScrollingEnabled(false);
            beritaDashboardAdapter = new BeritaDashboardAdapter(this);
            rvBeritaDashboard.setAdapter(beritaDashboardAdapter);

            beritaDashboardAdapter.setOnBeritaClickListener(berita -> {
                Intent intent = new Intent(DashboardActivity.this, DetailBeritaActivity.class);
                intent.putExtra(DetailBeritaActivity.EXTRA_BERITA, berita);
                startActivity(intent);
            });
        }

        // Initialize ViewModels
        perkaraViewModel = new ViewModelProvider(this).get(PerkaraViewModel.class);
        beritaViewModel = new ViewModelProvider(this).get(BeritaViewModel.class);

        // Observe profile changes
        perkaraViewModel.getUserProfileResult().observe(this, result -> {
            if (result == null) return;
            if (result.isLoading()) {
                if (swipeRefresh != null) {
                    swipeRefresh.setRefreshing(true);
                }
            } else {
                isProfileRefreshing = false;
                checkStopDashboardRefresh();
                if (result.isSuccess() && result.getData() != null) {
                    bindUserProfile(result.getData());
                } else if (result.isError()) {
                    Toast.makeText(this, result.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Observe SLA Reminders
        perkaraViewModel.getSlaRemindersResult().observe(this, result -> {
            if (result == null) return;
            if (result.isLoading()) {
                if (swipeRefresh != null) {
                    swipeRefresh.setRefreshing(true);
                }
            } else {
                isSlaRefreshing = false;
                checkStopDashboardRefresh();
                if (result.isSuccess() && result.getData() != null) {
                    List<Reminder> reminders = result.getData();
                    // Selaraskan jadwal alarm offline mandiri (H-3, H-1, H-0) ke sistem Android
                    com.kejaksaan.reminder.util.LocalAlarmScheduler.scheduleAlarmsForReminders(DashboardActivity.this, reminders);

                    if (reminders.isEmpty()) {
                        layoutSlaReminders.setVisibility(View.GONE);
                        rvSlaReminders.setVisibility(View.GONE);
                    } else {
                        layoutSlaReminders.setVisibility(View.VISIBLE);
                        rvSlaReminders.setVisibility(View.VISIBLE);
                        // Prioritaskan P-24 (termasuk yang lewat 7 hari / overdue) dan Overdue di urutan terdepan card
                        List<Reminder> sortedList = new ArrayList<>(reminders);
                        Collections.sort(sortedList, (r1, r2) -> {
                            int p1 = getReminderPriority(r1);
                            int p2 = getReminderPriority(r2);
                            if (p1 != p2) {
                                return Integer.compare(p1, p2);
                            }
                            return Integer.compare(r1.getSisaHari(), r2.getSisaHari());
                        });

                        List<Reminder> displayList = new ArrayList<>();
                        int maxCount = Math.min(10, sortedList.size());
                        for (int i = 0; i < maxCount; i++) {
                            displayList.add(sortedList.get(i));
                        }
                        reminderAdapter.setReminderList(displayList);
                    }
                } else if (result.isError()) {
                    layoutSlaReminders.setVisibility(View.GONE);
                    rvSlaReminders.setVisibility(View.GONE);
                }
            }
        });

        // Observe Dashboard Berita
        beritaViewModel.getDashboardBeritasResult().observe(this, result -> {
            if (result == null) return;
            if (result.isLoading()) {
                if (swipeRefresh != null) {
                    swipeRefresh.setRefreshing(true);
                }
            } else {
                isBeritaRefreshing = false;
                checkStopDashboardRefresh();
                if (result.isSuccess() && result.getData() != null) {
                    List<Berita> beritas = result.getData();
                    if (beritaDashboardAdapter != null) {
                        beritaDashboardAdapter.setBeritaList(beritas);
                    }
                }
            }
        });

        // Fetch API on start
        if (swipeRefresh != null) {
            swipeRefresh.post(() -> swipeRefresh.setRefreshing(true));
        }
        refreshDashboardData();

        // Setup Logout functionality
        AuthViewModel authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        
        View.OnClickListener logoutListener = v -> {
            v.setEnabled(false);
            Toast.makeText(this, "Keluar dari sesi...", Toast.LENGTH_SHORT).show();
            authViewModel.logout(() -> runOnUiThread(() -> {
                Intent intent = new Intent(DashboardActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }));
        };

        findViewById(R.id.btnLogout).setOnClickListener(logoutListener);
        findViewById(R.id.cardBtnLogout).setOnClickListener(logoutListener);

        // Menu Perkara
        findViewById(R.id.menuPerkara).setOnClickListener(v -> {
            Intent intent = new Intent(this, PerkaraListActivity.class);
            startActivity(intent);
        });

        // View All SLA Reminders -> Buka ReminderListActivity
        findViewById(R.id.tvViewAllSla).setOnClickListener(v -> {
            Intent intent = new Intent(this, ReminderListActivity.class);
            startActivity(intent);
        });

        // Menu Reminder -> Buka ReminderListActivity
        findViewById(R.id.menuReminder).setOnClickListener(v -> {
            Intent intent = new Intent(this, ReminderListActivity.class);
            startActivity(intent);
        });

        // Menu Timeline (Riwayat Aktivitas Perkara) -> Buka TimelineListActivity
        View menuTimeline = findViewById(R.id.menuTimeline);
        if (menuTimeline != null) {
            menuTimeline.setOnClickListener(v -> {
                Intent intent = new Intent(this, TimelineListActivity.class);
                startActivity(intent);
            });
        }

        // Card User Profile -> Buka ProfileActivity
        findViewById(R.id.cardUserProfile).setOnClickListener(v -> {
            Intent intent = new Intent(this, ProfileActivity.class);
            startActivity(intent);
        });

        // Grid Menu: Profile -> Buka ProfileActivity
        findViewById(R.id.menuProfile).setOnClickListener(v -> {
            Intent intent = new Intent(this, ProfileActivity.class);
            startActivity(intent);
        });

        // Grid Menu: Berita -> Buka BeritaListActivity
        View menuBerita = findViewById(R.id.menuBerita);
        if (menuBerita != null) {
            menuBerita.setOnClickListener(v -> {
                Intent intent = new Intent(this, BeritaListActivity.class);
                startActivity(intent);
            });
        }

        // View All Berita -> Buka BeritaListActivity
        View tvViewAllBerita = findViewById(R.id.tvViewAllBerita);
        if (tvViewAllBerita != null) {
            tvViewAllBerita.setOnClickListener(v -> {
                Intent intent = new Intent(this, BeritaListActivity.class);
                startActivity(intent);
            });
        }

        // Grid Menu: Log SLA (Audit Trail Reminder SLA) -> Buka ReminderLogListActivity
        View menuLogReminder = findViewById(R.id.menuLogReminder);
        if (menuLogReminder != null) {
            menuLogReminder.setOnClickListener(v -> {
                Intent intent = new Intent(this, ReminderLogListActivity.class);
                startActivity(intent);
            });
        }

        // Bottom Navigation Bar - Notifikasi
        View navNotifikasi = findViewById(R.id.navNotifikasi);
        if (navNotifikasi != null) {
            navNotifikasi.setOnClickListener(v -> {
                Intent intent = new Intent(this, NotificationActivity.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
            });
        }

        // Tangani navigasi jika aplikasi dibuka dari notifikasi FCM
        handlePendingNotificationIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handlePendingNotificationIntent(intent);
    }

    private void handlePendingNotificationIntent(Intent intent) {
        if (intent == null) return;
        int perkaraId = intent.getIntExtra(DetailPerkaraActivity.EXTRA_PERKARA_ID, -1);
        if (perkaraId <= 0 && intent.hasExtra("perkara_id")) {
            try {
                String pStr = intent.getStringExtra("perkara_id");
                if (pStr != null) perkaraId = Integer.parseInt(pStr.trim());
            } catch (Exception ignored) {}
        }
        if (perkaraId > 0) {
            Intent detailIntent = new Intent(this, DetailPerkaraActivity.class);
            detailIntent.putExtra(DetailPerkaraActivity.EXTRA_PERKARA_ID, perkaraId);
            String nomorPerkara = intent.getStringExtra(DetailPerkaraActivity.EXTRA_NOMOR_PERKARA);
            if (nomorPerkara == null) nomorPerkara = intent.getStringExtra("nomor_perkara");
            if (nomorPerkara != null) {
                detailIntent.putExtra(DetailPerkaraActivity.EXTRA_NOMOR_PERKARA, nomorPerkara);
            }
            startActivity(detailIntent);
        }
    }

    private void refreshDashboardData() {
        isProfileRefreshing = true;
        isSlaRefreshing = true;
        isBeritaRefreshing = true;
        perkaraViewModel.loadUserProfile();
        perkaraViewModel.loadSlaReminders("semua_mendesak");
        beritaViewModel.loadDashboardBeritas(5);
    }

    private void checkStopDashboardRefresh() {
        if (!isProfileRefreshing && !isSlaRefreshing && !isBeritaRefreshing && swipeRefresh != null) {
            swipeRefresh.setRefreshing(false);
        }
    }

    private void bindUserProfile(User user) {
        tvUserName.setText(user.getName());
        
        String jabatanOrRole = user.getJabatan() != null && !user.getJabatan().isEmpty() 
            ? user.getJabatan() 
            : user.getRole();
        tvUserRole.setText(jabatanOrRole);

        loadDashboardAvatar(user);
    }

    private void loadDashboardAvatar(User user) {
        if (imgProfileAvatar == null) return;

        SessionManager sm = new SessionManager(this);
        String localPath = sm.fetchLocalAvatarPath();
        File localFile = (localPath != null && !localPath.isEmpty()) ? new File(localPath) : null;
        boolean hasLocal = localFile != null && localFile.exists() && localFile.length() > 0;

        String remoteUrl = (user != null) ? user.getResolvedPhotoUrl() : null;
        if (remoteUrl == null || remoteUrl.trim().isEmpty()) {
            remoteUrl = sm.fetchFotoProfilUrl();
        }

        if (remoteUrl != null && !remoteUrl.trim().isEmpty()) {
            com.bumptech.glide.RequestBuilder<android.graphics.drawable.Drawable> errorBuilder = hasLocal
                    ? Glide.with(this).load(localFile).circleCrop()
                    : Glide.with(this).load(R.drawable.logo_kejaksaan).circleCrop();

            Glide.with(this)
                    .load(remoteUrl)
                    .placeholder(R.drawable.logo_kejaksaan)
                    .error(errorBuilder)
                    .signature(new ObjectKey(System.currentTimeMillis()))
                    .circleCrop()
                    .into(imgProfileAvatar);
        } else if (hasLocal) {
            Glide.with(this)
                    .load(localFile)
                    .signature(new ObjectKey(System.currentTimeMillis()))
                    .circleCrop()
                    .into(imgProfileAvatar);
        } else {
            imgProfileAvatar.setImageResource(R.drawable.logo_kejaksaan);
        }
    }

    private int getReminderPriority(Reminder r) {
        if (r == null) return 99;

        // Tahapan monitoring tanpa deadline (seperti Tahap II) tidak boleh masuk kategori darurat
        if (r.isMonitoringStage() || !r.hasDeadline()) {
            return 7;
        }

        boolean isP24 = isP24Reminder(r);
        int sisa = r.getSisaHari();

        // P-24 yang sudah lewat dari 7 hari (overdue) diprioritaskan paling depan pada card
        if (isP24 && sisa < 0) return 1;
        // Reminder Overdue lainnya
        if (sisa < 0) return 2;
        // P-24 Jatuh Tempo / H-3
        if (isP24 && sisa <= 3) return 3;
        // Jatuh tempo hari ini
        if (sisa == 0) return 4;
        // H-3
        if (sisa <= 3) return 5;
        // Aman / lainnya
        return 6;
    }

    private boolean isP24Reminder(Reminder r) {
        if (r == null) return false;
        String tahap = r.getTahap() != null ? r.getTahap() : "";
        String jenis = r.getJenisReminder() != null ? r.getJenisReminder() : "";
        return tahap.toUpperCase().contains("P-24") || tahap.toUpperCase().contains("P24")
                || jenis.toUpperCase().contains("P-24") || jenis.toUpperCase().contains("P24");
    }
}