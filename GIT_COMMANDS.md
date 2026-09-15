# Git Commands to Publish Anek Browser

## Initialize and Push to GitHub

```bash
# Initialize git repository
git init

# Add all files
git add .

# Initial commit
git commit -m "Initial commit: Anek Browser v1.0.0 - Complete production-ready Android browser

Features:
- Smart address bar with URL/search detection
- Multi-tab with incognito support
- Material 3 UI with dark/light themes
- WebView engine with secure SSL handling
- History, bookmarks, downloads
- Privacy controls, find in page, desktop mode
- Custom error pages, offline homepage
- GitHub Actions APK build"

# Set main branch
git branch -M main

# Add remote (replace with your GitHub repo URL)
git remote add origin https://github.com/YOUR_USERNAME/anek-browser.git

# Push to GitHub
git push -u origin main
```

## After Push - Build APK via GitHub Actions

1. Go to your GitHub repository
2. Click "Actions" tab
3. Select "Build Debug APK" workflow
4. Wait for build to complete (2-3 minutes)
5. Download artifact "anek-browser-debug-apk"
6. Install APK on device: `adb install app-debug.apk`

## Versioning

To update version:
- Edit `app/build.gradle.kts`:
  - `versionCode = 2`
  - `versionName = "1.0.1"`
- Commit and push, new APK will be built automatically

## Release Tags

```bash
git tag v1.0.0
git push origin v1.0.0
# Triggers release workflow
```
