package com.kejaksaan.reminder.service;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.kejaksaan.reminder.DetailPerkaraActivity;
import com.kejaksaan.reminder.R;
import com.kejaksaan.reminder.util.FcmTokenManager;
import com.kejaksaan.reminder.util.NotificationHelper;
import com.kejaksaan.reminder.util.SessionManager;

import java.util.Map;

/**
 * Service penerima pesan dan rotasi token Firebase Cloud Messaging (FCM).
 * Menangani:
 * 1. Rotasi token perangkat (onNewToken) dengan sinkronisasi ke backend jika user terautentikasi.
 * 2. Penerimaan payload notifikasi & data saat aplikasi berada di latar depan (foreground)
 *    maupun data-only message (onMessageReceived).
 * 3. Menampilkan notifikasi pada Notification Channel 'sipeta_reminder_channel' dan navigasi
 *    langsung ke DetailPerkaraActivity saat notifikasi ditekan.
 */
public class SipetaFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "SipetaFcmService";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);

        SessionManager sessionManager = new SessionManager(this);
        sessionManager.saveFcmToken(token);
        sessionManager.setFcmTokenRegistered(false);

        Log.d(TAG, "Token FCM baru diterima: " + FcmTokenManager.maskToken(token));

        // Jika user sedang aktif login, segera perbarui token ke backend Laravel
        if (sessionManager.isLoggedIn()) {
            FcmTokenManager.registerCurrentTokenToServer(this, null);
        } else {
            Log.d(TAG, "User belum login. Pendaftaran token ditunda hingga autentikasi siap.");
        }
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Log.d(TAG, "Pesan FCM diterima dari sender: " + remoteMessage.getFrom());

        // Pastikan Notification Channel sudah terdaftar
        NotificationHelper.createNotificationChannel(this);

        // Ekstrak title & body baik dari notification payload maupun data payload
        String title = "SIPETA Kejaksaan";
        String body = "Pembaruan notifikasi tahapan perkara.";

        if (remoteMessage.getNotification() != null) {
            if (remoteMessage.getNotification().getTitle() != null) {
                title = remoteMessage.getNotification().getTitle();
            }
            if (remoteMessage.getNotification().getBody() != null) {
                body = remoteMessage.getNotification().getBody();
            }
        }

        Map<String, String> data = remoteMessage.getData();
        if (data != null && !data.isEmpty()) {
            if (data.containsKey("title") && data.get("title") != null) {
                title = data.get("title");
            }
            if (data.containsKey("body") && data.get("body") != null) {
                body = data.get("body");
            }
        }

        showNotification(title, body, data);
    }

    /**
     * Membangun dan menampilkan notifikasi sistem terpusat melalui NotificationHelper
     * dengan nada dering kustom dan tombol interaktif (Buka Perkara & Tunda 2 Jam).
     */
    private void showNotification(String title, String body, Map<String, String> data) {
        int perkaraId = -1;
        String nomorPerkara = null;

        if (data != null && !data.isEmpty()) {
            String perkaraIdStr = data.get("perkara_id");
            if (perkaraIdStr != null) {
                try {
                    perkaraId = Integer.parseInt(perkaraIdStr.trim());
                } catch (NumberFormatException ignored) {}
            }

            nomorPerkara = data.get("nomor_perkara");
        }

        int notificationId = (perkaraId > 0) ? perkaraId : (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
        NotificationHelper.showReminderNotification(this, notificationId, perkaraId, nomorPerkara, title, body);
    }
}
