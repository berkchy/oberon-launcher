package com.oberon.launcher.launcher

import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.oberon.launcher.R
import com.oberon.launcher.data.AppInfo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    vm: LauncherViewModel,
    onOpenDrawer: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val gridColumns by vm.gridColumns.collectAsState()
    val favorites by vm.favorites.collectAsState()
    val visible by vm.visibleApps.collectAsState()
    val badges by vm.badges.collectAsState()
    val dark = isDarkWallpaperScrim()

    var selected by remember { mutableStateOf<AppInfo?>(null) }
    var showMenu by remember { mutableStateOf(false) }
    var dragAcc by remember { mutableStateOf(0f) }

    var wallpaper by remember { mutableStateOf<Bitmap?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) wallpaper = loadWallpaper(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        wallpaper = loadWallpaper(context)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val homeApps = remember(visible, favorites) {
        val favs = favorites.mapNotNull { key -> visible.find { it.key == key } }
        if (favs.isEmpty()) visible else favs
    }
    val hotseatApps = remember(visible, favorites) {
        favorites.mapNotNull { key -> visible.find { it.key == key } }.take(5)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onLongPress = { showMenu = true })
            }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onVerticalDrag = { _, dy -> dragAcc += dy },
                    onDragEnd = {
                        if (dragAcc < -140.dp.toPx()) onOpenDrawer()
                        dragAcc = 0f
                    },
                    onDragCancel = { dragAcc = 0f }
                )
            }
    ) {
        wallpaper?.let { bmp ->
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = if (dark) 0.35f else 0.15f))
        )

        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            AtAGlance()
            Spacer(Modifier.height(8.dp))

            SearchPill(onClick = onOpenDrawer)
            Spacer(Modifier.height(6.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(gridColumns.coerceIn(3, 7)),
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(homeApps, key = { it.key }) { app ->
                    val badge = badges[app.packageName] ?: 0
                    val packIcon = remember(app.packageName) { vm.packIconFor(app.packageName) }
                    AppGridItem(
                        app = app,
                        badgeCount = badge,
                        onClick = { vm.launch(app) },
                        onLongClick = { selected = app },
                        packIcon = packIcon
                    )
                }
                if (homeApps.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        EmptyHomeHint()
                    }
                }
            }

            if (hotseatApps.isNotEmpty()) {
                Hotseat(
                    apps = hotseatApps,
                    vm = vm,
                    onLongClick = { selected = it },
                    modifier = Modifier.navigationBarsPadding().padding(bottom = 10.dp)
                )
            }
        }
    }

    selected?.let { app ->
        AppActionsMenu(
            app = app,
            onHomeScreen = true,
            onOpen = { vm.launch(app) },
            onAddToHome = {},
            onRemoveFromHome = { vm.removeFavorite(app.key) },
            onAppInfo = { vm.openAppInfo(app) },
            onHide = { vm.toggleHidden(app.packageName) },
            onDismiss = { selected = null }
        )
    }

    if (showMenu) {
        HomeBackgroundMenu(
            onWallpaper = {
                showMenu = false
                openWallpaperPicker(context)
            },
            onSettings = {
                showMenu = false
                onOpenSettings()
            },
            onDismiss = { showMenu = false }
        )
    }
}

@Composable
private fun isDarkWallpaperScrim(): Boolean = androidx.compose.foundation.isSystemInDarkTheme()

@Composable
private fun AtAGlance() {
    val now by produceState(initialValue = 0L) {
        while (true) {
            value = System.currentTimeMillis()
            delay(1000)
        }
    }
    val timeFmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateFmt = remember { SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()) }
    val date = remember(now) { Date(now) }

    Column(modifier = Modifier.padding(start = 20.dp, top = 24.dp)) {
        Text(
            text = dateFmt.format(date).replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = timeFmt.format(date),
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Light
        )
    }
}

@Composable
private fun SearchPill(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(12.dp))
            Text(
                "Uygulama ve web ara…",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun Hotseat(
    apps: List<AppInfo>,
    vm: LauncherViewModel,
    onLongClick: (AppInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.92f),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            apps.forEach { app ->
                val badge = vm.badges.value[app.packageName] ?: 0
                val packIcon = remember(app.packageName) { vm.packIconFor(app.packageName) }
                AppGridItem(
                    app = app,
                    badgeCount = badge,
                    onClick = { vm.launch(app) },
                    onLongClick = { onLongClick(app) },
                    packIcon = packIcon,
                    modifier = Modifier.width(76.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyHomeHint() {
    Text(
        text = "Ana ekran boş. Uygulamalar sekmesini açmak için yukarı kaydır, ardından ikonlara uzun basarak beni doldur.",
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 24.dp)
    )
}

@Composable
private fun HomeBackgroundMenu(
    onWallpaper: () -> Unit,
    onSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Oberon") },
        text = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onWallpaper)
                        .padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(painterResource(R.drawable.ic_wallpaper), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(18.dp))
                    Text("Duvar Kağıdı", style = MaterialTheme.typography.bodyLarge)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onSettings)
                        .padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(18.dp))
                    Text("Ayarlar", style = MaterialTheme.typography.bodyLarge)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Kapat") } }
    )
}

private fun loadWallpaper(context: Context): Bitmap? = runCatching {
    val drawable = WallpaperManager.getInstance(context).drawable ?: return null
    when (drawable) {
        is BitmapDrawable -> drawable.bitmap
        else -> {
            val w = drawable.intrinsicWidth.coerceAtLeast(1)
            val h = drawable.intrinsicHeight.coerceAtLeast(1)
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            drawable.setBounds(0, 0, w, h)
            drawable.draw(canvas)
            bmp
        }
    }
}.getOrNull()

private fun openWallpaperPicker(context: Context) {
    val intent = Intent(Intent.ACTION_SET_WALLPAPER).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
        .onFailure { Toast.makeText(context, "Duvar kağıdı seçici bulunamadı", Toast.LENGTH_SHORT).show() }
}