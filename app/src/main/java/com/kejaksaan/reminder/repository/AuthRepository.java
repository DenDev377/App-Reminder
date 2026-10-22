package com.kejaksaan.reminder.repository;

import android.content.Context;

import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;

import com.kejaksaan.reminder.api.ApiService;
import com.kejaksaan.reminder.api.RetrofitClient;
import com.kejaksaan.reminder.model.LoginRequest;
import com.kejaksaan.reminder.model.LoginResponse;
import com.kejaksaan.reminder.model.User;
import com.kejaksaan.reminder.util.ApiResult;
import com.kejaksaan.reminder.util.FcmTokenManager;
import com.kejaksaan.reminder.util.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * AuthRepository — mengelola semua operasi otentikasi:
 * - Login dan penyimpanan token (serta registrasi token FCM perangkat)
 * - Logout dan pembersihan sesi (serta pencabutan token FCM perangkat)
 *
 * Pola:
 *   AuthRepository repo = new AuthRepository(context);
 *   repo.login(request, liveData);  // liveData di-observe dari ViewModel
 */
public class AuthRepository {

    private final Context context;
    private final ApiService apiService;
    private final SessionManager sessionManager;

    public AuthRepository(Context context) {
        this.context        = context.getApplicationContext();
        this.apiService     = RetrofitClient.getApiService(context);
        this.sessionManager = new SessionManager(context);
    }

    // ─── Login ───────────────────────────────────────────────────────────────────

    /**
     * Melakukan login ke server.
     * Jika berhasil, token dan data user disimpan di SessionManager,
     * serta token perangkat FCM didaftarkan ke backend menggunakan token Sanctum yang baru.
     *
     * @param request  Body login { email, password }
     * @param result   LiveData yang akan diupdate dengan ApiResult<User>
     */
    public void login(LoginRequest request, MutableLiveData<ApiResult<User>> result) {
        result.postValue(ApiResult.loading());

        apiService.login(request).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse body = response.body();

                    if (body.isSuccess()) {
                        // Simpan token
                        sessionManager.saveToken(body.getToken());
                        // Simpan info user
                        sessionManager.saveUserInfo(body.getUser());

                        // Daftarkan token FCM perangkat ke backend menggunakan sesi autentikasi yang baru
                        FcmTokenManager.registerCurrentTokenToServer(context, null);

                        result.postValue(ApiResult.success(body.getUser()));
                    } else {
                        // Server merespons 200 tapi success=false
                        result.postValue(ApiResult.error(
                                body.getMessage() != null
                                        ? body.getMessage()
                                        : "Login gagal"
                        ));
                    }
                } else {
                    // HTTP error (401, 422, dll.)
                    result.postValue(ApiResult.error(
                            "Login gagal (HTTP " + response.code() + ")"
                    ));
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                result.postValue(ApiResult.error(
                        "Tidak dapat terhubung ke server: " + t.getMessage()
                ));
            }
        });
    }

    // ─── Logout ──────────────────────────────────────────────────────────────────

    /**
     * Melakukan logout:
     * 1. Cabut token perangkat FCM di backend selagi otentikasi Sanctum masih berlaku (DELETE /api/device-token).
     * 2. Panggil API logout backend (/api/logout).
     * 3. Hapus sesi lokal dan reset RetrofitClient singleton.
     */
    public void logout(Context context, @Nullable Runnable onComplete) {
        // Cabut token FCM di server terlebih dahulu selagi token Sanctum masih aktif
        FcmTokenManager.revokeTokenFromServer(context, () -> {
            apiService.logout().enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    finalizeLogout(onComplete);
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    finalizeLogout(onComplete);
                }
            });
        });
    }

    public void logout(Context context) {
        logout(context, null);
    }

    private void finalizeLogout(@Nullable Runnable onComplete) {
        sessionManager.clearSession();
        RetrofitClient.reset();
        if (onComplete != null) {
            onComplete.run();
        }
    }

    // ─── Getters helper ──────────────────────────────────────────────────────────

    public boolean isLoggedIn() {
        return sessionManager.isLoggedIn();
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }
}
