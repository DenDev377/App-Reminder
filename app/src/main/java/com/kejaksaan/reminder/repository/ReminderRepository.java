package com.kejaksaan.reminder.repository;

import android.content.Context;
import androidx.lifecycle.MutableLiveData;

import com.kejaksaan.reminder.api.ApiService;
import com.kejaksaan.reminder.api.RetrofitClient;
import com.kejaksaan.reminder.model.ReminderResponse;
import com.kejaksaan.reminder.util.ApiResult;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReminderRepository {

    private final ApiService apiService;

    public ReminderRepository(Context context) {
        this.apiService = RetrofitClient.getApiService(context);
    }

    /**
     * Mengambil daftar reminder dengan filter urgensi dan halaman tertentu.
     *
     * @param urgensi  Filter urgensi (null jika semua)
     * @param page     Nomor halaman
     * @param result   LiveData yang menerima hasil
     */
    public void getRemindersPaged(String urgensi, int page, MutableLiveData<ApiResult<ReminderResponse.ReminderData>> result) {
        result.postValue(ApiResult.loading());

        apiService.getReminders(urgensi, page).enqueue(new Callback<ReminderResponse>() {
            @Override
            public void onResponse(Call<ReminderResponse> call, Response<ReminderResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ReminderResponse body = response.body();
                    if (body.isSuccess() && body.getData() != null) {
                        result.postValue(ApiResult.success(body.getData()));
                    } else {
                        result.postValue(ApiResult.error("Data reminder tidak tersedia"));
                    }
                } else if (response.code() == 401) {
                    result.postValue(ApiResult.error("UNAUTHORIZED:401:Sesi telah berakhir. Silakan login kembali."));
                } else {
                    result.postValue(ApiResult.error("Gagal memuat reminder (HTTP " + response.code() + ")"));
                }
            }

            @Override
            public void onFailure(Call<ReminderResponse> call, Throwable t) {
                result.postValue(ApiResult.error("Tidak dapat terhubung ke server: " + t.getMessage()));
            }
        });
    }

    /**
     * Mengambil perkara yang berada pada Tahap 2 / Tahap II dari API Perkara
     * dan mengonversinya menjadi objek Reminder agar dapat ditampilkan di Menu Reminder SLA.
     */
    public void getPerkarasTahap2(MutableLiveData<ApiResult<java.util.List<com.kejaksaan.reminder.model.Reminder>>> result) {
        apiService.getPerkaras(1, "").enqueue(new Callback<com.kejaksaan.reminder.model.PerkaraResponse>() {
            @Override
            public void onResponse(Call<com.kejaksaan.reminder.model.PerkaraResponse> call, Response<com.kejaksaan.reminder.model.PerkaraResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    com.kejaksaan.reminder.model.PerkaraResponse body = response.body();
                    if (body.isSuccess() && body.getData() != null && body.getData().getData() != null) {
                        java.util.List<com.kejaksaan.reminder.model.Reminder> tahap2List = new java.util.ArrayList<>();
                        for (com.kejaksaan.reminder.model.Perkara p : body.getData().getData()) {
                            if (p != null && com.kejaksaan.reminder.model.Reminder.isTahap2Active(p)) {
                                com.kejaksaan.reminder.model.Reminder r = com.kejaksaan.reminder.model.Reminder.fromPerkaraTahap2(p);
                                if (r != null) {
                                    tahap2List.add(r);
                                }
                            }
                        }
                        result.postValue(ApiResult.success(tahap2List));
                        return;
                    }
                }
                result.postValue(ApiResult.success(new java.util.ArrayList<>()));
            }

            @Override
            public void onFailure(Call<com.kejaksaan.reminder.model.PerkaraResponse> call, Throwable t) {
                result.postValue(ApiResult.success(new java.util.ArrayList<>()));
            }
        });
    }
}
