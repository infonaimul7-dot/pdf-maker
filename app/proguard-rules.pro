-keep class com.tom_roush.pdfbox.** { *; }
-dontwarn com.tom_roush.pdfbox.**
-keep class com.tom_roush.fontbox.** { *; }
-dontwarn com.tom_roush.fontbox.**
-dontwarn org.bouncycastle.**
-dontwarn java.awt.**
-dontwarn javax.awt.**
-dontwarn javax.imageio.**
-dontwarn javax.swing.**
-dontwarn org.w3c.dom.**

# Google Mobile Ads (AdMob)
-keep class com.google.android.gms.ads.** { *; }
-dontwarn com.google.android.gms.ads.**

# Guava / JSR305 / ErrorProne / CheckerFramework (R8 release minification)
-dontwarn com.google.errorprone.annotations.**
-dontwarn com.google.j2objc.annotations.**
-dontwarn javax.annotation.**
-dontwarn org.checkerframework.**
-dontwarn org.codehaus.mojo.animal_sniffer.**
-dontwarn java.lang.ClassValue
-dontwarn sun.misc.Unsafe
