package com.kejaksaan.reminder.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Helper untuk pemrosesan file gambar profil sebelum di-upload:
 * - Pembuatan URI foto kamera via FileProvider
 * - Penyesuaian rotasi EXIF foto HP
 * - Scaling down resolusi (maksimal 1024px)
 * - Kompresi JPEG berkualitas tinggi dengan ukuran file ringan (< 300KB)
 */
public class ImageUploadHelper {

    private static final int MAX_DIMENSION = 1024;
    private static final int JPEG_QUALITY = 85;

    /**
     * Buat file sementara dan dapatkan URI FileProvider untuk menampung jepretan kamera.
     */
    public static Uri createCameraTempUri(Context context) throws IOException {
        File cacheDir = new File(context.getCacheDir(), "camera");
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }
        File tempFile = File.createTempFile("camera_capture_", ".jpg", cacheDir);
        String authority = context.getPackageName() + ".fileprovider";
        return FileProvider.getUriForFile(context, authority, tempFile);
    }

    /**
     * Membaca URI gambar (galeri atau kamera), melakukan rotasi EXIF, scaling down,
     * dan mengompres ke file JPEG di folder cache.
     */
    public static File compressImageForUpload(Context context, Uri imageUri) throws IOException {
        File cacheDir = context.getCacheDir();
        File rawTempFile = File.createTempFile("raw_pick_", ".tmp", cacheDir);

        // 1. Salin isi stream URI ke file sementara
        try (InputStream in = context.getContentResolver().openInputStream(imageUri);
             OutputStream out = new FileOutputStream(rawTempFile)) {
            if (in == null) {
                throw new IOException("Tidak dapat membaca data gambar dari URI: " + imageUri);
            }
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            out.flush();
        }

        // 2. Baca rotasi EXIF
        int rotationDegrees = 0;
        try (InputStream exifStream = new FileInputStream(rawTempFile)) {
            ExifInterface exif = new ExifInterface(exifStream);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            if (orientation == ExifInterface.ORIENTATION_ROTATE_90) {
                rotationDegrees = 90;
            } else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) {
                rotationDegrees = 180;
            } else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) {
                rotationDegrees = 270;
            }
        } catch (Exception ignored) {
        }

        // 3. Baca dimensi gambar (bounds)
        BitmapFactory.Options boundsOptions = new BitmapFactory.Options();
        boundsOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(rawTempFile.getAbsolutePath(), boundsOptions);

        int srcWidth = boundsOptions.outWidth;
        int srcHeight = boundsOptions.outHeight;
        if (srcWidth <= 0 || srcHeight <= 0) {
            rawTempFile.delete();
            throw new IOException("File bukan gambar yang valid atau format tidak didukung");
        }

        // 4. Hitung inSampleSize
        int inSampleSize = 1;
        while ((srcWidth / inSampleSize) > MAX_DIMENSION || (srcHeight / inSampleSize) > MAX_DIMENSION) {
            inSampleSize *= 2;
        }

        BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
        decodeOptions.inSampleSize = inSampleSize;
        decodeOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;

        Bitmap decodedBitmap = BitmapFactory.decodeFile(rawTempFile.getAbsolutePath(), decodeOptions);
        rawTempFile.delete(); // Bersihkan file raw

        if (decodedBitmap == null) {
            throw new IOException("Gagal mendecode gambar");
        }

        // 5. Scaling presisi jika masih lebih besar dari MAX_DIMENSION
        int currentWidth = decodedBitmap.getWidth();
        int currentHeight = decodedBitmap.getHeight();
        float scale = 1.0f;
        if (currentWidth > MAX_DIMENSION || currentHeight > MAX_DIMENSION) {
            scale = Math.min((float) MAX_DIMENSION / currentWidth, (float) MAX_DIMENSION / currentHeight);
        }

        Matrix matrix = new Matrix();
        if (scale < 1.0f) {
            matrix.postScale(scale, scale);
        }
        if (rotationDegrees != 0) {
            matrix.postRotate(rotationDegrees);
        }

        Bitmap finalBitmap;
        if (scale < 1.0f || rotationDegrees != 0) {
            finalBitmap = Bitmap.createBitmap(decodedBitmap, 0, 0, currentWidth, currentHeight, matrix, true);
            if (finalBitmap != decodedBitmap) {
                decodedBitmap.recycle();
            }
        } else {
            finalBitmap = decodedBitmap;
        }

        // 6. Simpan bitmap ke file tujuan JPEG
        File outputFile = new File(cacheDir, "profile_upload_" + System.currentTimeMillis() + ".jpg");
        try (FileOutputStream out = new FileOutputStream(outputFile)) {
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out);
            out.flush();
        } finally {
            finalBitmap.recycle();
        }

        return outputFile;
    }

    /**
     * Menyalin file sumber ke tujuan.
     */
    public static void copyFile(File source, File dest) throws IOException {
        try (InputStream in = new FileInputStream(source);
             OutputStream out = new FileOutputStream(dest)) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            out.flush();
        }
    }
}
