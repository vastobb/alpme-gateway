# Keep NanoHTTPD and kotlinx.serialization generated serializers.
-keep class fi.iki.elonen.** { *; }
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class **$$serializer { *; }
