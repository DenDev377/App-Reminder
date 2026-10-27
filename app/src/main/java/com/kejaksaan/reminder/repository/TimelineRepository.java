package com.kejaksaan.reminder.repository;

import android.content.Context;
import androidx.lifecycle.MutableLiveData;

import com.kejaksaan.reminder.api.ApiService;
import com.kejaksaan.reminder.api.RetrofitClient;
import com.kejaksaan.reminder.model.AvailableActionsResponse;
import com.kejaksaan.reminder.model.GenericResponse;
import com.kejaksaan.reminder.model.PerkaraResponse;
import com.kejaksaan.reminder.model.PerkaraTimelineResponse;
import com.kejaksaan.reminder.model.ProcessActionRequest;
import com.kejaksaan.reminder.model.TimelinePagination;
import com.kejaksaan.reminder.model.TimelinePaginationResponse;
import com.kejaksaan.reminder.util.ApiResult;

import org.json.JSONObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TimelineRepository {

    private final ApiService apiService;

    public TimelineRepository(Context context) {
        this.apiService = RetrofitClient.getApiService(context);
    }

    /**
     * Mengambil daftar perkara unik untuk Menu Timeline (1 card per perkara, paginated & search).
     */
    public void getPerkarasForTimeline(int page, String search, MutableLiveData<ApiResult<PerkaraResponse.PerkaraPage>> result) {
        result.postValue(ApiResult.loading());

        apiService.getPerkaras(page, search).enqueue(new Callback<PerkaraResponse>() {
            @Override
            public void onResponse(Call<PerkaraResponse> call, Response<PerkaraResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    PerkaraResponse body = response.body();
                    if (body.isSuccess() && body.getData() != null) {
                        result.postValue(ApiResult.success(body.getData()));
                    } else {
                        result.postValue(ApiResult.error("Data perkara tidak tersedia"));
                    }
                } else if (response.code() == 401) {
                    result.postValue(ApiResult.error("UNAUTHORIZED:401:Sesi telah berakhir. Silakan login kembali."));
                } else {
                    result.postValue(ApiResult.error("Gagal memuat perkara (HTTP " + response.code() + ")"));
                }
            }

            @Override
            public void onFailure(Call<PerkaraResponse> call, Throwable t) {
                result.postValue(ApiResult.error("Tidak dapat terhubung ke server: " + t.getMessage()));
            }
        });
    }

    /**
     * Mengambil riwayat timeline lengkap untuk satu perkara tertentu.
     */
    public void getPerkaraTimeline(int perkaraId, MutableLiveData<ApiResult<PerkaraTimelineResponse>> result) {
        result.postValue(ApiResult.loading());

        apiService.getPerkaraTimeline(perkaraId).enqueue(new Callback<PerkaraTimelineResponse>() {
            @Override
            public void onResponse(Call<PerkaraTimelineResponse> call, Response<PerkaraTimelineResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    PerkaraTimelineResponse body = response.body();
                    if (body.isSuccess() && body.getData() != null) {
                        result.postValue(ApiResult.success(body));
                    } else {
                        result.postValue(ApiResult.error("Riwayat timeline perkara tidak tersedia"));
                    }
                } else if (response.code() == 401) {
                    result.postValue(ApiResult.error("UNAUTHORIZED:401:Sesi telah berakhir. Silakan login kembali."));
                } else {
                    result.postValue(ApiResult.error("Gagal memuat riwayat perkara (HTTP " + response.code() + ")"));
                }
            }

            @Override
            public void onFailure(Call<PerkaraTimelineResponse> call, Throwable t) {
                result.postValue(ApiResult.error("Tidak dapat terhubung ke server: " + t.getMessage()));
            }
        });
    }

    /**
     * Mengambil daftar global audit feed jika diperlukan.
     */
    public void getTimelinesPaged(int page, String search, MutableLiveData<ApiResult<TimelinePagination>> result) {
        result.postValue(ApiResult.loading());

        apiService.getTimelines(page, search).enqueue(new Callback<TimelinePaginationResponse>() {
            @Override
            public void onResponse(Call<TimelinePaginationResponse> call, Response<TimelinePaginationResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    TimelinePaginationResponse body = response.body();
                    if (body.isSuccess() && body.getData() != null) {
                        result.postValue(ApiResult.success(body.getData()));
                    } else {
                        result.postValue(ApiResult.error("Data timeline tidak tersedia"));
                    }
                } else if (response.code() == 401) {
                    result.postValue(ApiResult.error("UNAUTHORIZED:401:Sesi telah berakhir. Silakan login kembali."));
                } else {
                    result.postValue(ApiResult.error("Gagal memuat timeline (HTTP " + response.code() + ")"));
                }
            }

            @Override
            public void onFailure(Call<TimelinePaginationResponse> call, Throwable t) {
                result.postValue(ApiResult.error("Tidak dapat terhubung ke server: " + t.getMessage()));
            }
        });
    }

    // ─── Workflow Actions (Trigger Button SIPETA) ────────────────────────────────

    /**
     * Mengambil daftar aksi alur perkara yang dapat dieksekusi (GET /api/perkara/{id}/available-actions).
     */
    public void getAvailableActions(int perkaraId, MutableLiveData<ApiResult<AvailableActionsResponse>> result) {
        result.postValue(ApiResult.loading());

        apiService.getAvailableActions(perkaraId).enqueue(new Callback<AvailableActionsResponse>() {
            @Override
            public void onResponse(Call<AvailableActionsResponse> call, Response<AvailableActionsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AvailableActionsResponse body = response.body();
                    if (body.isSuccess()) {
                        result.postValue(ApiResult.success(body));
                    } else {
                        String msg = body.getMessage() != null ? body.getMessage() : "Gagal memuat aksi perkara";
                        result.postValue(ApiResult.error(msg));
                    }
                } else if (response.code() == 401) {
                    result.postValue(ApiResult.error("UNAUTHORIZED:401:Sesi telah berakhir. Silakan login kembali."));
                } else if (response.code() == 403) {
                    String msg = parseErrorMessage(response, "Akses ditolak: Anda tidak memiliki wewenang pada perkara ini.");
                    result.postValue(ApiResult.error("FORBIDDEN:403:" + msg));
                } else {
                    String msg = parseErrorMessage(response, "Gagal memuat aksi alur perkara (HTTP " + response.code() + ")");
                    result.postValue(ApiResult.error(msg));
                }
            }

            @Override
            public void onFailure(Call<AvailableActionsResponse> call, Throwable t) {
                result.postValue(ApiResult.error("Tidak dapat terhubung ke server: " + t.getMessage()));
            }
        });
    }

    /**
     * Mengeksekusi aksi workflow alur perkara (POST /api/perkara/{id}/actions).
     */
    public void processAction(int perkaraId, ProcessActionRequest request, MutableLiveData<ApiResult<GenericResponse>> result) {
        result.postValue(ApiResult.loading());

        apiService.processAction(perkaraId, request).enqueue(new Callback<GenericResponse>() {
            @Override
            public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    GenericResponse body = response.body();
                    if (body.isSuccess()) {
                        result.postValue(ApiResult.success(body));
                    } else {
                        String msg = body.getMessage() != null ? body.getMessage() : "Gagal memproses aksi perkara";
                        result.postValue(ApiResult.error(msg));
                    }
                } else if (response.code() == 401) {
                    result.postValue(ApiResult.error("UNAUTHORIZED:401:Sesi telah berakhir. Silakan login kembali."));
                } else if (response.code() == 403) {
                    String msg = parseErrorMessage(response, "Akses ditolak: Anda tidak ditugaskan sebagai jaksa pada perkara ini.");
                    result.postValue(ApiResult.error("FORBIDDEN:403:" + msg));
                } else if (response.code() == 409) {
                    String msg = parseErrorMessage(response, "Tahap atau status perkara telah berubah. Silakan segarkan data perkara Anda.");
                    result.postValue(ApiResult.error("CONFLICT:409:" + msg));
                } else if (response.code() == 422) {
                    String msg = parseErrorMessage(response, "Data yang dikirimkan tidak valid.");
                    result.postValue(ApiResult.error("VALIDATION:422:" + msg));
                } else {
                    String msg = parseErrorMessage(response, "Gagal memproses aksi perkara (HTTP " + response.code() + ")");
                    result.postValue(ApiResult.error(msg));
                }
            }

            @Override
            public void onFailure(Call<GenericResponse> call, Throwable t) {
                result.postValue(ApiResult.error("Tidak dapat terhubung ke server: " + t.getMessage()));
            }
        });
    }

    private String parseErrorMessage(Response<?> response, String defaultMessage) {
        try {
            if (response.errorBody() != null) {
                String errorJson = response.errorBody().string();
                JSONObject obj = new JSONObject(errorJson);
                if (obj.has("message")) {
                    return obj.getString("message");
                }
            }
        } catch (Exception ignored) {
        }
        return defaultMessage;
    }
}