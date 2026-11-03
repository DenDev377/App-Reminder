package com.kejaksaan.reminder.model;

import com.google.gson.annotations.SerializedName;
import com.kejaksaan.reminder.api.RetrofitClient;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Model Berita sesuai endpoint GET /api/berita.
 */
public class Berita implements Serializable {

    @SerializedName("id")
    private int id;

    @SerializedName("judul")
    private String judul;

    @SerializedName("slug")
    private String slug;

    @SerializedName("ringkasan")
    private String ringkasan;

    @SerializedName("konten")
    private String konten;

    @SerializedName("gambar")
    private String gambar;

    @SerializedName("gambar_url")
    private String gambarUrl;

    @SerializedName("tanggal")
    private String tanggal;

    @SerializedName("penulis")
    private String penulis;

    @SerializedName("views_count")
    private int viewsCount;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("updated_at")
    private String updatedAt;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getJudul() {
        return judul;
    }

    public void setJudul(String judul) {
        this.judul = judul;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getRingkasan() {
        return ringkasan;
    }

    public void setRingkasan(String ringkasan) {
        this.ringkasan = ringkasan;
    }

    public String getKonten() {
        return konten;
    }

    public void setKonten(String konten) {
        this.konten = konten;
    }

    public String getGambar() {
        return gambar;
    }

    public void setGambar(String gambar) {
        this.gambar = gambar;
    }

    public String getGambarUrl() {
        return gambarUrl;
    }

    public void setGambarUrl(String gambarUrl) {
        this.gambarUrl = gambarUrl;
    }

    public String getTanggal() {
        return tanggal;
    }

    public void setTanggal(String tanggal) {
        this.tanggal = tanggal;
    }

    public String getPenulis() {
        return penulis != null && !penulis.isEmpty() ? penulis : "Kejaksaan Negeri";
    }

    public void setPenulis(String penulis) {
        this.penulis = penulis;
    }

    public int getViewsCount() {
        return viewsCount;
    }

    public void setViewsCount(int viewsCount) {
        this.viewsCount = viewsCount;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * Mengembalikan URL gambar lengkap untuk dimuat oleh Glide.
     * Mengutamakan gambarUrl, namun jika domain tidak sinkron atau gambar lokal relatif,
     * dilakukan resolusi menggunakan RetrofitClient.BASE_URL.
     */
    public String getResolvedImageUrl() {
        if (gambarUrl != null && !gambarUrl.trim().isEmpty()) {
            return gambarUrl.trim();
        }

        if (gambar != null && !gambar.trim().isEmpty()) {
            String path = gambar.trim();
            if (path.startsWith("http://") || path.startsWith("https://")) {
                return path;
            }
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            return RetrofitClient.BASE_URL + "storage/" + path;
        }

        return null;
    }

    /**
     * Memformat tanggal 'YYYY-MM-DD' atau ISO datetime menjadi format Indonesia 'DD MMMM YYYY'.
     */
    public String getFormattedTanggal() {
        if (tanggal == null || tanggal.trim().isEmpty()) {
            return "-";
        }

        String rawDate = tanggal.trim();
        SimpleDateFormat inputFormat;
        if (rawDate.length() >= 10) {
            String datePart = rawDate.substring(0, 10);
            inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            try {
                Date parsed = inputFormat.parse(datePart);
                if (parsed != null) {
                    SimpleDateFormat outputFormat = new SimpleDateFormat("d MMMM yyyy", new Locale("id", "ID"));
                    return outputFormat.format(parsed);
                }
            } catch (ParseException e) {
                // Abaikan dan kembalikan raw date
            }
        }

        return rawDate;
    }
}

