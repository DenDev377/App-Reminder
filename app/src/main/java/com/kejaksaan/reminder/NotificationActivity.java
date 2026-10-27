package com.kejaksaan.reminder;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
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

import com.kejaksaan.reminder.adapter.TodayNotificationAdapter;
import com.kejaksaan.reminder.model.PushNotificationItem;
import com.kejaksaan.reminder.viewmodel.NotificationViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * NotificationActivity — Pusat Notifikasi Ramping, Real-time & Paginasi:
 * 1. Memprioritaskan notifikasi paling baru (Hari ini / Terkini).
 * 2. Notifikasi lama disembunyikan secara default (bisa di-expand dengan klik tombol).
 * 3. Mendukung paginasi ke server untuk memuat notifikasi lebih lama.
 */
public class NotificationActivity extends AppCompatActivity implements TodayNotificationAdapter.OnNotificationActionListener {

    // ─── Header Views ────────────────────────────────────────────────────────────
    private View btnBack;
    private View btnRefresh;

    // ─── List & Adapter ──────────────────────────────────────────────────────────
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView rvNotifications;
    private TodayNotificationAdapter adapter;
    private NotificationViewModel viewModel;

    // ─── Bottom Navigation ───────────────────────────────────────────────────────
    private View navDashboard;
    private View navNotifikasi;

    // ─── State ───────────────────────────────────────────────────────────────────
    private final List<PushNotificationItem> rawNotifications = new ArrayList<>();
    private boolean isEarlierExpanded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_notification);

        // Insets padding
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.headerLayout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), systemBars.top + 14, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.navBarBackground), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), systemBars.bottom + 12);
            return insets;
        });

        initViews();
        setupRecyclerView();
        setupListeners();
        setupViewModel();

        // Load initial data
        swipeRefresh.post(() -> swipeRefresh.setRefreshing(true));
        viewModel.loadInitial();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        btnRefresh = findViewById(R.id.btnRefresh);

        swipeRefresh = findViewById(R.id.swipeRefresh);
        rvNotifications = findViewById(R.id.rvNotifications);

        navDashboard = findViewById(R.id.navDashboard);
        navNotifikasi = findViewById(R.id.navNotifikasi);

        swipeRefresh.setColorSchemeColors(
                Color.parseColor("#00897B"),
                Color.parseColor("#0284C7"),
                Color.parseColor("#059669")
        );
    }

    private void setupRecyclerView() {
        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TodayNotificationAdapter(this, this);
        rvNotifications.setAdapter(adapter);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnRefresh.setOnClickListener(v -> {
            btnRefresh.animate()
                    .rotationBy(360f)
                    .setDuration(600)
                    .withEndAction(() -> btnRefresh.setRotation(0f))
                    .start();
            swipeRefresh.setRefreshing(true);
            viewModel.loadInitial();
        });

        swipeRefresh.setOnRefreshListener(() -> viewModel.loadInitial());

        // Bottom Navigation Bar
        navDashboard.setOnClickListener(v -> {
            Intent intent = new Intent(this, DashboardActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
            overridePendingTransition(0, 0);
        });

        navNotifikasi.setOnClickListener(v -> {
            rvNotifications.smoothScrollToPosition(0);
        });
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(NotificationViewModel.class);

        viewModel.getNotificationsList().observe(this, list -> {
            rawNotifications.clear();
            if (list != null) {
                rawNotifications.addAll(list);
            }
            rebuildFeed();
        });

        viewModel.getIsRefreshing().observe(this, refreshing -> {
            if (refreshing != null) {
                swipeRefresh.setRefreshing(refreshing);
            }
        });

        viewModel.getIsLoadingMore().observe(this, loading -> {
            rebuildFeed();
        });

        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ─── TodayNotificationAdapter.OnNotificationActionListener ───────────────────

    @Override
    public void onPerkaraClick(int perkaraId, String nomorPerkara) {
        Intent intent = new Intent(NotificationActivity.this, DetailPerkaraActivity.class);
        intent.putExtra(DetailPerkaraActivity.EXTRA_PERKARA_ID, perkaraId);
        if (nomorPerkara != null && !nomorPerkara.isEmpty()) {
            intent.putExtra(DetailPerkaraActivity.EXTRA_NOMOR_PERKARA, nomorPerkara);
        }
        startActivity(intent);
    }

    @Override
    public void onToggleEarlierClick() {
        isEarlierExpanded = !isEarlierExpanded;
        rebuildFeed();
    }

    @Override
    public void onLoadMoreClick() {
        viewModel.loadMore();
    }

    /**
     * Membangun linimasa notifikasi:
     * 1. Prioritaskan notifikasi paling baru (Hari ini / 3 terkini).
     * 2. Notifikasi lama disembunyikan secara default agar layar tidak terlalu panjang.
     * 3. Klik tombol untuk membuka/menutup notifikasi sebelumnya.
     * 4. Paginasi "Muat Lebih Banyak" jika masih ada halaman berikutnya di server.
     */
    private void rebuildFeed() {
        List<TodayNotificationAdapter.FeedItem> feedItems = new ArrayList<>();

        List<PushNotificationItem> todayList = new ArrayList<>();
        List<PushNotificationItem> earlierList = new ArrayList<>();

        for (PushNotificationItem item : rawNotifications) {
            if (item.isToday()) {
                todayList.add(item);
            } else {
                earlierList.add(item);
            }
        }

        // KASUS 1: Ada notifikasi Hari Ini
        if (!todayList.isEmpty()) {
            feedItems.add(TodayNotificationAdapter.FeedItem.createHeader(
                    "🔔 Hari Ini",
                    todayList.size(),
                    R.drawable.ic_notification,
                    Color.parseColor("#00897B")
            ));
            for (PushNotificationItem item : todayList) {
                feedItems.add(TodayNotificationAdapter.FeedItem.createPush(item));
            }

            // Jika ada notifikasi sebelumnya, sembunyikan secara default
            if (!earlierList.isEmpty()) {
                if (!isEarlierExpanded) {
                    feedItems.add(TodayNotificationAdapter.FeedItem.createToggleEarlier(
                            "Lihat Notifikasi Sebelumnya (" + earlierList.size() + ")",
                            false
                    ));
                } else {
                    feedItems.add(TodayNotificationAdapter.FeedItem.createHeader(
                            "🕐 Riwayat Sebelumnya",
                            earlierList.size(),
                            R.drawable.ic_notification,
                            Color.parseColor("#64748B")
                    ));
                    for (PushNotificationItem item : earlierList) {
                        feedItems.add(TodayNotificationAdapter.FeedItem.createPush(item));
                    }

                    feedItems.add(TodayNotificationAdapter.FeedItem.createToggleEarlier(
                            "Sembunyikan Notifikasi Sebelumnya",
                            true
                    ));

                    if (viewModel.hasMorePages()) {
                        feedItems.add(TodayNotificationAdapter.FeedItem.createLoadMore(
                                Boolean.TRUE.equals(viewModel.getIsLoadingMore().getValue())
                        ));
                    }
                }
            }
        }
        // KASUS 2: Belum ada notifikasi Hari Ini, tampilkan 3 notifikasi paling baru
        else if (!earlierList.isEmpty()) {
            int topCount = Math.min(3, earlierList.size());

            feedItems.add(TodayNotificationAdapter.FeedItem.createHeader(
                    "🔔 Notifikasi Terkini",
                    topCount,
                    R.drawable.ic_notification,
                    Color.parseColor("#00897B")
            ));

            for (int i = 0; i < topCount; i++) {
                feedItems.add(TodayNotificationAdapter.FeedItem.createPush(earlierList.get(i)));
            }

            // Notifikasi sisanya disembunyikan di balik tombol ekspansi
            if (earlierList.size() > topCount) {
                int remaining = earlierList.size() - topCount;

                if (!isEarlierExpanded) {
                    feedItems.add(TodayNotificationAdapter.FeedItem.createToggleEarlier(
                            "Lihat Notifikasi Sebelumnya (" + remaining + ")",
                            false
                    ));
                } else {
                    feedItems.add(TodayNotificationAdapter.FeedItem.createHeader(
                            "🕐 Riwayat Sebelumnya",
                            remaining,
                            R.drawable.ic_notification,
                            Color.parseColor("#64748B")
                    ));

                    for (int i = topCount; i < earlierList.size(); i++) {
                        feedItems.add(TodayNotificationAdapter.FeedItem.createPush(earlierList.get(i)));
                    }

                    feedItems.add(TodayNotificationAdapter.FeedItem.createToggleEarlier(
                            "Sembunyikan Notifikasi Sebelumnya",
                            true
                    ));

                    if (viewModel.hasMorePages()) {
                        feedItems.add(TodayNotificationAdapter.FeedItem.createLoadMore(
                                Boolean.TRUE.equals(viewModel.getIsLoadingMore().getValue())
                        ));
                    }
                }
            }
        }
        // KASUS 3: Benar-benar kosong
        else {
            feedItems.add(TodayNotificationAdapter.FeedItem.createEmpty(
                    "🎉 Belum Ada Notifikasi",
                    "Pembaruan alur penanganan perkara dan pengingat real-time akan muncul di sini."
            ));
        }

        adapter.setItems(feedItems);
    }
}
