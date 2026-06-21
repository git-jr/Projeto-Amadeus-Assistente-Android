# Add project specific ProGuard rules here.

# Preserve source file names and line numbers for crash stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ── EventBus ──────────────────────────────────────────────────────────────────
# @Subscribe methods are called via reflection at runtime
-keepclassmembers class ** {
    @org.greenrobot.eventbus.Subscribe <methods>;
}
-keep enum org.greenrobot.eventbus.ThreadMode { *; }

# ── Gson ──────────────────────────────────────────────────────────────────────
# Fields annotated with @SerializedName must keep their names
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# ── Room ──────────────────────────────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-keepclassmembers @androidx.room.Dao class * { *; }

# ── Firebase ──────────────────────────────────────────────────────────────────
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# ── Vosk (JNI / native bindings) ──────────────────────────────────────────────
-keep class org.vosk.** { *; }
-keepclassmembers class org.vosk.** { *; }

# ── Firebase AI Logic (covered by com.google.firebase.** rule above) ─────────
-keep class com.google.firebase.ai.** { *; }

# ── Joda-Time ─────────────────────────────────────────────────────────────────
# joda-convert is an optional compile-time dependency of joda-time; not shipped at runtime
-dontwarn org.joda.convert.**

# ── Jsoup ─────────────────────────────────────────────────────────────────────
-keep public class org.jsoup.** { *; }

# ── App packages that use reflection / EventBus ───────────────────────────────
-keep class com.paradoxo.amadeus.cognicao.** { *; }
-keep class com.paradoxo.amadeus.dao.** { *; }
-keep class com.paradoxo.amadeus.modelo.** { *; }
-keep enum com.paradoxo.amadeus.enums.** { *; }

# ── JNA (used by Vosk) ────────────────────────────────────────────────────────
# JNA's native initIDs() looks up fields by name via JNI — R8 must not rename them
-keep class com.sun.jna.** { *; }
-keepclassmembers class com.sun.jna.** { *; }
-dontwarn com.sun.jna.**

# ── LiteRT-LM (on-device LLM, JNI native bindings) ───────────────────────────
-keep class com.google.ai.edge.litertlm.** { *; }
-keepclassmembers class com.google.ai.edge.litertlm.** { *; }
-dontwarn com.google.ai.edge.litertlm.**
-keep enum com.paradoxo.amadeus.dao.room.entities.ModelStatus { *; }
