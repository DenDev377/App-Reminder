package com.kejaksaan.reminder.api;

import com.kejaksaan.reminder.model.ChangePasswordRequest;
import com.kejaksaan.reminder.model.GenericResponse;
import com.kejaksaan.reminder.model.LoginRequest;
import com.kejaksaan.reminder.model.LoginResponse;
import com.kejaksaan.reminder.model.PerkaraResponse;
import com.kejaksaan.reminder.model.UpdatePhoneRequest;
import com.kejaksaan.reminder.model.UserResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Query;

/**
 * Definisi semua endpoint API SIPETA.
 * Menggunakan Retrofit Call<T> (kompatibel penuh dengan Java).
 */
public interface ApiService {

    // ─── Authentication ──────────────────────────────────────────────────────────

    /**
     * POST /api/login
     * Body: { "email": "...", "password": "..." }
     * Response: { "success": true, "message": "...", "data": { "user": {...}, "token": "..." } }
     */
    @POST("api/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    /**
     * POST /api/logout
     * Header Authorization dipasang otomatis oleh AuthInterceptor.
     */
    @POST("api/logout")
    Call<Void> logout();

    // ─── User Profile ────────────────────────────────────────────────────────────

    /**
     * GET /api/user
     * Response: { "success": true, "data": { "user": {...} } }
     */
    @GET("api/user")
    Call<UserResponse> getUserProfile();

    /**
     * POST /api/user/change-password
     * Body: { "current_password": "...", "new_password": "...", "new_password_confirmation": "..." }
     * Response: { "success": true, "message": "Kata sandi berhasil diubah" }
     *
     * ⚠️ Sesuaikan path jika endpoint backend berbeda.
     */
    @POST("api/user/change-password")
    Call<GenericResponse> changePassword(@Body ChangePasswordRequest request);

    /**
     * PATCH /api/user/update-phone
     * Body: { "nomor_hp": "08123..." }
     * Response: { "success": true, "message": "Nomor HP berhasil diperbarui" }
     *
     * ⚠️ Sesuaikan path jika endpoint backend berbeda.
     */
    @PATCH("api/user/update-phone")
    Call<GenericResponse> updatePhone(@Body UpdatePhoneRequest request);

    // ─── Perkara (Kasus) ─────────────────────────────────────────────────────────

    /**
     * GET /api/perkara
     * Response: { "success": true, "data": { "current_page": 1, "data": [...] } }
     *
     * @param page  Halaman yang diminta (mulai dari 1), default 1 jika tidak diisi.
     */
    @GET("api/perkara")
    Call<PerkaraResponse> getPerkaras(@Query("page") int page);

    /**
     * Overload tanpa parameter page — memanggil halaman pertama.
     */
    @GET("api/perkara")
    Call<PerkaraResponse> getPerkaras();

    // ─── Reminder (SLA & Overdue) ────────────────────────────────────────────────
    
    /**
     * GET /api/reminders
     * Parameter: "urgensi" (misal: "semua_mendesak")
     */
    @GET("api/reminders")
    Call<com.kejaksaan.reminder.model.ReminderResponse> getReminders(@Query("urgensi") String urgensi);
}
