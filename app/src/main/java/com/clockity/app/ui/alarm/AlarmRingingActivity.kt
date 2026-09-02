package com.clockity.app.ui.alarm

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.view.KeyEvent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.clockity.app.service.AlarmReceiver
import com.clockity.app.ui.theme.*
import com.clockity.app.utils.PreferencesManager

class AlarmRingingActivity : ComponentActivity() {

    private var alarmId: Long = -1L
    private var snoozeMinutes: Int = 5

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Wake screen, bypass keyguard, keep screen on
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            keyguardManager?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        // Immersive full-screen (hide system bars)
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController.hide(WindowInsetsCompat.Type.systemBars())

        alarmId = intent.getLongExtra(AlarmReceiver.EXTRA_ALARM_ID, -1L)
        val label = intent.getStringExtra(AlarmReceiver.EXTRA_ALARM_LABEL) ?: "Alarm"
        val timeStr = intent.getStringExtra(AlarmReceiver.EXTRA_ALARM_TIME) ?: "07:00 AM"
        snoozeMinutes = intent.getIntExtra(AlarmReceiver.EXTRA_SNOOZE_MINUTES, 5)

        setContent {
            ClockityTheme {
                RingingScreen(
                    timeStr = timeStr,
                    label = label,
                    snoozeMinutes = snoozeMinutes,
                    onDismiss = { dismissAlarm() },
                    onSnooze = { snoozeAlarm() }
                )
            }
        }
    }

    private fun dismissAlarm() {
        val dismissIntent = Intent(this, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_DISMISS_ALARM
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
        }
        sendBroadcast(dismissIntent)
        finish()
    }

    private fun snoozeAlarm() {
        if (snoozeMinutes > 0) {
            val snoozeIntent = Intent(this, AlarmReceiver::class.java).apply {
                action = AlarmReceiver.ACTION_SNOOZE_ALARM
                putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
                putExtra(AlarmReceiver.EXTRA_SNOOZE_MINUTES, snoozeMinutes)
            }
            sendBroadcast(snoozeIntent)
        } else {
            dismissAlarm()
        }
        finish()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            val behavior = PreferencesManager.getVolumeKeyBehavior(this)
            when (behavior) {
                "Snooze" -> {
                    snoozeAlarm()
                    return true
                }
                "Dismiss" -> {
                    dismissAlarm()
                    return true
                }
                else -> return super.onKeyDown(keyCode, event)
            }
        }
        return super.onKeyDown(keyCode, event)
    }
}

@Composable
fun RingingScreen(
    timeStr: String,
    label: String,
    snoozeMinutes: Int,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit
) {
    // Pulse animation for ringing effect
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OneUIBlack)
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize()
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // Alarm Title & Large Digital Time
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = label.ifBlank { "Alarm" },
                    fontSize = 26.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OneUIYellow
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = timeStr,
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Light,
                    color = OneUITextPrimary,
                    modifier = Modifier.scale(pulseScale)
                )
            }

            // Bottom Actions (Snooze & Dismiss)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Snooze Pill (only if snooze is enabled)
                if (snoozeMinutes > 0) {
                    Button(
                        onClick = onSnooze,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = OneUICardElevated,
                            contentColor = OneUITextPrimary
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Snooze, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Snooze (${snoozeMinutes}m)",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Dismiss Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OneUIRed,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Tap to dismiss",
                    fontSize = 14.sp,
                    color = OneUITextSecondary
                )

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
