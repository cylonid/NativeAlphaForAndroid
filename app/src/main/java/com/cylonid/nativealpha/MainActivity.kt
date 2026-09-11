package com.cylonid.nativealpha

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.Html
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.cylonid.nativealpha.databinding.AddWebsiteDialogueBinding
import com.cylonid.nativealpha.fragments.webapplist.WebAppListFragment
import com.cylonid.nativealpha.helper.AdblockLifecycleHelper
import com.cylonid.nativealpha.model.DataManager
import com.cylonid.nativealpha.model.WebApp
import com.cylonid.nativealpha.util.Const
import com.cylonid.nativealpha.util.EntryPointUtils.entryPointReached
import com.google.android.material.floatingactionbutton.FloatingActionButton
import io.github.edsuns.adfilter.AdFilter
import com.cylonid.nativealpha.util.WindowInsetsUtils


class MainActivity : AppCompatActivity() {
    private lateinit var webAppListFragment: WebAppListFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        WindowInsetsUtils.applyAsPadding(this, false)
        webAppListFragment =
            supportFragmentManager.findFragmentById(R.id.fragment_container_view) as WebAppListFragment
        entryPointReached(this)

        if (DataManager.getInstance().websites.size == 0) {
            buildAddWebsiteDialog(getString(R.string.welcome_msg))
        }

        val fab = findViewById<FloatingActionButton>(R.id.fab)
        fab.setOnClickListener { buildAddWebsiteDialog(getString(R.string.add_webapp)) }
        personalizeToolbar()

        AdblockLifecycleHelper(this).trySyncOperation({ AdFilter.create(applicationContext) })

    }

    override fun onResume() {
        super.onResume()
        DataManager.getInstance().loadAppData();
        updateWebAppList()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra(Const.INTENT_BACKUP_RESTORED, false)) {
            updateWebAppList()

            buildImportSuccessDialog()
            intent.putExtra(Const.INTENT_BACKUP_RESTORED, false)
            intent.putExtra(Const.INTENT_REFRESH_NEW_THEME, false)
        }
        if (intent.getBooleanExtra(Const.INTENT_WEBAPP_CHANGED, false)) {
            updateWebAppList()
            intent.putExtra(Const.INTENT_WEBAPP_CHANGED, false)
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun updateWebAppList() {
        webAppListFragment.updateWebAppList()
    }

    private fun personalizeToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        @StringRes val appName =
            if (BuildConfig.FLAVOR == "extended") R.string.app_name_plus else R.string.app_name
        toolbar.setTitle(appName)
        setSupportActionBar(toolbar)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId

        if (id == R.id.action_settings) {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
            return true
        }
        if (id == R.id.action_about) {
            val intent = Intent(this, AboutActivity::class.java)
            startActivity(intent)
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    private fun buildImportSuccessDialog() {
        val message = """
            ${getString(R.string.import_success_dialog_txt2)}
            
            ${getString(R.string.import_success_dialog_txt3)}
            """.trimIndent()

        AlertDialog.Builder(this).setMessage(message)
            .setCancelable(false)
            .setTitle(
                getString(
                    R.string.import_success,
                    DataManager.getInstance().activeWebsitesCount
                )
            )
            .setPositiveButton(getString(R.string.ok)) { _: DialogInterface?, _: Int ->
                val webapps = DataManager.getInstance().activeWebsites
                for (i in webapps.indices.reversed()) {
                    val webapp = webapps[i]
                    val msg = Html.fromHtml(
                        getString(R.string.restore_shortcut, webapp.title),
                        Html.FROM_HTML_MODE_COMPACT
                    )
                    AlertDialog.Builder(this)
                        .setMessage(msg)
                        .setPositiveButton(R.string.ok) { _: DialogInterface?, _: Int ->
                            val frag = ShortcutDialogFragment.newInstance(webapp)
                            frag.show(supportFragmentManager, "SCFetcher-" + webapp.ID)
                        }
                        .setNegativeButton(R.string.cancel) { _: DialogInterface?, _: Int -> }
                        .create()
                        .show()

                }
            }
            .setNegativeButton(getString(R.string.cancel)) { _: DialogInterface?, _: Int -> }
            .create().show()
    }

    private fun buildAddWebsiteDialog(title: String) {
        val localBinding = AddWebsiteDialogueBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this@MainActivity)
            .setView(localBinding.root)
            .setTitle(title)
            .setPositiveButton(R.string.ok) { _: DialogInterface, _: Int ->
                val url = localBinding.websiteUrl.text.toString().trim()
                val urlWithProtocol =
                    if (url.startsWith("https://") || url.startsWith("http://")) url else "https://$url"
                val newSite = WebApp(
                    urlWithProtocol,
                    DataManager.getInstance().incrementedID,
                    DataManager.getInstance().incrementedOrder
                )
                newSite.applySettingsForNewWebApp()
                DataManager.getInstance().addWebsite(newSite)

                updateWebAppList()
                if (localBinding.switchCreateShortcut.isChecked) {
                    val frag = ShortcutDialogFragment.newInstance(newSite)
                    frag.show(supportFragmentManager, "SCFetcher-" + newSite.ID)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .create()

        dialog.show()
        val okButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        okButton.isEnabled = false
        localBinding.websiteUrl.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                okButton.isEnabled = !s.isNullOrBlank()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

    }
}


