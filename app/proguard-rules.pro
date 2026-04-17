# ─── Kotlin ───────────────────────────────────────────────────────────────────
-keep class kotlin.Metadata { *; }
-keepclassmembers class kotlinx.** { volatile <fields>; }
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# ─── Jetpack Compose ──────────────────────────────────────────────────────────
-keep class androidx.compose.** { *; }
-keep interface androidx.compose.** { *; }
-dontwarn androidx.compose.**

# ─── ViewModel ────────────────────────────────────────────────────────────────
-keep class * extends androidx.lifecycle.ViewModel { *; }

# ─── Uygulama data & ads sınıfları ───────────────────────────────────────────
-keepclassmembers class com.ilkokuluncu.app.data.** { *; }
-keepclassmembers class com.ilkokuluncu.app.ads.**  { *; }

# ─── Google Mobile Ads (AdMob) ────────────────────────────────────────────────
-keep class com.google.android.gms.ads.** { *; }
-keep interface com.google.android.gms.ads.** { *; }
-keep class com.google.ads.** { *; }
-dontwarn com.google.android.gms.ads.**

# ─── Google Play Services ─────────────────────────────────────────────────────
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# ─── Coil ─────────────────────────────────────────────────────────────────────
-keep class coil.** { *; }
-dontwarn coil.**
