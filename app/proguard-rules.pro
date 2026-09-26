# ---------------------------------------------------------------------------
# Media3 / ExoPlayer
# ---------------------------------------------------------------------------
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Media3 session keeps the controller/service across processes via reflection.
-keep class com.localmusic.player.playback.PlaybackService { *; }

# ---------------------------------------------------------------------------
# Room
# ---------------------------------------------------------------------------
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# ---------------------------------------------------------------------------
# Hilt / Dagger
# ---------------------------------------------------------------------------
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep,allowobfuscation @interface dagger.hilt.android.lifecycle.HiltViewModel
-keep class * extends androidx.lifecycle.ViewModel { <init>(...); }

# ---------------------------------------------------------------------------
# Kotlin coroutines
# ---------------------------------------------------------------------------
-dontwarn kotlinx.coroutines.**
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }

# ---------------------------------------------------------------------------
# Glance widget
# ---------------------------------------------------------------------------
-keep class * extends androidx.glance.appwidget.GlanceAppWidgetReceiver { *; }
-keep class * extends androidx.glance.appwidget.GlanceAppWidget { *; }
-keep class * implements androidx.glance.appwidget.action.ActionCallback { *; }

# ---------------------------------------------------------------------------
# Data classes used by reflection / serialization
# ---------------------------------------------------------------------------
-keepclassmembers class com.localmusic.player.data.db.** { *; }

# Keep enum values used through name() lookups.
-keepclassmembers enum * { *; }
