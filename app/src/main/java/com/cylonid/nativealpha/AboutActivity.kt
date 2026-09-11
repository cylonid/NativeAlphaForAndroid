package com.cylonid.nativealpha

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import com.cylonid.nativealpha.databinding.ActivityToolbarBaseBinding
import com.cylonid.nativealpha.util.ColorUtils.getColorResFromThemeAttr
import com.mikepenz.aboutlibraries.LibsBuilder
import mehdi.sakout.aboutpage.AboutPage
import mehdi.sakout.aboutpage.Element
import java.time.Year
import com.cylonid.nativealpha.util.WindowInsetsUtils

class AboutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val baseBinding = ActivityToolbarBaseBinding.inflate(layoutInflater)
        setContentView(baseBinding.root)
        WindowInsetsUtils.applyAsPadding(this, false)

        baseBinding.activityContent.addView(generateAboutPageView())

        val toolbar = baseBinding.toolbar.topAppBar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.app_info)

        onBackPressedDispatcher.addCallback(this) {
            finish()
        }

        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

    }

    private fun generateAboutPageView(): View {
        val page = AboutPage(this).apply {
            setDescription(
                """
                Native Alpha for Android
                by cylonid © ${Year.now().value}
                """.trimIndent()
            )
            setImage(R.drawable.native_alpha_foreground)
            addItem(Element().setTitle("Version " + BuildConfig.VERSION_NAME))
            addItem(addGitHubCustom("cylonid", "GitHub"))
            addPlayStore("com.cylonid.nativealpha.pro", "Play Store")
            addWebsite(
                "https://github.com/cylonid/NativeAlphaForAndroid/blob/dev/privacy_policy.md",
                getString(
                    R.string.privacy_policy
                )
            )
            if(BuildConfig.FLAVOR == "extendedGithub") {
                addItem(showLiberaPay())
            }
            addGroup(getString(R.string.eula_title))
            addItem(showEULA())
            addGroup(getString(R.string.license))
            addItem(showLicense())
            addItem(showOpenSourcelibs())
        }

        return page.create()

    }

    private fun addGitHubCustom(id: String, title: String): Element {
        val gitHubElement = Element()
        gitHubElement.setTitle(title)
        gitHubElement.setIconDrawable(R.drawable.about_icon_github)
        gitHubElement.setIconTint(
            getColorResFromThemeAttr(
                this, com.google.android.material.R.attr.colorOnSurface, R.color.about_github_color
            )
        )
        gitHubElement.setIconNightTint(R.color.about_item_dark_text_color)
        gitHubElement.setValue(id)

        val intent = Intent()
        intent.setAction(Intent.ACTION_VIEW)
        intent.addCategory(Intent.CATEGORY_BROWSABLE)
        intent.setData(Uri.parse(String.format("https://github.com/%s", id)))

        gitHubElement.setIntent(intent)

        return gitHubElement
    }

    fun showEULA(): Element {
        val license = Element()
        license.setTitle(getString(R.string.eula_content))
        return license
    }

    fun showLicense(): Element {
        val license = Element()

        license.setTitle(getString(R.string.gnu_license))
        license.setOnClickListener {
            val url = "https://www.gnu.org/licenses/gpl-3.0.txt"
            val i = Intent(Intent.ACTION_VIEW)
            i.setData(Uri.parse(url))
            startActivity(i)
        }
        return license
    }

    fun showLiberaPay(): Element {
        val element = Element()
        element.setTitle(getString(R.string.support_liberapay))
        element.setIconDrawable(R.drawable.liberapay_logo)
        element.skipTint = true

        element.setOnClickListener {
            val url = "https://liberapay.com/cylonid"
            val i = Intent(Intent.ACTION_VIEW)
            i.setData(Uri.parse(url))
            startActivity(i)
        }
        return element
    }

    fun showPayPal(): Element {
        val license = Element()

        license.setTitle(getString(R.string.paypal))
        license.setOnClickListener {
            val url = "https://paypal.me/cylonid"
            val i = Intent(Intent.ACTION_VIEW)
            i.setData(Uri.parse(url))
            startActivity(i)
        }
        return license
    }

    fun showOpenSourcelibs(): Element {
        val os = Element()
        os.setTitle(getString(R.string.open_source_libs))
        os.setOnClickListener {
            startActivity(
                LibsBuilder()
                    .withEdgeToEdge(true)
                    .withSearchEnabled(true)
                    .intent(this)
            )
        }
        return os
    }
}