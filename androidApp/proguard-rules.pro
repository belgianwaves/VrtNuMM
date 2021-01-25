-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt # core serialization annotations

-keep class com.bw.vrtnumm.shared.TokenResolver$LoginCreds {
    *;
}

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class com.bw.vrtnumm.**$$serializer { *; } # <-- change package name to your app's
-keepclassmembers class com.bw.vrtnu.** { # <-- change package name to your app's
    *** Companion;
}
-keepclasseswithmembers class com.bw.vrtnumm.** { # <-- change package name to your app's
    kotlinx.serialization.KSerializer serializer(...);
}