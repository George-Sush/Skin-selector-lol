package ru.skinselector.app

import android.app.DatePickerDialog
import android.content.SharedPreferences
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

internal data class RollHistoryEntry(
    val championId: String,
    val skinId: String,
    val chromaId: String?,
    val timestamp: Long
)

internal const val MAX_ROLL_HISTORY = 200
private const val ROLL_HISTORY_KEY = "roll_history"

enum class HistoryPeriod(val millis: Long) {
    ALL(0L), HOUR(60 * 60 * 1000L), DAY(24 * 60 * 60 * 1000L),
    WEEK(7 * 24 * 60 * 60 * 1000L), MONTH(30 * 24 * 60 * 60 * 1000L), YEAR(365 * 24 * 60 * 60 * 1000L)
}

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

internal fun clearHistoryBefore(history: List<RollHistoryEntry>, period: HistoryPeriod, now: Long = System.currentTimeMillis()): List<RollHistoryEntry> {
    if (period == HistoryPeriod.ALL) return emptyList()
    val threshold = now - period.millis
    return history.filter { it.timestamp < threshold }
}

@Composable
internal fun SettingsToggle(title: String, description: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val palette = LocalSkinPalette.current
    Surface(color = palette.panel, shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, color = palette.ink, fontWeight = FontWeight.Medium)
                Text(description, color = palette.muted, fontSize = 12.sp)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange, colors = SwitchDefaults.colors(
                checkedThumbColor = palette.ink, checkedTrackColor = palette.accent,
                uncheckedThumbColor = palette.muted, uncheckedTrackColor = palette.page
            ))
        }
    }
}

@Composable
internal fun RollHistoryScreen(
    champions: List<Champion>,
    history: List<RollHistoryEntry>,
    language: AppLanguage,
    modifier: Modifier = Modifier,
    onClearHistory: (HistoryPeriod) -> Unit
) {
    val palette = LocalSkinPalette.current
    val championById = champions.associateBy { it.id }
    var selectedChampionId by remember { mutableStateOf<String?>(null) }
    var championSearch by remember { mutableStateOf("") }
    var selectedPeriod by remember { mutableStateOf(HistoryPeriod.ALL) }
    var selectedDayStart by remember { mutableStateOf<Long?>(null) }
    var selectedDayLabel by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    var championMenuExpanded by remember { mutableStateOf(false) }
    var periodMenuExpanded by remember { mutableStateOf(false) }
    var expandedTimestamp by remember { mutableStateOf<Long?>(null) }
    var clearDialogOpen by remember { mutableStateOf(false) }
    var clearConfirmOpen by remember { mutableStateOf(false) }
    var clearPeriod by remember { mutableStateOf(HistoryPeriod.ALL) }

    val now = System.currentTimeMillis()
    val filteredHistory = history.filter { entry ->
        val championMatches = selectedChampionId == null || entry.championId == selectedChampionId
        val periodMatches = selectedPeriod == HistoryPeriod.ALL || entry.timestamp >= now - selectedPeriod.millis
        val dayMatches = selectedDayStart == null || entry.timestamp in selectedDayStart!! until (selectedDayStart!! + 24 * 60 * 60 * 1000L)
        championMatches && periodMatches && dayMatches
    }
    val frequencies = filteredHistory.groupingBy { "${it.championId}:${it.skinId}:${it.chromaId.orEmpty()}" }.eachCount()
    val chartEntries = frequencies.entries.sortedByDescending { it.value }.take(8)
    val maxCount = chartEntries.maxOfOrNull { it.value }?.toFloat()?.coerceAtLeast(1f) ?: 1f
    val dayCounts = (0..6).map { day ->
        val start = now - (day + 1) * 24 * 60 * 60 * 1000L
        val end = now - day * 24 * 60 * 60 * 1000L
        filteredHistory.count { it.timestamp in start until end }
    }.reversed()
    val maxDayCount = dayCounts.maxOrNull()?.coerceAtLeast(1) ?: 1
    val searchableChampions = champions.filter { it.matchesSearch(championSearch) }

    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text(tr(language, "История выпадений", "Roll history"), color = palette.ink, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(tr(language, "Фильтры", "Filters"), color = palette.muted, fontSize = 13.sp, modifier = Modifier.padding(top = 14.dp, bottom = 6.dp))
            OutlinedTextField(
                value = championSearch,
                onValueChange = { championSearch = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(tr(language, "Поиск чемпиона", "Search champion")) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = palette.ink, unfocusedTextColor = palette.ink,
                    focusedBorderColor = palette.accent, unfocusedBorderColor = palette.muted,
                    focusedLabelColor = palette.accent, unfocusedLabelColor = palette.muted,
                    cursorColor = palette.accent
                )
            )
            if (championSearch.isNotBlank()) {
                if (searchableChampions.isEmpty()) {
                    Text(
                        tr(language, "Персонажи не найдены", "No champions found"),
                        color = palette.muted,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                } else {
                    searchableChampions.take(8).forEach { match ->
                        TextButton(
                            onClick = {
                                selectedChampionId = match.id
                                championSearch = ""
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(match.displayName(language), color = palette.ink, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Box(Modifier.weight(1f)) {
                    FilterButton(
                        label = selectedChampionId?.let { championById[it]?.displayName(language) } ?: tr(language, "Все чемпионы", "All champions"),
                        onClick = { championMenuExpanded = true }
                    )
                    DropdownMenu(championMenuExpanded, { championMenuExpanded = false }) {
                        DropdownMenuItem(text = { Text(tr(language, "Все чемпионы", "All champions")) }, onClick = { selectedChampionId = null; championMenuExpanded = false })
                        searchableChampions.forEach { champion ->
                            DropdownMenuItem(text = { Text(champion.displayName(language)) }, onClick = { selectedChampionId = champion.id; championMenuExpanded = false })
                        }
                    }
                }
                Box(Modifier.weight(1f)) {
                    FilterButton(label = historyPeriodLabel(selectedPeriod, language), onClick = { periodMenuExpanded = true })
                    DropdownMenu(periodMenuExpanded, { periodMenuExpanded = false }) {
                        listOf(HistoryPeriod.ALL, HistoryPeriod.HOUR, HistoryPeriod.DAY, HistoryPeriod.WEEK, HistoryPeriod.MONTH, HistoryPeriod.YEAR).forEach { period ->
                            DropdownMenuItem(text = { Text(historyPeriodLabel(period, language)) }, onClick = { selectedPeriod = period; periodMenuExpanded = false })
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                FilterButton(
                    label = selectedDayLabel ?: tr(language, "Выбрать день", "Choose day"),
                    onClick = {
                        val calendar = Calendar.getInstance()
                        DatePickerDialog(context, { _, year, month, day ->
                            val selected = Calendar.getInstance().apply {
                                set(year, month, day, 0, 0, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            selectedDayStart = selected.timeInMillis
                            selectedDayLabel = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(selected.timeInMillis))
                        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
                    }
                )
                if (selectedDayStart != null) {
                    TextButton(onClick = { selectedDayStart = null; selectedDayLabel = null }) { Text(tr(language, "Сбросить", "Reset"), color = Color(0xFFC53945)) }
                }
            }
            Button(
                onClick = { clearDialogOpen = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC53945), contentColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) { Text(tr(language, "Очистить историю", "Clear history"), fontWeight = FontWeight.Bold) }
        }
        item {
            Text(tr(language, "Статистика", "Statistics"), color = palette.muted, fontSize = 14.sp)
            Card(colors = CardDefaults.cardColors(containerColor = palette.panel), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        StatItem(tr(language, "Всего", "Total"), filteredHistory.size.toString())
                        StatItem(tr(language, "Уникальных", "Unique"), frequencies.size.toString())
                        StatItem(tr(language, "За неделю", "This week"), filteredHistory.count { it.timestamp >= now - HistoryPeriod.WEEK.millis }.toString())
                    }
                    Text(tr(language, "Последние 7 дней", "Last 7 days"), color = palette.muted, fontSize = 12.sp)
                    Row(Modifier.fillMaxWidth().height(100.dp), horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.Bottom) {
                        dayCounts.forEachIndexed { index, count ->
                            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                                Text(if (count == 0) "" else count.toString(), color = palette.muted, fontSize = 10.sp)
                                Box(Modifier.fillMaxWidth().height((count.toFloat() / maxDayCount * 68f).coerceAtLeast(if (count > 0) 8f else 2f).dp).clip(RoundedCornerShape(5.dp)).background(palette.accent))
                                Text(weekdayLabel(index, language), color = palette.muted, fontSize = 9.sp)
                            }
                        }
                    }
                }
            }
        }
        item {
            Text(tr(language, "Самые частые результаты", "Most frequent results"), color = palette.muted, fontSize = 14.sp)
            Card(colors = CardDefaults.cardColors(containerColor = palette.panel), shape = RoundedCornerShape(18.dp)) {
                if (chartEntries.isEmpty()) {
                    Box(Modifier.fillMaxWidth().height(130.dp), contentAlignment = Alignment.Center) { Text(tr(language, "График появится после выпадений", "The chart will appear after your first roll"), color = palette.muted) }
                } else Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    chartEntries.forEach { (key, count) ->
                        val parts = key.split(':')
                        val champion = championById[parts[0]]
                        val skin = champion?.skins?.firstOrNull { it.id == parts.getOrNull(1) }
                        Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(skin?.displayName(language) ?: parts.getOrElse(1) { "?" }, color = palette.ink, fontSize = 12.sp, modifier = Modifier.weight(1f))
                            Box(Modifier.weight(1.2f).height(12.dp).clip(RoundedCornerShape(8.dp)).background(palette.page)) { Box(Modifier.fillMaxWidth(count / maxCount).height(12.dp).clip(RoundedCornerShape(8.dp)).background(palette.accent)) }
                            Text("  $count", color = palette.muted, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
        if (filteredHistory.isEmpty()) item { Text(tr(language, "По выбранным фильтрам выпадений нет", "No rolls match the selected filters"), color = palette.muted, modifier = Modifier.padding(vertical = 16.dp)) }
        else items(filteredHistory, key = { "${it.timestamp}_${it.championId}_${it.skinId}" }) { entry ->
            val champion = championById[entry.championId]
            val skin = champion?.skins?.firstOrNull { it.id == entry.skinId }
            val chroma = skin?.chromas?.firstOrNull { it.id == entry.chromaId }
            val expanded = expandedTimestamp == entry.timestamp
            Card(colors = CardDefaults.cardColors(containerColor = palette.panel), shape = RoundedCornerShape(14.dp), modifier = Modifier.clickable { expandedTimestamp = if (expanded) null else entry.timestamp }) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    SkinThumbnail(path = chroma?.assetPath ?: skin?.assetPath ?: "skins/${entry.championId}/${entry.skinId}.png", contentDescription = skin?.displayName(language) ?: entry.skinId, fallbackText = "✦")
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(skin?.displayName(language) ?: entry.skinId, color = palette.ink, fontWeight = FontWeight.Medium)
                        Text(listOfNotNull(champion?.displayName(language), chroma?.displayName(language)).joinToString(" · "), color = palette.muted, fontSize = 12.sp)
                        if (expanded) Text(SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()).format(Date(entry.timestamp)), color = palette.highlight, fontSize = 11.sp, modifier = Modifier.padding(top = 5.dp))
                    }
                    Text(SimpleDateFormat("dd.MM HH:mm", Locale.getDefault()).format(Date(entry.timestamp)), color = palette.muted, fontSize = 10.sp)
                }
            }
        }
    }

    if (clearDialogOpen) {
        AlertDialog(
            onDismissRequest = { clearDialogOpen = false },
            title = { Text(tr(language, "Очистить историю?" , "Clear history?")) },
            text = {
                Column {
                    Text(tr(language, "Выбери период для удаления:", "Choose a period to delete:"), color = palette.muted)
                    listOf(HistoryPeriod.HOUR, HistoryPeriod.DAY, HistoryPeriod.WEEK, HistoryPeriod.MONTH, HistoryPeriod.YEAR, HistoryPeriod.ALL).forEach { period ->
                        Row(Modifier.fillMaxWidth().clickable { clearPeriod = period }, verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = clearPeriod == period, onClick = { clearPeriod = period }, colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFC53945)))
                            Text(historyPeriodLabel(period, language), color = Color.Black)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { clearDialogOpen = false; clearConfirmOpen = true }) { Text(tr(language, "Продолжить", "Continue"), color = Color(0xFFC53945)) } },
            dismissButton = { TextButton(onClick = { clearDialogOpen = false }) { Text(tr(language, "Отмена", "Cancel"), color = palette.muted) } }
        )
    }
    if (clearConfirmOpen) {
        AlertDialog(
            onDismissRequest = { clearConfirmOpen = false },
            title = { Text(tr(language, "Подтвердить удаление?", "Confirm deletion?")) },
            text = { Text(tr(language, "Записи за период «${historyPeriodLabel(clearPeriod, language)}» будут удалены без возможности восстановления.", "Rolls from “${historyPeriodLabel(clearPeriod, language)}” will be permanently deleted."), color = palette.muted) },
            confirmButton = { Button(onClick = { onClearHistory(clearPeriod); clearConfirmOpen = false }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC53945), contentColor = Color.White)) { Text(tr(language, "Удалить", "Delete")) } },
            dismissButton = { TextButton(onClick = { clearConfirmOpen = false }) { Text(tr(language, "Отмена", "Cancel"), color = palette.muted) } }
        )
    }
}

@Composable
private fun FilterButton(label: String, onClick: () -> Unit) {
    val palette = LocalSkinPalette.current
    Surface(Modifier.fillMaxWidth().clickable(onClick = onClick), color = palette.panel, shape = RoundedCornerShape(12.dp)) {
        Text(label, color = palette.ink, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp), maxLines = 1)
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    val palette = LocalSkinPalette.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(value, color = palette.ink, fontWeight = FontWeight.Bold, fontSize = 18.sp); Text(label, color = palette.muted, fontSize = 10.sp) }
}

private fun weekdayLabel(index: Int, language: AppLanguage): String {
    val names = if (language == AppLanguage.ENGLISH) listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun") else listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.DAY_OF_YEAR, index - 6)
    val day = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7
    return names[day]
}

private fun historyPeriodLabel(period: HistoryPeriod, language: AppLanguage): String = when (period) {
    HistoryPeriod.ALL -> tr(language, "Всё время", "All time")
    HistoryPeriod.HOUR -> tr(language, "Последний час", "Last hour")
    HistoryPeriod.DAY -> tr(language, "Последний день", "Last day")
    HistoryPeriod.WEEK -> tr(language, "Последнюю неделю", "Last week")
    HistoryPeriod.MONTH -> tr(language, "Последний месяц", "Last month")
    HistoryPeriod.YEAR -> tr(language, "Последний год", "Last year")
}
