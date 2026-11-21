package com.wesee.app.ui.screens.pcd.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.wesee.app.R
import com.wesee.app.ui.components.softCard

/**
 * Área da câmera da Home. Mostra o [content] recebido (a Home usa a câmera real com IA: `LiveCameraContent`);
 * sem conteúdo, mostra o placeholder. [tiltDegrees] = leve inclinação do design (use 0 com a câmera ao vivo).
 */
@Composable
fun CameraArea(
    modifier: Modifier = Modifier,
    tiltDegrees: Float = 1.2f,
    content: @Composable BoxScope.() -> Unit = { CameraPlaceholder() },
) {
    Box(
        modifier.fillMaxWidth().height(250.dp).rotate(tiltDegrees).softCard(26.dp),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

@Composable
fun BoxScope.CameraPlaceholder() {
    Image(painterResource(R.drawable.ic_camera), "Câmera, disponível em breve", Modifier.size(42.dp))
}
