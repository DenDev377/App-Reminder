package com.kejaksaan.reminder.repository;

import android.content.Context;
import androidx.lifecycle.MutableLiveData;

import com.kejaksaan.reminder.api.ApiService;
import com.kejaksaan.reminder.api.RetrofitClient;
import com.kejaksaan.reminder.model.NotificationResponse;
import com.kejaksaan.reminder.model.PushNotificationItem;
import com.kejaksaan.reminder.model.Reminder;
import com.kejaksaan.reminder.model.ReminderResponse;
import com.kejaksaan.reminder.util.ApiResult;

import java.util.Collections;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Repository untuk memuat data Notifikasi Push & Pengingat SLA Harian.
 */
public class NotificationRepository {

    private final ApiService apiService;

    public NotificationRepository(Context context) {
        this.apiService = RetrofitClient.getApiService(context);
    }

    /**
     * Memuat riwayat push notification dari server (GET /api/notifications).
     */
    public void getNotifications(int page, int perPage, MutableLiveData<ApiResult<List<PushNotificationItem>>> result) {
        result.postValue(ApiResult.loading());

        apiService.getNotifications(page, perPage).enqueue(new Callback<NotificationResponse>() {
            @Override
            public void onResponse(Call<NotificationResponse> call, Response<NotificationResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    NotificationResponse body = response.body();
                    if (body.isSuccess() && body.getData() != null && body.getData().getData() != null) {
                        result.postValue(ApiResult.success(body.getData().getData()));
                    } else {
                        result.postValue(ApiResult.success(Collections.emptyList()));
                    }
                } else {
                    result.postValue(ApiResult.error("Gagal memuat notifikasi (HTTP " + response.code() + ")"));
                }
            }

            @Override
            public void onFailure(Call<NotificationResponse> call, Throwable t) {
                result.postValue(ApiResult.error("Tidak dapat terhubung ke server: " + t.getMessage()));
            }
        });
    }

    /**
     * Memuat notifikasi dengan metadata paginasi (current_page, last_page, total).
     */
    public void getNotificationsPaged(int page, int perPage, MutableLiveData<ApiResult<NotificationResponse.NotificationPagination>> result) {
        result.postValue(ApiResult.loading());

        apiService.getNotifications(page, perPage).enqueue(new Callback<NotificationResponse>() {
            @Override
            public void onResponse(Call<NotificationResponse> call, Response<NotificationResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    NotificationResponse body = response.body();
                    if (body.isSuccess() && body.getData() != null) {
                        result.postValue(ApiResult.success(body.getData()));
                    } else {
                        result.postValue(ApiResult.error("Data notifikasi kosong"));
                    }
                } else {
                    result.postValue(ApiResult.error("Gagal memuat notifikasi (HTTP " + response.code() + ")"));
                }
            }

            @Override
            public void onFailure(Call<NotificationResponse> call, Throwable t) {
                result.postValue(ApiResult.error("Tidak dapat terhubung ke server: " + t.getMessage()));
            }
        });
    }

    /**
     * Memuat reminder perkara mendesak yang memerlukan tindakan hari ini (GET /api/reminders?urgensi=semua_mendesak).
     */
    public void getUrgentReminders(MutableLiveData<ApiResult<List<Reminder>>> result) {
        result.postValue(ApiResult.loading());

        apiService.getReminders("semua_mendesak", 1).enqueue(new Callback<ReminderResponse>() {
            @Override
            public void onResponse(Call<ReminderResponse> call, Response<ReminderResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ReminderResponse body = response.body();
                    if (body.isSuccess() && body.getData() != null && body.getData().getData() != null) {
                        result.postValue(ApiResult.success(body.getData().getData()));
                    } else {
                        result.postValue(ApiResult.success(Collections.emptyList()));
                    }
                } else {
                    result.postValue(ApiResult.error("Gagal memuat reminder hari ini (HTTP " + response.code() + ")"));
                }
            }

            @Override
            public void onFailure(Call<ReminderResponse> call, Throwable t) {
                result.postValue(ApiResult.error("Tidak dapat terhubung ke server: " + t.getMessage()));
            }
        });
    }
}
