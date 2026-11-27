package com.wesee.app.device

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import kotlin.math.roundToInt
import com.wesee.app.R
import com.wesee.app.settings.tr

data class BatteryState(val percent: Int, val charging: Boolean)

/** Consumo médio estimado do app (câmera + GPS + IA) em % de bateria por hora: ~16%/h ≈ 6 h com 100%. */
const val DRAIN_PERCENT_PER_HOUR = 16f

/** tempo_estimado = bateria_atual / consumo_medio_por_hora */
fun BatteryState.remainingLabel(): String {
    if (charging) return tr(R.string.battery_charging)
    val hours = percent / DRAIN_PERCENT_PER_HOUR
    if (hours < 1f) return tr(R.string.battery_minutes_left, (hours * 60).roundToInt())
    val h = hours.roundToInt()
    return tr(if (h == 1) R.string.battery_hours_left_one else R.string.battery_hours_left_many, h)
}

private fun read(intent: Intent?): BatteryState {
    val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
    val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
    val percent = if (level >= 0 && scale > 0) level * 100 / scale else 0
    val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
    return BatteryState(percent, charging)
}

/** Bateria real do aparelho, atualizada em tempo real. */
@Composable
fun rememberBatteryState(): BatteryState {
    val context = LocalContext.current
    val filter = remember { IntentFilter(Intent.ACTION_BATTERY_CHANGED) }
    var state by remember { mutableStateOf(read(context.registerReceiver(null, filter))) }
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) { state = read(intent) }
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose { context.unregisterReceiver(receiver) }
    }
    return state
}
