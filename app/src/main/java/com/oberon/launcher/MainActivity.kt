package com.oberon.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.oberon.launcher.launcher.AppDrawer
import com.oberon.launcher.launcher.HomeScreen
import com.oberon.launcher.launcher.LauncherSurface
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
        var surface by rememberSaveable { mutableStateOf(LauncherSurface.Home) }

        BackHandler(enabled = surface != LauncherSurface.Home) {
            surface = LauncherSurface.Home
        }

        AnimatedContent(
            targetState = surface,
            transitionSpec = {
                when {
                    targetState == LauncherSurface.Drawer && initialState == LauncherSurface.Home ->
                        (slideInVertically(tween(260)) { it } + fadeIn(tween(220)))
                            .togetherWith(fadeOut(tween(140)))

                    targetState == LauncherSurface.Settings && initialState == LauncherSurface.Home ->
                        (slideInHorizontally(tween(260)) { it } + fadeIn(tween(220)))
                            .togetherWith(fadeOut(tween(140)))

                    else -> fadeIn(tween(180)).togetherWith(fadeOut(tween(140)))
                }
            },
            label = "launcher-surface"
        ) { target ->
            when (target) {
                LauncherSurface.Home -> HomeScreen(
                    vm = vm,
                    onOpenDrawer = { surface = LauncherSurface.Drawer },
                    onOpenSettings = { surface = LauncherSurface.Settings }
                )

                LauncherSurface.Drawer -> AppDrawer(
                    vm = vm,
                    onClose = { surface = LauncherSurface.Home },
                    onOpenSettings = { surface = LauncherSurface.Settings }
                )

                LauncherSurface.Settings -> SettingsScreen(
                    vm = vm,
                    onClose = { surface = LauncherSurface.Home }
                )
            }
        }
    }
}