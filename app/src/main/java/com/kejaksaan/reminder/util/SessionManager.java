package com.kejaksaan.reminder.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.kejaksaan.reminder.model.User;

/**
 * SessionManager — menyimpan data sesi pengguna secara lokal
 * menggunakan SharedPreferences dengan MODE_PRIVATE.
 *
 * Data yang disimpan:
 * - Token Otentikasi (Bearer)
 * - Info dasar User (id, name, email, role, nip, jabatan)
 */
public class SessionManager {

    private static final String PREF_NAME  = "SIPETA_SESSION";
    private static final String KEY_TOKEN  = "auth_token";
    private static final String KEY_USER_ID     = "user_id";
    private static final String KEY_USER_NAME   = "user_name";
    private static final String KEY_USER_EMAIL  = "user_email";
    private static final String KEY_USER_ROLE   = "user_role";
    private static final String KEY_USER_NIP    = "user_nip";
    private static final String KEY_USER_JABATAN        = "user_jabatan";
    private static final String KEY_USER_PANGKAT        = "user_pangkat_golongan";
    private static final String KEY_USER_FOTO_PROFIL    = "user_foto_profil";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
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
                .putString(KEY_USER_JABATAN, user.getJabatan())
                .putString(KEY_USER_PANGKAT, user.getPangkatGolongan())
                .putString(KEY_USER_FOTO_PROFIL, user.getFotoProfil())
                .apply();
    }

    public int fetchUserId()          { return prefs.getInt(KEY_USER_ID, -1); }
    public String fetchUserName()     { return prefs.getString(KEY_USER_NAME, ""); }
    public String fetchUserEmail()    { return prefs.getString(KEY_USER_EMAIL, ""); }
    public String fetchUserRole()     { return prefs.getString(KEY_USER_ROLE, ""); }
    public String fetchUserNip()      { return prefs.getString(KEY_USER_NIP, ""); }
    public String fetchUserJabatan()  { return prefs.getString(KEY_USER_JABATAN, ""); }
    public String fetchUserPangkat()  { return prefs.getString(KEY_USER_PANGKAT, ""); }
    public String fetchFotoProfil()   { return prefs.getString(KEY_USER_FOTO_PROFIL, ""); }

    // ─── Session Reset ───────────────────────────────────────────────────────────

    /** Hapus semua data sesi — dipanggil saat logout. */
    public void clearSession() {
        prefs.edit().clear().apply();
    }
}
