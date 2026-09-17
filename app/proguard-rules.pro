# Add project specific ProGuard rules here.
-keep class com.cineclassic.app.data.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
