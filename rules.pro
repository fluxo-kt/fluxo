# R8/ProGuard rules
# https://www.guardsquare.com/manual/configuration/examples#library

-target 1.8
-verbose
-dontskipnonpubliclibraryclasses
-dontskipnonpubliclibraryclassmembers
-allowaccessmodification

-adaptresourcefilenames    **.properties,**.gif,**.jpg,**.png,**.webp
-adaptresourcefilecontents **.properties,META-INF/MANIFEST.MF

-keep,allowoptimization,includedescriptorclasses public class * {
    public protected *;
}

-keepparameternames
-renamesourcefileattribute SourceFile
-keepattributes Signature,Exceptions,*Annotation*,
                InnerClasses,PermittedSubclasses,EnclosingMethod,
                Deprecated,SourceFile,LineNumberTable

-keepclassmembers,allowoptimization enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Optimization for Android MIN_SDK versions
# https://jakewharton.com/r8-optimization-value-assumption/
# https://www.guardsquare.com/en/products/proguard/manual/examples#androidsdk
-assumenosideeffects class android.os.Build$VERSION { int SDK_INT return 15..2147483647; }
