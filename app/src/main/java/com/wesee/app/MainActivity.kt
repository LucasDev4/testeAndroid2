package com.wesee.app

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.graphics.toArgb
import com.wesee.app.navigation.WeSeeApp
import com.wesee.app.settings.AppSettings
import com.wesee.app.ui.theme.CurrentPalette
import com.wesee.app.ui.theme.WeSeeTheme

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppSettings.init(this)   // idioma, tema e notificações salvos: lidos ANTES de desenhar a 1ª tela
        window.setBackgroundDrawable(ColorDrawable(CurrentPalette.page.toArgb()))   // sem "flash" claro ao abrir no escuro
        enableEdgeToEdge()
        setContent { WeSeeTheme { WeSeeApp() } }
    }
}
