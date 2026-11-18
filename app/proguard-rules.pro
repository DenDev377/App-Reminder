# ProGuard / R8 Rules for SIPETA Kejaksaan Android App

# ─── Retain Line Numbers & Source Information for Bug Reports ─────────────────
-keepattributes SourceFile,LineNumberTable

# ─── Annotations & Signatures ────────────────────────────────────────────────
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepattributes RuntimeVisibleAnnotations,RuntimeInvisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations,RuntimeInvisibleParameterAnnotations

# ─── Gson Serialization & App Models ──────────────────────────────────────────
# Mencegah obfuscation pada nama field model agar serialisasi JSON tetap akurat
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.kejaksaan.reminder.model.** { *; }

# ─── Retrofit 2 & OkHttp ──────────────────────────────────────────────────────
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keep class com.kejaksaan.reminder.api.** { *; }
-keepclasseswithmembers interface com.kejaksaan.reminder.api.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# ─── AndroidX Security (EncryptedSharedPreferences) ──────────────────────────
-keep class androidx.security.crypto.** { *; }
-dontwarn androidx.security.crypto.**

# ─── Glide ───────────────────────────────────────────────────────────────────
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep class com.bumptech.glide.** { *; }
-dontwarn com.bumptech.glide.**

# ─── Firebase Cloud Messaging ────────────────────────────────────────────────
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# ─── ViewModels & Lifecycle ──────────────────────────────────────────────────
-keep class com.kejaksaan.reminder.viewmodel.** { *; }
