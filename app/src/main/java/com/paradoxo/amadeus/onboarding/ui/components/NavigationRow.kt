package com.paradoxo.amadeus.onboarding.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun NavigationRow(
    current: Int,
    total: Int,
    accentColor: Color,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLast = current == total - 1

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilledIconButton(
            onClick = onPrevious,
            enabled = current > 0,
            modifier = Modifier.size(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = Color(0xFFF0F0F0),
                contentColor = Color(0xFF666666),
                disabledContainerColor = Color(0xFFF5F5F5),
                disabledContentColor = Color(0xFFCCCCCC)
            )
        ) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Anterior")
        }

        Text(
            text = "${current + 1} / $total",
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF888888)
        )

        FilledIconButton(
            onClick = onNext,
            modifier = Modifier.size(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = accentColor,
                contentColor = Color.White
            )
        ) {
            Icon(
                if (isLast) Icons.Rounded.Check else Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = if (isLast) "Começar" else "Próximo"
            )
        }
    }
}
