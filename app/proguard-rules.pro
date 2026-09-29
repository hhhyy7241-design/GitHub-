# ======================================================================
# ChatPro Security & Obfuscation ProGuard Rules
# ======================================================================

# 1. Obfuscation & Source Hiding
-repackageclasses ''
-allowaccessmodification
-renamesourcefileattribute 'SourceFile'
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# 2. Strip Logging in Release builds to prevent data leak in logcat
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# 3. Security Shield Protection
-keep class com.example.security.SecurityShield { *; }

# 4. Room Database & DAOs
-keep class androidx.room.** { *; }
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-dontwarn androidx.room.paging.**

# 5. Moshi & JSON Models
-keep class com.example.data.local.** { *; }
-keep class com.example.data.moodle.** { *; }
-keepclassmembers class * {
    @com.squareup.moshi.Json *;
    @com.squareup.moshi.JsonClass *;
}
-keep class com.squareup.moshi.** { *; }
-dontwarn com.squareup.moshi.**

# 6. OkHttp & Okio
-keepattributes Signature
-keepattributes *Annotation*
-keepclassmembers class okhttp3.OkHttpClient {
    *;
}
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**

# 7. Jetpack Compose & Kotlin Coroutines
-keep class androidx.compose.** { *; }
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# 8. Coil Image Loader
-keep class coil.** { *; }
-dontwarn coil.**
