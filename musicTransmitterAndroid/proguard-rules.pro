# Keep all ViewBinding generated classes and their bind()/inflate() methods
-keep class * implements androidx.viewbinding.ViewBinding {
    public static *** bind(android.view.View);
    public static *** inflate(...);
}

# Keep the ViewBindingPropertyDelegate library itself (на всякий случай)
-keep class by.kirich1409.viewbindingdelegate.** { *; }
-dontwarn by.kirich1409.viewbindingdelegate.**

-keep class org.jaudiotagger.** { *; }
-keepclassmembers class org.jaudiotagger.** { *; }
-dontwarn org.jaudiotagger.**

# JLayer (javazoom:jlayer:1.0.1)
-keep class javazoom.jl.** { *; }
-dontwarn javazoom.jl.**
-dontwarn javax.sound.**