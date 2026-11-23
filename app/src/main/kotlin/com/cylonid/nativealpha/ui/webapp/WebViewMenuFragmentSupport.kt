package com.cylonid.nativealpha.ui.webapp

import android.os.Bundle
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.cylonid.nativealpha.model.DataManager
import com.cylonid.nativealpha.model.WebApp
import com.cylonid.nativealpha.ui.theme.NativeAlphaTheme

internal data class WebViewMenuArgs(
    val title: String = "",
    val url: String = "",
    val canGoBack: Boolean = true,
    val canGoForward: Boolean = false,
    val webAppId: Int = -1,
)

internal object WebViewMenuFragmentSupport {
    private const val ARG_TITLE = "arg_title"
    private const val ARG_URL = "arg_url"
    private const val ARG_CAN_GO_BACK = "arg_can_go_back"
    private const val ARG_CAN_GO_FORWARD = "arg_can_go_forward"
    private const val ARG_WEBAPP_ID = "arg_webapp_id"

    fun createArgsBundle(
        title: String,
        url: String,
        canGoBack: Boolean,
        canGoForward: Boolean,
        webApp: WebApp?,
    ): Bundle =
        Bundle().apply {
            putString(ARG_TITLE, title)
            putString(ARG_URL, url)
            putBoolean(ARG_CAN_GO_BACK, canGoBack)
            putBoolean(ARG_CAN_GO_FORWARD, canGoForward)
            putInt(ARG_WEBAPP_ID, webApp?.ID ?: -1)
        }

    fun readArgs(arguments: Bundle?): WebViewMenuArgs =
        if (arguments == null) {
            WebViewMenuArgs()
        } else {
            WebViewMenuArgs(
                title = arguments.getString(ARG_TITLE, ""),
                url = arguments.getString(ARG_URL, ""),
                canGoBack = arguments.getBoolean(ARG_CAN_GO_BACK, true),
                canGoForward = arguments.getBoolean(ARG_CAN_GO_FORWARD, false),
                webAppId = arguments.getInt(ARG_WEBAPP_ID, -1),
            )
        }

    inline fun <reified T : DialogFragment> showMenuFragment(
        fragmentManager: FragmentManager,
        tag: String,
        title: String,
        url: String,
        canGoBack: Boolean,
        canGoForward: Boolean,
        webApp: WebApp?,
        createFragment: (Bundle) -> T,
    ): T? {
        if (fragmentManager.isStateSaved) return null
        (fragmentManager.findFragmentByTag(tag) as? DialogFragment)?.dismissAllowingStateLoss()
        val args = createArgsBundle(title, url, canGoBack, canGoForward, webApp)
        val fragment = createFragment(args)
        fragment.show(fragmentManager, tag)
        return fragment
    }

    fun createMenuComposeView(
        fragment: DialogFragment,
        args: WebViewMenuArgs,
        content: @Composable (WebViewMenuArgs, WebApp?) -> Unit,
    ): View {
        val webApp = if (args.webAppId != -1) DataManager.getInstance().getWebApp(args.webAppId) else null
        return ComposeView(fragment.requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                NativeAlphaTheme {
                    content(args, webApp)
                }
            }
        }
    }
}
