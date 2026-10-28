package ru.skinselector.app

import android.content.Context
import org.json.JSONObject
import java.util.Locale

enum class AppLanguage(val preferenceValue: String) {
    RUSSIAN("ru"),
    ENGLISH("en");

    companion object {
        fun fromPreference(value: String?): AppLanguage =
            entries.firstOrNull { it.preferenceValue == value } ?: RUSSIAN
    }
}

fun Champion.displayName(language: AppLanguage): String =
    if (language == AppLanguage.ENGLISH) englishName ?: name else name

fun Champion.displayTitle(language: AppLanguage): String =
    if (language == AppLanguage.ENGLISH) englishTitle ?: title else title

fun Champion.matchesSearch(query: String): Boolean {
    val normalized = query.trim()
    if (normalized.isBlank()) return true
    return listOf(id, name, englishName, displayName(AppLanguage.RUSSIAN), displayName(AppLanguage.ENGLISH))
        .filterNotNull()
        .any { it.contains(normalized, ignoreCase = true) }
}

fun Skin.displayName(language: AppLanguage): String =
    if (language == AppLanguage.ENGLISH) englishName ?: name else name

fun Chroma.displayName(language: AppLanguage): String =
    if (language == AppLanguage.ENGLISH) englishName ?: name else name

enum class ColorTheme(val preferenceValue: String, val labelRu: String, val labelEn: String) {
    VIOLET("violet", "Фиолетовая", "Violet"),
    OCEAN("ocean", "Океан", "Ocean"),
    FOREST("forest", "Лес", "Forest"),
    SUNSET("sunset", "Закат", "Sunset"),
    ROSE("rose", "Розовая", "Rose"),
    YELLOW("yellow", "Жёлтая", "Yellow"),
    NOXUS("noxus", "Ноксус", "Noxus"),
    DEMACIA("demacia", "Демасия", "Demacia"),
    SHADOW_ISLES("shadow_isles", "Острова Теней", "Shadow Isles"),
    PILTOVER("piltover", "Пилтовер", "Piltover");

    companion object {
        fun fromPreference(value: String?): ColorTheme =
            entries.firstOrNull { it.preferenceValue == value } ?: VIOLET
    }
}

data class SkinPalette(
    val ink: androidx.compose.ui.graphics.Color,
    val muted: androidx.compose.ui.graphics.Color,
    val panel: androidx.compose.ui.graphics.Color,
    val page: androidx.compose.ui.graphics.Color,
    val accent: androidx.compose.ui.graphics.Color,
    val highlight: androidx.compose.ui.graphics.Color
) {
    companion object {
        fun forTheme(theme: ColorTheme): SkinPalette = when (theme) {
            ColorTheme.VIOLET -> SkinPalette(
                androidx.compose.ui.graphics.Color(0xFFF4F2FF),
                androidx.compose.ui.graphics.Color(0xFF9A96B1),
                androidx.compose.ui.graphics.Color(0xFF1B1930),
                androidx.compose.ui.graphics.Color(0xFF100F1D),
                androidx.compose.ui.graphics.Color(0xFF9B7BFF),
                androidx.compose.ui.graphics.Color(0xFF66E0BE)
            )
            ColorTheme.OCEAN -> SkinPalette(
                androidx.compose.ui.graphics.Color(0xFFF0F8FF),
                androidx.compose.ui.graphics.Color(0xFF91AFC3),
                androidx.compose.ui.graphics.Color(0xFF15283A),
                androidx.compose.ui.graphics.Color(0xFF0C1620),
                androidx.compose.ui.graphics.Color(0xFF52B8E8),
                androidx.compose.ui.graphics.Color(0xFF71E2D2)
            )
            ColorTheme.FOREST -> SkinPalette(
                androidx.compose.ui.graphics.Color(0xFFF1FAF2),
                androidx.compose.ui.graphics.Color(0xFF95B4A0),
                androidx.compose.ui.graphics.Color(0xFF192D24),
                androidx.compose.ui.graphics.Color(0xFF0F1914),
                androidx.compose.ui.graphics.Color(0xFF62C985),
                androidx.compose.ui.graphics.Color(0xFFB2E77A)
            )
            ColorTheme.SUNSET -> SkinPalette(
                androidx.compose.ui.graphics.Color(0xFFFFF3F0),
                androidx.compose.ui.graphics.Color(0xFFC4A0A2),
                androidx.compose.ui.graphics.Color(0xFF301B20),
                androidx.compose.ui.graphics.Color(0xFF1A1013),
                androidx.compose.ui.graphics.Color(0xFFFF726A),
                androidx.compose.ui.graphics.Color(0xFFFFC46B)
            )
            ColorTheme.ROSE -> SkinPalette(
                androidx.compose.ui.graphics.Color(0xFFFFF2FA),
                androidx.compose.ui.graphics.Color(0xFFC1A0B9),
                androidx.compose.ui.graphics.Color(0xFF2D1B32),
                androidx.compose.ui.graphics.Color(0xFF190F1D),
                androidx.compose.ui.graphics.Color(0xFFE77DB5),
                androidx.compose.ui.graphics.Color(0xFFFFC17A)
            )
            ColorTheme.YELLOW -> SkinPalette(
                androidx.compose.ui.graphics.Color(0xFFFFF8E8),
                androidx.compose.ui.graphics.Color(0xFFC2B27D),
                androidx.compose.ui.graphics.Color(0xFF302A19),
                androidx.compose.ui.graphics.Color(0xFF19160F),
                androidx.compose.ui.graphics.Color(0xFFE7B93F),
                androidx.compose.ui.graphics.Color(0xFFFFE08A)
            )
            ColorTheme.NOXUS -> SkinPalette(
                androidx.compose.ui.graphics.Color(0xFFFFF1F1),
                androidx.compose.ui.graphics.Color(0xFFB8959A),
                androidx.compose.ui.graphics.Color(0xFF32191E),
                androidx.compose.ui.graphics.Color(0xFF1B0D10),
                androidx.compose.ui.graphics.Color(0xFFD64550),
                androidx.compose.ui.graphics.Color(0xFFFF9B72)
            )
            ColorTheme.DEMACIA -> SkinPalette(
                androidx.compose.ui.graphics.Color(0xFFF2F7FF),
                androidx.compose.ui.graphics.Color(0xFF91A8C4),
                androidx.compose.ui.graphics.Color(0xFF18283D),
                androidx.compose.ui.graphics.Color(0xFF0B1422),
                androidx.compose.ui.graphics.Color(0xFF6CA6E8),
                androidx.compose.ui.graphics.Color(0xFFF0C86A)
            )
            ColorTheme.SHADOW_ISLES -> SkinPalette(
                androidx.compose.ui.graphics.Color(0xFFEAFBF7),
                androidx.compose.ui.graphics.Color(0xFF82AAA1),
                androidx.compose.ui.graphics.Color(0xFF152C2D),
                androidx.compose.ui.graphics.Color(0xFF091719),
                androidx.compose.ui.graphics.Color(0xFF48C8B0),
                androidx.compose.ui.graphics.Color(0xFF9C86E8)
            )
            ColorTheme.PILTOVER -> SkinPalette(
                androidx.compose.ui.graphics.Color(0xFFF3FAFF),
                androidx.compose.ui.graphics.Color(0xFF87A8BA),
                androidx.compose.ui.graphics.Color(0xFF19303B),
                androidx.compose.ui.graphics.Color(0xFF0C181D),
                androidx.compose.ui.graphics.Color(0xFF48BBD0),
                androidx.compose.ui.graphics.Color(0xFFFFC765)
            )
        }
    }
}

object ChampionCatalog {
    fun load(context: Context): List<Champion> {
        val json = context.assets.open("champions.json").bufferedReader().use { it.readText() }
        val entries = JSONObject(json).getJSONArray("champions")
        val teemo = AppData.champions.first { it.id == "teemo" }.let(::localize)
        return buildList(entries.length()) {
            for (index in 0 until entries.length()) {
                val record = entries.getJSONObject(index)
                val id = record.getString("id")
                val name = record.getString("nameRu")
                val englishName = record.getString("nameEn")
                val skinsJson = record.getJSONArray("skins")
                val sourceSkins = buildList(skinsJson.length()) {
                    for (skinIndex in 0 until skinsJson.length()) {
                        val skin = skinsJson.getJSONObject(skinIndex)
                        add(
                            CatalogSkin(
                                id = skin.getString("id"),
                                name = localizedSkinName(skin, AppLanguage.RUSSIAN),
                                englishName = localizedSkinName(skin, AppLanguage.ENGLISH)
                            )
                        )
                    }
                }
                val skinsByName = sourceSkins.associateBy { normalizeSkinName(it.englishName) }
                val chromasBySkin = sourceSkins.mapNotNull { skin ->
                    val match = chromaNamePattern.matchEntire(skin.englishName) ?: return@mapNotNull null
                    val russianChromaName = russianChromaNamePattern.find(skin.name)?.groupValues?.get(1)
                        ?: return@mapNotNull null
                    val parent = skinsByName[normalizeSkinName(match.groupValues[1])] ?: return@mapNotNull null
                    val englishChromaName = match.groupValues[2]
                    parent.id to Chroma(
                        id = skin.id,
                        name = russianChromaName,
                        englishName = englishChromaName
                    )
                }.groupBy({ it.first }, { it.second })
                val chromaIds = chromasBySkin.values.flatten().mapTo(mutableSetOf()) { it.id }
                val skins = sourceSkins
                    .filterNot { it.id in chromaIds }
                    .map { skin ->
                        Skin(
                            id = skin.id,
                            name = skin.name,
                            englishName = skin.englishName,
                            chromas = chromasBySkin[skin.id].orEmpty()
                        )
                    }
                val official = Champion(
                    id = id,
                    name = name,
                    title = record.getString("titleRu"),
                    skins = skins,
                    englishName = englishName,
                    englishTitle = record.getString("titleEn")
                )
                add(if (id == "teemo") teemo else official)
            }
        }
    }

    private data class CatalogSkin(
        val id: String,
        val name: String,
        val englishName: String
    )

    private val chromaNamePattern = Regex("^(.+?)\\s+\\(([^()]*)\\)$")
    private val russianChromaNamePattern = Regex("''(.+?)''$")

    private fun normalizeSkinName(name: String): String =
        name.trim().replace(Regex("\\s+"), " ").lowercase(Locale.ROOT)

    private fun localizedSkinName(skin: JSONObject, language: AppLanguage): String {
        val label = if (language == AppLanguage.ENGLISH) {
            skin.getString("nameEn")
        } else {
            skin.getString("nameRu")
        }
        return when (label.lowercase(Locale.ROOT)) {
            "default" -> if (language == AppLanguage.ENGLISH) "Classic" else "Классический"
            else -> label
        }
    }

    private fun localize(champion: Champion): Champion {
        val englishNames = mapOf(
            "ahri" to ("Ahri" to "the Nine-Tailed Fox"),
            "lux" to ("Lux" to "the Lady of Luminosity"),
            "jinx" to ("Jinx" to "the Loose Cannon"),
            "yasuo" to ("Yasuo" to "the Unforgiven"),
            "miss_fortune" to ("Miss Fortune" to "the Bounty Hunter"),
            "teemo" to ("Teemo" to "the Swift Scout")
        )
        val skinNames = mapOf(
            "ahri:default" to "Classic",
            "ahri:spirit_blossom" to "Spirit Blossom",
            "ahri:star_guardian" to "Star Guardian",
            "ahri:arcana" to "Arcana",
            "lux:default" to "Classic",
            "lux:battle_academia" to "Battle Academia",
            "lux:star_guardian" to "Star Guardian",
            "lux:dark_cosmic" to "Dark Cosmic",
            "jinx:default" to "Classic",
            "jinx:odyssey" to "Odyssey",
            "jinx:cafe_cuties" to "Cafe Cuties",
            "jinx:arcane" to "Arcane",
            "yasuo:default" to "Classic",
            "yasuo:true_damage" to "True Damage",
            "yasuo:spirit_blossom" to "Spirit Blossom",
            "yasuo:high_noon" to "High Noon",
            "miss_fortune:default" to "Classic",
            "miss_fortune:battle_bunny" to "Battle Bunny",
            "miss_fortune:bewitching" to "Bewitching",
            "miss_fortune:arcade" to "Arcade",
            "teemo:default" to "Classic",
            "teemo:happy_elf" to "Happy Elf",
            "teemo:recon" to "Recon",
            "teemo:badger" to "Badger",
            "teemo:astronaut" to "Astronaut",
            "teemo:cottontail" to "Cottontail",
            "teemo:super" to "Super Teemo",
            "teemo:panda" to "Panda",
            "teemo:omega_squad" to "Omega Squad",
            "teemo:little_devil" to "Little Devil",
            "teemo:beemo" to "Beemo",
            "teemo:spirit_blossom" to "Spirit Blossom",
            "teemo:prestige_spirit_blossom" to "Prestige Spirit Blossom",
            "teemo:firecracker" to "Firecracker",
            "teemo:space_groove" to "Space Groove",
            "teemo:spirit_blossom_springs" to "Spirit Blossom Springs"
        )
        val chromaNames = mapOf(
            "night_bloom" to "Night Bloom",
            "pearl" to "Pearl",
            "obsidian" to "Obsidian",
            "rose_quartz" to "Rose Quartz",
            "sapphire" to "Sapphire",
            "ruby" to "Ruby",
            "emerald" to "Emerald",
            "chrono" to "Chrono",
            "aquamarine" to "Aquamarine",
            "speckled" to "Speckled",
            "turquoise" to "Turquoise",
            "amethyst" to "Amethyst",
            "sandstone" to "Sandstone",
            "catseye" to "Catseye",
            "peridot" to "Peridot",
            "citrine" to "Citrine",
            "tanzanite" to "Tanzanite",
            "golden_tiger" to "Golden Tiger",
            "rainbow" to "Rainbow"
        )
        val (name, title) = englishNames[champion.id] ?: (champion.name to champion.title)
        return champion.copy(
            englishName = name,
            englishTitle = title,
            skins = champion.skins.map { skin ->
                skin.copy(
                    englishName = skinNames["${champion.id}:${skin.id}"] ?: skin.name,
                    chromas = skin.chromas.map { chroma ->
                        chroma.copy(englishName = chromaNames[chroma.id] ?: chroma.name)
                    }
                )
            }
        )
    }
}
