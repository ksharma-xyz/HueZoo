# ── Huezoo ProGuard Rules ─────────────────────────────────────────────────────
# Refer to http://developer.android.com/guide/developing/tools/proguard.html

# Keep source file + line numbers for readable crash stack traces.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-keepattributes *Annotation*,Signature,Exceptions,InnerClasses,EnclosingMethod

# ── Kotlin ────────────────────────────────────────────────────────────────────
-keep class kotlin.** { *; }
-keep class kotlinx.** { *; }
-dontwarn kotlin.**
# Kotlin serialization (used by Navigation 3 for route serialization)
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class ** {
    @kotlinx.serialization.Serializable *;
}
-keep @kotlinx.serialization.Serializable class ** { *; }
# Navigation3 NavKey objects are serialized — keep all NavKey data classes/objects
-keep class xyz.ksharma.huezoo.navigation.** { *; }

# ── Kotlin Coroutines ─────────────────────────────────────────────────────────
-dontwarn kotlinx.coroutines.**
-keep class kotlinx.coroutines.** { *; }

# ── Koin (dependency injection) ───────────────────────────────────────────────
-keep class org.koin.** { *; }
-keepclassmembers class * {
    @org.koin.core.annotation.* *;
}
-dontwarn org.koin.**

# ── SQLDelight ────────────────────────────────────────────────────────────────
# Keep all generated query and adapter classes
-keep class xyz.ksharma.huezoo.data.db.** { *; }
-keep class app.cash.sqldelight.** { *; }
-dontwarn app.cash.sqldelight.**

# ── Google Mobile Ads (AdMob) ─────────────────────────────────────────────────
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.ads.** { *; }
-dontwarn com.google.android.gms.ads.**
# Mediation adapters
-keep class com.google.android.gms.internal.ads.** { *; }

# ── Google Play Billing ───────────────────────────────────────────────────────
-keep class com.android.billingclient.** { *; }
-dontwarn com.android.billingclient.**

# ── Compose ───────────────────────────────────────────────────────────────────
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# ── Enums / sealed classes ────────────────────────────────────────────────────
-keep public enum * { *; }
-keep class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ── Native methods ────────────────────────────────────────────────────────────
-keepclasseswithmembernames class * {
    native <methods>;
}

# ── WorkManager ──────────────────────────────────────────────────────────────
-keep class androidx.work.** { *; }
-keep class androidx.work.impl.** { *; }
-dontwarn androidx.work.**

# ── Room (used internally by WorkManager's WorkDatabase) ──────────────────────
-keep class androidx.room.** { *; }
-keep @androidx.room.Database class * { *; }
-keep @androidx.room.Entity class * { *; }
-keepclassmembers @androidx.room.Entity class * { *; }
-dontwarn androidx.room.**

# ── Firebase ──────────────────────────────────────────────────────────────────
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
# Crashlytics — keep mapping metadata so stack traces are symbolicated
-keepattributes *Annotation*
-keep class com.crashlytics.** { *; }
-dontwarn com.crashlytics.**
# Remote Config — keep field names used in config fetch/activate
-keep class com.google.android.gms.internal.firebase_remote_config.** { *; }
# GitLive KMP wrappers
-keep class dev.gitlive.firebase.** { *; }
-dontwarn dev.gitlive.firebase.**

# ── Suppress irrelevant warnings ──────────────────────────────────────────────
-dontwarn org.slf4j.**
-dontwarn java.awt.**
-dontwarn javax.annotation.**
