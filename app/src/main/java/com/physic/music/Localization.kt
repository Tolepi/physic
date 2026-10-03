package com.physic.music

import java.util.Locale

object L {
    // language code -> (english -> translation)
    private val es = mapOf(
        "welcome to Physic" to "folleto de Physic",
        "you can change all of this later in ~/settings" to "puedes cambiar todo esto despues en ~/settings",
        "profile" to "perfil", "theme" to "tema", "font" to "fuente",
        "corners" to "esquinas", "index folders" to "indexar carpetas", "about" to "acerca de",
        "your name" to "tu nombre", "welcome message" to "mensaje de bienvenida",
        "rounded corners" to "esquinas redondeadas",
        "your playlists" to "tus playlists", "new playlist" to "nueva playlist",
        "no playlists yet — make one!" to "sin playlists, crea una!",
        "empty — add songs from the songs tab (+ icon)" to "vacia — agrega canciones desde ~/songs (+)",
        "add to playlist" to "agregar a playlist",
        "total plays" to "reproducciones totales",
        "most played songs" to "mas reproducidas", "top albums" to "mejores albumes",
        "top artists" to "mejores artistas", "import stats json" to "importar estadisticas",
        "equalizer" to "ecualizador", "play something first" to "reproduce algo primero",
        "now playing" to "reproduciendo",
        "[ finish ]" to "[ terminar ]", "[ new playlist ]" to "[ nueva playlist ]",
        "[ shuffle all ]" to "[ aleatorio todo ]", "[import stats json]" to "[importar estadisticas]",
        "[set pfp]" to "[foto]", "[export]" to "[exportar]", "[import]" to "[importar]", "[x]" to "[x]",
        "[delete]" to "[borrar]", "apply" to "aplicar", "create" to "crear", "apply custom" to "aplicar personalizado",
        "edit name" to "editar nombre", "edit welcome" to "editar bienvenida", "ok" to "ok",
        "paste json" to "pegar json", "paste playlist json" to "pegar json", "add to playlist" to "agregar a playlist",
        "[delete]" to "[borrar]", "[ shuffle all ]" to "[ aleatorio ]", "lyrics" to "letras",
        "no .lrc sidecar found for this song" to "no se encontro .lrc", "sort:" to "ordenar",
        "rounded on" to "redondeado activado", "square (default)" to "cuadrado (por defecto)",
        "about" to "acerca de", "github.com/Tolepi/physic" to "github.com/Tolepi/physic",
        "vibecoded with love" to "viberan xq con amor",
    )
    private val fr = mapOf(
        "welcome to Physic" to "bienvenue sur Physic",
        "you can change all of this later in ~/settings" to "tu peux tout changer dans ~/settings",
        "profile" to "profil", "theme" to "theme", "font" to "police",
        "corners" to "coins", "index folders" to "indexer les dossiers", "about" to "a propos",
        "your name" to "ton nom", "welcome message" to "message de bienvenue",
        "rounded corners" to "coins arrondis",
        "your playlists" to "tes playlists", "no playlists yet — make one!" to "aucune playlist",
        "equalizer" to "egaliseur", "play something first" to "joue un morceau",
        "most played songs" to "plus jouees", "top albums" to "top albums", "top artists" to "top artistes",
    )
    private val de = mapOf(
        "welcome to Physic" to "Willkommen bei Physic",
        "you can change all of this later in ~/settings" to "alles spater in ~/settings anderbar",
        "profile" to "Profil", "theme" to "Thema", "font" to "Schriftart",
        "corners" to "Ecken", "index folders" to "Ordner indexieren", "about" to "uber",
        "your name" to "dein Name", "welcome message" to "Willkommensnachricht",
        "rounded corners" to "abgerundete Ecken",
        "your playlists" to "deine Playlists", "no playlists yet — make one!" to "keine Playlists",
        "equalizer" to "Entzerrer", "play something first" to "erst abspielen",
        "most played songs" to "meiste wiedergabe", "top albums" to "top alben", "top artists" to "top konkurrenz",
    )
    private val ja = mapOf(
        "welcome to Physic" to "Physicへようこそ",
        "you can change all of this later in ~/settings" to "あとで~/settingsで変更できます",
        "profile" to "プロフィール", "theme" to "テーマ", "font" to "フォント",
        "corners" to "角", "index folders" to "フォルダを登録", "about" to "概要",
        "your name" to "名前", "welcome message" to "ようこそメッセージ",
        "rounded corners" to "角を丸くする",
        "your playlists" to "プレイリスト", "no playlists yet — make one!" to "プレイリストなし",
        "equalizer" to "イコライザ", "play something first" to "先に再生してください",
        "most played songs" to "再生上位", "top albums" to "人気アルバム", "top artists" to "人気アーティスト",
    )
    private val zh = mapOf(
        "welcome to Physic" to "欢迎使用 Physic",
        "you can change all of this later in ~/settings" to "您可以稍后在 ~/settings 中修改",
        "profile" to "个人资料", "theme" to "主题", "font" to "字体",
        "corners" to "圆角", "index folders" to "索引文件夹", "about" to "关于",
        "your name" to "你的名字", "welcome message" to "欢迎信息",
        "rounded corners" to "圆角",
        "your playlists" to "你的播放列表", "no playlists yet — make one!" to "还没有播放列表",
        "equalizer" to "均衡器", "play something first" to "先播放歌曲",
        "most played songs" to "最常播放", "top albums" to "热门专辑", "top artists" to "热门艺术家",
    )
    private val it = mapOf(
        "welcome to Physic" to "Benvenuto in Physic",
        "you can change all of this later in ~/settings" to "puoi cambiare tutto in ~/settings",
        "profile" to "profilo", "theme" to "tema", "font" to "carattere",
        "corners" to "angoli", "index folders" to "indicizza cartelle", "about" to "info",
        "rounded corners" to "angoli arrotondati",
        "your playlists" to "le tue playlist", "no playlists yet — make one!" to "nessuna playlist",
        "equalizer" to "equalizzatore", "play something first" to "riproduci prima qualcosa",
        "most played songs" to "piu riprodotte", "top albums" to "top album", "top artists" to "top artisti",
    )
    private val pt = mapOf(
        "welcome to Physic" to "Bem-vindo ao Physic",
        "you can change all of this later in ~/settings" to "voce pode mudar tudo em ~/settings",
        "profile" to "perfil", "theme" to "tema", "font" to "fonte",
        "corners" to "cantos", "index folders" to "indexar pastas", "about" to "sobre",
        "rounded corners" to "cantos arredondados",
        "your playlists" to "suas playlists", "no playlists yet — make one!" to "sem playlists",
        "equalizer" to "equalizador", "play something first" to "toque algo primeiro",
        "most played songs" to "mais tocadas", "top albums" to "top albuns", "top artists" to "top artistas",
    )
    private val ru = mapOf(
        "welcome to Physic" to "Добро пожаловать в Physic",
        "you can change all of this later in ~/settings" to "потом в ~/settings",
        "profile" to "профиль", "theme" to "тема", "font" to "шрифт",
        "corners" to "углы", "index folders" to "индексировать папки", "about" to "о программе",
        "rounded corners" to "скруглённые углы",
        "your playlists" to "ваши плейлисты", "no playlists yet — make one!" to "нет плейлистов",
        "equalizer" to "эквалайзер", "play something first" to "сначала включите музыку",
        "most played songs" to "самые частые", "top albums" to "топ альбомы", "top artists" to "топ артисты",
    )
    private val ko = mapOf(
        "welcome to Physic" to "Physic에 오신 것을 환영합니다",
        "you can change all of this later in ~/settings" to "~/settings에서 변경 가능",
        "profile" to "프로필", "theme" to "테마", "font" to "글꼴",
        "corners" to "모서리", "index folders" to "폴더 색인", "about" to "정보",
        "rounded corners" to "둥근 모서리",
        "your playlists" to "내 플레이리스트", "no playlists yet — make one!" to "플레이리스트 없음",
        "equalizer" to "이퀄라이저", "play something first" to "먼저 재생하세요",
        "most played songs" to "많이 재생", "top albums" to "인기 앨범", "top artists" to "인기 아티스트",
    )
    private val ar = mapOf(
        "welcome to Physic" to "مرحباً بك في Physic",
        "you can change all of this later in ~/settings" to "يمكنك التغيير لاحقاً",
        "profile" to "الملف الشخصي", "theme" to "السمة", "font" to "الخط",
        "corners" to "الزوايا", "index folders" to "فهرسة المجلدات", "about" to "حول",
        "rounded corners" to "زوايا دائرية",
        "your playlists" to "قوائمك", "no playlists yet — make one!" to "لا قوائم بعد",
        "equalizer" to "المعادل", "play something first" to "شغّل شيئاً أولاً",
        "most played songs" to "الأكثر تشغيلاً", "top albums" to "الألبومات", "top artists" to "الفنانين",
    )

    fun tr(s: String, lang: String = Locale.getDefault().language): String = when (lang) {
        "es" -> es[s]
        "fr" -> fr[s]
        "de" -> de[s]
        "ja" -> ja[s]
        "zh" -> zh[s]
        "it" -> it[s]
        "pt" -> pt[s]
        "ru" -> ru[s]
        "ko" -> ko[s]
        "ar" -> ar[s]
        else -> null
    } ?: s
}
