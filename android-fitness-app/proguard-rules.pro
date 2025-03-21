# Add project specific ProGuard rules here.

# Keep the application class
-keep class com.app.fitness.FitnessApplication { *; }

# Keep all Entity classes
-keep class com.app.fitness.data.database.** { *; }

# Keep Room Database and DAOs
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *

# Keep Firebase Analytics
-keep class com.google.firebase.analytics.** { *; }
-keep class com.google.android.gms.measurement.** { *; }

# MPAndroidChart rules
-keep class com.github.mikephil.charting.** { *; }

# Keep ViewModel classes
-keep class * extends androidx.lifecycle.ViewModel {
    <init>();
}
-keep class * extends androidx.lifecycle.AndroidViewModel {
    <init>(android.app.Application);
}

# Keep Fragment classes
-keep class * extends androidx.fragment.app.Fragment { *; }

# Keep Service classes
-keep class * extends android.app.Service { *; }

# Keep custom views if any
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# Keep the R class and its fields
-keepclassmembers class **.R$* {
    public static <fields>;
}

# Keep enum classes
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep Parcelable classes
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep Serializable classes
-keep class * implements java.io.Serializable { *; }

# Keep Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# General Android rules
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature
-keepattributes Exceptions

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}