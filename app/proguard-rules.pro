# Proguard rules for Clockity
-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
