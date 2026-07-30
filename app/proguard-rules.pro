# R8 rules for the release build.
#
# The debug-signed sideload artefact CI produces is not minified, so these rules
# only matter once a release build is exercised. Keep them minimal and justified —
# a blanket `-keep class **` would defeat the point of shrinking.

# kotlinx.serialization generates serializer() companions that R8 cannot see are used.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.visceralfit.**$$serializer { *; }
-keepclasseswithmembers class com.visceralfit.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Room generates implementations reflectively resolved by name.
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-dontwarn androidx.room.paging.**

# Enum ids are read by name from DataStore and Room; keep valueOf/entries reachable.
-keepclassmembers enum com.visceralfit.domain.model.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep the line numbers in crash reports useful without exposing full source paths.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
