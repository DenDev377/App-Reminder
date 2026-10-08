package com.kejaksaan.reminder.api;

import android.content.Context;

import com.google.gson.GsonBuilder;
import com.kejaksaan.reminder.BuildConfig;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Singleton factory untuk Retrofit.
 *
 * Cara pakai:
 *   ApiService api = RetrofitClient.getApiService(context);
 *
 * Ganti BASE_URL sesuai lingkungan:
 *   - Emulator Android Studio : "http://10.0.2.2:8000/"
 *   - Perangkat fisik (LAN)   : "http://192.168.x.x:8000/"
 *   - Production              : "https://sipeta.kejaksaan.go.id/"
 */
public class RetrofitClient {

    // ─── Konfigurasi ─────────────────────────────────────────────────────────────
    public static final String BASE_URL = "http://192.168.1.47:8000/";

    private static Retrofit retrofitInstance = null;

    // ─── Singleton Retrofit ──────────────────────────────────────────────────────

    public static Retrofit getClient(Context context) {
        if (retrofitInstance == null) {
            synchronized (RetrofitClient.class) {
                if (retrofitInstance == null) {
                    retrofitInstance = buildRetrofit(context.getApplicationContext());
                }
            }
        }
        return retrofitInstance;
    }

    /**
     * Convenience method — langsung dapat ApiService.
     */
    public static ApiService getApiService(Context context) {
        return getClient(context).create(ApiService.class);
    }

    // ─── Internal builder ────────────────────────────────────────────────────────

    private static Retrofit buildRetrofit(Context context) {
        // Logging hanya aktif di DEBUG build
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(BuildConfig.DEBUG
                ? HttpLoggingInterceptor.Level.BODY
                : HttpLoggingInterceptor.Level.NONE);

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(new AuthInterceptor(context))   // Auth header
                .addInterceptor(logging)                         // Logging (DEBUG saja)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        return new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(
                        new GsonBuilder()
                                .setLenient()  // Toleran terhadap JSON tidak sempurna
                                .create()
                ))
                .build();
    }

    /**
     * Reset instance (berguna saat token berubah / logout).
     * Panggil ini setelah logout atau saat BASE_URL perlu diganti.
     */
    public static void reset() {
        retrofitInstance = null;
    }
}
