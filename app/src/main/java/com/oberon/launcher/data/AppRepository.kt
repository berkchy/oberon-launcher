package com.oberon.launcher.data

import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo

class AppRepository(private val context: Context) {

    fun loadApps(): List<AppInfo> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = runCatching { pm.queryIntentActivities(intent, 0) }.getOrDefault(emptyList())
        return resolved
            .asSequence()
            .filter { it.activityInfo.packageName != context.packageName }
            .mapNotNull { ri ->
                runCatching { toAppInfo(ri) }.getOrNull()
            }
            .distinctBy { it.key }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    fun findKey(key: String): AppInfo? {
        val split = key.split("/", limit = 2)
        if (split.size != 2) return null
        return runCatching {
            val ri = context.packageManager.resolveActivity(
                Intent().apply { setClassName(split[0], split[1]) },
                0
            ) ?: return null
            toAppInfo(ri)
        }.getOrNull()
    }

    private fun toAppInfo(ri: ResolveInfo): AppInfo {
        val pm = context.packageManager
        val activity = ri.activityInfo
        return AppInfo(
            packageName = activity.packageName,
            activityName = activity.name,
            label = ri.loadLabel(pm)?.toString() ?: activity.packageName,
            icon = ri.loadIcon(pm)
        )
    }

    fun launchApp(key: String): Boolean {
        val split = key.split("/", limit = 2)
        if (split.size != 2) return false
        return runCatching {
            val intent = Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setClassName(split[0], split[1])
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (intent.resolveActivity(context.packageManager) == null) return false
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }
}