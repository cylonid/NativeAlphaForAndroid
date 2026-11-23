package com.cylonid.nativealpha.ui.adblock

import android.app.ActivityManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.unit.dp
import com.cylonid.nativealpha.R
import com.cylonid.nativealpha.model.AdblockConfig
import com.cylonid.nativealpha.model.DataManager
import com.cylonid.nativealpha.ui.components.RoundedCardContainer
import com.cylonid.nativealpha.ui.components.SettingsSectionHeader
import com.cylonid.nativealpha.util.Const
import com.cylonid.nativealpha.util.ProcessUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AdblockConfigScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val dataManager = remember { DataManager.getInstance() }

    var adblockConfigs by remember {
        mutableStateOf(
            dataManager.settings.globalWebApp.adBlockSettings
                .toList(),
        )
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }

    fun persistList(updatedList: List<AdblockConfig>) {
        adblockConfigs = updatedList
        dataManager.settings.globalWebApp.adBlockSettings = ArrayList(updatedList)
        dataManager.saveGlobalSettings()
    }

    val undoLabel = stringResource(R.string.undo).uppercase()
    val itemRemovedTemplate = stringResource(R.string.x_was_removed)
    val entryAlreadyExistsMsg = stringResource(R.string.entry_already_exists)
    val restoreStandardSettingMsg = stringResource(R.string.restore_standard_setting)

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumFlexibleTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.adblock_config),
                        fontWeight = FontWeight.Bold,
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
                actions = {
                    IconButton(
                        onClick = { showRestoreDialog = true },
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Restore,
                            contentDescription = stringResource(R.string.restore_standard_setting),
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
        floatingActionButton = {
            if (adblockConfigs.size < 8) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = stringResource(R.string.add_a_new_adblock_provider),
                    )
                }
            }
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

            // ── Information Card ──
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
                        text = stringResource(R.string.adblock_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            SettingsSectionHeader(
                title = stringResource(R.string.adblock_config),
            )

            if (adblockConfigs.isEmpty()) {
                Card(
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceBright,
                        ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = stringResource(R.string.adblock_warning_text),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(8.dp))
                        TextButton(
                            onClick = {
                                persistList(Const.getDefaultAdBlockConfig())
                            },
                        ) {
                            Text(stringResource(R.string.restore_standard_setting))
                        }
                    }
                }
            } else {
                RoundedCardContainer(modifier = Modifier.fillMaxWidth()) {
                    adblockConfigs.forEachIndexed { index, config ->
                        ListItem(
                            content = {
                                Text(
                                    text = config.label,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                )
                            },
                            supportingContent = {
                                Text(
                                    text = config.value,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                            leadingContent = {
                                Box(
                                    modifier =
                                        Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(
                                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                            ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Security,
                                        contentDescription = null,
                                        modifier = Modifier.size(22.dp),
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            },
                            trailingContent = {
                                IconButton(
                                    onClick = {
                                        val removedItem = config
                                        val removedIndex = index
                                        val updated =
                                            adblockConfigs.toMutableList().apply {
                                                removeAt(index)
                                            }
                                        persistList(updated)

                                        coroutineScope.launch {
                                            val result =
                                                snackbarHostState.showSnackbar(
                                                    message = String.format(itemRemovedTemplate, removedItem.label),
                                                    actionLabel = undoLabel,
                                                    duration = SnackbarDuration.Short,
                                                )
                                            if (result == SnackbarResult.ActionPerformed) {
                                                val restored =
                                                    adblockConfigs.toMutableList().apply {
                                                        add(
                                                            removedIndex.coerceAtMost(size),
                                                            removedItem,
                                                        )
                                                    }
                                                persistList(restored)
                                            }
                                        }
                                    },
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.DeleteOutline,
                                        contentDescription = stringResource(R.string.delete),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            },
                            colors =
                                ListItemDefaults.colors(
                                    containerColor = MaterialTheme.colorScheme.surfaceBright,
                                ),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            Spacer(Modifier.height(80.dp))
        }
    }

    // ── Add Filter Dialog ──
    if (showAddDialog) {
        var label by remember { mutableStateOf("") }
        var url by remember { mutableStateOf("") }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                Text(stringResource(R.string.add_a_new_adblock_provider))
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = label,
                        onValueChange = { label = it },
                        label = { Text(stringResource(R.string.label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    OutlinedTextField(
                        value = url,
                        onValueChange = {
                            url = it
                            errorMessage = null
                        },
                        label = { Text(stringResource(R.string.url)) },
                        singleLine = true,
                        isError = errorMessage != null,
                        supportingText = errorMessage?.let { { Text(it) } },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = url.isNotBlank(),
                    onClick = {
                        val trimmedUrl = url.trim()
                        val formattedUrl =
                            if (trimmedUrl.startsWith("http://") || trimmedUrl.startsWith("https://")) {
                                trimmedUrl
                            } else {
                                "https://$trimmedUrl"
                            }

                        if (adblockConfigs.any { it.value == formattedUrl }) {
                            errorMessage = entryAlreadyExistsMsg
                            return@TextButton
                        }

                        val finalLabel = if (label.isNotBlank()) label.trim() else formattedUrl
                        val updated = adblockConfigs + AdblockConfig(finalLabel, formattedUrl)
                        persistList(updated)
                        showAddDialog = false
                    },
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }

    // ── Restore Defaults Dialog ──
    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text(stringResource(R.string.restore_standard_setting)) },
            text = {
                Text(stringResource(R.string.adblock_warning_text))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val defaultList = Const.getDefaultAdBlockConfig()
                        persistList(defaultList)
                        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
                        if (activityManager != null) {
                            ProcessUtils.closeAllWebAppsAndProcesses(activityManager)
                        }
                        showRestoreDialog = false
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(restoreStandardSettingMsg)
                        }
                    },
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }
}
