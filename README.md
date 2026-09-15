# Anek Browser - Chrome-like Professional Android Browser

A polished, fast, production-quality Android web browser built from scratch with Kotlin, Jetpack Compose, and WebView. Chrome-like UX, smooth animations, and professional features.

![License](https://img.shields.io/badge/license-MIT-blue.svg)
![Min SDK](https://img.shields.io/badge/minSdk-24-green.svg)
![Target SDK](https://img.shields.io/badge/targetSdk-34-green.svg)
![Version](https://img.shields.io/badge/version-1.2.0-blue.svg)
![Kotlin](https://img.shields.io/badge/kotlin-1.9.22-purple.svg)
![Compose](https://img.shields.io/badge/compose-BOM%202024.09.02-blue.svg)

## 📱 Download the APK

**[Direct download — latest nightly build](https://github.com/ANEQH/Anek-browser/releases/download/nightly/Anek-Browser-debug.apk)**

Or browse all builds on the **[Releases page](https://github.com/ANEQH/Anek-browser/releases)**.

No GitHub login is required for the link above. Every push to `main` rebuilds and
replaces the `nightly` release automatically. Tagged versions (`v1.2.0`, …) get
their own permanent release with a signed, minified APK.

### Installing
1. Download the APK on your phone.
2. Android will warn about apps from unknown sources — allow it for your browser
   or file manager.
3. Tap the downloaded APK to install.

> **Where the APK was hiding before:** the build had always succeeded, but the
> output was only available as a GitHub Actions *artifact*. Artifacts sit behind
> a login wall, are wrapped in a ZIP, and are nested in a `debug/` folder — so
> the APK was effectively unreachable. It is now published to a Release, which
> is a plain public URL. See
> [`.github/workflows/build-apk.yml`](.github/workflows/build-apk.yml).

## ✨ What's New in v1.2.0

### Fixed
- **Back / Forward / Reload / Find-next were dead.** The screen held a WebView
  reference that was never assigned, so every navigation button silently did
  nothing.
- **Pages reloaded whenever you opened Settings, Tabs, History or Bookmarks.**
  The WebView was destroyed on navigation; the browser screen now stays mounted
  and the other screens render on top of it.
- **Scroll jank.** The view layer was writing navigation state on every
  recomposition, feeding a recomposition loop. It now only writes on change.
- **`target="_blank"` leaked a renderer per popup** and never actually opened a
  new tab.
- **Camera / mic / location never worked** — every site permission request was
  auto-denied. They now show a proper prompt.
- **Release APKs were unsigned** and therefore uninstallable. A signing config
  now exists, with a debug-keystore fallback when no keystore is configured.
- Tab restore raced with tab auto-save and could resurrect stale tabs.
- WebView remote debugging was **always on in release builds** (anyone with a
  USB cable could inspect your traffic). It is now an opt-in Developer option.
- `toolbar position`, `on startup` and `homepage` settings were saved but never
  applied; the search-engine picker rendered every option twice.

### Added
- **Ad & tracker blocking** at the network layer, with a live blocked counter
  and a custom blocklist that supports `@allow` exceptions.
- **User scripts** — inject your own JavaScript per-site. This is the realistic
  equivalent of extensions: Chrome/Edge extensions **cannot** run in Android's
  WebView because the platform has no extension runtime.
- **Developer options** — runtime/device/WebView diagnostics, remote debugging
  with `chrome://inspect` instructions, live console capture, performance
  overlay, page-source viewer, a JavaScript console, hard reload, feature flags
  and built-in test pages (video, WebGL, viewport, speed).
- **Data saver** — strips `utm_*`, `fbclid`, `gclid` and similar tracking params.
- **Image blocking**, **force-dark web content**, **keep screen on during video**.
- **WebView warm-up** behind the splash screen, removing the 300–800 ms provider
  load from the first page.

## ✨ What's New in v1.1.0 - Chrome-like Upgrade

- **Chrome-like Omnibox** with intelligent suggestions from history, bookmarks, search
- **Professional Toolbar** - top omnibox with tab counter, bottom navigation, smooth animations
- **Advanced Tab System** - persistence across restarts, swipe to close, tab restore, memory optimization
- **Room Database** - full Room implementation for history, bookmarks, shortcuts, downloads
- **Enhanced WebView Engine** - file upload support, permission handling, fullscreen video, safe browsing
- **Polished Home Page** - Chrome-like shortcuts, recent, bookmarks, privacy card
- **Upgraded UI/UX** - Material 3, dynamic colors, edge-to-edge, swipe gestures, animations
- **Smart Address Bar** - URL/search/IP/localhost detection, configurable engines, copy/paste/share
- **Download Manager** - improved progress, status, file type icons

## Features

### Chrome-like Browsing Experience
- **Smart Omnibox**: Intelligent detection of URLs, IPs, localhost, search queries with suggestions
- **Professional Toolbar**: Back, forward, reload/stop, home, tab counter, three-dot menu
- **Tab Management**: Create, close, swipe to close, duplicate, close all, restore, persistence
- **Private Tabs**: Separate incognito session, no history saving, clear visual distinction
- **Navigation**: Smooth back/forward, reload, home, share, find in page, desktop toggle

### Home Page (Offline Capable, Chrome-like)
- Centered search bar with mic icon
- Quick shortcuts with favicon initials, add/remove/long-press
- Recently visited with Chrome-like cards
- Bookmarks section
- Privacy info card
- Customizable wallpaper, dark/light/system theme
- Clean empty states with call-to-action

### WebView Engine (Secure & Full-Featured)
- JavaScript, DOM storage, cookies, third-party cookie controls
- Zoom controls, wide viewport, responsive
- **File uploads** via `onShowFileChooser` - Chrome-like file picker
- Multiple windows/popups safely handled
- Geolocation, camera/microphone permission prompts
- Download handling via DownloadManager
- SSL error handling with warning page (never blindly bypass)
- HTTP error handling with custom pages
- **Fullscreen video** with exit button, orientation handling
- Find in page with next/prev, match count
- Safe browsing enabled (Android O+)

### Tab System - Advanced
- Multiple tabs with counter badge
- Tab overview grid (adaptive)
- Create, close, swipe to dismiss, close all, close others
- Reopen closed tabs (20 saved)
- Duplicate tab
- Tab persistence via DataStore JSON - restore after restart
- Incognito tabs separated, not restored
- Memory optimization - only current tab WebView active
- Long-press actions, pinned tabs support

### Smart Address Bar (Omnibox)
- Detects: `https://example.com`, `example.com`, `192.168.1.1`, `localhost:3000`, `hello world`
- Search engines: Google, Bing, DuckDuckGo, Brave, Custom
- Suggestions: history, bookmarks, search, URL
- Clear button, paste and go, copy URL
- Secure indicator (lock icon for HTTPS)
- No ToS violation - local suggestions only

### History - Chrome-like
- URL, title, timestamp, favicon initial, visit count
- Grouped by Today, Yesterday, date
- Search, delete individual, delete selected, clear all
- Open in current/new tab, copy link
- Sticky headers, Material 3 cards

### Bookmarks - Professional
- Add, edit, delete
- Folders support, create folder, move bookmark
- Search, filter by folder
- Import/export placeholder
- Chrome-like UI with chips for folders

### Download Manager - Proper
- Filename, URL, progress, status, size, time
- File type icons (image, video, audio, PDF)
- Open, share, delete
- Uses Android DownloadManager
- Scoped storage, FileProvider

### Privacy & Security
- Incognito mode with clear explanation of limitations
- Clear browsing data, cookies, cache, history
- Third-party cookie controls
- JavaScript toggle
- Permission manager
- Do Not Track
- Safe browsing
- Network security config (no cleartext except localhost)
- No dangerous JS bridges
- Honest privacy disclosure

### UI/UX Polish - Chrome-like
- Material 3, dynamic colors, edge-to-edge, splash screen
- Smooth animations (fade, slide, expand), rounded components
- Dark/Light/System theme
- Accessibility support, proper touch targets (48dp)
- Bottom navigation when browsing, top omnibox always visible
- Swipe to dismiss tabs, pull to refresh? (WebView native)
- Professional error pages with illustrations

### Performance
- Fast startup with splash screen
- Lazy Compose lists, efficient state with derivedStateOf
- WebView cleanup in DisposableEffect
- Coroutines off main thread (IO dispatcher for DB)
- No ANRs, no memory leaks, state restoration
- Optimized for mid-range and low-end devices
- Tab persistence without keeping WebViews in memory

## Tech Stack

- **Language**: Kotlin 1.9.22 + Serialization
- **UI**: Jetpack Compose BOM 2024.09.02, Material 3, Animation, Extended Icons
- **Web**: Android WebView + WebKit 1.9.0
- **Architecture**: MVVM, Coroutines, StateFlow, Room, DataStore
- **Database**: Room 2.6.1 (history, bookmarks, shortcuts, downloads) + DataStore (settings, tabs)
- **Min SDK**: 24 (Android 7.0)
- **Target/Compile SDK**: 34 (Android 14)
- **Gradle**: Kotlin DSL, AGP 8.5.2, KSP 1.9.22-1.0.17

## Project Structure

```
app/
├── src/main/java/com/anek/browser/
│   ├── MainActivity.kt (Chrome-like nav)
│   ├── browser/
│   │   ├── BrowserViewModel.kt (tab persistence, omnibox suggestions)
│   │   ├── Tab.kt (PersistedTab for JSON)
│   │   ├── SearchEngine.kt
│   │   └── UrlUtils.kt (IP, localhost, domain detection)
│   ├── data/datastore/
│   │   └── SettingsDataStore.kt (tabs JSON + settings)
│   ├── database/
│   │   ├── AppDatabase.kt (Room)
│   │   ├── dao/ (HistoryDao, BookmarkDao, etc with @Dao)
│   │   └── entity/ (@Entity)
│   ├── ui/
│   │   ├── theme/ (Material3, dynamic colors)
│   │   ├── components/
│   │   │   ├── AddressBar.kt (ChromeOmnibox, ChromeTopBar, BrowserBottomBar)
│   │   │   ├── BrowserMenu.kt (Chrome-like bottom sheet)
│   │   │   └── WebViewComponent.kt (file chooser, fullscreen, permissions)
│   │   └── screens/
│   │       ├── HomeScreen.kt (Chrome-like)
│   │       ├── BrowserScreen.kt (Chrome-like with omnibox)
│   │       ├── TabsScreen.kt (swipe to dismiss, FAB)
│   │       ├── HistoryScreen.kt (grouped, selection)
│   │       ├── BookmarksScreen.kt (folders)
│   │       ├── DownloadsScreen.kt (file icons)
│   │       └── SettingsScreen.kt (complete sections)
│   └── utils/
│       ├── Constants.kt (v1.1.0)
│       └── Extensions.kt
├── src/main/res/
└── AndroidManifest.xml
```

## Build Instructions

### Local Build
```bash
git clone https://github.com/ANEQH/Anek-browser.git
cd Anek-browser
export ANDROID_HOME=$HOME/Android/Sdk
./gradlew assembleDebug --no-daemon -Dorg.gradle.jvmargs="-Xmx4g"

# APK: app/build/outputs/apk/debug/app-debug.apk
```

Low-memory (2GB RAM):
```bash
./gradlew assembleDebug --no-daemon -Dorg.gradle.jvmargs="-Xmx512m -XX:+UseSerialGC"
```

### Android Studio
1. Open project
2. Sync Gradle
3. Run on device API 24+

### GitHub Actions - Automatic APK

Workflow `.github/workflows/build-apk.yml` triggers on push to main:

1. Checkout
2. JDK 17, Android SDK 34
3. Cache Gradle
4. Build debug APK
5. Upload artifact **Anek-Browser-APK**

**Get APK:** GitHub → Actions → Latest successful workflow → Artifacts → **Anek-Browser-APK**

Release workflow `.github/workflows/release.yml` triggers on tag `v*`:

- Builds release APK
- Uploads **Anek-Browser-Release-APK**
- Creates GitHub Release with APK

## Permissions

| Permission | Purpose |
|------------|---------|
| INTERNET | Load pages |
| ACCESS_NETWORK_STATE | Offline detection |
| ACCESS_FINE/COARSE_LOCATION | Geolocation (runtime) |
| CAMERA, RECORD_AUDIO | WebRTC (runtime) |
| POST_NOTIFICATIONS | Downloads (Android 13+) |

No excessive permissions. Scoped storage + FileProvider.

## Privacy

- Incognito: local history not saved, but ISP/sites may still track. Not Tor.
- No data collection by app. Sites may collect.
- Safe browsing via WebView's native safe browsing (Android O+)
- Third-party cookie blocking via `setAcceptThirdPartyCookies`

## Quality Checklist

- [x] Gradle builds (Room + KSP)
- [x] Kotlin/Compose compiles
- [x] AndroidManifest valid
- [x] No missing resources
- [x] WebView lifecycle correct
- [x] Tabs with persistence and swipe
- [x] History grouped, search, delete
- [x] Bookmarks with folders
- [x] Downloads with icons
- [x] Incognito separate
- [x] Settings complete (General/Privacy/Browser/Advanced/About)
- [x] Dark mode, edge-to-edge, dynamic colors
- [x] GitHub Actions YAML valid, artifact **Anek-Browser-APK**
- [x] No secrets committed, no token exposed
- [x] No SSL bypass, no unsafe JS bridges
- [x] File upload support, fullscreen video

## Versioning

- v1.1.0 (2) - Chrome-like upgrade
- v1.0.0 (1) - Initial release

## License

MIT - see LICENSE

## Roadmap

- [ ] Reader mode
- [ ] Ad-block lists (optional)
- [ ] Sync
- [ ] Extensions API

---

**Anek Browser** - A real Chrome-like browser, not a WebView demo. Fast, secure, private.
