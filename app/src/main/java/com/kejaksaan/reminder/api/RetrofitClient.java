package com.kejaksaan.reminder.api;

import android.content.Context;

import com.google.gson.GsonBuilder;
import com.kejaksaan.reminder.BuildConfig;

import java.util.concurrent.TimeUnit;

import okhttp3.CertificatePinner;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Singleton factory untuk Retrofit.
 *
 * Cara pakai:
 * ApiService api = RetrofitClient.getApiService(context);
 *
 * Ganti BASE_URL sesuai lingkungan:
 * - Emulator Android Studio : "http://10.0.2.2:8000/"
 * - Perangkat fisik (LAN) : "http://192.168.x.x:8000/"
 * - Production : "https://sipeta.kejaksaan.go.id/"
 */
public class RetrofitClient {

    // ─── Konfigurasi ─────────────────────────────────────────────────────────────
    public static final String BASE_URL = "https://sipeta.eu.cc/";

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

        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
                .addInterceptor(new AuthInterceptor(context)) // Auth header
                .addInterceptor(logging) // Logging (DEBUG saja)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS);

        // Certificate Pinning untuk domain produksi resmi (mengamankan dari serangan MitM)
        if (!BuildConfig.DEBUG) {
            try {
                java.net.URI uri = new java.net.URI(BASE_URL);
                String host = uri.getHost();
                if (host != null && host.equalsIgnoreCase("sipeta.eu.cc")) {
                    CertificatePinner certificatePinner = new CertificatePinner.Builder()
                            .add("sipeta.eu.cc",
                                    "sha256/LNHMrrTN9edIoH7ktI1lNQ3wLgO8d5fut047RXYIbQY=", // Leaf Pin
                                    "sha256/C5+lpZ7tcVwmwQIMcRtPbsQtWLABXhQzejna0wHFr8M="  // ISRG Root X1 Backup Pin
                            )
                            .build();
                    clientBuilder.certificatePinner(certificatePinner);
                }
            } catch (Exception ignored) {
            }
        }

        OkHttpClient okHttpClient = clientBuilder.build();

        return new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(
                        new GsonBuilder()
                                .setLenient() // Toleran terhadap JSON tidak sempurna
                                .create()))
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
