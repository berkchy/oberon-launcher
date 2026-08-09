package com.oberon.launcher.launcher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.oberon.launcher.data.AppInfo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(vm: LauncherViewModel, modifier: Modifier = Modifier) {
    val gridColumns by vm.gridColumns.collectAsState()
    val favorites by vm.favorites.collectAsState()
    val visible by vm.visibleApps.collectAsState()
    val badges by vm.badges.collectAsState()
    val searchEngine by vm.searchEngine.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }

    val filtered = remember(visible, query) {
        if (query.isNotBlank()) {
            visible.filter {
                it.label.contains(query, ignoreCase = true) ||
                    it.packageName.contains(query, ignoreCase = true)
            }
        } else visible
    }

    val homeApps = remember(filtered, favorites, visible, query) {
        if (query.isNotBlank()) filtered
        else {
            val favs = favorites.mapNotNull { key -> visible.find { it.key == key } }
            if (favs.isEmpty()) visible else favs
        }
    }

    val dockApps = remember(favorites, visible) {
        favorites.mapNotNull { key -> visible.find { it.key == key } }.take(5)
    }

    var selected by remember { mutableStateOf<AppInfo?>(null) }

    Column(modifier = modifier.fillMaxSize().statusBarsPadding()) {
        Clock()
        Spacer(Modifier.height(12.dp))

        SearchField(
            value = query,
            onValueChange = { query = it },
            onSearch = { vm.webSearch(query, searchEngine) }
        )
        Spacer(Modifier.height(4.dp))

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

        Dock(dockApps = dockApps, vm = vm, onItemLongClick = { selected = it })
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
}

@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
        placeholder = { Text("Uygulama veya web ara…") },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (value.isNotBlank()) {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(Icons.Filled.Clear, contentDescription = null)
                }
            }
        },
        singleLine = true,
        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { onSearch() }),
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            imeAction = androidx.compose.ui.text.input.ImeAction.Search
        ),
        shape = RoundedCornerShape(28.dp)
    )
}

@Composable
private fun Clock() {
    val now by produceState(initialValue = 0L) {
        while (true) {
            value = System.currentTimeMillis()
            delay(1000)
        }
    }
    val timeFmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateFmt = remember { SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()) }
    val date = remember(now) { Date(now) }

    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = timeFmt.format(date),
            style = MaterialTheme.typography.displayMedium,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Light
        )
        Text(
            text = dateFmt.format(date).replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun EmptyHomeHint() {
    Text(
        text = "Ana ekran boş. Uygulamalar sekmesinden ikonlara uzun basarak beni doldur.",
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 24.dp)
    )
}

@Composable
private fun Dock(
    dockApps: List<AppInfo>,
    vm: LauncherViewModel,
    onItemLongClick: (AppInfo) -> Unit
) {
    if (dockApps.isEmpty()) return
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            dockApps.forEach { app ->
                Box {
                    val badge = vm.badges.value[app.packageName] ?: 0
                    val packIcon = remember(app.packageName) { vm.packIconFor(app.packageName) }
                    AppGridItem(
                        app = app,
                        badgeCount = badge,
                        onClick = { vm.launch(app) },
                        onLongClick = { onItemLongClick(app) },
                        packIcon = packIcon,
                        modifier = Modifier.width(72.dp)
                    )
                }
            }
        }
    }
}