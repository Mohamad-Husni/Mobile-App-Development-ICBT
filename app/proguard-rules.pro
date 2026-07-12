# PrintXpress release ProGuard/R8 rules.

# Keep line numbers for readable stack traces in crash reports.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# --- Firebase / Google Play Services ---
# Firebase SDKs ship their own consumer rules via AAR metadata, but keep the
# model classes we hand to Firebase Database/Auth so field names survive
# reflection-based (de)serialization.
-keepclassmembers class com.example.printxpress.** {
    public <init>();
}
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# --- SQLite (androidx.sqlite / android.database.sqlite) ---
-keep class android.database.sqlite.** { *; }
-dontwarn android.database.sqlite.**

# --- AndroidX / Material Components ---
-keep class com.google.android.material.** { *; }
-dontwarn com.google.android.material.**
