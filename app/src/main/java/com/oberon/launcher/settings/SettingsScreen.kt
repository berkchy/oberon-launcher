package com.oberon.launcher.settings

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.oberon.launcher.R
import com.oberon.launcher.data.AppInfo
import com.oberon.launcher.launcher.LauncherViewModel
import com.oberon.launcher.launcher.AppIcon
import com.oberon.launcher.theme.AccentOptions
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(vm: LauncherViewModel, onClose: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val themeMode by vm.themeMode.collectAsState()
    val accentOption by vm.accentOption.collectAsState()
    val accentColor by vm.accentColor.collectAsState()
    val gridColumns by vm.gridColumns.collectAsState()
    val drawerSort by vm.drawerSort.collectAsState()
    val searchEngine by vm.searchEngine.collectAsState()
    val badgesEnabled by vm.badgesEnabled.collectAsState()
    val iconPackName by vm.iconPackName.collectAsState()

    var showHidden by rememberSaveable { mutableStateOf(false) }
    var showReset by rememberSaveable { mutableStateOf(false) }

    val saveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    val json = vm.exportJson()
                    context.contentResolver.openOutputStream(uri)?.use {
                        it.write(json.toByteArray())
                    }
                    toast(context, "Yedek oluşturuldu")
                }.onFailure { toast(context, "Yedekleme hatası") }
            }
        }
    }

    val openLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    val text = context.contentResolver.openInputStream(uri)
                        ?.bufferedReader()?.use { it.readText() }
                    if (!text.isNullOrEmpty()) {
                        vm.importJson(text)
                        toast(context, "Yedek geri yüklendi")
                    }
                }.onFailure { toast(context, "Geri yükleme hatası") }
            }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth().statusBarsPadding().navigationBarsPadding(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 32.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Geri")
                }
                Text("Ayarlar", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            }
        }

        item {
            Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(56.dp).clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_nav_apps),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("Oberon Launcher", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Text("v1.0", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                }
            }
        }

        item { GroupHeader("Görünüm") }

        item {
            SettingLabel("Tema")
            ChipRow(
                options = listOf(
                    "system" to "Sistem",
                    "light" to "Açık",
                    "dark" to "Koyu"
                ),
                current = themeMode,
                onSelect = { vm.setThemeMode(it) }
            )
        }

        item {
            SettingLabel("Renk")
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = accentOption == "dynamic",
                    onClick = { vm.setAccentOption("dynamic") },
                    label = { Text("Dinamik (Monet)") }
                )
                Spacer(Modifier.width(8.dp))
                AccentOptions.forEach { option ->
                    val selected = accentOption == "custom" && option.color.value.toLong() == accentColor
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (selected) 42.dp else 36.dp)
                            .clip(CircleShape)
                            .background(option.color)
                            .then(
                                if (selected) {
                                    Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                } else Modifier
                            )
                            .clickable {
                                vm.setAccentOption("custom")
                                vm.setAccentColor(option.color.value.toLong())
                            }
                    )
                }
            }
        }

        item {
            SettingLabel("Izgara Boyutu: $gridColumns sütun")
            Slider(
                value = gridColumns.toFloat(),
                onValueChange = { vm.setGridColumns(it.roundToInt()) },
                valueRange = 3f..7f,
                steps = 3,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }
        item { GroupHeader("Uygulama Sekmesi") }

        item {
            SettingLabel("Sıralama")
            ChipRow(
                options = listOf(
                    "alpha" to "A-Z",
                    "recent" to "Son Kullanılan",
                    "most" to "En Çok Kullanılan"
                ),
                current = drawerSort,
                onSelect = { vm.setDrawerSort(it) }
            )
        }

        item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }
        item { GroupHeader("Arama") }

        item {
            SettingLabel("Arama Motoru")
            ChipRow(
                options = listOf(
                    "google" to "Google",
                    "bing" to "Bing",
                    "duckduckgo" to "DuckDuckGo"
                ),
                current = searchEngine,
                onSelect = { vm.setSearchEngine(it) }
            )
        }

        item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }
        item { GroupHeader("Bildirimler") }

        item {
            SettingTile(
                icon = { Icon(Icons.Filled.Star, contentDescription = null) },
                title = "Bildirim Rozetleri",
                subtitle = "İkonların üzerinde bildirim sayısı",
                trailing = {
                    Switch(checked = badgesEnabled, onCheckedChange = { vm.setBadgesEnabled(it) })
                }
            )
        }

        item {
            OutlinedButton(
                onClick = {
                    runCatching {
                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    }
                },
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth()
            ) {
                Text("Bildirim Erişimini Ayarla")
            }
        }

        item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }
        item { GroupHeader("Gizlilik") }

        item {
            SettingTile(
                icon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                title = "Gizli Uygulamalar",
                subtitle = "Gösterilmeyecek uygulamaları seç",
                onClick = { showHidden = true }
            )
        }

        item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }
        item { GroupHeader("İkon Paketi") }

        item {
            SettingNote(iconPackName ?: "İkon paketi bulunamadı (experimental)")
        }

        item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }
        item { GroupHeader("Veri ve Yedekleme") }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { saveLauncher.launch("oberon-backup.json") },
                    modifier = Modifier.weight(1f)
                ) { Text("Yedek Oluştur") }
                OutlinedButton(
                    onClick = { openLauncher.launch(arrayOf("application/json")) },
                    modifier = Modifier.weight(1f)
                ) { Text("Geri Yükle") }
            }
        }

        item {
            SettingTile(
                icon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                title = "Fabrika Ayarları",
                subtitle = "Tüm tercihleri sıfırla",
                onClick = { showReset = true }
            )
        }

        item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }
        item { GroupHeader("Hakkında") }

        item {
            SettingNote("Oberon Launcher v1.0 — Hafif ve akıcı, Android 13 tarzı. GitHub Actions ile derlenir.")
        }
    }

    if (showHidden) {
        HiddenAppsDialog(vm = vm, onDismiss = { showHidden = false })
    }

    if (showReset) {
        AlertDialog(
            onDismissRequest = { showReset = false },
            title = { Text("Sıfırla") },
            text = { Text("Tüm ayarlar, favoriler ve gizli liste silinecek. Emin misin?") },
            confirmButton = {
                TextButton(onClick = {
                    showReset = false
                    scope.launch {
                        vm.resetAll()
                        toast(context, "Ayarlar sıfırlandı")
                    }
                }) { Text("Sıfırla") }
            },
            dismissButton = { TextButton(onClick = { showReset = false }) { Text("Vazgeç") } }
        )
    }
}

@Composable
private fun GroupHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp)
    )
}

@Composable
private fun ChipRow(
    options: List<Pair<String, String>>,
    current: String,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (value, label) ->
            FilterChip(
                selected = current == value,
                onClick = { onSelect(value) },
                label = { Text(label) }
            )
        }
    }
}

@Composable
private fun SettingTile(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) { icon() }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
        if (trailing != null) trailing()
    }
}

@Composable
private fun SettingNote(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun HiddenAppsDialog(vm: LauncherViewModel, onDismiss: () -> Unit) {
    val apps by vm.apps.collectAsState()
    val hidden by vm.hidden.collectAsState()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Gizli Uygulamalar") },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 360.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 4.dp)
            ) {
                items(apps, key = { it.key }) { app ->
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clickable { vm.toggleHidden(app.packageName) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppIcon(app = app, size = 40.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(app.label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        Switch(
                            checked = app.packageName in hidden,
                            onCheckedChange = { vm.toggleHidden(app.packageName) }
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Kapat") } }
    )
}

private fun toast(context: android.content.Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}