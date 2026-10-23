package com.kejaksaan.reminder;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.kejaksaan.reminder.util.NotificationHelper;
import com.kejaksaan.reminder.util.SessionManager;

/**
 * SplashActivity — Layar pembuka (Splash Screen) SIPETA Mobile.
 *
 * True Fullscreen / Immersive:
 * - Menembus area camera cutout / notch di bagian atas
 * - Status bar disembunyikan agar tampilan full layar tanpa bar hitam
 * - Animasi loading kapsul emas halus
 */
public class SplashActivity extends AppCompatActivity {

    private ProgressBar progressBarLoading;
    private TextView tvLoadingStatus;
    private ValueAnimator progressAnimator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Izinkan rendering penuh menembus area camera cutout / notch (agar bagian atas tidak terpotong)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getWindow().getAttributes().layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }

        // 2. Fullscreen & Edge-to-Edge sejati
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        // 3. Sembunyikan status bar agar splash screen 100% full layar tanpa potongan bar hitam
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (controller != null) {
            controller.hide(WindowInsetsCompat.Type.statusBars());
            controller.setSystemBarsBehavior(
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        }

        setContentView(R.layout.activity_splash);

        // Inisialisasi Notification Channel sejak awal aplikasi terbuka
        NotificationHelper.createNotificationChannel(this);

        progressBarLoading = findViewById(R.id.progressBarLoading);
        tvLoadingStatus = findViewById(R.id.tvLoadingStatus);

        startLoadingAnimation();
    }

    private void startLoadingAnimation() {
        if (progressBarLoading == null) {
            proceedToNextScreen();
            return;
        }

        progressAnimator = ValueAnimator.ofInt(0, 100);
        progressAnimator.setDuration(2400); // 2.4 detik durasi splash
        progressAnimator.setInterpolator(new DecelerateInterpolator());

        progressAnimator.addUpdateListener(animation -> {
            int progress = (int) animation.getAnimatedValue();
            progressBarLoading.setProgress(progress);

            if (tvLoadingStatus != null) {
                if (progress < 30) {
                    tvLoadingStatus.setText("Memuat data aplikasi...");
                } else if (progress < 70) {
                    tvLoadingStatus.setText("Memeriksa sesi pengguna...");
                } else if (progress < 95) {
                    tvLoadingStatus.setText("Menyiapkan SIPETA Mobile...");
                } else {
                    tvLoadingStatus.setText("Selesai");
                }
            }
        });

        progressAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                proceedToNextScreen();
            }
        });

        progressAnimator.start();
    }

    private void proceedToNextScreen() {
        if (isFinishing() || isDestroyed()) return;

        SessionManager sessionManager = new SessionManager(this);
        Intent intent;

        if (sessionManager.isLoggedIn()) {
            intent = new Intent(SplashActivity.this, DashboardActivity.class);
        } else {
            intent = new Intent(SplashActivity.this, MainActivity.class);
        }

        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (progressAnimator != null && progressAnimator.isRunning()) {
            progressAnimator.cancel();
        }
    }
}
