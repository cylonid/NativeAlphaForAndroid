package com.cylonid.nativealpha.ui.settings

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.webkit.CookieManager
import android.webkit.WebStorage
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.MenuOpen
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.SmartButton
import androidx.compose.material.icons.rounded.Swipe
import androidx.compose.material.icons.rounded.ViewCarousel
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cylonid.nativealpha.MainActivity
import com.cylonid.nativealpha.R
import com.cylonid.nativealpha.WebAppSettingsActivity
import com.cylonid.nativealpha.activities.AdblockConfigActivity
import com.cylonid.nativealpha.model.DataManager
import com.cylonid.nativealpha.model.GlobalSettings
import com.cylonid.nativealpha.ui.components.RoundedCardContainer
import com.cylonid.nativealpha.ui.components.SettingsActionItem
import com.cylonid.nativealpha.ui.components.SettingsSectionHeader
import com.cylonid.nativealpha.ui.components.SettingsSwitchItem
import com.cylonid.nativealpha.ui.webapp.WebAppMenuStyle
import com.cylonid.nativealpha.util.Const
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    onDynamicColorChanged: (Boolean) -> Unit = {},
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val dataManager = remember { DataManager.getInstance() }

    var settings by remember { mutableStateOf(dataManager.settings.copy(), policy = neverEqualPolicy()) }
    var showMenuStyleDialog by remember { mutableStateOf(false) }

    fun updateSettings(modify: GlobalSettings.() -> Unit) {
        val updated = settings.copy()
        updated.modify()
        settings = updated
        dataManager.settings = updated
    }

    val exportSuccessMsg = stringResource(R.string.export_success)
    val exportFailedMsg = stringResource(R.string.export_failed)
    val importFailedMsg = stringResource(R.string.import_failed)

    val exportLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("*/*"),
        ) { uri ->
            if (uri != null) {
                dataManager.saveGlobalSettings()
                val success = dataManager.saveSharedPreferencesToFile(uri)
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        message = if (success) exportSuccessMsg else exportFailedMsg,
                    )
                }
            }
        }

    val importLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument(),
        ) { uri ->
            if (uri != null) {
                val success = dataManager.loadSharedPreferencesFromFile(uri)
                if (!success) {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            message = importFailedMsg,
                        )
                    }
                } else {
                    WebStorage.getInstance().deleteAllData()
                    CookieManager.getInstance().removeAllCookies(null)
                    dataManager.loadAppData()
                    val intent =
                        Intent(context, MainActivity::class.java).apply {
                            putExtra(Const.INTENT_BACKUP_RESTORED, true)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                    context.startActivity(intent)
                    (context as? Activity)?.finish()
                }
            }
        }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumFlexibleTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.global_settings),
                        fontWeight = FontWeight.Bold,
                    )
                },
                subtitle = {
                    Text(
                        text =
                            stringResource(
                                R.string.these_settings_are_applied_globally_and_override_app_specific_settings,
                            ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        shapes =
                            IconButtonDefaults.shapes(
                                shape = CircleShape,
                                pressedShape = RoundedCornerShape(percent = 18),
                            ),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .navigationBarsPadding(),
        ) {
            Spacer(Modifier.height(8.dp))

            // ── Section 1: Configuration & Defaults ──
            SettingsSectionHeader(stringResource(R.string.general))

            RoundedCardContainer(modifier = Modifier.fillMaxWidth()) {
                SettingsActionItem(
                    title = stringResource(R.string.adblock_config),
                    icon = Icons.Rounded.Security,
                    trailingIcon = Icons.AutoMirrored.Rounded.OpenInNew,
                    onClick = {
                        val intent =
                            Intent(context, AdblockConfigActivity::class.java).apply {
                                action = Intent.ACTION_VIEW
                            }
                        context.startActivity(intent)
                    },
                )

                SettingsActionItem(
                    title = stringResource(R.string.global_web_app_settings),
                    description =
                        stringResource(
                            R.string.these_settings_are_applied_globally_and_override_app_specific_settings,
                        ),
                    icon = Icons.Rounded.Language,
                    trailingIcon = Icons.AutoMirrored.Rounded.OpenInNew,
                    onClick = {
                        val intent =
                            Intent(context, WebAppSettingsActivity::class.java).apply {
                                putExtra(Const.INTENT_WEBAPPID, settings.globalWebApp.ID)
                                action = Intent.ACTION_VIEW
                            }
                        context.startActivity(intent)
                    },
                )
            }

            Spacer(Modifier.height(16.dp))

            // ── Section 2: Theme / Appearance ──
            SettingsSectionHeader(stringResource(R.string.theme))

            RoundedCardContainer(modifier = Modifier.fillMaxWidth()) {
                val isAtLeastAndroid12 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                SettingsSwitchItem(
                    title = stringResource(R.string.material_you_theme),
                    description =
                        stringResource(
                            if (isAtLeastAndroid12) {
                                R.string.material_you_theme_summary
                            } else {
                                R.string.material_you_requires_android_12
                            },
                        ),
                    checked = isAtLeastAndroid12 && settings.isDynamicColor,
                    enabled = isAtLeastAndroid12,
                    icon = Icons.Rounded.Palette,
                    onCheckedChange = { isChecked ->
                        updateSettings { isDynamicColor = isChecked }
                        onDynamicColorChanged(isChecked)
                    },
                )

                val currentMenuStyle = WebAppMenuStyle.fromId(settings.webAppMenuStyle)
                val menuStyleSummary =
                    when (currentMenuStyle) {
                        WebAppMenuStyle.BOTTOM_SHEET -> stringResource(R.string.webapp_menu_style_bottom_sheet)
                        WebAppMenuStyle.FLOATING_DIALOG -> stringResource(R.string.webapp_menu_style_floating_dialog)
                        WebAppMenuStyle.CLASSIC_DROPDOWN -> stringResource(R.string.webapp_menu_style_classic_dropdown)
                    }

                SettingsActionItem(
                    title = stringResource(R.string.webapp_menu_style),
                    description = menuStyleSummary,
                    icon = Icons.AutoMirrored.Rounded.MenuOpen,
                    trailingIcon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    onClick = { showMenuStyleDialog = true },
                )
            }

            Spacer(Modifier.height(16.dp))

            // ── Section 3: Browsing & Gestures ──
            SettingsSectionHeader(stringResource(R.string.webapp_section_misc))

            RoundedCardContainer(modifier = Modifier.fillMaxWidth()) {
                SettingsSwitchItem(
                    title = stringResource(R.string.clear_cache_after_usage),
                    checked = settings.isClearCache,
                    icon = Icons.Rounded.CleaningServices,
                    onCheckedChange = { isChecked ->
                        updateSettings { isClearCache = isChecked }
                    },
                )

                SettingsSwitchItem(
                    title = stringResource(R.string.show_progress_bar_during_page_load),
                    checked = settings.isShowProgressbar,
                    icon = Icons.Rounded.HourglassTop,
                    onCheckedChange = { isChecked ->
                        updateSettings { isShowProgressbar = isChecked }
                    },
                )

                SettingsSwitchItem(
                    title = stringResource(R.string.pref_pdf_viewer_title),
                    description = stringResource(R.string.pref_pdf_viewer_summary),
                    checked = settings.isUseBuiltinPdfViewer,
                    icon = Icons.Rounded.PictureAsPdf,
                    onCheckedChange = { isChecked ->
                        updateSettings {
                            isUseBuiltinPdfViewer = isChecked
                            globalWebApp.isUseBuiltinPdfViewer = isChecked
                        }
                    },
                )
            }

            Spacer(Modifier.height(16.dp))

            RoundedCardContainer(modifier = Modifier.fillMaxWidth()) {
                SettingsSwitchItem(
                    title = stringResource(R.string.global_settings_multitouch_reload),
                    checked = settings.isMultitouchReload,
                    icon = Icons.Rounded.Refresh,
                    onCheckedChange = { isChecked ->
                        updateSettings { isMultitouchReload = isChecked }
                    },
                )

                SettingsSwitchItem(
                    title =
                        stringResource(
                            R.string.use_two_finger_swipes_for_browser_forward_and_backward_navigation,
                        ),
                    checked = settings.isTwoFingerMultitouch,
                    icon = Icons.Rounded.Swipe,
                    onCheckedChange = { isChecked ->
                        updateSettings { isTwoFingerMultitouch = isChecked }
                    },
                )

                SettingsSwitchItem(
                    title =
                        stringResource(
                            R.string.use_three_finger_swipes_to_switch_between_web_apps_experimental,
                        ),
                    checked = settings.isThreeFingerMultitouch,
                    icon = Icons.Rounded.ViewCarousel,
                    onCheckedChange = { isChecked ->
                        updateSettings { isThreeFingerMultitouch = isChecked }
                    },
                )

                SettingsSwitchItem(
                    title = stringResource(R.string.always_show_software_buttons),
                    checked = settings.alwaysShowSoftwareButtons,
                    icon = Icons.Rounded.SmartButton,
                    onCheckedChange = { isChecked ->
                        updateSettings { alwaysShowSoftwareButtons = isChecked }
                    },
                )
            }

            Spacer(Modifier.height(16.dp))

            // ── Section 4: Backup & Restore ──
            SettingsSectionHeader(stringResource(R.string.backup))

            RoundedCardContainer(modifier = Modifier.fillMaxWidth()) {
                SettingsActionItem(
                    title = stringResource(R.string.export_settings_web_apps),
                    icon = Icons.Rounded.CloudUpload,
                    trailingIcon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    onClick = {
                        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                        val currentDateTime = sdf.format(Date())
                        exportLauncher.launch("NativeAlpha_$currentDateTime")
                    },
                )

                SettingsActionItem(
                    title = stringResource(R.string.import_settings_web_apps),
                    icon = Icons.Rounded.CloudDownload,
                    trailingIcon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    onClick = {
                        importLauncher.launch(arrayOf("*/*"))
                    },
                )
            }

            Spacer(Modifier.height(24.dp))
        }

        if (showMenuStyleDialog) {
            val currentStyle = WebAppMenuStyle.fromId(settings.webAppMenuStyle)
            AlertDialog(
                onDismissRequest = { showMenuStyleDialog = false },
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = stringResource(R.string.webapp_menu_style),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = stringResource(R.string.webapp_menu_style_summary),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        WebAppMenuStyle.entries.forEach { style ->
                            val isSelected = style == currentStyle
                            val styleTitle =
                                when (style) {
                                    WebAppMenuStyle.BOTTOM_SHEET -> stringResource(R.string.webapp_menu_style_bottom_sheet)
                                    WebAppMenuStyle.FLOATING_DIALOG -> stringResource(R.string.webapp_menu_style_floating_dialog)
                                    WebAppMenuStyle.CLASSIC_DROPDOWN -> stringResource(R.string.webapp_menu_style_classic_dropdown)
                                }
                            val styleDesc =
                                when (style) {
                                    WebAppMenuStyle.BOTTOM_SHEET -> stringResource(R.string.webapp_menu_style_bottom_sheet_desc)
                                    WebAppMenuStyle.FLOATING_DIALOG -> stringResource(R.string.webapp_menu_style_floating_dialog_desc)
                                    WebAppMenuStyle.CLASSIC_DROPDOWN -> stringResource(R.string.webapp_menu_style_classic_dropdown_desc)
                                }

                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            updateSettings { webAppMenuStyle = style.id }
                                            showMenuStyleDialog = false
                                        }.padding(horizontal = 8.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        updateSettings { webAppMenuStyle = style.id }
                                        showMenuStyleDialog = false
                                    },
                                )
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = styleTitle,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                    )
                                    Text(
                                        text = styleDesc,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showMenuStyleDialog = false }) {
                        Text(stringResource(android.R.string.cancel))
                    }
                },
            )
        }
    }
}
