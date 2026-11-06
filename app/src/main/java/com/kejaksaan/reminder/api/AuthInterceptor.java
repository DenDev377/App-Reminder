package com.kejaksaan.reminder.api;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.kejaksaan.reminder.MainActivity;
import com.kejaksaan.reminder.util.SessionManager;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * OkHttp Interceptor yang:
 * 1. Menambahkan header "Accept: application/json" pada setiap request.
 * 2. Menambahkan header "Authorization: Bearer {token}" jika user sudah login
 *    DAN path bukan "/api/login".
 * 3. Mencegat respons HTTP 401 Unauthorized secara global jika sesi telah kedaluwarsa:
 *    - Menghapus sesi lokal (SessionManager.clearSession())
 *    - Mereset singleton Retrofit (RetrofitClient.reset())
 *    - Menampilkan notifikasi dan mengarahkan user kembali ke halaman Login (MainActivity) secara otomatis.
 */
public class AuthInterceptor implements Interceptor {

    private final Context context;
    private final SessionManager sessionManager;

    public AuthInterceptor(Context context) {
        this.context = context.getApplicationContext();
        this.sessionManager = new SessionManager(this.context);
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();
        Request.Builder requestBuilder = originalRequest.newBuilder();

        // Selalu set Accept: application/json
        requestBuilder.header("Accept", "application/json");

        // Jangan lampirkan token untuk endpoint login
        String path = originalRequest.url().encodedPath();
        boolean isLoginPath = path.contains("api/login");

        if (!isLoginPath) {
            String token = sessionManager.fetchToken();
            if (token != null && !token.isEmpty()) {
                requestBuilder.header("Authorization", "Bearer " + token);
            }
        }

        Response response = chain.proceed(requestBuilder.build());

        // Intercept global HTTP 401 Unauthorized (sesi habis / token tidak valid)
        if (response.code() == 401 && !isLoginPath) {
            synchronized (AuthInterceptor.class) {
                if (sessionManager.isLoggedIn()) {
                    sessionManager.clearSession();
                    RetrofitClient.reset();

                    new Handler(Looper.getMainLooper()).post(() -> {
                        Toast.makeText(context, "Sesi telah berakhir. Silakan login kembali.", Toast.LENGTH_LONG).show();
                        Intent intent = new Intent(context, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        context.startActivity(intent);
                    });
                }
            }
        }

        return response;
    }
}
