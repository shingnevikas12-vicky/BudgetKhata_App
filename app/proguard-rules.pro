# Keep Room generated implementations
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep class com.budgetkhata.data.local.** { *; }

# Gson serialized backup models
-keep class com.budgetkhata.data.backup.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
