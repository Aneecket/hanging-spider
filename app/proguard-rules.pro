# Keep model classes used by Firebase Realtime Database serialization.
-keep class com.hangingspider.game.data.model.** { *; }
-keepattributes Signature, *Annotation*, InnerClasses, EnclosingMethod

# Firebase RTDB uses reflection on annotated getters/setters.
-keepclassmembers class * {
    @com.google.firebase.database.PropertyName <methods>;
    @com.google.firebase.database.PropertyName <fields>;
}
-keepnames class com.google.firebase.database.core.PersistentConnection
-keepnames class com.google.firebase.database.connection.WebSocketConnection

# Firebase KTX reified getValue<T>() uses an anonymous GenericTypeIndicator<T>.
# R8 must NOT obfuscate/rename the type or its subclasses, or the runtime check
# "Not a direct subclass of GenericTypeIndicator" throws and crashes on first read.
-keep class com.google.firebase.database.GenericTypeIndicator { *; }
-keep class * extends com.google.firebase.database.GenericTypeIndicator { *; }

# Google Sign-In / Credential Manager — sun.misc.Unsafe usage.
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**

# AdMob keeps webview/JS bridges and its dynamite modules.
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.ads.** { *; }
-dontwarn com.google.android.gms.**

# Kotlin metadata and coroutines.
-keepattributes RuntimeVisibleAnnotations, AnnotationDefault
-dontwarn kotlinx.coroutines.**

# Compose stability inference — leave classes untouched.
-keep class androidx.compose.** { *; }
