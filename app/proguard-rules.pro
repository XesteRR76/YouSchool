# ProGuard rules for YouSchool
-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
