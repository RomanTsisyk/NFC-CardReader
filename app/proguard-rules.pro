# ============================================
# NFC Card Reader ProGuard Rules
# ============================================

# Preserve line number information for debugging stack traces
-keepattributes SourceFile,LineNumberTable

# Hide the original source file name
-renamesourcefileattribute SourceFile

# ============================================
# Kotlin
# ============================================
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# ============================================
# Coroutines
# ============================================
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# ============================================
# Room Database
# ============================================
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Keep Room entity fields
-keepclassmembers class io.github.romantsisyk.nfccardreader.data.local.entity.** {
    <fields>;
}

# ============================================
# Hilt / Dagger
# ============================================
-keepclasseswithmembers class * {
    @dagger.* <methods>;
}
-keep class dagger.* { *; }
-keep class javax.inject.* { *; }
-keep class * extends dagger.hilt.internal.GeneratedComponentManager { *; }
-dontwarn dagger.internal.codegen.**

# ============================================
# Compose
# ============================================
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# ============================================
# Domain Models - Keep for serialization
# ============================================
-keep class io.github.romantsisyk.nfccardreader.domain.model.** { *; }
-keep class io.github.romantsisyk.nfccardreader.domain.repository.** { *; }

# Keep EMV tags enum
-keep enum io.github.romantsisyk.nfccardreader.domain.EmvTag { *; }

# ============================================
# NFC Error types
# ============================================
-keep enum io.github.romantsisyk.nfccardreader.domain.model.NfcError { *; }
-keep class io.github.romantsisyk.nfccardreader.domain.model.NfcResult$** { *; }

# ============================================
# ViewModels
# ============================================
-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keep class io.github.romantsisyk.nfccardreader.presentation.viewmodel.** { *; }

# ============================================
# JSON / Serialization
# ============================================
-keepattributes Signature
-keepattributes *Annotation*

# ============================================
# Security - Additional obfuscation
# ============================================
# Rename classes aggressively
-repackageclasses 'o'
-allowaccessmodification

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# ============================================
# NFC Classes - Keep for Android system
# ============================================
-keep class android.nfc.** { *; }
-keep class android.nfc.tech.** { *; }

# ============================================
# General Android
# ============================================
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep Parcelables
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep Serializables
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ============================================
# Enums
# ============================================
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}