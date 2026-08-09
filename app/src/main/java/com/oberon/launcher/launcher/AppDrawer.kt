package com.oberon.launcher.launcher

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.oberon.launcher.data.AppInfo

@Composable
fun AppDrawer(
    vm: LauncherViewModel,
    onClose: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sortedApps by vm.sortedVisibleApps.collectAsState()
    val gridColumns by vm.gridColumns.collectAsState()
    val badges by vm.badges.collectAsState()
    val drawerSort by vm.drawerSort.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    var sortExpanded by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<AppInfo?>(null) }
    var dragAcc by remember { mutableStateOf(0f) }

    val filtered = remember(sortedApps, query) {
        if (query.isNotBlank()) {
            sortedApps.filter {
                it.label.contains(query, ignoreCase = true) ||
                    it.packageName.contains(query, ignoreCase = true)
            }
        } else sortedApps
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onVerticalDrag = { _, dy -> dragAcc += dy },
                    onDragEnd = {
                        if (dragAcc > 140.dp.toPx()) onClose()
                        dragAcc = 0f
                    },
                    onDragCancel = { dragAcc = 0f }
                )
            }
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Kapat")
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Uygulama ara…") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotBlank()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Filled.Clear, contentDescription = null)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(28.dp)
            )
            Box {
                IconButton(onClick = { sortExpanded = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Sıralama")
                }
                DropdownMenu(expanded = sortExpanded, onDismissRequest = { sortExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text("A-Z") },
                        onClick = { vm.setDrawerSort("alpha"); sortExpanded = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Son Kullanılan") },
                        onClick = { vm.setDrawerSort("recent"); sortExpanded = false }
                    )
                    DropdownMenuItem(
                        text = { Text("En Çok Kullanılan") },
                        onClick = { vm.setDrawerSort("most"); sortExpanded = false }
                    )
                }
            }
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Filled.Settings, contentDescription = "Ayarlar")
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = sortLabel(drawerSort),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(gridColumns.coerceIn(3, 7)),
            modifier = Modifier.fillMaxSize().navigationBarsPadding(),
            contentPadding = PaddingValues(
                horizontal = 12.dp,
                vertical = 8.dp
            ),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(filtered, key = { it.key }) { app ->
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
            if (filtered.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = "Sonuç yok",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(vertical = 24.dp)
                    )
                }
            }
        }
    }

    selected?.let { app ->
        AppActionsMenu(
            app = app,
            onHomeScreen = false,
            onOpen = { vm.launch(app) },
            onAddToHome = { vm.addFavorite(app.key) },
            onRemoveFromHome = {},
            onAppInfo = { vm.openAppInfo(app) },
            onHide = { vm.toggleHidden(app.packageName) },
            onDismiss = { selected = null }
        )
    }
}

private fun sortLabel(sort: String): String = when (sort) {
    "recent" -> "Son kullanılanlara göre"
    "most" -> "En çok kullanılanlara göre"
    else -> "Alfabetik sıraya göre"
}