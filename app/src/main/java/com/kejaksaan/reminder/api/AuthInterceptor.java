package com.kejaksaan.reminder.api;

import android.content.Context;

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
 */
public class AuthInterceptor implements Interceptor {

    private final SessionManager sessionManager;

    public AuthInterceptor(Context context) {
        this.sessionManager = new SessionManager(context);
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

        return chain.proceed(requestBuilder.build());
    }
}
