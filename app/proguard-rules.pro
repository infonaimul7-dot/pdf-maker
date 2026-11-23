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
