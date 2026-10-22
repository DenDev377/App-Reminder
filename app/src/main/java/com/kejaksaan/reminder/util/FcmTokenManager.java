package com.kejaksaan.reminder.util;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.firebase.messaging.FirebaseMessaging;
import com.kejaksaan.reminder.api.ApiService;
import com.kejaksaan.reminder.api.RetrofitClient;
import com.kejaksaan.reminder.model.DeviceTokenRequest;
import com.kejaksaan.reminder.model.GenericResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Manajer token perangkat FCM untuk sinkronisasi dengan backend Laravel:
 * - Registrasi token saat user login (POST /api/device-token)
 * - Pencabutan token saat user logout (DELETE /api/device-token)
 * - Rotasi token aman (onNewToken)
 * - Menjaga privasi: tidak mencatat token utuh ke log publik
 */
public class FcmTokenManager {

    private static final String TAG = "FcmTokenManager";

    /**
     * Daftarkan token FCM perangkat saat ini ke backend Laravel.
     * Hanya dieksekusi jika pengguna telah memiliki sesi login aktif (Sanctum Bearer token).
     * Jika pengguna belum login, token hanya disimpan lokal di SessionManager dan akan didaftarkan
     * segera setelah proses login selesai.
     */
    public static void registerCurrentTokenToServer(Context context, @Nullable Runnable onComplete) {
        SessionManager sessionManager = new SessionManager(context);

        if (!sessionManager.isLoggedIn()) {
            Log.d(TAG, "Sesi login belum aktif. Pendaftaran token ditunda hingga user berhasil login.");
            if (onComplete != null) onComplete.run();
            return;
        }

        String existingToken = sessionManager.fetchFcmToken();
        if (existingToken != null && !existingToken.trim().isEmpty()) {
            sendTokenToBackend(context, sessionManager, existingToken.trim(), onComplete);
        } else {
            // Ambil token dari FirebaseMessaging jika belum tersimpan di prefs
            FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    String token = task.getResult();
                    sessionManager.saveFcmToken(token);
                    sendTokenToBackend(context, sessionManager, token, onComplete);
                } else {
                    Log.w(TAG, "Gagal mengambil token FCM dari FirebaseMessaging: "
                            + (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                    if (onComplete != null) onComplete.run();
                }
            });
        }
    }

    private static void sendTokenToBackend(Context context,
                                           SessionManager sessionManager,
                                           String token,
                                           @Nullable Runnable onComplete) {
        String deviceId = sessionManager.getOrCreateDeviceId();
        ApiService apiService = RetrofitClient.getApiService(context);
        DeviceTokenRequest request = new DeviceTokenRequest(token, deviceId, "android");

        apiService.registerDeviceToken(request).enqueue(new Callback<GenericResponse>() {
            @Override
            public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    sessionManager.setFcmTokenRegistered(true);
                    Log.i(TAG, "Token perangkat FCM berhasil didaftarkan ke server (ID: "
                            + maskToken(token) + ")");
                } else {
                    sessionManager.setFcmTokenRegistered(false);
                    Log.w(TAG, "Server merespons gagal saat registrasi token (HTTP " + response.code() + ")");
                }
                if (onComplete != null) onComplete.run();
            }

            @Override
            public void onFailure(Call<GenericResponse> call, Throwable t) {
                sessionManager.setFcmTokenRegistered(false);
                Log.w(TAG, "Gagal koneksi saat mendaftarkan token FCM ke server: " + t.getMessage());
                if (onComplete != null) onComplete.run();
            }
        });
    }

    /**
     * Cabut token perangkat FCM dari backend Laravel sebelum logout.
     * Menggunakan sesi otentikasi Sanctum yang masih berlaku.
     * Menjamin onComplete selalu dipanggil meskipun terjadi error jaringan.
     */
    public static void revokeTokenFromServer(Context context, Runnable onComplete) {
        SessionManager sessionManager = new SessionManager(context);

        if (!sessionManager.isLoggedIn()) {
            if (onComplete != null) onComplete.run();
            return;
        }

        String token = sessionManager.fetchFcmToken();
        String deviceId = sessionManager.getOrCreateDeviceId();

        if ((token == null || token.trim().isEmpty()) && (deviceId == null || deviceId.trim().isEmpty())) {
            if (onComplete != null) onComplete.run();
            return;
        }

        ApiService apiService = RetrofitClient.getApiService(context);
        DeviceTokenRequest request = new DeviceTokenRequest(token, deviceId, "android");

        apiService.revokeDeviceToken(request).enqueue(new Callback<GenericResponse>() {
            @Override
            public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                sessionManager.setFcmTokenRegistered(false);
                Log.i(TAG, "Token perangkat FCM berhasil dicabut dari server.");
                if (onComplete != null) onComplete.run();
            }

            @Override
            public void onFailure(Call<GenericResponse> call, Throwable t) {
                // Jangan batalkan logout lokal jika server offline / network error
                sessionManager.setFcmTokenRegistered(false);
                Log.w(TAG, "Jaringan gagal saat mencabut token FCM di server, lanjutkan logout lokal.");
                if (onComplete != null) onComplete.run();
            }
        });
    }

    /**
     * Sensor token untuk keperluan log agar tidak membocorkan data rahasia.
     */
    public static String maskToken(String token) {
        if (token == null) return "null";
        int len = token.length();
        if (len <= 10) return "***";
        return token.substring(0, 6) + "..." + token.substring(len - 4);
    }
}
