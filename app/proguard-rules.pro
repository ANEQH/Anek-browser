# Keep WebView related classes
-keepclassmembers class * extends android.webkit.WebViewClient {
    public void *(android.webkit.WebView, java.lang.String, android.graphics.Bitmap);
    public boolean *(android.webkit.WebView, java.lang.String);
}
-keepclassmembers class * extends android.webkit.WebChromeClient {
    public void *(android.webkit.WebView, java.lang.String);
}

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# DataStore
-keep class androidx.datastore.** { *; }

# Keep model classes
-keep class com.anek.browser.database.entity.** { *; }
-keep class com.anek.browser.data.** { *; }

# Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

# Webkit
-keep class androidx.webkit.** { *; }

# Compose
-keep class androidx.compose.** { *; }
