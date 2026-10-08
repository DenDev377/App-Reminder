package com.kejaksaan.reminder.repository;

import android.content.Context;

import androidx.lifecycle.MutableLiveData;

import com.kejaksaan.reminder.api.ApiService;
import com.kejaksaan.reminder.api.RetrofitClient;
import com.kejaksaan.reminder.model.Perkara;
import com.kejaksaan.reminder.model.PerkaraResponse;
import com.kejaksaan.reminder.model.User;
import com.kejaksaan.reminder.model.UserResponse;
import com.kejaksaan.reminder.util.ApiResult;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * PerkaraRepository — mengelola pengambilan data kasus (perkara)
 * dan profil pengguna dari API.
 *
 * Pola pemakaian (di ViewModel):
 *   PerkaraRepository repo = new PerkaraRepository(context);
 *   repo.getPerkaras(1, liveData);
 */
public class PerkaraRepository {

    private final ApiService apiService;

    public PerkaraRepository(Context context) {
        this.apiService = RetrofitClient.getApiService(context);
    }

    // ─── Daftar Perkara ──────────────────────────────────────────────────────────

    /**
     * Mengambil daftar perkara (paginated).
     *
     * @param page    Halaman yang diminta (mulai dari 1)
     * @param result  LiveData yang akan menerima ApiResult<List<Perkara>>
     */
    public void getPerkaras(int page, MutableLiveData<ApiResult<List<Perkara>>> result) {
        result.postValue(ApiResult.loading());

        apiService.getPerkaras(page).enqueue(new Callback<PerkaraResponse>() {
            @Override
            public void onResponse(Call<PerkaraResponse> call, Response<PerkaraResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    PerkaraResponse body = response.body();

                    if (body.isSuccess() && body.getPerkaraList() != null) {
                        result.postValue(ApiResult.success(body.getPerkaraList()));
                    } else {
                        result.postValue(ApiResult.error("Data perkara kosong atau tidak tersedia"));
                    }
                } else {
                    result.postValue(ApiResult.error(
                            "Gagal memuat perkara (HTTP " + response.code() + ")"
                    ));
                }
            }

            @Override
            public void onFailure(Call<PerkaraResponse> call, Throwable t) {
                result.postValue(ApiResult.error(
                        "Tidak dapat terhubung ke server: " + t.getMessage()
                ));
            }
        });
    }

    /**
     * Overload: ambil halaman pertama langsung.
     */
    public void getPerkaras(MutableLiveData<ApiResult<List<Perkara>>> result) {
        getPerkaras(1, result);
    }

    // ─── Profil Pengguna ─────────────────────────────────────────────────────────

    /**
     * Mengambil profil user yang sedang login via GET /api/user.
     *
     * @param result  LiveData yang akan menerima ApiResult<User>
     */
    public void getUserProfile(MutableLiveData<ApiResult<User>> result) {
        result.postValue(ApiResult.loading());

        apiService.getUserProfile().enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(Call<UserResponse> call, Response<UserResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    UserResponse body = response.body();

                    if (body.isSuccess() && body.getUser() != null) {
                        result.postValue(ApiResult.success(body.getUser()));
                    } else {
                        result.postValue(ApiResult.error("Profil tidak ditemukan"));
                    }
                } else {
                    result.postValue(ApiResult.error(
                            "Gagal memuat profil (HTTP " + response.code() + ")"
                    ));
                }
            }

            @Override
            public void onFailure(Call<UserResponse> call, Throwable t) {
                result.postValue(ApiResult.error(
                        "Tidak dapat terhubung ke server: " + t.getMessage()
                ));
            }
        });
    }
}
