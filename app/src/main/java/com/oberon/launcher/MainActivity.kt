package com.oberon.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.oberon.launcher.launcher.AppDrawer
import com.oberon.launcher.launcher.BottomNav
import com.oberon.launcher.launcher.HomeScreen
import com.oberon.launcher.launcher.LauncherTab
import com.oberon.launcher.launcher.LauncherViewModel
import com.oberon.launcher.settings.SettingsScreen
import com.oberon.launcher.theme.OberonTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OberonLauncherApp()
        }
    }
}

@Composable
fun OberonLauncherApp(vm: LauncherViewModel = viewModel()) {
    val themeMode by vm.themeMode.collectAsState()
    val accentOption by vm.accentOption.collectAsState()
    val accentColor by vm.accentColor.collectAsState()

    val darkTheme = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }

    OberonTheme(
        darkTheme = darkTheme,
        accentOption = accentOption,
        accentColor = Color(accentColor)
    ) {
        var tab by rememberSaveable { mutableStateOf(LauncherTab.Home) }

        Scaffold(
            bottomBar = { BottomNav(selected = tab, onSelect = { tab = it }) }
        ) { padding ->
            AnimatedContent(
                targetState = tab,
                transitionSpec = {
                    (fadeIn(androidx.compose.animation.core.tween(220)) +
                        slideInHorizontally { it / 6 })
                        .togetherWith(
                            fadeOut(androidx.compose.animation.core.tween(120)) +
                                slideOutHorizontally { -it / 6 }
                        )
                },
                label = "launcher-tabs"
            ) { target ->
                when (target) {
                    LauncherTab.Home -> HomeScreen(vm, Modifier.padding(bottom = padding.calculateBottomPadding()))
                    LauncherTab.Apps -> AppDrawer(vm, Modifier.padding(bottom = padding.calculateBottomPadding()))
                    LauncherTab.Settings -> SettingsScreen(vm, Modifier.padding(bottom = padding.calculateBottomPadding()))
                }
            }
        }
    }
}