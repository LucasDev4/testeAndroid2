package com.wesee.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wesee.app.R
import com.wesee.app.ui.theme.WeSeeBlue

enum class PcdTab { Home, Chat, Maps, Account }

private val Inactive = Color(0xFF7FB4F9)

@Composable
private fun NavItem(label: String, icon: Int, selected: Boolean, onClick: (() -> Unit)?, modifier: Modifier) {
    Column(
        modifier.fillMaxHeight().then(if (onClick != null) Modifier.clickable(role = Role.Tab, onClick = onClick) else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (selected) Box(Modifier.width(28.dp).height(4.dp).background(WeSeeBlue, RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp)))
        else Spacer(Modifier.height(4.dp))
        Spacer(Modifier.height(9.dp))
        Image(painterResource(icon), null, Modifier.size(22.dp))
        Spacer(Modifier.height(3.dp))
        WeSeeText(label, if (selected) WeSeeBlue else Inactive, 10, if (selected) FontWeight.Bold else FontWeight.SemiBold)
    }
}

/** Barra inferior do PCD. Maps e Account aparecem para manter o design, mas ainda não têm tela. */
@Composable
fun PcdBottomBar(current: PcdTab, onHome: () -> Unit, onChat: () -> Unit) {
    val shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)
    Row(
        Modifier.fillMaxWidth().shadow(10.dp, shape).background(Color.White, shape).navigationBarsPadding().height(62.dp),
    ) {
        NavItem("Home", if (current == PcdTab.Home) R.drawable.nav_home_selected else R.drawable.nav_home,
            current == PcdTab.Home, onHome, Modifier.weight(1f))
        NavItem("Chat", if (current == PcdTab.Chat) R.drawable.nav_chat_selected else R.drawable.nav_chat,
            current == PcdTab.Chat, onChat, Modifier.weight(1f))
        NavItem("Maps", R.drawable.nav_maps, false, null, Modifier.weight(1f))
        NavItem("Account", R.drawable.nav_account, false, null, Modifier.weight(1f))
    }
}
