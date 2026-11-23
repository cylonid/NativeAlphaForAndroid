package com.cylonid.nativealpha.ui.about

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.Payment
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cylonid.nativealpha.BuildConfig
import com.cylonid.nativealpha.DependenciesActivity
import com.cylonid.nativealpha.R
import com.cylonid.nativealpha.model.ReleaseNotesRepository
import com.cylonid.nativealpha.ui.components.ReleaseNotesBottomSheet
import com.cylonid.nativealpha.ui.components.RoundedCardContainer
import com.cylonid.nativealpha.ui.components.SettingsActionItem
import com.cylonid.nativealpha.ui.components.SettingsSectionHeader
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showEulaDialog by remember { mutableStateOf(false) }
    var showReleaseNotesSheet by remember { mutableStateOf(false) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val context = LocalContext.current
    var releaseNotes by remember { mutableStateOf<com.cylonid.nativealpha.model.ReleaseNotes?>(null) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        releaseNotes =
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                ReleaseNotesRepository.load(context)
            }
    }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumFlexibleTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_info),
                        fontWeight = FontWeight.Bold,
                    )
                },
                subtitle = {
                    Text(
                        text = stringResource(R.string.app_name),
                        maxLines = 1,
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
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
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
            Spacer(Modifier.height(16.dp))

            // ── App Hero / Branding Header ──
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Surface(
                        shape = RoundedCornerShape(28.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                        tonalElevation = 2.dp,
                        modifier = Modifier.size(96.dp),
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Image(
                                painter = painterResource(R.drawable.native_alpha_foreground),
                                contentDescription = stringResource(R.string.app_name),
                                modifier = Modifier.size(80.dp),
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    Spacer(Modifier.height(6.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ) {
                        Text(
                            text = "v${BuildConfig.VERSION_NAME}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = stringResource(R.string.about_maintained_by, "JoestarLabs"),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )

                    Spacer(Modifier.height(2.dp))

                    Text(
                        text = stringResource(R.string.about_originally_by, "cylonid"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            // ── Section 1: Links & Community ──
            SettingsSectionHeader(stringResource(R.string.about_section_community))

            RoundedCardContainer(modifier = Modifier.fillMaxWidth()) {
                SettingsActionItem(
                    title = stringResource(R.string.whats_new),
                    description = "Version v${BuildConfig.VERSION_NAME}",
                    icon = Icons.Rounded.AutoAwesome,
                    trailingIcon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    onClick = { showReleaseNotesSheet = true },
                )

                SettingsActionItem(
                    title = stringResource(R.string.about_fork_repo),
                    description = "JoestarLabs/NativeAlpha",
                    icon = Icons.Rounded.Code,
                    trailingIcon = Icons.AutoMirrored.Rounded.OpenInNew,
                    onClick = {
                        openUrl(context, "https://github.com/JoestarLabs/NativeAlpha")
                    },
                )

                SettingsActionItem(
                    title = stringResource(R.string.about_upstream_repo),
                    description = "cylonid/NativeAlphaForAndroid",
                    icon = Icons.Rounded.Code,
                    trailingIcon = Icons.AutoMirrored.Rounded.OpenInNew,
                    onClick = {
                        openUrl(context, "https://github.com/cylonid/NativeAlphaForAndroid")
                    },
                )

                if (BuildConfig.FLAVOR != "extended") {
                    SettingsActionItem(
                        title = "Google Play",
                        description = "Native Alpha Plus (Support original dev)",
                        icon = Icons.Rounded.ShoppingBag,
                        trailingIcon = Icons.AutoMirrored.Rounded.OpenInNew,
                        onClick = {
                            openUrl(context, "market://details?id=com.cylonid.nativealpha.pro")
                        },
                    )
                }

                SettingsActionItem(
                    title = stringResource(R.string.privacy_policy),
                    description = "JoestarLabs/NativeAlpha",
                    icon = Icons.Rounded.Security,
                    trailingIcon = Icons.AutoMirrored.Rounded.OpenInNew,
                    onClick = {
                        openUrl(
                            context,
                            "https://github.com/JoestarLabs/NativeAlpha/blob/main/privacy_policy.md",
                        )
                    },
                )
            }

            if (BuildConfig.FLAVOR == "extendedGithub") {
                Spacer(Modifier.height(16.dp))

                // ── Section 2: Support Development ──
                SettingsSectionHeader(stringResource(R.string.support_development))

                RoundedCardContainer(modifier = Modifier.fillMaxWidth()) {
                    SettingsActionItem(
                        title = stringResource(R.string.support_liberapay),
                        description = "liberapay.com/cylonid",
                        icon = Icons.Rounded.Favorite,
                        trailingIcon = Icons.AutoMirrored.Rounded.OpenInNew,
                        onClick = { openUrl(context, "https://liberapay.com/cylonid") },
                    )

                    SettingsActionItem(
                        title = stringResource(R.string.paypal),
                        description = "paypal.me/cylonid",
                        icon = Icons.Rounded.Payment,
                        trailingIcon = Icons.AutoMirrored.Rounded.OpenInNew,
                        onClick = { openUrl(context, "https://paypal.me/cylonid") },
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Section 3: Legal & Open Source ──
            SettingsSectionHeader(stringResource(R.string.license))

            RoundedCardContainer(modifier = Modifier.fillMaxWidth()) {
                SettingsActionItem(
                    title = stringResource(R.string.eula_title),
                    description = "End User License Agreement",
                    icon = Icons.Rounded.Gavel,
                    trailingIcon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    onClick = { showEulaDialog = true },
                )

                SettingsActionItem(
                    title = stringResource(R.string.gnu_license),
                    description = "GNU General Public License v3.0",
                    icon = Icons.Rounded.Description,
                    trailingIcon = Icons.AutoMirrored.Rounded.OpenInNew,
                    onClick = { openUrl(context, "https://www.gnu.org/licenses/gpl-3.0.txt") },
                )

                SettingsActionItem(
                    title = stringResource(R.string.open_source_libs),
                    description = stringResource(R.string.open_source_dependencies_desc),
                    icon = Icons.Rounded.Extension,
                    trailingIcon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    onClick = {
                        try {
                            context.startActivity(Intent(context, DependenciesActivity::class.java))
                        } catch (e: Exception) {
                            Timber.w(e, "Failed to open DependenciesActivity")
                        }
                    },
                )
            }

            Spacer(Modifier.height(28.dp))
        }
    }

    if (showEulaDialog) {
        AlertDialog(
            onDismissRequest = { showEulaDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.eula_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = stringResource(R.string.eula_content),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showEulaDialog = false }) {
                    Text(stringResource(R.string.ok))
                }
            },
            shape = RoundedCornerShape(28.dp),
        )
    }

    val loadedNotes = releaseNotes
    if (showReleaseNotesSheet && loadedNotes != null) {
        ReleaseNotesBottomSheet(
            releaseNotes = loadedNotes,
            onDismiss = { showReleaseNotesSheet = false },
        )
    }
}

private fun openUrl(
    context: android.content.Context,
    url: String,
) {
    com.cylonid.nativealpha.util.IntentUtils
        .openExternalUrl(context, url)
}
