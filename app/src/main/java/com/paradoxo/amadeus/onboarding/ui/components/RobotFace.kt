package com.paradoxo.amadeus.onboarding.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun RobotFace(
    modifier: Modifier = Modifier,
    antennaColor: Color = Color(0xFFE83D6B)
) {
    Canvas(modifier = modifier) {
        val headRadius = size.minDimension * 0.44f
        val cx = size.width / 2
        val cy = size.height / 2 + headRadius * 0.12f

        // Drop shadow
        drawCircle(
            color = Color.Black.copy(alpha = 0.08f),
            radius = headRadius + 3.dp.toPx(),
            center = Offset(cx, cy + 5.dp.toPx())
        )

        // Head circle
        drawCircle(color = Color.White, radius = headRadius, center = Offset(cx, cy))
        drawCircle(
            color = Color(0xFFDDDDDD),
            radius = headRadius,
            center = Offset(cx, cy),
            style = Stroke(width = 1.5.dp.toPx())
        )

        // Antenna stem
        drawLine(
            color = Color(0xFFBBBBBB),
            start = Offset(cx, cy - headRadius + 1.dp.toPx()),
            end = Offset(cx, cy - headRadius - 18.dp.toPx()),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Antenna dot
        drawCircle(
            color = antennaColor,
            radius = 5.5.dp.toPx(),
            center = Offset(cx, cy - headRadius - 24.dp.toPx())
        )

        // Eyes
        val eyeW = headRadius * 0.20f
        val eyeH = headRadius * 0.28f
        val eyeY = cy - headRadius * 0.08f
        val eyeDx = headRadius * 0.34f

        drawRoundRect(
            color = Color(0xFF2D2D2D),
            topLeft = Offset(cx - eyeDx - eyeW / 2, eyeY - eyeH / 2),
            size = Size(eyeW, eyeH),
            cornerRadius = CornerRadius(eyeW * 0.35f)
        )
        drawRoundRect(
            color = Color(0xFF2D2D2D),
            topLeft = Offset(cx + eyeDx - eyeW / 2, eyeY - eyeH / 2),
            size = Size(eyeW, eyeH),
            cornerRadius = CornerRadius(eyeW * 0.35f)
        )

        // Smile
        val smilePath = Path().apply {
            moveTo(cx - headRadius * 0.22f, cy + headRadius * 0.22f)
            cubicTo(
                cx - headRadius * 0.08f, cy + headRadius * 0.38f,
                cx + headRadius * 0.08f, cy + headRadius * 0.38f,
                cx + headRadius * 0.22f, cy + headRadius * 0.22f
            )
        }
        drawPath(smilePath, color = Color(0xFF2D2D2D), style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))

        // Rosy cheeks
        drawCircle(antennaColor.copy(alpha = 0.22f), headRadius * 0.14f, Offset(cx - headRadius * 0.44f, cy + headRadius * 0.20f))
        drawCircle(antennaColor.copy(alpha = 0.22f), headRadius * 0.14f, Offset(cx + headRadius * 0.44f, cy + headRadius * 0.20f))
    }
}
