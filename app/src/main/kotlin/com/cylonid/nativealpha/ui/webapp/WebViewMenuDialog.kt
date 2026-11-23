package com.cylonid.nativealpha.ui.webapp

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.cylonid.nativealpha.model.WebApp

class WebViewMenuDialogFragment : DialogFragment() {
    private var args: WebViewMenuArgs = WebViewMenuArgs()
    private var listener: WebViewMenuListener? = null

    companion object {
        private const val TAG = "WebViewMenuDialogFragment"

        @JvmStatic
        fun show(
            fragmentManager: FragmentManager,
            title: String,
            url: String,
            canGoBack: Boolean,
            canGoForward: Boolean,
            webApp: WebApp?,
            listener: WebViewMenuListener,
        ): WebViewMenuDialogFragment? =
            WebViewMenuFragmentSupport.showMenuFragment(
                fragmentManager = fragmentManager,
                tag = TAG,
                title = title,
                url = url,
                canGoBack = canGoBack,
                canGoForward = canGoForward,
                webApp = webApp,
            ) { bundle ->
                WebViewMenuDialogFragment().apply {
                    this.listener = listener
                    arguments = bundle
                }
            }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (listener == null && context is WebViewMenuListener) {
            listener = context
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        args = WebViewMenuFragmentSupport.readArgs(arguments)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            window.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
            window.setBackgroundDrawableResource(android.R.color.transparent)
            window.setDimAmount(0.35f)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View =
        WebViewMenuFragmentSupport.createMenuComposeView(this, args) { menuArgs, webApp ->
            WebViewMenuDialogContent(
                title = menuArgs.title,
                url = menuArgs.url,
                canGoBack = menuArgs.canGoBack,
                canGoForward = menuArgs.canGoForward,
                webApp = webApp,
                onBack = { listener?.onBack() },
                onForward = { listener?.onForward() },
                onReload = { listener?.onReload() },
                onShare = { listener?.onShare() },
                onCopyUrl = { listener?.onCopyUrl() },
                onMainMenu = { listener?.onMainMenu() },
                onFallbackContextMenu = { listener?.onFallbackContextMenu() },
                onCloseWebApp = { listener?.onCloseWebApp() },
                onDismiss = { dismiss() },
            )
        }
}

@Composable
fun WebViewMenuDialogContent(
    title: String,
    url: String,
    canGoBack: Boolean,
    canGoForward: Boolean,
    webApp: WebApp?,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onReload: () -> Unit,
    onShare: () -> Unit,
    onCopyUrl: () -> Unit,
    onMainMenu: () -> Unit,
    onFallbackContextMenu: () -> Unit,
    onCloseWebApp: () -> Unit,
    onDismiss: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .widthIn(max = 380.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            border =
                BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                ),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 14.dp),
            ) {
                WebViewMenuHeader(
                    title = title,
                    url = url,
                    webApp = webApp,
                    onDismiss = onDismiss,
                )

                WebViewMenuQuickBar(
                    canGoBack = canGoBack,
                    canGoForward = canGoForward,
                    onBack = onBack,
                    onReload = onReload,
                    onForward = onForward,
                    onShare = onShare,
                    onCopyUrl = onCopyUrl,
                    onDismiss = onDismiss,
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                )

                WebViewMenuActionList(
                    onMainMenu = onMainMenu,
                    onFallbackContextMenu = onFallbackContextMenu,
                    onCloseWebApp = onCloseWebApp,
                    onDismiss = onDismiss,
                )
            }
        }
    }
}
