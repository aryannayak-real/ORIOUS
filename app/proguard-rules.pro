-keepattributes LineNumberTable,SourceFile
-renamesourcefileattribute SourceFile
-dontobfuscate

-keepclasseswithmembers public class androidx.recyclerview.widget.RecyclerView { *; }
-keep class app.orious.authenticator.ui.fragments.preferences.*
-keep class app.orious.authenticator.importers.** { *; }
-keep class * extends com.google.protobuf.GeneratedMessageLite { *; }

-dontwarn javax.naming.**
