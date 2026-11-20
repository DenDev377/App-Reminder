package com.kejaksaan.reminder.util;

import android.util.Log;

import org.json.JSONObject;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import javax.net.ssl.SSLException;

import retrofit2.Response;

/**
 * Utilitas penerjemah pesan error teknis ke Bahasa Indonesia yang ramah pengguna.
 * Menangani error jaringan, kebijakan keamanan Android (Cleartext/HTTPS), timeout, dan kode HTTP.
 */
public class ErrorMessageHelper {

    private static final String TAG = "ErrorMessageHelper";

    /**
     * Menerjemahkan Throwable / Exception jaringan dari Retrofit onFailure menjadi
     * kalimat Bahasa Indonesia yang mudah dipahami oleh pengguna / Jaksa.
     */
    public static String parseThrowable(Throwable t) {
        if (t == null) {
            return "Terjadi kendala saat menghubungkan ke server. Silakan coba lagi.";
        }

        String rawMsg = t.getMessage() != null ? t.getMessage() : "";
        String lowerMsg = rawMsg.toLowerCase();

        // 1. Masalah Kebijakan Keamanan Jaringan Android (Cleartext HTTP / Security Policy)
        if (lowerMsg.contains("cleartext") || lowerMsg.contains("security policy")
                || lowerMsg.contains("not permitted by network security")) {
            return "Koneksi HTTP tidak diizinkan oleh kebijakan keamanan sistem. Pastikan menggunakan protokol aman HTTPS atau IP jaringan pengujian telah didaftarkan.";
        }

        // 2. Waktu Koneksi Habis (Timeout)
        if (t instanceof SocketTimeoutException || lowerMsg.contains("timeout") || lowerMsg.contains("timed out")) {
            return "Waktu koneksi habis (Timeout). Server membutuhkan waktu terlalu lama untuk merespons. Periksa kembali kestabilan koneksi internet Anda.";
        }

        // 3. Gagal Terhubung ke Server / Port Tertutup / Server Mati
        if (t instanceof ConnectException || lowerMsg.contains("connection refused")
                || lowerMsg.contains("failed to connect") || lowerMsg.contains("connectexception")) {
            return "Tidak dapat terhubung ke server. Pastikan server backend sedang aktif dan alamat/port yang dimasukkan sudah benar.";
        }

        // 4. Domain / IP Host Tidak Ditemukan (Unknown Host / DNS Gagal)
        if (t instanceof UnknownHostException || lowerMsg.contains("unable to resolve host")
                || lowerMsg.contains("no address associated with hostname")) {
            return "Alamat server tidak dapat ditemukan. Periksa koneksi internet Anda atau pastikan domain/IP server sudah tepat.";
        }

        // 5. Sertifikat SSL / HTTPS / Certificate Pinning
        if (t instanceof SSLException || lowerMsg.contains("ssl") || lowerMsg.contains("certificate")
                || lowerMsg.contains("handshake") || lowerMsg.contains("certpath")) {
            return "Koneksi ditolak demi keamanan (Sertifikat keamanan SSL server tidak valid atau tidak cocok dengan standar keamanan Kejaksaan).";
        }

        // 6. Jaringan Terputus / Tidak Ada Rute
        if (lowerMsg.contains("network is unreachable") || lowerMsg.contains("no route to host")
                || lowerMsg.contains("enonetwork")) {
            return "Jaringan internet tidak dapat dijangkau. Pastikan perangkat Anda terhubung ke Wi-Fi atau data seluler aktif.";
        }

        // 7. Koneksi Terputus di Tengah Jalan (Broken Pipe / Connection Reset)
        if (lowerMsg.contains("connection reset") || lowerMsg.contains("broken pipe")
                || lowerMsg.contains("software caused connection abort")) {
            return "Koneksi ke server terputus di tengah proses. Silakan segarkan halaman dan coba lagi.";
        }

        // 8. Gangguan Format JSON / Data Rusak
        if (lowerMsg.contains("json") || lowerMsg.contains("malformed") || lowerMsg.contains("syntax")) {
            return "Format data respon dari server tidak sesuai atau terjadi kendala pemrosesan data di server.";
        }

        // Pesan default yang ramah
        return "Tidak dapat terhubung ke server. Periksa koneksi internet Anda atau coba beberapa saat lagi.";
    }

    /**
     * Menerjemahkan respons error dari server (HTTP status code dan isi errorBody JSON).
     */
    public static String parseResponseError(Response<?> response, String defaultMessage) {
        if (response == null) {
            return defaultMessage != null ? defaultMessage : "Terjadi kendala pada sistem server.";
        }

        int code = response.code();
        String extractedMsg = null;

        // Coba ekstrak pesan error dari JSON response body jika ada
        try {
            if (response.errorBody() != null) {
                String errorJson = response.errorBody().string();
                JSONObject obj = new JSONObject(errorJson);
                if (obj.has("message")) {
                    extractedMsg = obj.getString("message");
                } else if (obj.has("error")) {
                    extractedMsg = obj.getString("error");
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "Error parsing error body: " + e.getMessage());
        }

        // Terjemahkan pesan bahasa Inggris umum dari backend Laravel / API
        if (extractedMsg != null && !extractedMsg.trim().isEmpty()) {
            String lower = extractedMsg.trim().toLowerCase();
            if (lower.equals("unauthenticated.") || lower.contains("unauthenticated")) {
                return "Sesi Anda telah berakhir. Silakan login kembali.";
            }
            if (lower.contains("csrf token mismatch")) {
                return "Sesi formulir keamanan kedaluwarsa. Silakan muat ulang halaman.";
            }
            if (lower.contains("the given data was invalid") || lower.contains("validation error")) {
                return "Data yang dikirimkan tidak valid atau belum lengkap.";
            }
            if (lower.contains("too many requests") || lower.contains("too many attempts")) {
                return "Terlalu banyak permintaan dalam waktu singkat. Silakan tunggu beberapa saat.";
            }
            if (lower.contains("not found")) {
                return "Data atau layanan yang diminta tidak ditemukan di server.";
            }
            if (lower.contains("server error") || lower.contains("internal server error")) {
                return "Terjadi kendala pada sistem server backend. Silakan coba beberapa saat lagi.";
            }
            if (lower.contains("unauthorized") || lower.contains("forbidden")) {
                return "Akses ditolak. Anda tidak memiliki wewenang untuk tindakan ini.";
            }

            // Jika pesan dari server sudah berupa Bahasa Indonesia atau pesan khusus aplikasi
            return extractedMsg.trim();
        }

        // Jika tidak ada pesan khusus dari body, gunakan deskripsi kode HTTP yang ramah
        return getHttpCodeMessage(code, defaultMessage);
    }

    /**
     * Mendapatkan deskripsi kode status HTTP dalam Bahasa Indonesia yang informatif.
     */
    public static String getHttpCodeMessage(int code, String defaultMessage) {
        switch (code) {
            case 400:
                return "Permintaan tidak dapat diproses oleh server (Format salah / 400).";
            case 401:
                return "Sesi telah berakhir. Silakan login kembali (401).";
            case 403:
                return "Akses ditolak. Anda tidak memiliki wewenang untuk tindakan ini (403).";
            case 404:
                return "Data atau layanan tidak ditemukan di server (404).";
            case 409:
                return "Data telah mengalami perubahan status sebelumnya. Silakan segarkan halaman.";
            case 422:
                return "Data yang dimasukkan tidak memenuhi kriteria validasi sistem (422).";
            case 429:
                return "Terlalu banyak permintaan. Silakan tunggu beberapa saat sebelum mencoba lagi.";
            case 500:
                return "Terjadi kendala pada sistem server internal (500). Silakan coba lagi nanti.";
            case 502:
                return "Server gerbang tidak merespons (Bad Gateway 502). Backend kemungkinan sedang restart.";
            case 503:
                return "Layanan server sedang dalam pemeliharaan atau kelebihan beban (503).";
            case 504:
                return "Waktu tunggu server habis (Gateway Timeout 504).";
            default:
                if (defaultMessage != null && !defaultMessage.trim().isEmpty()) {
                    return defaultMessage;
                }
                return "Terjadi kendala komunikasi dengan server (Kode: " + code + ").";
        }
    }
}

