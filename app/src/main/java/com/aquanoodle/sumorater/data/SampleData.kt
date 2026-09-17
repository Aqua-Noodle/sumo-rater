package com.aquanoodle.sumorater.data

/**
 * Bundled fallback so the app works offline on first launch: Aki 2026 day 5.
 * Mirrors the prototype's SAMPLE constant. Keyed by "{bashoId}-{day}".
 */
val SAMPLE: Map<String, Map<String, List<RawBoutRow>>> = mapOf(
    "202609-5" to mapOf(
        "Juryo" to listOf(
            RawBoutRow(101, "Shishi", "Juryo 1 East", 102, "Asakoryu", "Juryo 1 West"),
            RawBoutRow(103, "Hakuoho", "Juryo 2 East", 104, "Tokihayate", "Juryo 2 West"),
            RawBoutRow(105, "Kayo", "Juryo 3 East", 106, "Nishikigi", "Juryo 3 West"),
            RawBoutRow(107, "Roga", "Juryo 4 East", 108, "Shimazuumi", "Juryo 4 West"),
            RawBoutRow(110, "Tamashoho", "Juryo 5 West", 109, "Kotoshoho", "Juryo 5 East"),
            RawBoutRow(111, "Tochitaikai", "Juryo 6 East", 112, "Mitakeumi", "Juryo 6 West"),
            RawBoutRow(113, "Kagayaki", "Juryo 7 East", 114, "Daiamami", "Juryo 7 West"),
            RawBoutRow(116, "Shonannoumi", "Juryo 8 West", 115, "Tsurugisho", "Juryo 8 East"),
            RawBoutRow(117, "Bushozan", "Juryo 9 East", 118, "Ryuden", "Juryo 9 West"),
            RawBoutRow(119, "Kitanowaka", "Juryo 10 East", 120, "Hokutofuji", "Juryo 10 West"),
        ),
        "Makuuchi" to listOf(
            RawBoutRow(209, "Ichiyamamoto", "Maegashira 8 East", 210, "Tamawashi", "Maegashira 8 West"),
            RawBoutRow(208, "Midorifuji", "Maegashira 7 West", 207, "Churanoumi", "Maegashira 7 East"),
            RawBoutRow(205, "Kinbozan", "Maegashira 6 East", 206, "Meisei", "Maegashira 6 West"),
            RawBoutRow(203, "Atamifuji", "Maegashira 5 East", 204, "Gonoyama", "Maegashira 5 West"),
            RawBoutRow(201, "Hiradoumi", "Maegashira 4 East", 202, "Ura", "Maegashira 4 West"),
            RawBoutRow(211, "Takerufuji", "Komusubi 1 East", 212, "Tobizaru", "Maegashira 3 West"),
            RawBoutRow(213, "Aonishiki", "Sekiwake 1 East", 214, "Oho", "Maegashira 3 East"),
            RawBoutRow(215, "Kirishima", "Ozeki 1 West", 216, "Daieisho", "Maegashira 1 West"),
            RawBoutRow(217, "Kotozakura", "Ozeki 1 East", 218, "Wakatakakage", "Maegashira 2 East"),
            RawBoutRow(219, "Hoshoryu", "Yokozuna 1 West", 220, "Takayasu", "Maegashira 1 East"),
            RawBoutRow(221, "Onosato", "Yokozuna 1 East", 222, "Abi", "Maegashira 2 West"),
        ),
    ),
)
