package com.kejaksaan.reminder;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.kejaksaan.reminder.adapter.ReminderAdapter;
import com.kejaksaan.reminder.model.Reminder;
import com.kejaksaan.reminder.model.User;
import com.kejaksaan.reminder.viewmodel.AuthViewModel;
import com.kejaksaan.reminder.viewmodel.PerkaraViewModel;

import java.util.ArrayList;
import java.util.List;

public class DashboardActivity extends AppCompatActivity {

    private TextView tvUserName;
    private TextView tvUserRole;
    private ImageView imgProfileAvatar;
    private RecyclerView rvSlaReminders;
    private View layoutSlaReminders;
    private SwipeRefreshLayout swipeRefresh;
    private NestedScrollView nestedScrollView;

    private PerkaraViewModel perkaraViewModel;
    private ReminderAdapter reminderAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dashboard);

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
        swipeRefresh = findViewById(R.id.swipeRefresh);
        nestedScrollView = findViewById(R.id.nestedScrollView);

        // Setup SwipeRefreshLayout
        if (swipeRefresh != null) {
            swipeRefresh.setColorSchemeColors(Color.parseColor("#00897B"), Color.parseColor("#D97706"));
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

        // Setup RecyclerView
        reminderAdapter = new ReminderAdapter(this);
        rvSlaReminders.setAdapter(reminderAdapter);

        // Initialize ViewModel
        perkaraViewModel = new ViewModelProvider(this).get(PerkaraViewModel.class);

        // Observe profile changes
        perkaraViewModel.getUserProfileResult().observe(this, result -> {
            if (swipeRefresh != null) {
                swipeRefresh.setRefreshing(false);
            }
            if (result.isSuccess() && result.getData() != null) {
                bindUserProfile(result.getData());
            } else if (result.isError()) {
                Toast.makeText(this, result.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        // Observe SLA Reminders
        perkaraViewModel.getSlaRemindersResult().observe(this, result -> {
            if (swipeRefresh != null) {
                swipeRefresh.setRefreshing(false);
            }
            if (result.isSuccess() && result.getData() != null) {
                List<Reminder> reminders = result.getData();
                if (reminders.isEmpty()) {
                    layoutSlaReminders.setVisibility(View.GONE);
                    rvSlaReminders.setVisibility(View.GONE);
                } else {
                    layoutSlaReminders.setVisibility(View.VISIBLE);
                    rvSlaReminders.setVisibility(View.VISIBLE);
                    // Ambil maksimal 3
                    List<Reminder> top3 = new ArrayList<>();
                    for (int i = 0; i < Math.min(3, reminders.size()); i++) {
                        top3.add(reminders.get(i));
                    }
                    reminderAdapter.setReminderList(top3);
                }
            } else if (result.isError()) {
                // Sembunyikan jika error
                layoutSlaReminders.setVisibility(View.GONE);
                rvSlaReminders.setVisibility(View.GONE);
            }
        });

        // Fetch API on start
        refreshDashboardData();

        // Setup Logout functionality
        AuthViewModel authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        
        View.OnClickListener logoutListener = v -> {
            authViewModel.logout();
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
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

        // Grid Menu: Log SLA (Audit Trail Reminder SLA) -> Buka ReminderLogListActivity
        View menuLogReminder = findViewById(R.id.menuLogReminder);
        if (menuLogReminder != null) {
            menuLogReminder.setOnClickListener(v -> {
                Intent intent = new Intent(this, ReminderLogListActivity.class);
                startActivity(intent);
            });
        }
    }

    private void refreshDashboardData() {
        perkaraViewModel.loadUserProfile();
        perkaraViewModel.loadSlaReminders("semua_mendesak");
    }

    private void bindUserProfile(User user) {
        tvUserName.setText(user.getName());
        
        // Show jabatan if available, otherwise fallback to role
        String jabatanOrRole = user.getJabatan() != null && !user.getJabatan().isEmpty() 
            ? user.getJabatan() 
            : user.getRole();
        tvUserRole.setText(jabatanOrRole);

        // Load image using Glide
        String photoUrl = user.getResolvedPhotoUrl();
        if (photoUrl != null) {
            Glide.with(this)
                 .load(photoUrl)
                 .placeholder(R.drawable.logo_kejaksaan)
                 .error(R.drawable.logo_kejaksaan)
                 .circleCrop()
                 .into(imgProfileAvatar);
        }
    }
}
