package com.wesee.app.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.drawable.ColorDrawable
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.core.view.WindowCompat
import com.wesee.app.settings.AppSettings

private tailrec fun Context.activityOrNull(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.activityOrNull()
    else -> null
}

/**
 * Raiz visual do app. Faz duas coisas, as duas aplicadas NA HORA e sem reiniciar:
 *  1. Idioma: fornece um Context cujos recursos seguem [AppSettings.language] (stringResource/values-en passam a valer
 *     em todo o app). O idioma do aparelho não é alterado.
 *  2. Tema: ajusta os ícones da barra de status/navegação (claro/escuro) e o fundo da janela. As cores das telas vêm de
 *     [WeSeePalette] (veja Color.kt) e recompõem sozinhas.
 */
@Composable
fun WeSeeTheme(content: @Composable () -> Unit) {
    val base = LocalContext.current
    val language = AppSettings.language
    val dark = AppSettings.darkTheme
    val localized = remember(base, language) { AppSettings.localizedContext(base, language) }

    SideEffect {
        base.activityOrNull()?.window?.let { window ->
            window.setBackgroundDrawable(ColorDrawable(CurrentPalette.page.toArgb()))
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.isAppearanceLightStatusBars = !dark      // ícones escuros só quando o fundo é claro
            controller.isAppearanceLightNavigationBars = !dark
        }
    }
    CompositionLocalProvider(
        LocalContext provides localized,
        LocalConfiguration provides localized.resources.configuration,
    ) { content() }
}

/** Ícones e logos do app são silhuetas em azul escuro: no tema escuro são recoloridos com a cor de texto do tema. */
@Composable
fun themedTint(): ColorFilter? = if (AppSettings.darkTheme) ColorFilter.tint(WeSeeInk) else null

/** Igual a [themedTint], porém com a cor de destaque (ex.: item selecionado da barra inferior). */
@Composable
fun accentTint(): ColorFilter? = if (AppSettings.darkTheme) ColorFilter.tint(WeSeeAccent) else null

/** Image que segue o tema (ícones de uma cor sobre o fundo/cards). */
@Composable
fun ThemedImage(
    painter: Painter, contentDescription: String?, modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.Center, contentScale: ContentScale = ContentScale.Fit,
    colorFilter: ColorFilter? = themedTint(),
) = Image(painter, contentDescription, modifier, alignment, contentScale, colorFilter = colorFilter)
