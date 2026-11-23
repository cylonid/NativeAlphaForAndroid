package com.cylonid.nativealpha.ui.webapplist.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cylonid.nativealpha.model.WebApp
import com.cylonid.nativealpha.util.ShortcutIconUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val AVATAR_PALETTE = ShortcutIconUtils.AVATAR_PALETTE_COLORS.map { Color(it) }

@Composable
fun WebAppFavicon(
    webApp: WebApp,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
) {
    val context = LocalContext.current
    var iconBitmap by remember(webApp.ID) {
        mutableStateOf<Bitmap?>(ShortcutIconUtils.getCachedIcon(webApp.ID))
    }

    LaunchedEffect(webApp.ID, webApp.baseUrl) {
        if (iconBitmap == null) {
            iconBitmap =
                withContext(Dispatchers.IO) {
                    ShortcutIconUtils.getIcon(context, webApp.ID)
                        ?: ShortcutIconUtils.autoFetchAndSaveFavicon(context, webApp.ID, webApp.baseUrl)
                }
        }
        ShortcutIconUtils.iconUpdates.collect { updatedId ->
            if (updatedId == webApp.ID) {
                iconBitmap =
                    withContext(Dispatchers.IO) {
                        ShortcutIconUtils.getIcon(context, webApp.ID)
                    }
            }
        }
    }

    val currentBitmap = iconBitmap
    if (currentBitmap != null) {
        Image(
            bitmap = currentBitmap.asImageBitmap(),
            contentDescription = webApp.title,
            modifier =
                modifier
                    .size(size)
                    .clip(CircleShape),
        )
    } else {
        val cleanTitle = webApp.title.trim().ifEmpty { webApp.baseUrl.removePrefix("https://").removePrefix("http://") }
        val initial = cleanTitle.firstOrNull()?.uppercaseChar()?.toString() ?: "W"
        val avatarColor = AVATAR_PALETTE[ShortcutIconUtils.avatarColorIndex(cleanTitle)]

        Box(
            modifier =
                modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(avatarColor),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = initial,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.44f).sp,
            )
        }
    }
}
