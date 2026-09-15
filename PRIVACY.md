# Privacy Policy - Anek Browser

**Last Updated: 2024**

## Overview

Anek Browser is designed with privacy in mind. This document explains what data is collected and how it is handled.

## Data Collection

**Anek Browser itself collects NO data.** We do not have servers, analytics, or tracking.

- No personal information collected
- No browsing history sent to any server
- No analytics, crash reporting (unless you opt-in via system)
- No ads, no trackers in the browser itself

## Local Storage

The following data is stored **locally on your device only**:

- Browsing history (if not in incognito)
- Bookmarks
- Shortcuts
- Downloads history
- Settings (search engine, theme, etc.)
- Cookies and site data (managed by WebView)

This data never leaves your device unless you explicitly export/share it.

## Permissions

- **INTERNET**: Required to load web pages
- **ACCESS_NETWORK_STATE**: Detect offline state
- **Location, Camera, Microphone**: Only if you grant per-site via WebView permission prompt. Browser does not access these directly.
- **POST_NOTIFICATIONS**: For download notifications (Android 13+)
- **Storage (legacy)**: For downloads on Android 9 and below

## Third-Party Services

When you browse websites, those websites may collect data per their own privacy policies. Anek Browser does not control third-party sites.

Search engines (Google, Bing, DuckDuckGo, Brave) have their own privacy policies when you search.

## Incognito Mode Limitations

Incognito/Private tabs:
- Do NOT save history, cookies, site data after closing
- Do NOT make you anonymous
- Your ISP, employer, network admin, and visited websites can still track you
- Does NOT hide your IP address
- Does NOT encrypt traffic beyond HTTPS

For true anonymity, use Tor Browser.

## Cookies

- First-party cookies: Enabled by default
- Third-party cookies: Can be blocked in Settings -> Privacy
- Cookies are managed by Android WebView and stored locally

## Do Not Track

If enabled in Settings, browser sends "DNT: 1" header. Websites may ignore it.

## Safe Browsing

Architecture is in place to warn about malicious sites. Full Google Safe Browsing API integration is planned but not yet implemented with API key. Current version does basic checks.

## Children's Privacy

App is not directed to children under 13. No data collected.

## Changes

We may update this policy. Check GitHub repository for latest version.

## Contact

Open an issue on GitHub: https://github.com/anek-browser/anek-browser/issues

## Open Source

Anek Browser is open source under MIT License. You can audit the code.
