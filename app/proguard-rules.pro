# ---------------------------------------------------------------------------
# WebView
# ---------------------------------------------------------------------------
-keepclassmembers class * extends android.webkit.WebViewClient {
    public void *(android.webkit.WebView, java.lang.String, android.graphics.Bitmap);
    public boolean *(android.webkit.WebView, java.lang.String);
    public void *(android.webkit.WebView, java.lang.String);
}
-keepclassmembers class * extends android.webkit.WebChromeClient {
    public void *(android.webkit.WebView, java.lang.String);
}
# Any @JavascriptInterface method must survive shrinking or the bridge silently
# disappears at runtime (used by the user-script engine and dev tools).
-keepattributes JavascriptInterface
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keepclassmembers class com.anek.browser.** {
    @android.webkit.JavascriptInterface <methods>;
}

# ---------------------------------------------------------------------------
# Room
# ---------------------------------------------------------------------------
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-dontwarn androidx.room.paging.**

# ---------------------------------------------------------------------------
# Coroutines
# ---------------------------------------------------------------------------
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.**

# ---------------------------------------------------------------------------
# DataStore
# ---------------------------------------------------------------------------
-keep class androidx.datastore.** { *; }

# ---------------------------------------------------------------------------
# Model classes
# ---------------------------------------------------------------------------
-keep class com.anek.browser.database.entity.** { *; }
-keep class com.anek.browser.data.** { *; }

# ---------------------------------------------------------------------------
# kotlinx.serialization
#
# Tab persistence serialises `PersistedTab`. Without these rules R8 strips the
# generated `$serializer` companion and restoring tabs crashes on a release
# build with NoSuchMethodError / SerializationException.
# ---------------------------------------------------------------------------
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.anek.browser.**$$serializer { *; }
-keepclassmembers class com.anek.browser.** {
    *** Companion;
}
-keepclasseswithmembers class com.anek.browser.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# ---------------------------------------------------------------------------
# AndroidX WebKit
# ---------------------------------------------------------------------------
-keep class androidx.webkit.** { *; }

# ---------------------------------------------------------------------------
# Compose
#
# Compose ships its own consumer ProGuard rules, so a blanket
# `-keep class androidx.compose.** { *; }` is unnecessary and defeats shrinking.
# Keep only the reflective entry points that actually matter.
# ---------------------------------------------------------------------------
-keep class androidx.compose.runtime.** { *; }
-keepclassmembers class androidx.compose.ui.** {
    @androidx.compose.runtime.Composable <methods>;
}
-dontwarn androidx.compose.**
