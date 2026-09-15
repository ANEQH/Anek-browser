package com.anek.browser.ui.components

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Message
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.*
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.anek.browser.browser.Tab
import com.anek.browser.data.datastore.BrowserSettings
import com.anek.browser.downloads.DownloadHandler
import com.anek.browser.utils.Constants
import com.anek.browser.web.AdBlocker
import com.anek.browser.web.ConsoleLevel
import com.anek.browser.web.ConsoleLog
import com.anek.browser.web.UserScriptEngine

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewComponent(
    tab: Tab,
    browserSettings: BrowserSettings,
    onUrlChanged: (String) -> Unit,
    onTitleChanged: (String) -> Unit,
    onProgressChanged: (Int, Boolean) -> Unit,
    onNavigationStateChanged: (Boolean, Boolean) -> Unit,
    onIconChanged: (String?) -> Unit = {},
    onPageFinished: (String) -> Unit = {},
    onRequestPermission: (String, (Boolean) -> Unit) -> Unit = { _, cb -> cb(false) },
    findQuery: String = "",
    isFindActive: Boolean = false,
    onFindResult: (Int, Int) -> Unit = { _, _ -> },
    onShowFileChooser: ((ValueCallback<Array<Uri>>, Intent) -> Unit)? = null,
    // --- added for v1.2.0 ---
    onOpenNewTab: (String) -> Unit = {},
    onWebViewReady: (WebView?) -> Unit = {},
    onLoadStarted: () -> Unit = {},
    onLoadFinished: () -> Unit = {},
    customBlockDomains: Set<String> = emptySet(),
    scriptEngine: UserScriptEngine? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var errorState by remember { mutableStateOf<BrowserError?>(null) }
    var sslErrorState by remember { mutableStateOf<SslError?>(null) }
    var customView by remember { mutableStateOf<android.view.View?>(null) }
    var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }
    var filePathCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }

    /*
     * Mirrors of the settings/callbacks captured at factory time.
     *
     * A WebView's WebViewClient is installed once, in the AndroidView factory,
     * and that closure would otherwise pin the *first* values of these
     * parameters forever. Reading through these refs keeps the client current
     * without recreating the WebView (which would reload the page and lose all
     * state — the single biggest source of jank in the previous version).
     */
    val settingsRef = remember { mutableStateOf(browserSettings) }
    settingsRef.value = browserSettings
    val blockDomainsRef = remember { mutableStateOf(customBlockDomains) }
    blockDomainsRef.value = customBlockDomains
    val scriptsEnabledRef = remember { mutableStateOf(true) }
    scriptsEnabledRef.value = browserSettings.userscriptsEnabled
    val engineRef = remember { mutableStateOf(scriptEngine) }
    engineRef.value = scriptEngine
    val navStateRef = remember { mutableStateOf(onNavigationStateChanged) }
    navStateRef.value = onNavigationStateChanged
    val newTabRef = remember { mutableStateOf(onOpenNewTab) }
    newTabRef.value = onOpenNewTab
    val loadStartRef = remember { mutableStateOf(onLoadStarted) }
    loadStartRef.value = onLoadStarted
    val loadFinishRef = remember { mutableStateOf(onLoadFinished) }
    loadFinishRef.value = onLoadFinished
    val pageFinishedRef = remember { mutableStateOf(onPageFinished) }
    pageFinishedRef.value = onPageFinished
    val titleRef = remember { mutableStateOf(onTitleChanged) }
    titleRef.value = onTitleChanged
    val urlRef = remember { mutableStateOf(onUrlChanged) }
    urlRef.value = onUrlChanged
    val progressRef = remember { mutableStateOf(onProgressChanged) }
    progressRef.value = onProgressChanged
    val permissionRef = remember { mutableStateOf(onRequestPermission) }
    permissionRef.value = onRequestPermission
    val findResultRef = remember { mutableStateOf(onFindResult) }
    findResultRef.value = onFindResult

    val fileChooserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        val uris = when {
            result.resultCode != Activity.RESULT_OK -> null
            data?.clipData != null -> {
                Array(data.clipData!!.itemCount) { i ->
                    data.clipData!!.getItemAt(i).uri
                }
            }
            data?.data != null -> arrayOf(data.data!!)
            else -> null
        }
        filePathCallback?.onReceiveValue(uris)
        filePathCallback = null
    }

    LaunchedEffect(findQuery, isFindActive) {
        webViewRef?.let { wv ->
            if (isFindActive && findQuery.isNotBlank()) {
                wv.findAllAsync(findQuery)
            } else {
                wv.clearMatches()
            }
        }
    }

    // Keep the screen awake while a video is fullscreen, per the user's setting.
    val activity = context as? Activity
    LaunchedEffect(customView, browserSettings.keepScreenOnVideo) {
        val window = activity?.window ?: return@LaunchedEffect
        if (customView != null && browserSettings.keepScreenOnVideo) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    BackHandler(enabled = isFindActive || customView != null) {
        if (customView != null) {
            customView = null
            customViewCallback?.onCustomViewHidden()
            customViewCallback = null
        } else if (isFindActive) {
            webViewRef?.clearMatches()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (errorState != null) {
            ErrorPage(
                error = errorState!!,
                onRetry = {
                    errorState = null
                    webViewRef?.reload()
                },
                onHome = {
                    errorState = null
                    urlRef.value(Constants.HOME_PAGE_URL)
                },
                onBack = {
                    errorState = null
                    if (webViewRef?.canGoBack() == true) webViewRef?.goBack()
                }
            )
        } else {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )

                        // Security hardening - remove risky legacy JS bridges.
                        removeJavascriptInterface("searchBoxJavaBridge_")
                        removeJavascriptInterface("accessibility")
                        removeJavascriptInterface("accessibilityTraversal")

                        // Unique per-WebView data directory would be needed for
                        // true multi-process isolation; a single shared profile is
                        // what a normal browser wants, so keep the default.
                        isHorizontalScrollBarEnabled = false
                        overScrollMode = android.view.View.OVER_SCROLL_IF_CONTENT_SCROLLS

                        val s = settingsRef.value
                        val ws = this.settings
                        ws.javaScriptEnabled = s.javaScriptEnabled
                        ws.domStorageEnabled = true
                        ws.databaseEnabled = true
                        ws.allowFileAccess = false
                        ws.allowContentAccess = true
                        @Suppress("DEPRECATION")
                        ws.allowFileAccessFromFileURLs = false
                        @Suppress("DEPRECATION")
                        ws.allowUniversalAccessFromFileURLs = false
                        ws.javaScriptCanOpenWindowsAutomatically = true
                        ws.setSupportMultipleWindows(true)
                        ws.loadsImagesAutomatically = s.imagesEnabled
                        ws.blockNetworkImage = !s.imagesEnabled
                        ws.useWideViewPort = true
                        ws.loadWithOverviewMode = true
                        ws.builtInZoomControls = s.zoomControls
                        ws.displayZoomControls = false
                        ws.textZoom = s.textScaling
                        ws.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                        ws.cacheMode = WebSettings.LOAD_DEFAULT
                        ws.userAgentString = buildUserAgent(s, tab.isDesktopMode, ctx)
                        // Allows autoplay so embedded video actually starts.
                        ws.mediaPlaybackRequiresUserGesture = false
                        // Renders off-screen tiles ahead of scroll: smoother scrolling.
                        ws.offscreenPreRaster = true
                        ws.setSupportZoom(true)
                        ws.javaScriptCanOpenWindowsAutomatically = true

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            ws.safeBrowsingEnabled = s.safeBrowsing
                        }
                        applyForceDark(this, s)

                        val cookieManager = CookieManager.getInstance()
                        cookieManager.setAcceptCookie(true)
                        cookieManager.setAcceptThirdPartyCookies(this, !s.blockThirdPartyCookies)

                        webViewClient = object : WebViewClient() {

                            override fun shouldInterceptRequest(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): WebResourceResponse? {
                                val url = request?.url?.toString() ?: return null
                                val st = settingsRef.value
                                val isMain = request.isForMainFrame

                                // Never filter the top-level document or internal schemes.
                                if (!isMain && (st.adBlockEnabled || st.trackerBlockEnabled ||
                                        blockDomainsRef.value.isNotEmpty())
                                ) {
                                    if (AdBlocker.shouldBlock(
                                            url,
                                            st.adBlockEnabled,
                                            st.trackerBlockEnabled,
                                            blockDomainsRef.value
                                        )
                                    ) {
                                        return AdBlocker.emptyResponse(url)
                                    }
                                }
                                return super.shouldInterceptRequest(view, request)
                            }

                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                val url = request?.url?.toString() ?: return false

                                // External schemes go to the system.
                                if (!url.startsWith("http://", true) && !url.startsWith("https://", true)) {
                                    val scheme = url.substringBefore(":", "").lowercase()
                                    val external = scheme in setOf(
                                        "intent", "market", "tel", "mailto", "sms",
                                        "geo", "whatsapp", "upi", "youtube", "tg"
                                    )
                                    if (external) {
                                        return try {
                                            val intent = if (url.startsWith("intent://")) {
                                                Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                                            } else {
                                                Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                            }
                                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            ctx.startActivity(intent)
                                            true
                                        } catch (_: Exception) {
                                            false
                                        }
                                    }
                                    // Unknown scheme: swallow it rather than erroring.
                                    return true
                                }

                                if (request.isRedirect) return false

                                val target = if (settingsRef.value.dataSaver) stripTrackingParams(url) else url
                                if (target != url) {
                                    view?.loadUrl(target)
                                    return true
                                }
                                return false
                            }

                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                url?.let { urlRef.value(it) }
                                progressRef.value(0, true)
                                loadStartRef.value()
                                errorState = null
                                sslErrorState = null

                                // Early user scripts: run before the page settles.
                                // NB: `this` here is the WebViewClient, so the
                                // WebView must come from the callback parameter.
                                if (scriptsEnabledRef.value && url != null && view != null) {
                                    engineRef.value?.inject(view, url, atEnd = false)
                                }
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                view?.let { navStateRef.value(it.canGoBack(), it.canGoForward()) }
                                progressRef.value(100, false)
                                url?.let { pageFinishedRef.value(it) }
                                view?.title?.let { titleRef.value(it) }
                                loadFinishRef.value()

                                // Late user scripts: DOM is ready.
                                if (scriptsEnabledRef.value && url != null && view != null) {
                                    engineRef.value?.inject(view, url, atEnd = true)
                                }
                            }

                            override fun onReceivedError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                error: WebResourceError?
                            ) {
                                super.onReceivedError(view, request, error)
                                if (request?.isForMainFrame == true) {
                                    val errorCode = error?.errorCode ?: -1
                                    // ERR_UNKNOWN_URL_SCHEME / blocked sub-frames are not page errors.
                                    val description = error?.description?.toString() ?: "Unknown error"
                                    val failingUrl = request.url.toString()
                                    errorState = BrowserError(
                                        code = errorCode,
                                        description = description,
                                        url = failingUrl,
                                        type = when (errorCode) {
                                            ERROR_HOST_LOOKUP -> ErrorType.DNS
                                            ERROR_TIMEOUT -> ErrorType.TIMEOUT
                                            ERROR_CONNECT, ERROR_FAILED_SSL_HANDSHAKE -> ErrorType.CONNECTION
                                            else -> ErrorType.GENERIC
                                        }
                                    )
                                }
                            }

                            override fun onReceivedHttpError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                errorResponse: WebResourceResponse?
                            ) {
                                super.onReceivedHttpError(view, request, errorResponse)
                                if (request?.isForMainFrame == true) {
                                    val status = errorResponse?.statusCode ?: 0
                                    if (status >= 400) {
                                        errorState = BrowserError(
                                            code = status,
                                            description = "HTTP $status",
                                            url = request.url.toString(),
                                            type = ErrorType.HTTP
                                        )
                                    }
                                }
                            }

                            override fun onReceivedSslError(
                                view: WebView?,
                                handler: SslErrorHandler?,
                                error: SslError?
                            ) {
                                sslErrorState = error
                                errorState = BrowserError(
                                    code = -11,
                                    description = "SSL Certificate error: ${error?.primaryError}",
                                    url = error?.url ?: view?.url ?: "",
                                    type = ErrorType.SSL,
                                    sslError = error,
                                    sslHandler = handler
                                )
                            }

                            override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                                super.doUpdateVisitedHistory(view, url, isReload)
                                url?.let { urlRef.value(it) }
                            }
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                super.onProgressChanged(view, newProgress)
                                progressRef.value(newProgress, newProgress < 100)
                            }

                            override fun onReceivedTitle(view: WebView?, title: String?) {
                                super.onReceivedTitle(view, title)
                                title?.let { titleRef.value(it) }
                            }

                            override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
                                super.onReceivedIcon(view, icon)
                            }

                            override fun onConsoleMessage(message: ConsoleMessage?): Boolean {
                                if (settingsRef.value.captureConsole && message != null) {
                                    ConsoleLog.add(
                                        level = when (message.messageLevel()) {
                                            ConsoleMessage.MessageLevel.ERROR -> ConsoleLevel.ERROR
                                            ConsoleMessage.MessageLevel.WARNING -> ConsoleLevel.WARN
                                            ConsoleMessage.MessageLevel.TIP -> ConsoleLevel.TIP
                                            else -> ConsoleLevel.LOG
                                        },
                                        message = message.message() ?: "",
                                        source = "${message.sourceId()}:${message.lineNumber()}"
                                    )
                                }
                                return super.onConsoleMessage(message)
                            }

                            override fun onGeolocationPermissionsShowPrompt(
                                origin: String?,
                                callback: GeolocationPermissions.Callback?
                            ) {
                                permissionRef.value("geolocation") { granted ->
                                    if (granted) callback?.invoke(origin, true, false)
                                    else callback?.invoke(origin, false, false)
                                }
                            }

                            override fun onPermissionRequest(request: PermissionRequest?) {
                                request?.let { req ->
                                    val resources = req.resources
                                    val needsVideo = resources.contains(PermissionRequest.RESOURCE_VIDEO_CAPTURE)
                                    val needsAudio = resources.contains(PermissionRequest.RESOURCE_AUDIO_CAPTURE)
                                    val perm = when {
                                        needsVideo && needsAudio -> "camera_mic"
                                        needsVideo -> "camera"
                                        needsAudio -> "mic"
                                        else -> "other"
                                    }
                                    permissionRef.value(perm) { granted ->
                                        if (granted) req.grant(req.resources)
                                        else req.deny()
                                    }
                                }
                            }

                            override fun onShowCustomView(view: android.view.View?, callback: CustomViewCallback?) {
                                // An existing fullscreen view must be dismissed first,
                                // otherwise the video surface stays stuck on screen.
                                if (customView != null) {
                                    callback?.onCustomViewHidden()
                                    return
                                }
                                customView = view
                                customViewCallback = callback
                            }

                            override fun onHideCustomView() {
                                customView = null
                                customViewCallback?.onCustomViewHidden()
                                customViewCallback = null
                            }

                            /**
                             * Handles target="_blank" / window.open().
                             *
                             * The previous version created a WebView, handed it to
                             * the transport and then never destroyed it — a leak of
                             * a full renderer per popup. Here the temporary WebView
                             * exists only long enough to learn the URL, then it is
                             * destroyed and the URL is opened as a real new tab.
                             */
                            override fun onCreateWindow(
                                view: WebView?,
                                isDialog: Boolean,
                                isUserGesture: Boolean,
                                resultMsg: Message?
                            ): Boolean {
                                if (!isUserGesture) return false
                                val transport = resultMsg?.obj as? WebView.WebViewTransport ?: return false

                                val temp = WebView(ctx)
                                temp.webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(
                                        v: WebView?,
                                        request: WebResourceRequest?
                                    ): Boolean {
                                        val url = request?.url?.toString()
                                        runCatching { temp.destroy() }
                                        if (!url.isNullOrBlank()) newTabRef.value(url)
                                        return true
                                    }
                                }
                                transport.webView = temp
                                resultMsg.sendToTarget()
                                return true
                            }

                            override fun onShowFileChooser(
                                webView: WebView?,
                                filePathCallbackParam: ValueCallback<Array<Uri>>?,
                                fileChooserParams: FileChooserParams?
                            ): Boolean {
                                if (filePathCallbackParam == null) return false
                                filePathCallback?.onReceiveValue(null)
                                filePathCallback = filePathCallbackParam

                                try {
                                    val intent = fileChooserParams?.createIntent()
                                        ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                                            addCategory(Intent.CATEGORY_OPENABLE)
                                            type = "*/*"
                                        }
                                    // Offer the camera as well, like Chrome does.
                                    val chooser = Intent(Intent.ACTION_CHOOSER).apply {
                                        putExtra(Intent.EXTRA_INTENT, intent)
                                        putExtra(Intent.EXTRA_TITLE, "Choose an action")
                                    }
                                    fileChooserLauncher.launch(chooser)
                                } catch (_: Exception) {
                                    filePathCallback = null
                                    filePathCallbackParam.onReceiveValue(null)
                                    return false
                                }
                                return true
                            }
                        }

                        setDownloadListener { url, userAgent, contentDisposition, mimetype, _ ->
                            DownloadHandler.downloadFile(ctx, url, userAgent, contentDisposition, mimetype)
                        }

                        setFindListener { activeMatchOrdinal, numberOfMatches, _ ->
                            findResultRef.value(activeMatchOrdinal, numberOfMatches)
                        }

                        webViewRef = this
                        onWebViewReady(this)

                        if (tab.url != Constants.HOME_PAGE_URL && tab.url.isNotBlank()) {
                            loadUrl(tab.url)
                        }
                    }
                },
                update = { webView ->
                    val s = browserSettings
                    val ws = webView.settings

                    if (ws.javaScriptEnabled != s.javaScriptEnabled) ws.javaScriptEnabled = s.javaScriptEnabled
                    if (ws.textZoom != s.textScaling) ws.textZoom = s.textScaling
                    if (ws.builtInZoomControls != s.zoomControls) ws.builtInZoomControls = s.zoomControls
                    if (ws.loadsImagesAutomatically != s.imagesEnabled) {
                        ws.loadsImagesAutomatically = s.imagesEnabled
                        ws.blockNetworkImage = !s.imagesEnabled
                    }
                    CookieManager.getInstance().setAcceptThirdPartyCookies(webView, !s.blockThirdPartyCookies)
                    applyForceDark(webView, s)

                    val desiredUA = buildUserAgent(s, tab.isDesktopMode, context)
                    if (ws.userAgentString != desiredUA) {
                        ws.userAgentString = desiredUA
                        if (!tab.isHomePage()) webView.reload()
                    }

                    /*
                     * Only navigate when the requested URL genuinely differs from
                     * what this WebView is showing. The old check compared against
                     * `webView.url`, which is null during redirects and for
                     * about:blank, causing spurious reloads mid-navigation.
                     */
                    if (tab.url.isNotBlank() && tab.url != Constants.HOME_PAGE_URL) {
                        val current = webView.url
                        val sameAsCurrent = current != null && urlsEquivalent(current, tab.url)
                        val originalMatches = webView.originalUrl?.let { urlsEquivalent(it, tab.url) } == true
                        if (!sameAsCurrent && !originalMatches && webView.progress >= 100) {
                            webView.loadUrl(tab.url)
                        }
                    }

                    /*
                     * Publish navigation state only when it actually changed.
                     * Previously this ran on every recomposition and wrote to the
                     * ViewModel's StateFlow each time, which re-triggered
                     * recomposition — a feedback loop that showed up as scroll
                     * jank and dropped frames.
                     */
                    val canBack = webView.canGoBack()
                    val canForward = webView.canGoForward()
                    if (canBack != tab.canGoBack || canForward != tab.canGoForward) {
                        navStateRef.value(canBack, canForward)
                    }
                },
                onRelease = { webView ->
                    webView.stopLoading()
                    webView.webChromeClient = null
                    @Suppress("DEPRECATION")
                    webView.webViewClient = WebViewClient()
                    webView.loadUrl("about:blank")
                    webView.clearHistory()
                    webView.removeAllViews()
                    webView.destroy()
                    webViewRef = null
                    onWebViewReady(null)
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Fullscreen video overlay
        AnimatedVisibility(
            visible = customView != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            customView?.let { view ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(android.graphics.Color.BLACK)
                ) {
                    AndroidView(
                        factory = { view },
                        modifier = Modifier.fillMaxSize()
                    )
                    IconButton(
                        onClick = {
                            customView = null
                            customViewCallback?.onCustomViewHidden()
                            customViewCallback = null
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Exit fullscreen",
                            tint = androidx.compose.ui.graphics.Color.White
                        )
                    }
                }
            }
        }
    }
}

/** `true` when two URLs point at the same resource, ignoring fragment and trailing slash. */
private fun urlsEquivalent(a: String, b: String): Boolean {
    fun norm(u: String) = u.substringBefore('#').trimEnd('/').lowercase()
    return norm(a) == norm(b)
}

/**
 * Applies WebView's darkening of web content.
 *
 * `setAlgorithmicDarkeningAllowed` (API 33+ / Chrome 105+) supersedes the
 * deprecated `setForceDark`. Both are wrapped because a missing or outdated
 * WebView provider throws rather than no-ops.
 */
@Suppress("DEPRECATION")
private fun applyForceDark(webView: WebView, s: BrowserSettings) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            androidx.webkit.WebViewCompat.setAlgorithmicDarkeningAllowed(webView, s.forceDarkWebContent)
        } else {
            androidx.webkit.WebViewCompat.setForceDark(
                webView,
                if (s.forceDarkWebContent) androidx.webkit.WebSettingsCompat.FORCE_DARK_ON
                else androidx.webkit.WebSettingsCompat.FORCE_DARK_OFF
            )
        }
    } catch (_: Throwable) {
        // Older providers may not implement either API.
    }
}

/** Common tracking parameters, stripped when Data saver is on. */
private val TRACKING_PARAMS = setOf(
    "utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content", "utm_id",
    "fbclid", "gclid", "gclsrc", "dclid", "msclkid", "yclid", "twclid",
    "igshid", "mc_cid", "mc_eid", "s_kwcid", "_hsenc", "_hsmi", "vero_id",
    "ref_src", "ref_url", "cmpid", "pk_campaign", "pk_kwd", "trk", "trkCampaign"
)

private fun stripTrackingParams(url: String): String {
    if (!url.contains('?')) return url
    return try {
        val uri = Uri.parse(url)
        val query = uri.query ?: return url
        val kept = query.split('&').filter { pair ->
            val key = pair.substringBefore('=').lowercase()
            key.isNotEmpty() && key !in TRACKING_PARAMS
        }
        val base = url.substringBefore('?')
        if (kept.isEmpty()) base else "$base?${kept.joinToString("&")}"
    } catch (_: Exception) {
        url
    }
}

private fun buildUserAgent(settings: BrowserSettings, isDesktop: Boolean, context: Context): String {
    val defaultUA = WebSettings.getDefaultUserAgent(context)
    val custom = settings.customUserAgent
    if (custom.isNotBlank()) return custom
    return if (isDesktop) {
        Constants.DESKTOP_USER_AGENT
    } else {
        if (defaultUA.contains("AnekBrowser")) defaultUA
        else defaultUA + Constants.MOBILE_USER_AGENT_SUFFIX
    }
}

data class BrowserError(
    val code: Int,
    val description: String,
    val url: String,
    val type: ErrorType,
    val sslError: SslError? = null,
    val sslHandler: SslErrorHandler? = null
)

enum class ErrorType {
    DNS, TIMEOUT, CONNECTION, HTTP, SSL, GENERIC
}

@Composable
fun ErrorPage(
    error: BrowserError,
    onRetry: () -> Unit,
    onHome: () -> Unit,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))
            Icon(
                imageVector = when (error.type) {
                    ErrorType.SSL -> Icons.Default.Lock
                    ErrorType.DNS -> Icons.Default.Search
                    ErrorType.TIMEOUT -> Icons.Default.HourglassEmpty
                    ErrorType.HTTP -> Icons.Default.Error
                    else -> Icons.Default.Warning
                },
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = when (error.type) {
                    ErrorType.SSL -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = when (error.type) {
                    ErrorType.SSL -> "Your connection is not private"
                    ErrorType.DNS -> "This site can't be reached"
                    ErrorType.TIMEOUT -> "Connection timed out"
                    ErrorType.HTTP -> "This page isn't working"
                    else -> "Unable to load page"
                },
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = when (error.type) {
                    ErrorType.SSL -> "Attackers might be trying to steal your information from ${error.url.take(30)} (for example, passwords, messages, or credit cards)."
                    ErrorType.DNS -> "DNS_PROBE_FINISHED_NXDOMAIN"
                    ErrorType.TIMEOUT -> "The server took too long to respond."
                    ErrorType.HTTP -> error.description
                    else -> error.description
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = error.url.take(80),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(24.dp))

            if (error.type == ErrorType.SSL) {
                Text(
                    "This site's certificate is not trusted. Proceeding may be unsafe.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onBack) { Text("Back to safety") }
                    Button(
                        onClick = { error.sslHandler?.proceed() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("Proceed anyway") }
                }
                Spacer(Modifier.height(12.dp))
            }

            // Wrapped so three buttons never overflow a narrow (≤360dp) screen.
            FlowRowCompat {
                OutlinedButton(onClick = onBack) { Text("Back") }
                FilledTonalButton(onClick = onHome) { Text("Home") }
                Button(onClick = onRetry) { Text("Reload") }
            }

            Spacer(Modifier.height(24.dp))
            Text(
                "Error code: ${error.code}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** A row that wraps instead of clipping on small phones. */
@Composable
private fun FlowRowCompat(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        content = content
    )
}
