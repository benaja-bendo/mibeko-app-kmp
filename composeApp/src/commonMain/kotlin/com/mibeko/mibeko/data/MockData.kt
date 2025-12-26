package com.mibeko.mibeko.data

val MOCK_LAW_CODES = listOf(
    LawCodeSpec("1", "Code de la Famille", "shield", "2023"),
    LawCodeSpec("2", "Code du Travail", "gavel", "2021"),
    LawCodeSpec("3", "Code Pénal", "law", "2022"),
    LawCodeSpec("4", "Code Civil", "book", "2019"),
)

val MOCK_ARTICLES = listOf(
    ArticleSpec(
        id = "a1",
        codeId = "2",
        number = "Article 45",
        title = "Licenciement",
        breadcrumb = "Code du Travail > Livre 1 > Titre 2",
        content = """
            Tout licenciement doit être motivé par une cause réelle et sérieuse...
            
            Tout licenciement doit être motivé par une cause réelle et sérieuse. J'ansrte motivé par une cause réelle et sérieuse...
        """.trimIndent(),
        isFavorite = true
    ),
    ArticleSpec(
        id = "a2",
        codeId = "2",
        number = "Article 82",
        title = "Licenciement",
        breadcrumb = "Code du Travail",
        content = "...motif de licenciement et de licenciement... vont motif de licenciement senorant : être coinienent doit être motivé [une cause réelle et sérieuse...",
        isFavorite = false
    ),
    ArticleSpec(
        id = "a3",
        codeId = "1",
        number = "Article 12",
        title = "Mariage",
        breadcrumb = "Code de la Famille",
        content = "Le mariage est l'union de l'homme et de la femme...",
        isFavorite = true
    )
)

val MOCK_EXPLORER_NODES = listOf(
    ExplorerNode("e1", "Code de la Famille", NodeType.CODE),
    ExplorerNode("e2", "Code du Travail", NodeType.CODE),
    ExplorerNode("e3", "Code Pénal", NodeType.CODE),
    ExplorerNode("e4", "Code Civil", NodeType.CODE),
)
