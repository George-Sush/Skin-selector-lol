package ru.skinselector.app

data class Chroma(
    val id: String,
    val name: String,
    val assetPath: String? = null,
    val englishName: String? = null
)

data class Skin(
    val id: String,
    val name: String,
    val chromas: List<Chroma> = emptyList(),
    val assetPath: String? = null,
    val splashAssetPath: String? = null,
    val englishName: String? = null
)

data class Champion(
    val id: String,
    val name: String,
    val title: String,
    val skins: List<Skin>,
    val englishName: String? = null,
    val englishTitle: String? = null
)

enum class RollMode(val label: String) {
    SKINS_ONLY("Только скины"),
    TOGETHER("Скин и хрома"),
    IN_STAGES("Сначала скин")
}

object AppData {
    val champions = listOf(
        Champion(
            id = "ahri",
            name = "Ахри",
            title = "Девятихвостая лиса",
            skins = listOf(
                Skin("default", "Классическая"),
                Skin(
                    "spirit_blossom",
                    "Дух цветения",
                    listOf(
                        Chroma("night_bloom", "Ночное цветение"),
                        Chroma("pearl", "Жемчужная"),
                        Chroma("obsidian", "Обсидиановая")
                    )
                ),
                Skin(
                    "star_guardian",
                    "Страж звёзд",
                    listOf(
                        Chroma("rose_quartz", "Розовый кварц"),
                        Chroma("sapphire", "Сапфировая")
                    )
                ),
                Skin("arcana", "Аркана")
            )
        ),
        Champion(
            id = "lux",
            name = "Люкс",
            title = "Сияющая дева",
            skins = listOf(
                Skin("default", "Классическая"),
                Skin(
                    "battle_academia",
                    "Боевой университет",
                    listOf(
                        Chroma("ruby", "Рубиновая"),
                        Chroma("emerald", "Изумрудная"),
                        Chroma("sapphire", "Сапфировая")
                    )
                ),
                Skin(
                    "star_guardian",
                    "Страж звёзд",
                    listOf(
                        Chroma("rose_quartz", "Розовый кварц"),
                        Chroma("obsidian", "Обсидиановая")
                    )
                ),
                Skin("dark_cosmic", "Тёмная космическая")
            )
        ),
        Champion(
            id = "jinx",
            name = "Джинкс",
            title = "Преступница из Зауна",
            skins = listOf(
                Skin("default", "Классическая"),
                Skin(
                    "odyssey",
                    "Одиссея",
                    listOf(
                        Chroma("ruby", "Рубиновая"),
                        Chroma("pearl", "Жемчужная"),
                        Chroma("emerald", "Изумрудная")
                    )
                ),
                Skin(
                    "cafe_cuties",
                    "Кафе «Сладости»",
                    listOf(
                        Chroma("sapphire", "Сапфировая"),
                        Chroma("obsidian", "Обсидиановая")
                    )
                ),
                Skin("arcane", "Аркейн")
            )
        ),
        Champion(
            id = "yasuo",
            name = "Ясуо",
            title = "Непрощённый",
            skins = listOf(
                Skin("default", "Классический"),
                Skin(
                    "true_damage",
                    "Истинный урон",
                    listOf(
                        Chroma("ruby", "Рубиновая"),
                        Chroma("sapphire", "Сапфировая"),
                        Chroma("pearl", "Жемчужная")
                    )
                ),
                Skin(
                    "spirit_blossom",
                    "Дух цветения",
                    listOf(
                        Chroma("night_bloom", "Ночное цветение"),
                        Chroma("obsidian", "Обсидиановая")
                    )
                ),
                Skin("high_noon", "Дикий Запад")
            )
        ),
        Champion(
            id = "miss_fortune",
            name = "Мисс Фортуна",
            title = "Охотница за головами",
            skins = listOf(
                Skin("default", "Классическая"),
                Skin(
                    "battle_bunny",
                    "Боевой кролик",
                    listOf(
                        Chroma("ruby", "Рубиновая"),
                        Chroma("emerald", "Изумрудная"),
                        Chroma("obsidian", "Обсидиановая")
                    )
                ),
                Skin(
                    "bewitching",
                    "Заколдованная",
                    listOf(
                        Chroma("pearl", "Жемчужная"),
                        Chroma("sapphire", "Сапфировая")
                    )
                ),
                Skin("arcade", "Аркадная")
            )
        ),
        Champion(
            id = "teemo",
            name = "Тимо",
            title = "Быстрый разведчик",
            skins = listOf(
                Skin("default", "Классический"),
                Skin("happy_elf", "Радостный эльф"),
                Skin("recon", "Разведчик"),
                Skin("badger", "Барсук"),
                Skin(
                    "astronaut",
                    "Астронавт",
                    listOf(Chroma("chrono", "Время"))
                ),
                Skin(
                    "cottontail",
                    "Зайчик",
                    listOf(
                        Chroma("aquamarine", "Аквамарин"),
                        Chroma("rose_quartz", "Розовый кварц"),
                        Chroma("speckled", "Крапчатый"),
                        Chroma("turquoise", "Бирюза"),
                        Chroma("amethyst", "Аметист")
                    )
                ),
                Skin("super", "Супер-Тимо"),
                Skin("panda", "Панда"),
                Skin(
                    "omega_squad",
                    "Отряд «Омега»",
                    listOf(
                        Chroma("pearl", "Жемчуг"),
                        Chroma("sandstone", "Песчаник"),
                        Chroma("obsidian", "Обсидиан")
                    )
                ),
                Skin(
                    "little_devil",
                    "Дьяволёнок",
                    listOf(
                        Chroma("catseye", "Кошачий глаз"),
                        Chroma("ruby", "Рубин"),
                        Chroma("aquamarine", "Аквамарин"),
                        Chroma("rose_quartz", "Розовый кварц"),
                        Chroma("emerald", "Изумруд"),
                        Chroma("amethyst", "Аметист"),
                        Chroma("pearl", "Жемчуг"),
                        Chroma("obsidian", "Обсидиан"),
                        Chroma("turquoise", "Бирюза")
                    )
                ),
                Skin(
                    "beemo",
                    "Жужжимо",
                    listOf(
                        Chroma("ruby", "Рубин"),
                        Chroma("peridot", "Перидот"),
                        Chroma("aquamarine", "Аквамарин"),
                        Chroma("amethyst", "Аметист"),
                        Chroma("rose_quartz", "Розовый кварц"),
                        Chroma("obsidian", "Обсидиан")
                    )
                ),
                Skin("spirit_blossom", "Дух цветения"),
                Skin("prestige_spirit_blossom", "Престижный Дух цветения"),
                Skin(
                    "firecracker",
                    "Пиротехник",
                    listOf(
                        Chroma("ruby", "Рубин"),
                        Chroma("emerald", "Изумруд"),
                        Chroma("rose_quartz", "Розовый кварц"),
                        Chroma("sapphire", "Сапфир"),
                        Chroma("citrine", "Цитрин"),
                        Chroma("tanzanite", "Танзанит"),
                        Chroma("obsidian", "Обсидиан"),
                        Chroma("pearl", "Жемчуг"),
                        Chroma("golden_tiger", "Золотой тигр")
                    )
                ),
                Skin(
                    "space_groove",
                    "Галактика грува",
                    listOf(
                        Chroma("ruby", "Рубин"),
                        Chroma("catseye", "Кошачий глаз"),
                        Chroma("obsidian", "Обсидиан"),
                        Chroma("peridot", "Перидот"),
                        Chroma("rose_quartz", "Розовый кварц"),
                        Chroma("rainbow", "Радуга")
                    )
                ),
                Skin(
                    "spirit_blossom_springs",
                    "Дух цветения на водах",
                    listOf(
                        Chroma("ruby", "Рубин"),
                        Chroma("catseye", "Кошачий глаз"),
                        Chroma("emerald", "Изумруд"),
                        Chroma("turquoise", "Бирюза"),
                        Chroma("rose_quartz", "Розовый кварц"),
                        Chroma("tanzanite", "Танзанит"),
                        Chroma("pearl", "Жемчуг"),
                        Chroma("obsidian", "Обсидиан")
                    )
                )
            )
        )
    )
}
