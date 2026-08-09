package com.oberon.launcher.data

import android.graphics.drawable.Drawable

data class AppInfo(
    val packageName: String,
    val activityName: String,
    val label: String,
    val icon: Drawable?,
    val lastUsed: Long = 0L,
    val useCount: Long = 0L
) {
    val key: String get() = "$packageName/$activityName"
}