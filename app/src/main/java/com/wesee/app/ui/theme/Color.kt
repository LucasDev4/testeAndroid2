package com.wesee.app.ui.theme

import androidx.compose.ui.graphics.Color
import com.wesee.app.settings.AppSettings

/**
 * Paleta do WeSee por PAPEL. Os valores da paleta CLARA são exatamente as cores que o app já usava (o tema claro não muda).
 * Os nomes antigos (WeSeeBlue, WeSeeCard...) continuam existindo e agora seguem o tema escolhido:
 * eles leem [AppSettings.darkTheme] (estado do Compose), então qualquer tela que os use recompõe ao trocar o tema.
 */
class WeSeePalette(
    /** Fundo das telas. */ val page: Color,
    /** Cards, campos de texto e diálogos. */ val surface: Color,
    /** Azul institucional como SUPERFÍCIE (botões, cards azuis, painéis); o texto sobre ele é branco. */ val blue: Color,
    /** Texto e ícones principais sobre [page]/[surface]. */ val ink: Color,
    /** Texto escuro de maior contraste sobre superfícies claras (mensagens, balões). */ val inkDeep: Color,
    /** Texto secundário. */ val mid: Color,
    /** Destaque / item selecionado. */ val accent: Color,
    /** Barra inferior. */ val barBg: Color,
    /** Divisores e bordas. */ val divider: Color,
    /** Item inativo da barra inferior. */ val inactive: Color,
    /** Tom mais escuro do azul (degradês). */ val deep: Color,
    /** Trilhas/indicadores inativos. */ val bar: Color,
    /** Texto suave sobre superfícies azuis. */ val softText: Color,
    /** Áreas de digitação e chips sobre cards (branco no claro). */ val field: Color,
    /** Cards secundários (ex.: alertas não selecionados). */ val surfaceAlt: Color,
    /** Trilhas de barras de posição. */ val track: Color,
    /** Círculos/botões discretos (sino). */ val chip: Color,
    /** Painéis laterais. */ val panel: Color,
    /** Texto auxiliar discreto. */ val muted: Color,
)

val LightPalette = WeSeePalette(
    page = Color(0xFFE0EAF4), surface = Color(0xFFE0EAF4), blue = Color(0xFF004789), ink = Color(0xFF004789),
    inkDeep = Color(0xFF002A55), mid = Color(0xFF3F78B5), accent = Color(0xFF004789), barBg = Color.White,
    divider = Color(0xFF3F78B5), inactive = Color(0xFF7FB4F9), deep = Color(0xFF002A55), bar = Color(0xFF9DB0C8),
    softText = Color(0xFFB9CCE3), field = Color.White,
    surfaceAlt = Color(0xFFEFF5FB), track = Color(0xFFEAF2FB), chip = Color(0xFFBFD0E3), panel = Color(0xFFE6EFF9), muted = Color(0xFF6B7A8C),
)

val DarkPalette = WeSeePalette(
    page = Color(0xFF121820), surface = Color(0xFF243447), blue = Color(0xFF0A5CA8), ink = Color(0xFFF1F5F9),
    inkDeep = Color(0xFFF1F5F9), mid = Color(0xFFB8C7D9), accent = Color(0xFF69B7FF), barBg = Color(0xFF1B2633),
    divider = Color(0xFF34465A), inactive = Color(0xFF8FA3B8), deep = Color(0xFF073E73), bar = Color(0xFF34465A),
    softText = Color(0xFFB9CCE3), field = Color(0xFF1B2633),
    surfaceAlt = Color(0xFF243447), track = Color(0xFF34465A), chip = Color(0xFF34465A), panel = Color(0xFF1B2633), muted = Color(0xFFB8C7D9),
)

/** Paleta ativa (segue o tema escolhido). */
val CurrentPalette: WeSeePalette get() = if (AppSettings.darkTheme) DarkPalette else LightPalette

// ---- Nomes originais (mantidos): seguem o tema ----
/** Azul como superfície (botões/painéis, texto branco por cima). Para TEXTO sobre fundo do tema use [WeSeeInk]. */
val WeSeeBlue: Color get() = CurrentPalette.blue
/** Fundo das telas ([WeSeePage]) — nome antigo. Para cards/campos use [WeSeeSurface]. */
val WeSeeCard: Color get() = CurrentPalette.page
val WeSeeBar: Color get() = CurrentPalette.bar
val WeSeeDeep: Color get() = CurrentPalette.deep
val WeSeeSoftText: Color get() = CurrentPalette.softText
val WeSeeMid: Color get() = CurrentPalette.mid

// ---- Papéis novos ----
val WeSeePage: Color get() = CurrentPalette.page
val WeSeeSurface: Color get() = CurrentPalette.surface
val WeSeeInk: Color get() = CurrentPalette.ink
val WeSeeInkDeep: Color get() = CurrentPalette.inkDeep
val WeSeeAccent: Color get() = CurrentPalette.accent
val WeSeeBarBg: Color get() = CurrentPalette.barBg
val WeSeeDivider: Color get() = CurrentPalette.divider
val WeSeeInactive: Color get() = CurrentPalette.inactive
val WeSeeField: Color get() = CurrentPalette.field
val WeSeeSurfaceAlt: Color get() = CurrentPalette.surfaceAlt
val WeSeeTrack: Color get() = CurrentPalette.track
val WeSeeChip: Color get() = CurrentPalette.chip
val WeSeePanel: Color get() = CurrentPalette.panel
val WeSeeMuted: Color get() = CurrentPalette.muted

/** Cor pontual que muda com o tema (para casos que não têm papel na paleta). */
fun themed(light: Color, dark: Color): Color = if (AppSettings.darkTheme) dark else light

/** Intensidade dos brilhos decorativos: bem mais suave no tema escuro. */
fun glowScale(): Float = if (AppSettings.darkTheme) 0.3f else 1f
