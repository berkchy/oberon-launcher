package com.oberon.launcher.launcher

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.ui.platform.LocalDensity
import com.oberon.launcher.data.AppInfo
import android.graphics.drawable.Drawable
import kotlin.math.roundToInt

enum class LauncherSurface { Home, Drawer, Settings }

@Composable
fun AppIcon(
    app: AppInfo,
    size: Dp = 52.dp,
    packIcon: Drawable? = null,
    badgeCount: Int = 0
) {
    Box(contentAlignment = Alignment.TopEnd) {
        val px = with(LocalDensity.current) { (size.value * density).roundToInt() }
        val bitmap = remember(app.key, packIcon) {
            runCatching { (packIcon ?: app.icon)?.toBitmap(px, px) }.getOrNull()
        }
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = app.label,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(size)
            )
        } else {
            Box(
                modifier = Modifier.size(size).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = app.label.take(1).uppercase(),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        if (badgeCount > 0) {
            Badge(badgeCount)
        }
    }
}

@Composable
fun Badge(count: Int) {
    Box(
        modifier = Modifier
            .sizeIn(minWidth = 18.dp, minHeight = 18.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.error)
            .padding(horizontal = 4.dp, vertical = 1.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (count > 9) "9+" else count.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onError,
            fontWeight = FontWeight.Bold
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppGridItem(
    app: AppInfo,
    badgeCount: Int,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    packIcon: Drawable? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(vertical = 8.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AppIcon(app = app, packIcon = packIcon, badgeCount = badgeCount)
        Spacer(Modifier.height(6.dp))
        Text(
            text = app.label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
        )
    }
}

@Composable
fun AppActionsMenu(
    app: AppInfo,
    onHomeScreen: Boolean,
    onOpen: () -> Unit,
    onAddToHome: () -> Unit,
    onRemoveFromHome: () -> Unit,
    onAppInfo: () -> Unit,
    onHide: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(app.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        text = {
            Column {
                MenuRow(Icons.Filled.PlayArrow, "Aç") { onOpen(); onDismiss() }
                if (onHomeScreen) {
                    MenuRow(Icons.Filled.Clear, "Ana Ekrandan Kaldır") { onRemoveFromHome(); onDismiss() }
                } else {
                    MenuRow(Icons.Filled.Add, "Ana Ekrana Ekle") { onAddToHome(); onDismiss() }
                }
                MenuRow(Icons.Filled.Info, "Uygulama Bilgisi") { onAppInfo(); onDismiss() }
                MenuRow(Icons.Filled.Lock, "Gizle") { onHide(); onDismiss() }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Kapat") } }
    )
}

@Composable
private fun MenuRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}