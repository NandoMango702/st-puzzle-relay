-dontobfuscate
-keepattributes SourceFile,LineNumberTable
-keep @io.github.nandomango702.stpuzzlerelay.backend.UsedByJNI class * {*;}
-keepclassmembers class * {
    @io.github.nandomango702.stpuzzlerelay.backend.UsedByJNI <methods>;
}
-keepclasseswithmembers class * {
    @io.github.nandomango702.stpuzzlerelay.backend.UsedByJNI <methods>;
}
-keepclasseswithmembers class * {
    @io.github.nandomango702.stpuzzlerelay.backend.UsedByJNI <fields>;
}
-keepclasseswithmembers class * {
    @io.github.nandomango702.stpuzzlerelay.backend.UsedByJNI <init>(...);
}
-printusage /tmp/usage.txt