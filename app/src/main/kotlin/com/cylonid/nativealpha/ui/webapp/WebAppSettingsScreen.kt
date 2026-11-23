package com.cylonid.nativealpha.ui.webapp

import android.app.Activity
import android.app.ActivityManager
import android.app.TimePickerDialog
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AppShortcut
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Cookie
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DataSaverOn
import androidx.compose.material.icons.rounded.DesktopWindows
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.HideImage
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Javascript
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.cylonid.nativealpha.R
import com.cylonid.nativealpha.ShortcutDialogFragment
import com.cylonid.nativealpha.helper.BiometricPromptHelper
import com.cylonid.nativealpha.model.DataManager
import com.cylonid.nativealpha.model.SandboxManager
import com.cylonid.nativealpha.model.WebApp
import com.cylonid.nativealpha.ui.components.RoundedCardContainer
import com.cylonid.nativealpha.ui.components.SettingsActionItem
import com.cylonid.nativealpha.ui.components.SettingsSectionHeader
import com.cylonid.nativealpha.ui.components.SettingsSwitchItem
import com.cylonid.nativealpha.util.Const
import com.cylonid.nativealpha.util.DateUtils
import com.cylonid.nativealpha.util.ProcessUtils
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WebAppSettingsScreen(
    webappId: Int,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val dataManager = remember { DataManager.getInstance() }
    val isGlobalWebApp = webappId == dataManager.settings.globalWebApp.ID

    val initialWebApp =
        remember {
            if (isGlobalWebApp) {
                dataManager.settings.globalWebApp
            } else {
                dataManager.getWebAppIgnoringGlobalOverride(webappId, true)
            }
        }

    if (initialWebApp == null) {
        LaunchedEffect(Unit) {
            onNavigateBack()
        }
        return
    }

    var webapp by remember { mutableStateOf(WebApp(initialWebApp), policy = neverEqualPolicy()) }
    var isDirty by remember { mutableStateOf(false) }

    fun updateWebApp(modify: WebApp.() -> Unit) {
        val updated = WebApp(webapp)
        updated.modify()
        webapp = updated
        isDirty = true
    }

    fun saveSettings() {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        if (isGlobalWebApp) {
            if (activityManager != null) {
                ProcessUtils.closeAllWebAppsAndProcesses(activityManager)
            }
            dataManager.settings.globalWebApp = webapp
            dataManager.settings.isUseBuiltinPdfViewer = webapp.isUseBuiltinPdfViewer
            dataManager.saveGlobalSettings()
        } else {
            if (activityManager != null) {
                for (task in activityManager.appTasks.orEmpty()) {
                    val id = task.taskInfo?.baseIntent?.getIntExtra(Const.INTENT_WEBAPPID, -1) ?: -1
                    if (id == webappId) task.finishAndRemoveTask()
                }
                ProcessUtils.killWebSandbox(webapp.containerId, activityManager)
            }
            dataManager.replaceWebApp(webapp)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            val isConfigChange = (context as? Activity)?.isChangingConfigurations == true
            if (isDirty && !isConfigChange) {
                saveSettings()
            }
        }
    }

    fun showTimePicker(
        initialTime: String?,
        onSelected: (String) -> Unit,
    ) {
        val cal = DateUtils.convertStringToCalendar(initialTime) ?: Calendar.getInstance()
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                val dt =
                    Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, hourOfDay)
                        set(Calendar.MINUTE, minute)
                    }
                onSelected(DateUtils.getHourMinFormat().format(dt.time))
            },
            cal[Calendar.HOUR_OF_DAY],
            cal[Calendar.MINUTE],
            true,
        ).show()
    }

    val enableBioPromptTitle = stringResource(R.string.bioprompt_enable_restriction)
    val disableBioPromptTitle = stringResource(R.string.bioprompt_disable_restricition)

    fun onBiometricToggle(enable: Boolean) {
        val fragmentActivity = context as? FragmentActivity
        if (fragmentActivity != null) {
            val helper = BiometricPromptHelper(fragmentActivity)
            val promptTitle = if (enable) enableBioPromptTitle else disableBioPromptTitle
            helper.showPrompt(
                { updateWebApp { isBiometricProtection = enable } },
                {},
                promptTitle,
            )
        } else {
            updateWebApp { isBiometricProtection = enable }
        }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val screenTitle =
        if (isGlobalWebApp) {
            stringResource(R.string.global_web_app_settings)
        } else {
            webapp.title.ifBlank { stringResource(R.string.web_app_settings) }
        }

    val screenSubtitle =
        if (isGlobalWebApp) {
            null
        } else {
            webapp.baseUrl
        }

    val isSettingsEnabled = isGlobalWebApp || webapp.isOverrideGlobalSettings

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumFlexibleTopAppBar(
                title = {
                    Text(
                        text = screenTitle,
                        fontWeight = FontWeight.Bold,
                    )
                },
                subtitle = {
                    screenSubtitle?.let { sub ->
                        Text(
                            text = sub,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
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

            // ── Global Info Banner ──
            if (isGlobalWebApp) {
                Card(
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceBright,
                        ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text =
                                stringResource(
                                    R.string.these_settings_are_applied_globally_and_override_app_specific_settings,
                                ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            } else {
                // ── Web App Identity (Per-App Only) ──
                SettingsSectionHeader(stringResource(R.string.web_app_settings))

                RoundedCardContainer(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                    ) {
                        OutlinedTextField(
                            value = webapp.title,
                            onValueChange = { newTitle ->
                                updateWebApp { title = newTitle }
                            },
                            label = { Text(stringResource(R.string.label)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Spacer(Modifier.height(12.dp))

                        OutlinedTextField(
                            value = webapp.baseUrl,
                            onValueChange = { newUrl ->
                                updateWebApp { baseUrl = newUrl }
                            },
                            label = { Text(stringResource(R.string.start_url)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    SettingsActionItem(
                        title = stringResource(R.string.re_create_shortcut),
                        description = stringResource(R.string.create_shortcut_on_home_screen),
                        icon = Icons.Rounded.AppShortcut,
                        onClick = {
                            val activity = context as? AppCompatActivity
                            if (activity != null) {
                                val frag = ShortcutDialogFragment.newInstance(webapp)
                                frag.show(activity.supportFragmentManager, "SCFetcher-${webapp.ID}")
                            }
                        },
                    )
                }

                Spacer(Modifier.height(16.dp))

                RoundedCardContainer(modifier = Modifier.fillMaxWidth()) {
                    SettingsSwitchItem(
                        title = stringResource(R.string.override_global_settings),
                        checked = webapp.isOverrideGlobalSettings,
                        icon = Icons.Rounded.Tune,
                        onCheckedChange = { isChecked ->
                            updateWebApp { isOverrideGlobalSettings = isChecked }
                        },
                    )
                }

                Spacer(Modifier.height(16.dp))
            }

            // ── Section 1: Browsing & Behavior ──
            SettingsSectionHeader(stringResource(R.string.webapp_section_misc))

            RoundedCardContainer(modifier = Modifier.fillMaxWidth()) {
                SettingsSwitchItem(
                    title = stringResource(R.string.allow_javascript),
                    checked = webapp.isAllowJs,
                    icon = Icons.Rounded.Javascript,
                    enabled = isSettingsEnabled,
                    onCheckedChange = { isChecked ->
                        updateWebApp {
                            isAllowJs = isChecked
                            if (!isChecked) {
                                isRequestDesktop = false
                                isUseAdblock = false
                            }
                        }
                    },
                )

                SettingsSwitchItem(
                    title = stringResource(R.string.request_website_in_desktop_version),
                    checked = webapp.isRequestDesktop,
                    icon = Icons.Rounded.DesktopWindows,
                    enabled = isSettingsEnabled && webapp.isAllowJs,
                    onCheckedChange = { isChecked ->
                        updateWebApp {
                            isRequestDesktop = isChecked
                            if (isChecked) {
                                isUseCustomUserAgent = false
                            }
                        }
                    },
                )

                SettingsSwitchItem(
                    title = stringResource(R.string.activate_two_finger_zoom),
                    checked = webapp.isEnableZooming,
                    icon = Icons.Rounded.ZoomIn,
                    enabled = isSettingsEnabled,
                    onCheckedChange = { isChecked ->
                        updateWebApp { isEnableZooming = isChecked }
                    },
                )

                SettingsSwitchItem(
                    title = stringResource(R.string.open_external_links_in_browser_app),
                    checked = webapp.isOpenUrlExternal,
                    icon = Icons.Rounded.OpenInBrowser,
                    enabled = isSettingsEnabled,
                    onCheckedChange = { isChecked ->
                        updateWebApp { isOpenUrlExternal = isChecked }
                    },
                )

                SettingsSwitchItem(
                    title = stringResource(R.string.pref_pdf_viewer_title),
                    description = stringResource(R.string.pref_pdf_viewer_summary),
                    checked = webapp.isUseBuiltinPdfViewer,
                    icon = Icons.Rounded.PictureAsPdf,
                    enabled = isSettingsEnabled,
                    onCheckedChange = { isChecked ->
                        updateWebApp { isUseBuiltinPdfViewer = isChecked }
                    },
                )

                SettingsSwitchItem(
                    title = stringResource(R.string.use_standard_context_menu_permanently),
                    checked = webapp.alwaysUseFallbackContextMenu,
                    icon = Icons.Rounded.Menu,
                    enabled = isSettingsEnabled,
                    onCheckedChange = { isChecked ->
                        updateWebApp { alwaysUseFallbackContextMenu = isChecked }
                    },
                )
            }

            Spacer(Modifier.height(16.dp))

            // ── Section 2: Privacy & Security ──
            SettingsSectionHeader(stringResource(R.string.webapp_section_cookies))

            RoundedCardContainer(modifier = Modifier.fillMaxWidth()) {
                SettingsSwitchItem(
                    title = stringResource(R.string.accept_cookies),
                    checked = webapp.isAllowCookies,
                    icon = Icons.Rounded.Cookie,
                    enabled = isSettingsEnabled,
                    onCheckedChange = { isChecked ->
                        updateWebApp {
                            isAllowCookies = isChecked
                            if (!isChecked) {
                                isAllowThirdPartyCookies = false
                            }
                        }
                    },
                )

                SettingsSwitchItem(
                    title = stringResource(R.string.accept_third_party_cookies),
                    checked = webapp.isAllowThirdPartyCookies,
                    icon = Icons.Rounded.Cookie,
                    enabled = isSettingsEnabled && webapp.isAllowCookies,
                    onCheckedChange = { isChecked ->
                        updateWebApp { isAllowThirdPartyCookies = isChecked }
                    },
                )

                SettingsSwitchItem(
                    title = stringResource(R.string.enable_adblock_experimental),
                    checked = webapp.isUseAdblock,
                    icon = Icons.Rounded.Block,
                    enabled = isSettingsEnabled && webapp.isAllowJs,
                    onCheckedChange = { isChecked ->
                        updateWebApp { isUseAdblock = isChecked }
                    },
                )

                SettingsSwitchItem(
                    title = stringResource(R.string.block_all_third_party_requests),
                    checked = webapp.isBlockThirdPartyRequests,
                    icon = Icons.Rounded.Shield,
                    enabled = isSettingsEnabled,
                    onCheckedChange = { isChecked ->
                        updateWebApp { isBlockThirdPartyRequests = isChecked }
                    },
                )

                SettingsSwitchItem(
                    title = stringResource(R.string.allow_http),
                    checked = webapp.isAllowHttp,
                    icon = Icons.Rounded.LockOpen,
                    enabled = isSettingsEnabled,
                    onCheckedChange = { isChecked ->
                        updateWebApp { isAllowHttp = isChecked }
                    },
                )

                SettingsSwitchItem(
                    title = stringResource(R.string.allow_drm_content),
                    checked = webapp.isDrmAllowed,
                    icon = Icons.Rounded.Key,
                    enabled = isSettingsEnabled,
                    onCheckedChange = { isChecked ->
                        updateWebApp { isDrmAllowed = isChecked }
                    },
                )
            }

            Spacer(Modifier.height(16.dp))

            // ── Section 3: Device Permissions & Security ──
            SettingsSectionHeader(stringResource(R.string.webapp_section_security))

            RoundedCardContainer(modifier = Modifier.fillMaxWidth()) {
                SettingsSwitchItem(
                    title = stringResource(R.string.allow_location_access),
                    checked = webapp.isAllowLocationAccess,
                    icon = Icons.Rounded.Place,
                    enabled = isSettingsEnabled,
                    onCheckedChange = { isChecked ->
                        updateWebApp { isAllowLocationAccess = isChecked }
                    },
                )

                SettingsSwitchItem(
                    title = stringResource(R.string.allow_camera_access),
                    checked = webapp.isCameraPermission,
                    icon = Icons.Rounded.CameraAlt,
                    enabled = isSettingsEnabled,
                    onCheckedChange = { isChecked ->
                        updateWebApp { isCameraPermission = isChecked }
                    },
                )

                SettingsSwitchItem(
                    title = stringResource(R.string.allow_microphone_access),
                    checked = webapp.isMicrophonePermission,
                    icon = Icons.Rounded.Mic,
                    enabled = isSettingsEnabled,
                    onCheckedChange = { isChecked ->
                        updateWebApp { isMicrophonePermission = isChecked }
                    },
                )

                SettingsSwitchItem(
                    title = stringResource(R.string.enable_access_restriction),
                    checked = webapp.isBiometricProtection,
                    icon = Icons.Rounded.Fingerprint,
                    enabled = isSettingsEnabled,
                    onCheckedChange = { isChecked ->
                        onBiometricToggle(isChecked)
                    },
                )
            }

            Spacer(Modifier.height(16.dp))

            // ── Section 4: Display, Dark Mode & Kiosk ──
            SettingsSectionHeader(stringResource(R.string.dark_mode))

            RoundedCardContainer(modifier = Modifier.fillMaxWidth()) {
                SettingsSwitchItem(
                    title = stringResource(R.string.force_dark_mode),
                    checked = webapp.isForceDarkMode,
                    icon = Icons.Rounded.DarkMode,
                    enabled = isSettingsEnabled,
                    onCheckedChange = { isChecked ->
                        updateWebApp {
                            isForceDarkMode = isChecked
                            if (!isChecked) {
                                isUseTimespanDarkMode = false
                            }
                        }
                    },
                )

                SettingsSwitchItem(
                    title = stringResource(R.string.limit_dark_mode_to_time_span),
                    checked = webapp.isUseTimespanDarkMode,
                    icon = Icons.Rounded.Schedule,
                    enabled = isSettingsEnabled && webapp.isForceDarkMode,
                    onCheckedChange = { isChecked ->
                        updateWebApp { isUseTimespanDarkMode = isChecked }
                    },
                )

                if (webapp.isUseTimespanDarkMode && webapp.isForceDarkMode) {
                    SettingsActionItem(
                        title = stringResource(R.string.begin),
                        description = webapp.timespanDarkModeBegin ?: "22:00",
                        icon = Icons.Rounded.Schedule,
                        enabled = isSettingsEnabled,
                        trailingIcon = Icons.AutoMirrored.Rounded.OpenInNew,
                        onClick = {
                            showTimePicker(webapp.timespanDarkModeBegin) { selected ->
                                updateWebApp { timespanDarkModeBegin = selected }
                            }
                        },
                    )

                    SettingsActionItem(
                        title = stringResource(R.string.end),
                        description = webapp.timespanDarkModeEnd ?: "06:00",
                        icon = Icons.Rounded.Schedule,
                        enabled = isSettingsEnabled,
                        trailingIcon = Icons.AutoMirrored.Rounded.OpenInNew,
                        onClick = {
                            showTimePicker(webapp.timespanDarkModeEnd) { selected ->
                                updateWebApp { timespanDarkModeEnd = selected }
                            }
                        },
                    )
                }

                SettingsSwitchItem(
                    title = stringResource(R.string.show_fullscreen),
                    checked = webapp.isShowFullscreen,
                    icon = Icons.Rounded.Fullscreen,
                    enabled = isSettingsEnabled,
                    onCheckedChange = { isChecked ->
                        updateWebApp { isShowFullscreen = isChecked }
                    },
                )

                SettingsSwitchItem(
                    title = stringResource(R.string.keep_screen_awake),
                    checked = webapp.isKeepAwake,
                    icon = Icons.Rounded.Lightbulb,
                    enabled = isSettingsEnabled,
                    onCheckedChange = { isChecked ->
                        updateWebApp { isKeepAwake = isChecked }
                    },
                )
            }

            Spacer(Modifier.height(16.dp))

            // ── Section 5: Media Playback ──
            SettingsSectionHeader(stringResource(R.string.allow_media_playback_in_background))

            RoundedCardContainer(modifier = Modifier.fillMaxWidth()) {
                SettingsSwitchItem(
                    title = stringResource(R.string.allow_media_playback_in_background),
                    checked = webapp.isAllowMediaPlaybackInBackground,
                    icon = Icons.Rounded.MusicNote,
                    enabled = isSettingsEnabled,
                    onCheckedChange = { isChecked ->
                        updateWebApp {
                            isAllowMediaPlaybackInBackground = isChecked
                        }
                    },
                )
            }

            Spacer(Modifier.height(16.dp))

            // ── Section 6: Data Saving & Auto-Reload ──
            SettingsSectionHeader(stringResource(R.string.webapp_section_datasaving))

            RoundedCardContainer(modifier = Modifier.fillMaxWidth()) {
                SettingsSwitchItem(
                    title = stringResource(R.string.request_data_saving_page),
                    checked = webapp.isSendSavedataRequest,
                    icon = Icons.Rounded.DataSaverOn,
                    enabled = isSettingsEnabled,
                    onCheckedChange = { isChecked ->
                        updateWebApp { isSendSavedataRequest = isChecked }
                    },
                )

                SettingsSwitchItem(
                    title = stringResource(R.string.do_not_load_images),
                    checked = webapp.isBlockImages,
                    icon = Icons.Rounded.HideImage,
                    enabled = isSettingsEnabled,
                    onCheckedChange = { isChecked ->
                        updateWebApp { isBlockImages = isChecked }
                    },
                )

                SettingsSwitchItem(
                    title = stringResource(R.string.webapp_autoreload_switch),
                    checked = webapp.isAutoreload,
                    icon = Icons.Rounded.Autorenew,
                    enabled = isSettingsEnabled,
                    onCheckedChange = { isChecked ->
                        updateWebApp { isAutoreload = isChecked }
                    },
                )

                if (webapp.isAutoreload) {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                    ) {
                        OutlinedTextField(
                            value = if (webapp.timeAutoreload > 0) webapp.timeAutoreload.toString() else "",
                            onValueChange = { str ->
                                val intVal = str.filter { it.isDigit() }.toIntOrNull() ?: 0
                                updateWebApp { timeAutoreload = intVal }
                            },
                            label = { Text(stringResource(R.string.webapp_interval_for_reload)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            enabled = isSettingsEnabled,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Section 7: Expert & Container Settings ──
            SettingsSectionHeader(stringResource(R.string.expert_settings))

            RoundedCardContainer(modifier = Modifier.fillMaxWidth()) {
                SettingsSwitchItem(
                    title = stringResource(R.string.show_expert_settings),
                    checked = webapp.isShowExpertSettings,
                    icon = Icons.Rounded.Psychology,
                    enabled = isSettingsEnabled,
                    onCheckedChange = { isChecked ->
                        updateWebApp { isShowExpertSettings = isChecked }
                    },
                )

                if (webapp.isShowExpertSettings) {
                    if (!isGlobalWebApp) {
                        SettingsSwitchItem(
                            title = stringResource(R.string.enable_sandbox),
                            checked = webapp.isUseContainer,
                            icon = Icons.Rounded.Layers,
                            enabled = isSettingsEnabled,
                            onCheckedChange = { isChecked ->
                                updateWebApp {
                                    isUseContainer = isChecked
                                    containerId =
                                        if (isChecked) {
                                            SandboxManager.getInstance().calculateNextFreeContainerId()
                                        } else {
                                            Const.NO_CONTAINER
                                        }
                                }
                            },
                        )
                    }

                    SettingsSwitchItem(
                        title = stringResource(R.string.use_custom_user_agent),
                        checked = webapp.isUseCustomUserAgent,
                        icon = Icons.Rounded.Badge,
                        enabled = isSettingsEnabled && !webapp.isRequestDesktop,
                        onCheckedChange = { isChecked ->
                            updateWebApp { isUseCustomUserAgent = isChecked }
                        },
                    )

                    if (webapp.isUseCustomUserAgent && !webapp.isRequestDesktop) {
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                        ) {
                            OutlinedTextField(
                                value = webapp.userAgent ?: "",
                                onValueChange = { ua ->
                                    updateWebApp { userAgent = ua }
                                },
                                label = { Text(stringResource(R.string.user_agent)) },
                                singleLine = false,
                                maxLines = 3,
                                enabled = isSettingsEnabled,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }

                    if (!isGlobalWebApp) {
                        SettingsSwitchItem(
                            title = stringResource(R.string.ignore_ssl_errors),
                            description = stringResource(R.string.ssl_permanent_warning),
                            checked = webapp.isIgnoreSslErrors,
                            icon = Icons.Rounded.Warning,
                            enabled = isSettingsEnabled,
                            onCheckedChange = { isChecked ->
                                updateWebApp { isIgnoreSslErrors = isChecked }
                            },
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
