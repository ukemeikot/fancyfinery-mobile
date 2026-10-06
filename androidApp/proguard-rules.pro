# Add project specific ProGuard rules here.
-keep class com.fancyfinery.mobile.** { *; }

# Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }

# Ktor
-keep class io.ktor.** { *; }
-keep class kotlinx.coroutines.** { *; }

# Room
-keep class * extends androidx.room3.RoomDatabase
-keep @androidx.room3.Entity class *
-dontwarn androidx.room3.paging.**

# Koin
-keep class org.koin.** { *; }
