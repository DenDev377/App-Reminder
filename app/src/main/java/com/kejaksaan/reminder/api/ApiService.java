package com.kejaksaan.reminder.api;

import com.kejaksaan.reminder.model.ChangePasswordRequest;
import com.kejaksaan.reminder.model.DeviceTokenRequest;
import com.kejaksaan.reminder.model.GenericResponse;
import com.kejaksaan.reminder.model.LoginRequest;
import com.kejaksaan.reminder.model.LoginResponse;
import com.kejaksaan.reminder.model.PerkaraResponse;
import com.kejaksaan.reminder.model.PerkaraTimelineResponse;
import com.kejaksaan.reminder.model.ReminderLogResponse;
import com.kejaksaan.reminder.model.ReminderResponse;
import com.kejaksaan.reminder.model.TimelinePaginationResponse;
import com.kejaksaan.reminder.model.UpdatePhoneRequest;
import com.kejaksaan.reminder.model.UserResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.HTTP;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Definisi semua endpoint API SIPETA.
 * Menggunakan Retrofit Call<T> (kompatibel penuh dengan Java).
 */
public interface ApiService {

    // --- Authentication ---

    @POST("api/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @POST("api/logout")
    Call<Void> logout();

    // --- Device Token (FCM Push Notifications) ---

    @POST("api/device-token")
    Call<GenericResponse> registerDeviceToken(@Body DeviceTokenRequest request);

    @HTTP(method = "DELETE", path = "api/device-token", hasBody = true)
    Call<GenericResponse> revokeDeviceToken(@Body DeviceTokenRequest request);

    // --- User Profile ---

    @GET("api/user")
    Call<UserResponse> getUserProfile();

    @POST("api/user/change-password")
    Call<GenericResponse> changePassword(@Body ChangePasswordRequest request);

    @PATCH("api/user/update-phone")
    Call<GenericResponse> updatePhone(@Body UpdatePhoneRequest request);

    // --- Perkara (Kasus) ---

    @GET("api/perkara")
    Call<PerkaraResponse> getPerkaras(@Query("page") int page);

    @GET("api/perkara")
    Call<PerkaraResponse> getPerkaras(
        @Query("page") int page,
        @Query("search") String search
    );

    @GET("api/perkara")
    Call<PerkaraResponse> getPerkaras();

    // --- Reminder (SLA & Overdue) ---
    
    @GET("api/reminders")
    Call<ReminderResponse> getReminders(@Query("urgensi") String urgensi);

    @GET("api/reminders")
    Call<ReminderResponse> getReminders(
        @Query("urgensi") String urgensi,
        @Query("page") Integer page
    );

    // --- Log Reminders (Audit Trail / Riwayat Reminder SLA) ---

    @GET("api/reminders/logs")
    Call<ReminderLogResponse> getReminderLogs(
        @Query("status") String status,
        @Query("search") String search,
        @Query("tahap") String tahap,
        @Query("page") Integer page,
        @Query("per_page") Integer perPage
    );

    // --- Timeline (Audit Trail & Riwayat Perkara) ---

    @GET("api/timelines")
    Call<TimelinePaginationResponse> getTimelines(
        @Query("page") int page,
        @Query("search") String search
    );

    @GET("api/perkara/{id}/timeline")
    Call<PerkaraTimelineResponse> getPerkaraTimeline(@Path("id") int perkaraId);
}