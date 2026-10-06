# R8 rules for the release build.
#
# Only what is actually needed, with the reason recorded. A proguard file that
# keeps everything "to be safe" silently turns minification off and nobody
# notices until the APK is twice the size it should be.

# --- Ktor --------------------------------------------------------------------
# Ktor's IntellijIdeaDebugDetector probes java.lang.management to decide whether
# a debugger is attached. That package does not exist on Android, and the code
# path is never reached there — but R8 still fails the build on the dangling
# reference. Warning only: the classes genuinely are absent, by design.
-dontwarn java.lang.management.**
-dontwarn org.slf4j.**
-dontwarn io.ktor.util.debug.**

# Ktor picks its engine through a service loader, so the implementation is only
# ever referenced reflectively.
-keep class io.ktor.client.engine.okhttp.** { *; }

# --- kotlinx.serialization ---------------------------------------------------
# Serializers are generated as companions and resolved by name at runtime.
# Without these, every @Serializable DTO decodes to an exception in release and
# works perfectly in debug — the worst possible failure mode.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# Our own wire types, kept whole: their property NAMES are the wire format, so
# renaming them changes the JSON the app sends and receives.
-keep,includedescriptorclasses class com.fancyfinery.mobile.**$$serializer { *; }
-keepclassmembers class com.fancyfinery.mobile.** {
    *** Companion;
    *** serializer(...);
}

# --- Navigation 3 ------------------------------------------------------------
# Destinations are serialized into the back stack and restored by type.
-keep class com.fancyfinery.mobile.core.navigation.** { *; }

# --- Room --------------------------------------------------------------------
-keep class androidx.room3.** { *; }
-keep @androidx.room3.Entity class * { *; }
-dontwarn androidx.room.paging.**

# --- Koin --------------------------------------------------------------------
# Koin resolves by type at runtime; constructors must survive.
-keepclassmembers class com.fancyfinery.mobile.** {
    public <init>(...);
}

# --- Google Credential Manager ----------------------------------------------
-keep class com.google.android.libraries.identity.googleid.** { *; }
-dontwarn com.google.android.libraries.identity.googleid.**

# --- Coil --------------------------------------------------------------------
-dontwarn coil3.**

# --- Compose -----------------------------------------------------------------
# Keeps composable names readable in crash reports. Costs a little size and
# repays it the first time a release-only crash has to be read.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
