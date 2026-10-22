# Keep Glance widget entry points referenced only from the manifest.
-keep class androidx.glance.** { *; }
-keep class com.devil.calendar.widget.** { *; }

# Room generated implementations.
-keep class * extends androidx.room.RoomDatabase { <init>(); }

# Hilt / Dagger
-dontwarn dagger.hilt.**

# Keep enum values used by DataStore serialization.
-keepclassmembers enum * { *; }
