package ru.skinselector.app

import android.content.SharedPreferences
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale

internal data class RollHistoryEntry(
    val championId: String,
    val skinId: String,
    val chromaId: String?,
    val timestamp: Long
)

internal const val MAX_ROLL_HISTORY = 200
private const val ROLL_HISTORY_KEY = "roll_history"

internal fun loadRollHistory(preferences: SharedPreferences): List<RollHistoryEntry> =
    preferences.getString(ROLL_HISTORY_KEY, null)
        ?.lineSequence()
        ?.mapNotNull { line ->
            val fields = line.split('|')
            if (fields.size != 4) return@mapNotNull null
            val time = fields[3].toLongOrNull() ?: return@mapNotNull null
            RollHistoryEntry(fields[0], fields[1], fields[2].ifEmpty { null }, time)
        }
        ?.take(MAX_ROLL_HISTORY)
        ?.toList()
        .orEmpty()

internal fun saveRollHistory(preferences: SharedPreferences, history: List<RollHistoryEntry>) {
    val serialized = history.take(MAX_ROLL_HISTORY).joinToString("\n") {
        "${it.championId}|${it.skinId}|${it.chromaId.orEmpty()}|${it.timestamp}"
    }
    preferences.edit().putString(ROLL_HISTORY_KEY, serialized).apply()
}

@androidx.compose.runtime.Composable
internal fun SettingsToggle(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val palette = LocalSkinPalette.current
    Surface(color = palette.panel, shape = RoundedCornerShape(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, color = palette.ink, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
                Text(description, color = palette.muted, fontSize = 12.sp)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = palette.ink,
                    checkedTrackColor = palette.accent,
                    uncheckedThumbColor = palette.muted,
                    uncheckedTrackColor = palette.page
                )
            )
        }
    }
}

@androidx.compose.runtime.Composable
internal fun RollHistoryScreen(
    champions: List<Champion>,
    history: List<RollHistoryEntry>,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    val palette = LocalSkinPalette.current
    val championById = champions.associateBy { it.id }
    val frequencies = history.groupingBy { "${it.championId}:${it.skinId}:${it.chromaId.orEmpty()}" }.eachCount()
    val chartEntries = frequencies.entries.sortedByDescending { it.value }.take(8)
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(tr(language, "История выпадений", "Roll history"), color = palette.ink, fontSize = 24.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            Text(tr(language, "Частота выпадений", "Roll frequency"), color = palette.muted, fontSize = 14.sp, modifier = Modifier.padding(top = 14.dp, bottom = 8.dp))
            Card(colors = CardDefaults.cardColors(containerColor = palette.panel), shape = RoundedCornerShape(18.dp)) {
                if (chartEntries.isEmpty()) {
                    Box(Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                        Text(tr(language, "График появится после выпадений", "The chart will appear after your first roll"), color = palette.muted)
                    }
                } else {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        val maxCount = chartEntries.maxOf { it.value }.toFloat().coerceAtLeast(1f)
                        chartEntries.forEach { (key, count) ->
                            val parts = key.split(':')
                            val champion = championById[parts[0]]
                            val skin = champion?.skins?.firstOrNull { it.id == parts.getOrNull(1) }
                            Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(skin?.displayName(language) ?: parts.getOrElse(1) { "?" }, color = palette.ink, fontSize = 12.sp, modifier = Modifier.weight(1f))
                                Box(Modifier.weight(1.3f).height(12.dp).clip(RoundedCornerShape(8.dp)).background(palette.page)) {
                                    Box(Modifier.fillMaxWidth(count / maxCount).height(12.dp).clip(RoundedCornerShape(8.dp)).background(palette.accent))
                                }
                                Text("  $count", color = palette.muted, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
            Text(tr(language, "Последние результаты", "Recent rolls"), color = palette.muted, fontSize = 14.sp, modifier = Modifier.padding(top = 10.dp))
        }
        if (history.isEmpty()) {
            item { Text(tr(language, "Пока выпадений нет", "No rolls yet"), color = palette.muted, modifier = Modifier.padding(vertical = 16.dp)) }
        } else items(history) { entry ->
            val champion = championById[entry.championId]
            val skin = champion?.skins?.firstOrNull { it.id == entry.skinId }
            val chroma = skin?.chromas?.firstOrNull { it.id == entry.chromaId }
            Card(colors = CardDefaults.cardColors(containerColor = palette.panel), shape = RoundedCornerShape(14.dp)) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    SkinThumbnail(path = chroma?.assetPath ?: skin?.assetPath ?: "skins/${entry.championId}/${entry.skinId}.png", contentDescription = skin?.displayName(language) ?: entry.skinId, fallbackText = "✦")
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(skin?.displayName(language) ?: entry.skinId, color = palette.ink, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
                        Text(listOfNotNull(champion?.displayName(language), chroma?.displayName(language)).joinToString(" · "), color = palette.muted, fontSize = 12.sp)
                    }
                    Text(SimpleDateFormat("dd.MM HH:mm", Locale.getDefault()).format(Date(entry.timestamp)), color = palette.muted, fontSize = 10.sp)
                }
            }
        }
    }
}
