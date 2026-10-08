package com.kejaksaan.reminder.repository;

import android.content.Context;

import androidx.lifecycle.MutableLiveData;

import com.kejaksaan.reminder.api.ApiService;
import com.kejaksaan.reminder.api.RetrofitClient;
import com.kejaksaan.reminder.model.LoginRequest;
import com.kejaksaan.reminder.model.LoginResponse;
import com.kejaksaan.reminder.model.User;
import com.kejaksaan.reminder.util.ApiResult;
import com.kejaksaan.reminder.util.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * AuthRepository — mengelola semua operasi otentikasi:
 * - Login dan penyimpanan token
 * - Logout dan pembersihan sesi
 *
 * Pola:
 *   AuthRepository repo = new AuthRepository(context);
 *   repo.login(request, liveData);  // liveData di-observe dari ViewModel
 */
public class AuthRepository {

    private final ApiService apiService;
    private final SessionManager sessionManager;

    public AuthRepository(Context context) {
        this.apiService     = RetrofitClient.getApiService(context);
        this.sessionManager = new SessionManager(context);
    }

    // ─── Login ───────────────────────────────────────────────────────────────────

    /**
     * Melakukan login ke server.
     * Jika berhasil, token dan data user disimpan di SessionManager.
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
     * Melakukan logout: hapus sesi lokal dan panggil API logout.
     * Retrofit singleton di-reset agar token lama tidak tersisa di cache OkHttp.
     */
    public void logout(Context context) {
        // Hapus lokal dulu
        sessionManager.clearSession();

        // Hit endpoint logout (best-effort, tidak perlu await)
        apiService.logout().enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) { /* no-op */ }

            @Override
            public void onFailure(Call<Void> call, Throwable t) { /* no-op */ }
        });

        // Reset singleton agar interceptor membaca token kosong di sesi berikutnya
        RetrofitClient.reset();
    }

    // ─── Getters helper ──────────────────────────────────────────────────────────

    public boolean isLoggedIn() {
        return sessionManager.isLoggedIn();
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }
}
