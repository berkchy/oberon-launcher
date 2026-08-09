package com.oberon.launcher.data

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.drawable.Drawable

/**
 * Minimal ikon paketi desteği. 'com.fede.launcher.THEME_ICONPACK' intentiyle ilan
 * edilen paketleri tarar ve drawable kaynaklarını paket adına göre çözmeye çalışır.
 */
class IconPackLoader(context: Context) {

    private class Pack(val packageName: String, val label: String, val res: Resources?)

    private val packs: List<Pack> = runCatching {
        val pm = context.packageManager
        val intent = Intent("com.fede.launcher.THEME_ICONPACK")
        pm.queryIntentActivities(intent, 0)
            .mapNotNull { ri ->
                val pkg = ri.activityInfo.packageName
                val label = runCatching { ri.loadLabel(pm).toString() }.getOrDefault(pkg)
                val res = runCatching {
                    context.createPackageContext(pkg, Context.CONTEXT_INCLUDE_CODE).resources
                }.getOrNull()
                Pack(pkg, label, res)
            }
    }.getOrDefault(emptyList())

    val installedPackName: String? get() = packs.firstOrNull()?.label

    fun iconFor(packageName: String): Drawable? {
        for (pack in packs) {
            val res = pack.res ?: continue
            val candidateNames = listOf(packageName, packageName.replace('.', '_'))
            for (name in candidateNames) {
                val id = res.getIdentifier(name, "drawable", pack.packageName)
                if (id != 0) {
                    val d = runCatching { res.getDrawable(id, null) }.getOrNull()
                    if (d != null) return d
                }
            }
        }
        return null
    }
}