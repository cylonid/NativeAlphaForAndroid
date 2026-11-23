package com.cylonid.nativealpha.ui.webapp

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentManager
import com.cylonid.nativealpha.model.WebApp
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class WebViewMenuBottomSheetDialogFragment : BottomSheetDialogFragment() {
    private var args: WebViewMenuArgs = WebViewMenuArgs()
    private var listener: WebViewMenuListener? = null

    companion object {
        private const val TAG = "WebViewMenuBottomSheetDialogFragment"

        @JvmStatic
        fun show(
            fragmentManager: FragmentManager,
            title: String,
            url: String,
            canGoBack: Boolean,
            canGoForward: Boolean,
            webApp: WebApp?,
            listener: WebViewMenuListener,
        ): WebViewMenuBottomSheetDialogFragment? =
            WebViewMenuFragmentSupport.showMenuFragment(
                fragmentManager = fragmentManager,
                tag = TAG,
                title = title,
                url = url,
                canGoBack = canGoBack,
                canGoForward = canGoForward,
                webApp = webApp,
            ) { bundle ->
                WebViewMenuBottomSheetDialogFragment().apply {
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
        (dialog as? BottomSheetDialog)?.let { bottomSheetDialog ->
            bottomSheetDialog.behavior.apply {
                state = BottomSheetBehavior.STATE_EXPANDED
                skipCollapsed = true
            }
            bottomSheetDialog.window?.let { window ->
                WindowCompat.setDecorFitsSystemWindows(window, false)
            }
            bottomSheetDialog
                .findViewById<View>(
                    com.google.android.material.R.id.design_bottom_sheet,
                )?.let { bottomSheet ->
                    bottomSheet.background = null
                    bottomSheet.setPadding(0, 0, 0, 0)
                }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View =
        WebViewMenuFragmentSupport.createMenuComposeView(this, args) { menuArgs, webApp ->
            WebViewMenuBottomSheetContent(
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
fun WebViewMenuBottomSheetContent(
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
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 6.dp,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp),
        ) {
            // Drag Handle
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier =
                        Modifier
                            .width(36.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)),
                )
            }

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
