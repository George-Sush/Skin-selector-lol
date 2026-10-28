package ru.skinselector.app

import android.os.Bundle
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException

internal val LocalSkinPalette = staticCompositionLocalOf { SkinPalette.forTheme(ColorTheme.VIOLET) }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val preferences = getSharedPreferences("skin_selector", MODE_PRIVATE)
        val champions = ChampionCatalog.load(this)
        setContent {
            MaterialTheme {
                SkinSelectorApp(
                    champions = champions,
                    preferences = preferences,
                    onSave = { key, value -> preferences.edit().putString(key, value).apply() },
                    onSaveSet = { key, value -> saveStringSet(preferences, key, value) }
                )
            }
        }
    }
}

@Composable
private fun SkinSelectorApp(
    champions: List<Champion>,
    preferences: SharedPreferences,
    onSave: (String, String) -> Unit,
    onSaveSet: (String, Set<String>) -> Unit
) {
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    var languageValue by rememberSaveable {
        mutableStateOf(preferences.getString("language", AppLanguage.RUSSIAN.preferenceValue) ?: "ru")
    }
    val language = AppLanguage.fromPreference(languageValue)
    var enabledChampions by remember {
        mutableStateOf(
            preferences.getStringSet("enabled_champions", null)?.toSet()
                ?: champions.map { it.id }.toSet()
        )
    }
    var favoriteChampions by remember {
        mutableStateOf(preferences.getStringSet("favorite_champions", null)?.toSet().orEmpty())
    }
    var championSearch by rememberSaveable { mutableStateOf("") }
    var selectedChampionId by rememberSaveable {
        mutableStateOf(
            preferences.getString("champion", champions.first().id) ?: champions.first().id
        )
    }
    if (enabledChampions.isNotEmpty() && selectedChampionId !in enabledChampions) {
        selectedChampionId = champions.firstOrNull { it.id in enabledChampions }?.id ?: champions.first().id
    }
    var modeName by rememberSaveable {
        mutableStateOf(preferences.getString("mode", RollMode.SKINS_ONLY.name) ?: RollMode.SKINS_ONLY.name)
    }
    var selectedThemeValue by rememberSaveable {
        mutableStateOf(preferences.getString("theme", ColorTheme.VIOLET.preferenceValue) ?: "violet")
    }
    var autoThemeEnabled by rememberSaveable {
        mutableStateOf(preferences.getBoolean("auto_skin_theme", false))
    }
    var splashOpacity by rememberSaveable {
        mutableStateOf(preferences.getFloat("splash_opacity", 0.24f).coerceIn(0f, 1f))
    }
    var uniqueRollsEnabled by rememberSaveable {
        mutableStateOf(preferences.getBoolean("unique_rolls", true))
    }
    var revealAnimationEnabled by rememberSaveable {
        mutableStateOf(preferences.getBoolean("reveal_animation", true))
    }
    var rollHistory by remember {
        mutableStateOf(loadRollHistory(preferences))
    }
    var revealAnimationTrigger by rememberSaveable { mutableStateOf(0) }
    val mode = RollMode.entries.firstOrNull { it.name == modeName } ?: RollMode.SKINS_ONLY
    val champion = champions.firstOrNull { it.id == selectedChampionId } ?: champions.first()
    var enabledSkins by remember(champion.id) {
        mutableStateOf(
            preferences.getStringSet("skins_${champion.id}", null)
                ?.toSet()
                ?: champion.skins.map { it.id }.toSet()
        )
    }
    var enabledChromas by remember(champion.id) {
        mutableStateOf(
            preferences.getStringSet("chromas_${champion.id}", null)
                ?.toSet()
                ?: champion.skins.flatMap { skin -> skin.chromas.map { "${skin.id}:${it.id}" } }.toSet()
        )
    }
    var resultSkinId by rememberSaveable { mutableStateOf<String?>(null) }
    var resultChromaId by rememberSaveable { mutableStateOf<String?>(null) }
    var expandedSkinIds by rememberSaveable { mutableStateOf("") }
    val selectedTheme = ColorTheme.fromPreference(selectedThemeValue)
    val activeTheme = if (autoThemeEnabled && champion.id == "teemo" && resultSkinId != null) {
        themeForTeemoSkin(resultSkinId)
    } else {
        selectedTheme
    }

    CompositionLocalProvider(LocalSkinPalette provides SkinPalette.forTheme(activeTheme)) {
        val palette = LocalSkinPalette.current
        Scaffold(
            containerColor = palette.page,
            bottomBar = {
                if (!settingsOpen) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(palette.panel)
                            .padding(horizontal = 28.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        TabItem("🎲", tr(language, "Рандом", "Random"), selectedTab == 0) { selectedTab = 0 }
                        TabItem("▤", tr(language, "История", "History"), selectedTab == 1) { selectedTab = 1 }
                        TabItem("✦", tr(language, "Ещё", "More"), selectedTab == 2) { selectedTab = 2 }
                    }
                }
            }
        ) { padding ->
            if (settingsOpen) {
                SettingsScreen(
                    champions = champions,
                    enabledChampions = enabledChampions,
                    language = language,
                    selectedTheme = selectedTheme,
                    autoThemeEnabled = autoThemeEnabled,
                    splashOpacity = splashOpacity,
                    uniqueRollsEnabled = uniqueRollsEnabled,
                    revealAnimationEnabled = revealAnimationEnabled,
                    favoriteChampions = favoriteChampions,
                    modifier = Modifier.padding(padding),
                    onBack = { settingsOpen = false },
                    onLanguageChange = {
                        languageValue = it.preferenceValue
                        onSave("language", it.preferenceValue)
                    },
                    onThemeChange = {
                        selectedThemeValue = it.preferenceValue
                        onSave("theme", it.preferenceValue)
                    },
                    onAutoThemeChange = {
                        autoThemeEnabled = it
                        preferences.edit().putBoolean("auto_skin_theme", it).apply()
                    },
                    onSplashOpacityChange = {
                        splashOpacity = it
                        preferences.edit().putFloat("splash_opacity", it).apply()
                    },
                    onUniqueRollsChange = {
                        uniqueRollsEnabled = it
                        preferences.edit().putBoolean("unique_rolls", it).apply()
                    },
                    onRevealAnimationChange = {
                        revealAnimationEnabled = it
                        preferences.edit().putBoolean("reveal_animation", it).apply()
                    },
                    onEnableAllChampions = {
                        val allIds = champions.map { champion -> champion.id }.toSet()
                        enabledChampions = allIds
                        onSaveSet("enabled_champions", allIds)
                    },
                    onDisableAllChampions = {
                        enabledChampions = emptySet()
                        onSaveSet("enabled_champions", emptySet())
                        resultSkinId = null
                        resultChromaId = null
                    },
                    onDisableNonFavoriteChampions = {
                        val favoriteIds = favoriteChampions.intersect(champions.map { it.id }.toSet())
                        enabledChampions = favoriteIds
                        onSaveSet("enabled_champions", favoriteIds)
                        if (selectedChampionId !in favoriteIds) {
                            favoriteIds.firstOrNull()?.let { id ->
                                selectedChampionId = id
                                onSave("champion", id)
                            }
                            resultSkinId = null
                            resultChromaId = null
                        }
                    },
                    onFavoriteToggle = { id, favorite ->
                        favoriteChampions = favoriteChampions.toMutableSet().apply {
                            if (favorite) add(id) else remove(id)
                        }
                        onSaveSet("favorite_champions", favoriteChampions)
                    },
                    onChampionToggle = { id, enabled ->
                        val next = enabledChampions.toMutableSet().apply {
                            if (enabled) add(id) else remove(id)
                        }
                        enabledChampions = next
                        onSaveSet("enabled_champions", next)
                        if (enabled && selectedChampionId !in next) {
                            selectedChampionId = id
                            onSave("champion", id)
                            resultSkinId = null
                            resultChromaId = null
                        }
                        if (selectedChampionId == id && !enabled) {
                            val nextChampion = champions.firstOrNull { it.id in next }
                            if (nextChampion != null) {
                                selectedChampionId = nextChampion.id
                                onSave("champion", nextChampion.id)
                            }
                            resultSkinId = null
                            resultChromaId = null
                        }
                    }
                )
            } else if (selectedTab == 0) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    top = 24.dp,
                    bottom = 28.dp
                ),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                item {
                    Header(language, onSettings = { settingsOpen = true })
                }
                item {
                    if (enabledChampions.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                tr(language, "Все персонажи отключены", "All champions are disabled"),
                                color = palette.ink,
                                fontWeight = FontWeight.SemiBold
                            )
                            TextButton(onClick = { settingsOpen = true }) {
                                Text(tr(language, "Открыть настройки", "Open settings"), color = palette.accent)
                            }
                        }
                    } else {
                        ChampionPicker(
                            champion = champion,
                            champions = champions.filter { it.id in enabledChampions }
                                .sortedWith(compareByDescending<Champion> { it.id in favoriteChampions }
                                    .thenBy { it.displayName(language) }),
                            language = language,
                            onSelect = {
                                selectedChampionId = it.id
                                onSave("champion", it.id)
                                resultSkinId = null
                                resultChromaId = null
                            }
                        )
                    }
                }
                if (enabledChampions.isNotEmpty()) {
                    item {
                        val filteredChampions = champions.filter {
                            it.id in enabledChampions &&
                                it.displayName(language).contains(championSearch.trim(), ignoreCase = true)
                        }.sortedWith(
                            compareByDescending<Champion> { it.id in favoriteChampions }
                                .thenBy { it.displayName(language) }
                        )
                        OutlinedTextField(
                            value = championSearch,
                            onValueChange = { championSearch = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text(tr(language, "Найти персонажа", "Search champions")) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = palette.ink,
                                unfocusedTextColor = palette.ink,
                                focusedBorderColor = palette.accent,
                                unfocusedBorderColor = palette.muted,
                                focusedLabelColor = palette.accent,
                                unfocusedLabelColor = palette.muted,
                                cursorColor = palette.accent
                            )
                        )
                        if (championSearch.isNotBlank()) {
                            if (filteredChampions.isEmpty()) {
                                Text(
                                    tr(language, "Персонажи не найдены", "No champions found"),
                                    color = palette.muted,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            } else {
                                filteredChampions.take(8).forEach { match ->
                                    TextButton(
                                        onClick = {
                                            selectedChampionId = match.id
                                            onSave("champion", match.id)
                                            resultSkinId = null
                                            resultChromaId = null
                                            championSearch = ""
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            match.displayName(language),
                                            color = palette.ink,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                if (enabledChampions.isNotEmpty()) {
                    item {
                        RollPanel(
                            champion = champion,
                            language = language,
                            mode = mode,
                            resultSkinId = resultSkinId,
                            resultChromaId = resultChromaId,
                            splashOpacity = splashOpacity,
                            revealAnimationEnabled = revealAnimationEnabled,
                            revealAnimationTrigger = revealAnimationTrigger,
                            onRoll = {
                                val previous = rollHistory.firstOrNull { it.championId == champion.id }
                                val availableSkins = champion.skins.filter { it.id in enabledSkins }
                                fun <T> chooseWithoutRepeat(entries: List<T>, repeatsPrevious: (T) -> Boolean): T? {
                                    if (entries.isEmpty()) return null
                                    val alternatives = if (uniqueRollsEnabled && entries.size > 1) entries.filterNot(repeatsPrevious) else entries
                                    return (alternatives.ifEmpty { entries }).random()
                                }
                                when (mode) {
                                    RollMode.SKINS_ONLY -> {
                                        val chosen = chooseWithoutRepeat(availableSkins) { it.id == previous?.skinId }
                                        resultSkinId = chosen?.id
                                        resultChromaId = null
                                    }
                                    RollMode.TOGETHER -> {
                                        val entries = availableSkins.flatMap { skin ->
                                            listOf(skin.id to null) + skin.chromas
                                                .filter { "${skin.id}:${it.id}" in enabledChromas }
                                                .map { skin.id to it.id }
                                        }
                                        val chosen = chooseWithoutRepeat(entries) { it.first == previous?.skinId && it.second == previous.chromaId }
                                        chosen?.let { (skinId, chromaId) ->
                                            resultSkinId = skinId
                                            resultChromaId = chromaId
                                        }
                                    }
                                    RollMode.IN_STAGES -> {
                                        val skin = champion.skins.firstOrNull { it.id == resultSkinId }
                                        val availableChromas = skin?.chromas.orEmpty()
                                            .filter { "${skin?.id}:${it.id}" in enabledChromas }
                                        if (skin != null && availableChromas.isNotEmpty() && resultChromaId == null) {
                                            val chosen = chooseWithoutRepeat(availableChromas) { it.id == previous?.chromaId }
                                            resultChromaId = chosen?.id
                                        } else {
                                            val chosen = chooseWithoutRepeat(availableSkins) { it.id == previous?.skinId }
                                            resultSkinId = chosen?.id
                                            resultChromaId = null
                                        }
                                    }
                                }
                                resultSkinId?.let { skinId ->
                                    val result = RollHistoryEntry(champion.id, skinId, resultChromaId, System.currentTimeMillis())
                                    rollHistory = (listOf(result) + rollHistory).take(MAX_ROLL_HISTORY)
                                    saveRollHistory(preferences, rollHistory)
                                    if (revealAnimationEnabled) revealAnimationTrigger++
                                    if (champion.id == "teemo") {
                                        preferences.edit().putString("last_result_skin", skinId).apply()
                                    }
                                }
                            },
                            hasEnabledSkins = champion.skins.any { it.id in enabledSkins },
                            enabledChromas = enabledChromas,
                            onModeChange = {
                                modeName = it.name
                                onSave("mode", it.name)
                                resultSkinId = null
                                resultChromaId = null
                            }
                        )
                    }
                }
                if (enabledChampions.isNotEmpty()) {
                    item {
                        val activeChromaCount = champion.skins
                            .filter { it.id in enabledSkins }
                            .sumOf { skin -> skin.chromas.count { "${skin.id}:${it.id}" in enabledChromas } }
                        PoolHeader(champion, enabledSkins.size, activeChromaCount, language)
                    }
                }
                if (enabledChampions.isNotEmpty()) items(champion.skins, key = { it.id }) { skin ->
                    val isExpanded = skin.id in expandedSkinIds.split(",")
                    SkinRow(
                        championId = champion.id,
                        skin = skin,
                        language = language,
                        enabled = skin.id in enabledSkins,
                        enabledChromas = enabledChromas,
                        expanded = isExpanded,
                        onExpand = {
                            val expanded = expandedSkinIds.split(",").filter { it.isNotBlank() }.toMutableSet()
                            if (isExpanded) expanded.remove(skin.id) else expanded.add(skin.id)
                            expandedSkinIds = expanded.joinToString(",")
                        },
                        onSkinToggle = { checked ->
                            val next = enabledSkins.toMutableSet().apply {
                                if (checked) add(skin.id) else remove(skin.id)
                            }
                            enabledSkins = next
                            saveSet(preferences, "skins_${champion.id}", next)
                            if (!checked && resultSkinId == skin.id) {
                                resultSkinId = null
                                resultChromaId = null
                            }
                        },
                        onChromaToggle = { chroma, checked ->
                            val key = "${skin.id}:${chroma.id}"
                            val next = enabledChromas.toMutableSet().apply {
                                if (checked) add(key) else remove(key)
                            }
                            enabledChromas = next
                            saveSet(preferences, "chromas_${champion.id}", next)
                            if (!checked && resultSkinId == skin.id && resultChromaId == chroma.id) {
                                resultChromaId = null
                            }
                        }
                    )
                }
                if (enabledChampions.isNotEmpty()) item {
                    Text(
                        tr(language, "Каталог персонажей и скинов Riot Data Dragon 16.21.1.", "Champion and skin catalog from Riot Data Dragon 16.21.1."),
                        color = palette.muted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            } else if (selectedTab == 1) {
                RollHistoryScreen(
                    champions = champions,
                    history = rollHistory,
                    language = language,
                    modifier = Modifier.padding(padding)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("✦", fontSize = 42.sp, color = palette.accent)
                        Spacer(Modifier.height(12.dp))
                        Text(tr(language, "Здесь пока пусто", "Nothing here yet"), color = palette.ink, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        Text(tr(language, "Эта вкладка будет доступна позже", "This tab will be available later"), color = palette.muted, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(language: AppLanguage, onSettings: () -> Unit) {
    val palette = LocalSkinPalette.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("SKIN", color = palette.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Spacer(Modifier.width(7.dp))
                    Text("SELECTOR", color = palette.ink, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                }
            }
            TextButton(onClick = onSettings) {
                Text("⚙  ${tr(language, "Настройки", "Settings")}", color = palette.muted, fontSize = 12.sp)
            }
        }
        Text(tr(language, "На кого выпадет?", "Who will you get?"), color = palette.ink, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text(tr(language, "Выбери чемпиона и доверься удаче", "Choose a champion and leave it to chance"), color = palette.muted, fontSize = 14.sp)
    }
}

@Composable
private fun SettingsScreen(
    champions: List<Champion>,
    enabledChampions: Set<String>,
    language: AppLanguage,
    selectedTheme: ColorTheme,
    autoThemeEnabled: Boolean,
    splashOpacity: Float,
    uniqueRollsEnabled: Boolean,
    revealAnimationEnabled: Boolean,
    favoriteChampions: Set<String>,
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
    onThemeChange: (ColorTheme) -> Unit,
    onAutoThemeChange: (Boolean) -> Unit,
    onSplashOpacityChange: (Float) -> Unit,
    onUniqueRollsChange: (Boolean) -> Unit,
    onRevealAnimationChange: (Boolean) -> Unit,
    onEnableAllChampions: () -> Unit,
    onDisableAllChampions: () -> Unit,
    onDisableNonFavoriteChampions: () -> Unit,
    onFavoriteToggle: (String, Boolean) -> Unit,
    onChampionToggle: (String, Boolean) -> Unit
) {
    val palette = LocalSkinPalette.current
    var query by rememberSaveable { mutableStateOf("") }
    val filteredChampions = remember(champions, language, query, favoriteChampions) {
        champions.filter {
            it.displayName(language).contains(query.trim(), ignoreCase = true)
        }.sortedWith(
            compareByDescending<Champion> { it.id in favoriteChampions }
                .thenBy { it.displayName(language) }
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onBack) {
                    Text("‹  ${tr(language, "Назад", "Back")}", color = palette.accent)
                }
                Text(
                    tr(language, "Настройки", "Settings"),
                    color = palette.ink,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )
            }
        }
        item {
            SettingsSection(title = tr(language, "ЯЗЫК", "LANGUAGE")) {
                AppLanguage.entries.forEach { option ->
                    SettingsChoice(
                        label = if (option == AppLanguage.RUSSIAN) "Русский" else "English",
                        selected = option == language,
                        onClick = { onLanguageChange(option) }
                    )
                }
            }
        }
        item {
            SettingsSection(title = tr(language, "ЦВЕТОВАЯ СХЕМА", "COLOR THEME")) {
                ColorTheme.entries.forEach { theme ->
                    SettingsChoice(
                        label = if (language == AppLanguage.ENGLISH) theme.labelEn else theme.labelRu,
                        selected = theme == selectedTheme,
                        onClick = { onThemeChange(theme) },
                        markerColor = SkinPalette.forTheme(theme).accent
                    )
                }
            }
        }
        item {
            Surface(color = palette.panel, shape = RoundedCornerShape(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            tr(language, "Цвет по скину Тимо", "Match colors to Teemo skins"),
                            color = palette.ink,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            tr(language, "Тема меняется при выпадении скина", "Change the theme when a skin is rolled"),
                            color = palette.muted,
                            fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = autoThemeEnabled,
                        onCheckedChange = onAutoThemeChange,
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
        item {
            Surface(color = palette.panel, shape = RoundedCornerShape(16.dp)) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            tr(language, "Прозрачность сплеш-арта", "Splash art opacity"),
                            color = palette.ink,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "${(splashOpacity * 100).toInt()}%",
                            color = palette.muted,
                            fontSize = 12.sp
                        )
                    }
                    Slider(
                        value = splashOpacity,
                        onValueChange = onSplashOpacityChange,
                        valueRange = 0f..1f,
                        colors = androidx.compose.material3.SliderDefaults.colors(
                            thumbColor = palette.accent,
                            activeTrackColor = palette.accent,
                            inactiveTrackColor = palette.muted
                        )
                    )
                }
            }
        }
        item {
            SettingsToggle(
                title = tr(language, "Гарантия уникальных выпадений", "Unique drops guarantee"),
                description = tr(language, "Не повторять предыдущий скин или хрому", "Avoid repeating the previous skin or chroma"),
                checked = uniqueRollsEnabled,
                onCheckedChange = onUniqueRollsChange
            )
        }
        item {
            SettingsToggle(
                title = tr(language, "Анимация выпадения", "Result reveal animation"),
                description = tr(language, "Мягкое появление выбранного скина", "A subtle reveal animation for the selected skin"),
                checked = revealAnimationEnabled,
                onCheckedChange = onRevealAnimationChange
            )
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "${tr(language, "ПЕРСОНАЖИ В ПУЛЕ", "CHAMPIONS IN POOL")} · ${enabledChampions.size}/${champions.size}",
                        color = palette.muted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Row {
                        TextButton(onClick = onEnableAllChampions) {
                            Text(tr(language, "Включить всех", "Enable all"), color = palette.accent, fontSize = 12.sp)
                        }
                        TextButton(onClick = onDisableAllChampions) {
                            Text(tr(language, "Выключить всё", "Disable all"), color = palette.accent, fontSize = 12.sp)
                        }
                    }
                    TextButton(onClick = onDisableNonFavoriteChampions) {
                        Text(
                            tr(language, "Выключить всех, кроме избранных", "Disable all except favorites"),
                            color = palette.accent,
                            fontSize = 12.sp
                        )
                    }
                }
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                singleLine = true,
                label = { Text(tr(language, "Поиск персонажа", "Search champions")) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = palette.ink,
                    unfocusedTextColor = palette.ink,
                    focusedBorderColor = palette.accent,
                    unfocusedBorderColor = palette.muted,
                    focusedLabelColor = palette.accent,
                    unfocusedLabelColor = palette.muted,
                    cursorColor = palette.accent
                )
            )
        }
        if (filteredChampions.isEmpty()) {
            item {
                Text(
                    tr(language, "Персонажи не найдены", "No champions found"),
                    color = palette.muted,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
        } else {
            items(filteredChampions, key = { it.id }) { champion ->
                Surface(color = palette.panel, shape = RoundedCornerShape(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { onFavoriteToggle(champion.id, champion.id !in favoriteChampions) },
                            modifier = Modifier.padding(end = 2.dp)
                        ) {
                            Text(
                                if (champion.id in favoriteChampions) "★" else "☆",
                                color = if (champion.id in favoriteChampions) palette.highlight else palette.muted,
                                fontSize = 22.sp
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            Text(champion.displayName(language), color = palette.ink, fontWeight = FontWeight.Medium)
                            Text(
                                "${champion.skins.size} ${tr(language, "скинов", "skins")}",
                                color = palette.muted,
                                fontSize = 11.sp
                            )
                        }
                        Checkbox(
                            checked = champion.id in enabledChampions,
                            onCheckedChange = { onChampionToggle(champion.id, it) },
                            enabled = true,
                            colors = CheckboxDefaults.colors(
                                checkedColor = palette.accent,
                                uncheckedColor = palette.muted,
                                checkmarkColor = palette.ink
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    val palette = LocalSkinPalette.current
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, color = palette.muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
        Surface(color = palette.panel, shape = RoundedCornerShape(16.dp)) {
            Column(content = content, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
        }
    }
}

@Composable
private fun SettingsChoice(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    markerColor: Color? = null
) {
    val palette = LocalSkinPalette.current
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = palette.accent,
                unselectedColor = palette.muted
            )
        )
        Text(label, color = palette.ink, modifier = Modifier.weight(1f))
        if (markerColor != null) {
            Box(
                Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(markerColor)
            )
        }
    }
}

@Composable
private fun ChampionPicker(
    champion: Champion,
    champions: List<Champion>,
    language: AppLanguage,
    onSelect: (Champion) -> Unit
) {
    val palette = LocalSkinPalette.current
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel(tr(language, "ПЕРСОНАЖ", "CHAMPION"))
        Box {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = true },
                shape = RoundedCornerShape(18.dp),
                color = palette.panel
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Avatar(champion.displayName(language).take(1), palette.accent)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(champion.displayName(language), color = palette.ink, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        Text(champion.displayTitle(language), color = palette.muted, fontSize = 12.sp)
                    }
                    Text("⌄", color = palette.muted, fontSize = 22.sp)
                }
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                champions.forEach { item ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(item.displayName(language))
                                Text(item.displayTitle(language), fontSize = 12.sp, color = palette.muted)
                            }
                        },
                        onClick = {
                            expanded = false
                            onSelect(item)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun RollPanel(
    champion: Champion,
    language: AppLanguage,
    mode: RollMode,
    resultSkinId: String?,
    resultChromaId: String?,
    splashOpacity: Float,
    revealAnimationEnabled: Boolean,
    revealAnimationTrigger: Int,
    hasEnabledSkins: Boolean,
    enabledChromas: Set<String>,
    onModeChange: (RollMode) -> Unit,
    onRoll: () -> Unit
) {
    val palette = LocalSkinPalette.current
    val resultSkin = champion.skins.firstOrNull { it.id == resultSkinId }
    val resultChroma = resultSkin?.chromas?.firstOrNull { it.id == resultChromaId }
    val enabledChromasForResult = resultSkin?.let { skin ->
        skin.chromas.filter { "${skin.id}:${it.id}" in enabledChromas }
    }.orEmpty()
    val stageReadyForChroma = mode == RollMode.IN_STAGES &&
        resultSkin != null &&
        enabledChromasForResult.isNotEmpty() &&
        resultChromaId == null
    val canRoll = hasEnabledSkins
    val imagePath = resultChroma?.let {
        it.assetPath ?: "skins/${champion.id}/${resultSkin?.id}/${it.id}.png"
    } ?: resultSkin?.let {
        it.assetPath ?: "skins/${champion.id}/${it.id}.png"
    }
    val splashImagePath = resultSkin?.let {
        it.splashAssetPath ?: "skins/${champion.id}/splashes/${it.id}.jpg"
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = palette.panel),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Column {
                    Text(tr(language, "ТВОЙ РЕЗУЛЬТАТ", "YOUR RESULT"), color = palette.muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
                    Spacer(Modifier.height(5.dp))
                    Text(
                        resultSkin?.displayName(language) ?: tr(language, "Пора испытать удачу", "Ready to roll?"),
                        color = palette.ink,
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp
                    )
                    if (resultChroma != null) {
                        Text(resultChroma.displayName(language), color = palette.highlight, fontSize = 14.sp)
                    } else if (resultSkin != null && mode == RollMode.IN_STAGES && enabledChromasForResult.isNotEmpty()) {
                        Text(tr(language, "Скин выпал — теперь можно крутить хрому", "Skin rolled — now roll a chroma"), color = palette.muted, fontSize = 12.sp)
                    } else if (resultSkin != null && mode == RollMode.IN_STAGES && resultSkin.chromas.isNotEmpty()) {
                        Text(tr(language, "Включи хрому этого скина в настройках пула", "Enable this skin's chromas in the pool"), color = palette.muted, fontSize = 12.sp)
                    } else if (resultSkin != null && mode == RollMode.IN_STAGES) {
                        Text(tr(language, "Для этого скина нет хром", "This skin has no chromas"), color = palette.muted, fontSize = 12.sp)
                    } else if (resultSkin != null) {
                        Text(tr(language, "Скин выбран", "Skin selected"), color = palette.muted, fontSize = 12.sp)
                    } else {
                        Text(tr(language, "Скин или его вариант появится здесь", "The selected skin or chroma appears here"), color = palette.muted, fontSize = 12.sp)
                    }
                }
                val revealScale = remember { Animatable(1f) }
                val revealAlpha = remember { Animatable(1f) }
                var lastRevealTrigger by remember { mutableStateOf(revealAnimationTrigger) }
                LaunchedEffect(revealAnimationTrigger, revealAnimationEnabled) {
                    if (revealAnimationEnabled && revealAnimationTrigger != lastRevealTrigger) {
                        lastRevealTrigger = revealAnimationTrigger
                        revealScale.snapTo(0.94f)
                        revealAlpha.snapTo(0.65f)
                        revealScale.animateTo(1f, animationSpec = tween(320))
                        revealAlpha.animateTo(1f, animationSpec = tween(320))
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Brush.linearGradient(listOf(palette.accent.copy(alpha = .24f), palette.panel))),
                    contentAlignment = Alignment.Center
                ) {
                    if (splashImagePath != null) {
                        AssetImage(
                            path = splashImagePath,
                            contentScale = ContentScale.Crop,
                            alpha = splashOpacity,
                            fallbackText = ""
                        )
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            palette.page.copy(alpha = .48f),
                                            palette.page.copy(alpha = .68f)
                                        )
                                    )
                                )
                        )
                    }
                    if (imagePath != null) {
                        Box(
                            modifier = Modifier
                                .graphicsLayer {
                                    scaleX = revealScale.value
                                    scaleY = revealScale.value
                                    alpha = revealAlpha.value
                                }
                                .size(width = 160.dp, height = 208.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(palette.panel.copy(alpha = .84f)),
                            contentAlignment = Alignment.Center
                        ) {
                            AssetImage(imagePath, contentScale = ContentScale.Fit)
                        }
                    } else {
                        Text("?", color = palette.ink, fontSize = 42.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                RollMode.entries.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onModeChange(item) }
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = mode == item,
                            onClick = { onModeChange(item) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = palette.accent,
                                unselectedColor = palette.muted
                            )
                        )
                        Column {
                            Text(rollModeLabel(item, language), color = palette.ink, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text(
                                when (item) {
                                    RollMode.SKINS_ONLY -> tr(language, "Хромы не участвуют", "Chromas are excluded")
                                    RollMode.TOGETHER -> tr(language, "Скин или включённая хрома — одним броском", "One roll for a skin or enabled chroma")
                                    RollMode.IN_STAGES -> tr(language, "Сначала скин, следующим нажатием — хрома", "Skin first, then chroma on the next roll")
                                },
                                color = palette.muted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
            Button(
                onClick = onRoll,
                enabled = canRoll,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(containerColor = palette.accent, contentColor = Color.White)
            ) {
                Text(
                    when {
                        !hasEnabledSkins -> tr(language, "Включи хотя бы один скин", "Enable at least one skin")
                        stageReadyForChroma -> tr(language, "Крутить хрому", "Roll chroma")
                        mode == RollMode.IN_STAGES && resultSkin != null -> tr(language, "Выбрать другой скин", "Roll another skin")
                        mode == RollMode.IN_STAGES -> tr(language, "Выбрать скин", "Roll a skin")
                        else -> tr(language, "Крутить", "Roll")
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
private fun PoolHeader(champion: Champion, enabledSkinCount: Int, enabledChromaCount: Int, language: AppLanguage) {
    val palette = LocalSkinPalette.current
    Row(verticalAlignment = Alignment.Bottom) {
        Column(Modifier.weight(1f)) {
            Text(tr(language, "ПУЛ ВЫБОРА", "ROLL POOL"), color = palette.ink, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(champion.displayName(language), color = palette.muted, fontSize = 12.sp)
        }
        Text("$enabledSkinCount ${tr(language, "скинов", "skins")} · $enabledChromaCount ${tr(language, "хром", "chromas")}", color = palette.muted, fontSize = 11.sp)
    }
}

@Composable
private fun SkinRow(
    championId: String,
    skin: Skin,
    language: AppLanguage,
    enabled: Boolean,
    enabledChromas: Set<String>,
    expanded: Boolean,
    onExpand: () -> Unit,
    onSkinToggle: (Boolean) -> Unit,
    onChromaToggle: (Chroma, Boolean) -> Unit
) {
    val palette = LocalSkinPalette.current
    Card(
        colors = CardDefaults.cardColors(containerColor = palette.panel),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = enabled,
                    onCheckedChange = onSkinToggle,
                    colors = CheckboxDefaults.colors(
                        checkedColor = palette.accent,
                        uncheckedColor = palette.muted,
                        checkmarkColor = palette.ink
                    )
                )
                SkinThumbnail(
                    path = skin.assetPath ?: "skins/$championId/${skin.id}.png",
                    contentDescription = skin.name,
                    fallbackText = skin.name.take(1)
                )
                Column(
                    Modifier
                        .weight(1f)
                        .clickable { onExpand() }
                        .padding(vertical = 8.dp)
                ) {
                    Text(skin.displayName(language), color = palette.ink, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text(
                        if (skin.chromas.isEmpty()) tr(language, "Без хром", "No chromas") else "${skin.chromas.size} ${tr(language, "хром", "chromas")}",
                        color = palette.muted,
                        fontSize = 11.sp
                    )
                }
                Text(
                    if (expanded) "⌃" else "⌄",
                    color = palette.muted,
                    fontSize = 18.sp,
                    modifier = Modifier.clickable { onExpand() }
                )
            }
            if (expanded && skin.chromas.isNotEmpty()) {
                HorizontalDivider(color = Color.White.copy(alpha = .07f))
                Column(Modifier.padding(start = 22.dp, end = 14.dp, top = 6.dp, bottom = 8.dp)) {
                    skin.chromas.forEach { chroma ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = "${skin.id}:${chroma.id}" in enabledChromas,
                                onCheckedChange = { onChromaToggle(chroma, it) },
                                enabled = enabled,
                                colors = CheckboxDefaults.colors(
                                    checkedColor = palette.accent,
                                    uncheckedColor = palette.muted,
                                    checkmarkColor = palette.ink,
                                    disabledCheckedColor = palette.accent.copy(alpha = .5f)
                                )
                            )
                            SkinThumbnail(
                                path = chroma.assetPath ?: "skins/$championId/${skin.id}/${chroma.id}.png",
                                contentDescription = "${skin.name}: ${chroma.name}",
                                fallbackText = chroma.name.take(1),
                                size = 40.dp
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(chroma.displayName(language), color = if (enabled) palette.ink else palette.muted, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabItem(icon: String, label: String, selected: Boolean, onClick: () -> Unit) {
    val palette = LocalSkinPalette.current
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(icon, color = if (selected) palette.accent else palette.muted, fontSize = 20.sp)
        Text(label, color = if (selected) palette.ink else palette.muted, fontSize = 10.sp)
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, color = LocalSkinPalette.current.muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp)
}

@Composable
private fun Avatar(letter: String, color: Color) {
    val palette = LocalSkinPalette.current
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = .2f))
            .border(1.dp, color.copy(alpha = .5f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(letter, color = palette.ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
internal fun SkinThumbnail(
    path: String,
    contentDescription: String,
    fallbackText: String,
    size: androidx.compose.ui.unit.Dp = 48.dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF292448)),
        contentAlignment = Alignment.Center
    ) {
        AssetImage(
            path = path,
            contentScale = ContentScale.Fit,
            contentDescription = contentDescription,
            fallbackText = fallbackText
        )
    }
}

@Composable
private fun AssetImage(
    path: String,
    contentScale: ContentScale = ContentScale.Crop,
    contentDescription: String? = null,
    fallbackText: String = "?",
    alpha: Float = 1f
) {
    val context = LocalContext.current
    val bitmap by produceState<android.graphics.Bitmap?>(initialValue = null, path) {
        value = withContext(Dispatchers.IO) {
            try {
                context.assets.open(path).use { BitmapFactory.decodeStream(it) }
            } catch (_: FileNotFoundException) {
                null
            }
        }
    }
    val image = bitmap?.asImageBitmap()
    if (image != null) {
        Image(
            bitmap = image,
            contentDescription = contentDescription,
            contentScale = contentScale,
            alpha = alpha,
            modifier = Modifier.fillMaxSize()
        )
    } else if (fallbackText.isNotEmpty()) {
        Text(fallbackText, color = LocalSkinPalette.current.ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

internal fun tr(language: AppLanguage, russian: String, english: String): String =
    if (language == AppLanguage.ENGLISH) english else russian

private fun rollModeLabel(mode: RollMode, language: AppLanguage): String = when (mode) {
    RollMode.SKINS_ONLY -> tr(language, "Только скины", "Skins only")
    RollMode.TOGETHER -> tr(language, "Скин и хрома", "Skin and chroma")
    RollMode.IN_STAGES -> tr(language, "Сначала скин", "Skin first")
}

private fun themeForTeemoSkin(skinId: String?): ColorTheme = when (skinId) {
    "little_devil", "firecracker" -> ColorTheme.SUNSET
    "beemo" -> ColorTheme.YELLOW
    "cottontail", "spirit_blossom_springs" -> ColorTheme.FOREST
    "space_groove", "astronaut" -> ColorTheme.OCEAN
    "spirit_blossom", "prestige_spirit_blossom", "happy_elf" -> ColorTheme.ROSE
    else -> ColorTheme.VIOLET
}

private fun saveSet(
    preferences: SharedPreferences,
    key: String,
    values: Set<String>
) {
    preferences.edit().putStringSet(key, values.toSet()).apply()
}

private fun saveStringSet(
    preferences: SharedPreferences,
    key: String,
    values: Set<String>
) {
    preferences.edit().putStringSet(key, values.toSet()).apply()
}
