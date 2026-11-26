package com.kejaksaan.reminder.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.kejaksaan.reminder.model.User;

import java.util.Map;

/**
 * SessionManager — mengelola penyimpanan sesi pengguna secara terenkripsi (Hardware-backed AES-256-GCM).
 * Menggunakan EncryptedSharedPreferences untuk mengamankan:
 * - Token Otentikasi (Bearer)
 * - Identitas Pegawai (NIP, NRP, nomor HP, email, nama)
 * - Token FCM & Device ID
 *
 * Mencegah kebocoran data jika perangkat di-backup (ADB) atau di-root.
 */
public class SessionManager {

    private static final String TAG = "SessionManager";
    private static final String LEGACY_PREF_NAME = "SIPETA_SESSION";
    private static final String SECURE_PREF_NAME = "SIPETA_SECURE_SESSION";

    private static final String KEY_TOKEN  = "auth_token";
    private static final String KEY_USER_ID     = "user_id";
    private static final String KEY_USER_NAME   = "user_name";
    private static final String KEY_USER_EMAIL  = "user_email";
    private static final String KEY_USER_ROLE   = "user_role";
    private static final String KEY_USER_NIP    = "user_nip";
    private static final String KEY_USER_NRP    = "user_nrp";
    private static final String KEY_USER_NOMOR_HP = "user_nomor_hp";
    private static final String KEY_USER_JABATAN        = "user_jabatan";
    private static final String KEY_USER_PANGKAT        = "user_pangkat_golongan";
    private static final String KEY_USER_FOTO_PROFIL    = "user_foto_profil";
    private static final String KEY_USER_FOTO_PROFIL_URL = "user_foto_profil_url";
    private static final String KEY_LOCAL_AVATAR_PATH   = "local_avatar_path";

    private static final String KEY_FCM_TOKEN           = "fcm_device_token";
    private static final String KEY_FCM_TOKEN_REGISTERED = "fcm_token_registered";
    private static final String KEY_DEVICE_ID           = "device_unique_id";

    private static final String KEY_BATTERY_OPT_DISMISSED = "battery_opt_dismissed_session";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = createSecurePreferences(context);
    }

    private SharedPreferences createSecurePreferences(Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            SharedPreferences securePrefs = EncryptedSharedPreferences.create(
                    context,
                    SECURE_PREF_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );

            // Migrasi transparan dari data plaintext versi lama (jika ada)
            migrateLegacyPrefs(context, securePrefs);

            return securePrefs;
        } catch (Exception e) {
            Log.e(TAG, "Gagal menginisialisasi EncryptedSharedPreferences, fallback ke MODE_PRIVATE: " + e.getMessage());
            return context.getSharedPreferences(LEGACY_PREF_NAME, Context.MODE_PRIVATE);
        }
    }

    private void migrateLegacyPrefs(Context context, SharedPreferences securePrefs) {
        try {
            SharedPreferences legacy = context.getSharedPreferences(LEGACY_PREF_NAME, Context.MODE_PRIVATE);
            Map<String, ?> all = legacy.getAll();
            if (all != null && !all.isEmpty()) {
                SharedPreferences.Editor editor = securePrefs.edit();
                for (Map.Entry<String, ?> entry : all.entrySet()) {
                    String key = entry.getKey();
                    Object val = entry.getValue();
                    if (val instanceof String) {
                        editor.putString(key, (String) val);
                    } else if (val instanceof Integer) {
                        editor.putInt(key, (Integer) val);
                    } else if (val instanceof Boolean) {
                        editor.putBoolean(key, (Boolean) val);
                    } else if (val instanceof Float) {
                        editor.putFloat(key, (Float) val);
                    } else if (val instanceof Long) {
                        editor.putLong(key, (Long) val);
                    }
                }
                editor.apply();
                legacy.edit().clear().apply();
                Log.i(TAG, "Migrasi data sesi ke EncryptedSharedPreferences berhasil dilakukan.");
            }
        } catch (Exception e) {
            Log.w(TAG, "Catatan migrasi preferensi: " + e.getMessage());
        }
    }

    // ─── Token ───────────────────────────────────────────────────────────────────

    public void saveToken(String token) {
        prefs.edit().putString(KEY_TOKEN, token).apply();
    }

    /** @return token atau null jika belum login */
    public String fetchToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    public boolean isLoggedIn() {
        return fetchToken() != null;
    }

    // ─── FCM Device Token ────────────────────────────────────────────────────────

    public void saveFcmToken(String fcmToken) {
        prefs.edit().putString(KEY_FCM_TOKEN, fcmToken).apply();
    }

    public String fetchFcmToken() {
        return prefs.getString(KEY_FCM_TOKEN, null);
    }

    public boolean isFcmTokenRegistered() {
        return prefs.getBoolean(KEY_FCM_TOKEN_REGISTERED, false);
    }

    public void setFcmTokenRegistered(boolean registered) {
        prefs.edit().putBoolean(KEY_FCM_TOKEN_REGISTERED, registered).apply();
    }

    /**
     * Mengambil ID perangkat yang konsisten untuk instalasi aplikasi ini,
     * atau menghasilkan UUID acak baru jika belum pernah dibuat.
     */
    public String getOrCreateDeviceId() {
        String deviceId = prefs.getString(KEY_DEVICE_ID, null);
        if (deviceId == null || deviceId.trim().isEmpty()) {
            deviceId = java.util.UUID.randomUUID().toString();
            prefs.edit().putString(KEY_DEVICE_ID, deviceId).apply();
        }
        return deviceId;
    }

    // ─── User Info ───────────────────────────────────────────────────────────────

    /**
     * Simpan seluruh data User ke SharedPreferences.
     * Dipanggil setelah login berhasil.
     */
    public void saveUserInfo(User user) {
        if (user == null) return;
        prefs.edit()
                .putInt(KEY_USER_ID, user.getId())
                .putString(KEY_USER_NAME, user.getName())
                .putString(KEY_USER_EMAIL, user.getEmail())
                .putString(KEY_USER_ROLE, user.getRole())
                .putString(KEY_USER_NIP, user.getNip())
                .putString(KEY_USER_NRP, user.getNrp())
                .putString(KEY_USER_NOMOR_HP, user.getNomorHp())
                .putString(KEY_USER_JABATAN, user.getJabatan())
                .putString(KEY_USER_PANGKAT, user.getPangkatGolongan())
                .putString(KEY_USER_FOTO_PROFIL, user.getFotoProfil())
                .putString(KEY_USER_FOTO_PROFIL_URL, user.getResolvedPhotoUrl())
                .apply();
    }

    public int fetchUserId()          { return prefs.getInt(KEY_USER_ID, -1); }
    public String fetchUserName()     { return prefs.getString(KEY_USER_NAME, ""); }
    public String fetchUserEmail()    { return prefs.getString(KEY_USER_EMAIL, ""); }
    public String fetchUserRole()     { return prefs.getString(KEY_USER_ROLE, ""); }
    public String fetchUserNip()      { return prefs.getString(KEY_USER_NIP, ""); }
    public String fetchUserNrp()      { return prefs.getString(KEY_USER_NRP, ""); }
    public String fetchUserNomorHp()  { return prefs.getString(KEY_USER_NOMOR_HP, ""); }
    public String fetchUserJabatan()  { return prefs.getString(KEY_USER_JABATAN, ""); }
    public String fetchUserPangkat()  { return prefs.getString(KEY_USER_PANGKAT, ""); }
    public String fetchFotoProfil()   { return prefs.getString(KEY_USER_FOTO_PROFIL, ""); }
    public String fetchFotoProfilUrl(){ return prefs.getString(KEY_USER_FOTO_PROFIL_URL, ""); }

    public void saveLocalAvatarPath(String path) {
        prefs.edit().putString(KEY_LOCAL_AVATAR_PATH, path).apply();
    }

    public String fetchLocalAvatarPath() {
        return prefs.getString(KEY_LOCAL_AVATAR_PATH, null);
    }

    // ─── Battery Optimization Prompt (Per Session) ───────────────────────────────

    /**
     * Mengecek apakah pengguna memilih 'Nanti Saja' pada sesi login saat ini.
     * Flag ini otomatis ter-reset saat pengguna logout (clearSession).
     */
    public boolean isBatteryOptDismissedInSession() {
        return prefs.getBoolean(KEY_BATTERY_OPT_DISMISSED, false);
    }

    public void setBatteryOptDismissedInSession(boolean dismissed) {
        prefs.edit().putBoolean(KEY_BATTERY_OPT_DISMISSED, dismissed).apply();
    }

    // ─── Session Reset ───────────────────────────────────────────────────────────

    /**
     * Hapus sesi pengguna saat logout.
     * Token FCM dan Device ID dipertahankan secara aman
     * agar perangkat dapat langsung mendaftarkan token untuk user berikutnya setelah login baru.
     */
    public void clearSession() {
        String fcmToken = fetchFcmToken();
        String deviceId = prefs.getString(KEY_DEVICE_ID, null);

        prefs.edit().clear().apply();

        if (fcmToken != null) {
            saveFcmToken(fcmToken);
        }
        if (deviceId != null) {
            prefs.edit().putString(KEY_DEVICE_ID, deviceId).apply();
        }
        setFcmTokenRegistered(false);
    }
}
