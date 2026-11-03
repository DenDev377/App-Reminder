package com.kejaksaan.reminder.repository;

import android.content.Context;

import androidx.lifecycle.MutableLiveData;

import com.kejaksaan.reminder.api.ApiService;
import com.kejaksaan.reminder.api.RetrofitClient;
import com.kejaksaan.reminder.model.BeritaResponse;
import com.kejaksaan.reminder.util.ApiResult;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * BeritaRepository — Mengelola pemanggilan API untuk data Berita.
 */
public class BeritaRepository {

    private final ApiService apiService;

    public BeritaRepository(Context context) {
        this.apiService = RetrofitClient.getApiService(context);
    }

    /**
     * Mengambil daftar berita dengan filter paginasi dan pencarian.
     *
     * @param page    Nomor halaman (mulai dari 1)
     * @param perPage Jumlah item per halaman (default 10)
     * @param search  Kata kunci pencarian (opsional)
     * @param result  LiveData untuk ApiResult<BeritaResponse>
     */
    public void getBeritas(int page, int perPage, String search, MutableLiveData<ApiResult<BeritaResponse>> result) {
        result.postValue(ApiResult.loading());

        String query = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        Integer pageParam = page > 0 ? page : null;
        Integer perPageParam = perPage > 0 ? perPage : null;

        apiService.getBeritas(pageParam, perPageParam, query).enqueue(new Callback<BeritaResponse>() {
            @Override
            public void onResponse(Call<BeritaResponse> call, Response<BeritaResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    BeritaResponse body = response.body();
                    if (body.isSuccess() && body.getData() != null) {
                        result.postValue(ApiResult.success(body));
                    } else {
                        result.postValue(ApiResult.error("Data berita tidak tersedia"));
                    }
                } else if (response.code() == 401) {
                    result.postValue(ApiResult.error("UNAUTHORIZED:401:Sesi telah berakhir. Silakan login kembali."));
                } else {
                    result.postValue(ApiResult.error("Gagal memuat berita (HTTP " + response.code() + ")"));
                }
            }

            @Override
            public void onFailure(Call<BeritaResponse> call, Throwable t) {
                result.postValue(ApiResult.error("Tidak dapat terhubung ke server: " + t.getMessage()));
            }
        });
    }
}

