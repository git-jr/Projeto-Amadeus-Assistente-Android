package com.paradoxo.amadeus.onboarding.ui.pages

import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paradoxo.amadeus.onboarding.ui.components.RobotFace

@Composable
fun AgentModeContent(
    modifier: Modifier = Modifier,
    bgColor: Color,
    accentColor: Color
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.1f)
            .clip(RoundedCornerShape(24.dp))
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        // "NOVO" badge
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(14.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(accentColor)
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text("NOVO", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
        }

        // Floating icons (top-left, top-right, bottom-right, bottom-left)
        val floatingIcons = listOf(
            Triple(Icons.Rounded.CalendarMonth, (-105).dp, (-55).dp),
            Triple(Icons.Rounded.Email, 100.dp, (-65).dp),
            Triple(Icons.Rounded.AccessTime, 108.dp, 30.dp),
            Triple(Icons.Rounded.Description, (-100).dp, 45.dp),
        )
        floatingIcons.forEach { (icon, dx, dy) ->
            FloatingIconChip(icon = icon, dx = dx, dy = dy)
        }

        // Glow halo behind robot
        Box(
            modifier = Modifier
                .size(136.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.14f))
        )

        RobotFace(
            modifier = Modifier.size(112.dp),
            antennaColor = accentColor
        )
    }
}

@Composable
private fun FloatingIconChip(
    icon: ImageVector,
    dx: Dp,
    dy: Dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "float_${icon.name}")
    val floatY by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(tween(2200, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "float_y_${icon.name}"
    )
    Box(
        modifier = Modifier
            .offset(x = dx, y = dy + floatY.dp)
            .shadow(3.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .padding(10.dp)
    ) {
        Icon(icon, contentDescription = null, tint = Color(0xFF777777), modifier = Modifier.size(20.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun AgentModeContentPreview() {
    MaterialTheme {
        AgentModeContent(
            bgColor = Color(0xFFFDEDF1),
            accentColor = Color(0xFFE83D6B)
        )
    }
}
