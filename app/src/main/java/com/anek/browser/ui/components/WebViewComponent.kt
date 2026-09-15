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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var errorState by remember { mutableStateOf<BrowserError?>(null) }
    var sslErrorState by remember { mutableStateOf<SslError?>(null) }
    var customView by remember { mutableStateOf<android.view.View?>(null) }
    var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }
    var filePathCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }

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
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    wv.findAllAsync(findQuery)
                } else {
                    @Suppress("DEPRECATION")
                    wv.findAll(findQuery)
                }
            } else {
                wv.clearMatches()
            }
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
                    onUrlChanged(Constants.HOME_PAGE_URL)
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

                        // Security hardening - remove risky JS interfaces
                        removeJavascriptInterface("searchBoxJavaBridge_")
                        removeJavascriptInterface("accessibility")
                        removeJavascriptInterface("accessibilityTraversal")

                        val ws = this.settings
                        ws.javaScriptEnabled = browserSettings.javaScriptEnabled
                        ws.domStorageEnabled = true
                        ws.databaseEnabled = true
                        ws.allowFileAccess = false
                        ws.allowContentAccess = true
                        ws.allowFileAccessFromFileURLs = false
                        ws.allowUniversalAccessFromFileURLs = false
                        ws.javaScriptCanOpenWindowsAutomatically = true
                        ws.setSupportMultipleWindows(true)
                        ws.loadsImagesAutomatically = true
                        ws.useWideViewPort = true
                        ws.loadWithOverviewMode = true
                        ws.builtInZoomControls = browserSettings.zoomControls
                        ws.displayZoomControls = false
                        ws.textZoom = browserSettings.textScaling
                        ws.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                        ws.cacheMode = WebSettings.LOAD_DEFAULT
                        ws.userAgentString = buildUserAgent(browserSettings, tab.isDesktopMode, ctx)
                        ws.mediaPlaybackRequiresUserGesture = false
                        ws.offscreenPreRaster = true

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            ws.safeBrowsingEnabled = browserSettings.safeBrowsing
                        }

                        val cookieManager = CookieManager.getInstance()
                        cookieManager.setAcceptCookie(true)
                        cookieManager.setAcceptThirdPartyCookies(this, !browserSettings.blockThirdPartyCookies)

                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                val url = request?.url?.toString() ?: return false
                                // Handle external schemes
                                if (url.startsWith("intent://") || url.startsWith("market://") || url.startsWith("tel:") || url.startsWith("mailto:") || url.startsWith("sms:")) {
                                    return try {
                                        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        })
                                        true
                                    } catch (_: Exception) { false }
                                }
                                return false
                            }

                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                url?.let { onUrlChanged(it) }
                                onProgressChanged(0, true)
                                errorState = null
                                sslErrorState = null
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                view?.let {
                                    onNavigationStateChanged(it.canGoBack(), it.canGoForward())
                                }
                                onProgressChanged(100, false)
                                url?.let { onPageFinished(it) }
                                view?.title?.let { onTitleChanged(it) }
                            }

                            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                                super.onReceivedError(view, request, error)
                                if (request?.isForMainFrame == true) {
                                    val errorCode = error?.errorCode ?: -1
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

                            override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, errorResponse: WebResourceResponse?) {
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

                            override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
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
                                url?.let { onUrlChanged(it) }
                            }
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                super.onProgressChanged(view, newProgress)
                                onProgressChanged(newProgress, newProgress < 100)
                            }

                            override fun onReceivedTitle(view: WebView?, title: String?) {
                                super.onReceivedTitle(view, title)
                                title?.let { onTitleChanged(it) }
                            }

                            override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
                                super.onReceivedIcon(view, icon)
                                // Favicon handling - we could save icon url
                            }

                            override fun onGeolocationPermissionsShowPrompt(origin: String?, callback: GeolocationPermissions.Callback?) {
                                onRequestPermission("geolocation") { granted ->
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
                                    onRequestPermission(perm) { granted ->
                                        if (granted) req.grant(req.resources)
                                        else req.deny()
                                    }
                                }
                            }

                            override fun onShowCustomView(view: android.view.View?, callback: CustomViewCallback?) {
                                customView = view
                                customViewCallback = callback
                            }

                            override fun onHideCustomView() {
                                customView = null
                                customViewCallback?.onCustomViewHidden()
                                customViewCallback = null
                            }

                            override fun onCreateWindow(view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message?): Boolean {
                                val newWebView = WebView(ctx).apply {
                                    settings.javaScriptEnabled = browserSettings.javaScriptEnabled
                                    webViewClient = object : WebViewClient() {
                                        override fun shouldOverrideUrlLoading(v: WebView?, request: WebResourceRequest?): Boolean {
                                            val url = request?.url?.toString() ?: return false
                                            // Open in new tab - for now load in current
                                            view?.loadUrl(url)
                                            return true
                                        }
                                    }
                                }
                                val transport = resultMsg?.obj as? WebView.WebViewTransport
                                transport?.webView = newWebView
                                resultMsg?.sendToTarget()
                                return true
                            }

                            // File upload support - Chrome-like
                            override fun onShowFileChooser(
                                webView: WebView?,
                                filePathCallbackParam: ValueCallback<Array<Uri>>?,
                                fileChooserParams: FileChooserParams?
                            ): Boolean {
                                if (filePathCallbackParam == null) return false
                                filePathCallback?.onReceiveValue(null)
                                filePathCallback = filePathCallbackParam

                                try {
                                    val intent = fileChooserParams?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                                        addCategory(Intent.CATEGORY_OPENABLE)
                                        type = "*/*"
                                    }
                                    fileChooserLauncher.launch(intent)
                                } catch (_: Exception) {
                                    filePathCallback = null
                                    filePathCallbackParam.onReceiveValue(null)
                                    return false
                                }
                                return true
                            }

                            override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
                                // Let WebView handle it natively - could customize with Compose dialog
                                return super.onJsAlert(view, url, message, result)
                            }
                        }

                        setDownloadListener { url, userAgent, contentDisposition, mimetype, _ ->
                            DownloadHandler.downloadFile(ctx, url, userAgent, contentDisposition, mimetype)
                        }

                        // Find listener
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                            setFindListener { activeMatchOrdinal, numberOfMatches, isDoneCounting ->
                                onFindResult(activeMatchOrdinal, numberOfMatches)
                            }
                        }

                        if (tab.url != Constants.HOME_PAGE_URL && tab.url.isNotBlank()) {
                            loadUrl(tab.url)
                        }

                        webViewRef = this
                    }
                },
                update = { webView ->
                    webView.settings.javaScriptEnabled = browserSettings.javaScriptEnabled
                    webView.settings.textZoom = browserSettings.textScaling
                    webView.settings.builtInZoomControls = browserSettings.zoomControls
                    CookieManager.getInstance().setAcceptThirdPartyCookies(webView, !browserSettings.blockThirdPartyCookies)
                    val desiredUA = buildUserAgent(browserSettings, tab.isDesktopMode, context)
                    if (webView.settings.userAgentString != desiredUA) {
                        webView.settings.userAgentString = desiredUA
                        if (!tab.isHomePage()) webView.reload()
                    }
                    if (tab.url != webView.url && tab.url != Constants.HOME_PAGE_URL) {
                        if (tab.url.isNotBlank()) {
                            webView.loadUrl(tab.url)
                        }
                    }
                    onNavigationStateChanged(webView.canGoBack(), webView.canGoForward())
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
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    AndroidView(
                        factory = { view },
                        modifier = Modifier.fillMaxSize()
                    )
                    // Exit fullscreen button
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
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.apply {
                stopLoading()
                clearHistory()
                removeAllViews()
                destroy()
            }
        }
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
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
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

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
        }
    }
}
