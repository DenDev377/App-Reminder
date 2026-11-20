package com.kejaksaan.reminder.repository;

import android.content.Context;
import androidx.lifecycle.MutableLiveData;

import com.kejaksaan.reminder.api.ApiService;
import com.kejaksaan.reminder.api.RetrofitClient;
import com.kejaksaan.reminder.model.Perkara;
import com.kejaksaan.reminder.model.PerkaraResponse;
import com.kejaksaan.reminder.model.ReminderLogResponse;
import com.kejaksaan.reminder.util.ApiResult;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Repository untuk data Audit Trail / Log Reminders SLA.
 */
public class ReminderLogRepository {

    private final ApiService apiService;

    public ReminderLogRepository(Context context) {
        this.apiService = RetrofitClient.getApiService(context);
    }

    public ApiService getApiService() {
        return apiService;
    }

    /**
     * Mengambil daftar perkara resmi dari endpoint GET /api/perkara
     * untuk melengkapi informasi perkara induk (tahap_saat_ini, status_saat_ini, tersangka, jaksa).
     */
    public void getPerkaras(int page, String search, MutableLiveData<ApiResult<List<Perkara>>> result) {
        String query = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        apiService.getPerkaras(page, query).enqueue(new Callback<PerkaraResponse>() {
            @Override
            public void onResponse(Call<PerkaraResponse> call, Response<PerkaraResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    PerkaraResponse body = response.body();
                    if (body.isSuccess() && body.getData() != null && body.getData().getData() != null) {
                        result.postValue(ApiResult.success(body.getData().getData()));
                    } else {
                        result.postValue(ApiResult.success(new ArrayList<>()));
                    }
                } else if (response.code() == 401) {
                    result.postValue(ApiResult.error("UNAUTHORIZED:401:Sesi telah berakhir. Silakan login kembali."));
                } else {
                    result.postValue(ApiResult.error(com.kejaksaan.reminder.util.ErrorMessageHelper.parseResponseError(response, "Gagal memuat data perkara")));
                }
            }

            @Override
            public void onFailure(Call<PerkaraResponse> call, Throwable t) {
                result.postValue(ApiResult.error(com.kejaksaan.reminder.util.ErrorMessageHelper.parseThrowable(t)));
            }
        });
    }

    /**
     * Mengambil data Log Reminder SLA dengan filter opsional (status, search, tahap) dan paginasi.
     *
     * @param status   Filter status ("all", "selesai", "aktif", "overdue") atau null
     * @param search   Kata kunci pencarian nomor perkara, tersangka, jaksa, jenis reminder
     * @param tahap    Filter nama tahap perkara (misal "Koordinasi Awal") atau null
     * @param page     Nomor halaman
     * @param perPage  Jumlah item per halaman (default 20)
     * @param result   MutableLiveData untuk menampung ApiResult
     */
    public void getReminderLogs(String status, String search, String tahap, int page, int perPage,
                                MutableLiveData<ApiResult<ReminderLogResponse>> result) {
        result.postValue(ApiResult.loading());

        // Normalisasi parameter jika string kosong
        String statusParam = (status != null && !status.trim().isEmpty() && !status.equalsIgnoreCase("all"))
                ? status.trim()
                : null;
        String searchParam = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        String tahapParam = (tahap != null && !tahap.trim().isEmpty() && !tahap.equalsIgnoreCase("all"))
                ? tahap.trim()
                : null;

        apiService.getReminderLogs(statusParam, searchParam, tahapParam, page, perPage)
                .enqueue(new Callback<ReminderLogResponse>() {
                    @Override
                    public void onResponse(Call<ReminderLogResponse> call, Response<ReminderLogResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            ReminderLogResponse body = response.body();
                            if (body.isSuccess() && body.getData() != null) {
                                result.postValue(ApiResult.success(body));
                            } else {
                                String msg = body.getMessage() != null ? body.getMessage() : "Data log reminder tidak tersedia";
                                result.postValue(ApiResult.error(msg));
                            }
                        } else if (response.code() == 401) {
                            result.postValue(ApiResult.error("UNAUTHORIZED:401:Sesi telah berakhir. Silakan login kembali."));
                        } else {
                            result.postValue(ApiResult.error(com.kejaksaan.reminder.util.ErrorMessageHelper.parseResponseError(response, "Gagal memuat log reminder")));
                        }
                    }

                    @Override
                    public void onFailure(Call<ReminderLogResponse> call, Throwable t) {
                        result.postValue(ApiResult.error(com.kejaksaan.reminder.util.ErrorMessageHelper.parseThrowable(t)));
                    }
                });
    }
}
