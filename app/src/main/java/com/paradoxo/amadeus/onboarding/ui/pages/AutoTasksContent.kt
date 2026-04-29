package com.paradoxo.amadeus.onboarding.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AutoTasksContent(
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
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 24.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TaskItem(
                icon = Icons.Rounded.Analytics,
                title = "Analisar dados",
                done = true,
                iconBg = Color(0xFFFDE8ED),
                iconTint = Color(0xFFE83D6B),
                checkColor = Color(0xFFE83D6B)
            )
            TaskItem(
                icon = Icons.Rounded.FolderOpen,
                title = "Organizar informações",
                done = false,
                iconBg = accentColor.copy(alpha = 0.12f),
                iconTint = accentColor,
                checkColor = accentColor
            )
            TaskItem(
                icon = Icons.AutoMirrored.Rounded.Send,
                title = "Enviar relatório",
                done = true,
                iconBg = Color(0xFFE8F5ED),
                iconTint = Color(0xFF4CAF6B),
                checkColor = Color(0xFF4CAF6B)
            )
        }
    }
}

@Composable
private fun TaskItem(
    icon: ImageVector,
    title: String,
    done: Boolean,
    iconBg: Color,
    iconTint: Color,
    checkColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF333333))
            Box(
                modifier = Modifier
                    .fillMaxWidth(if (done) 1f else 0.55f)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFFEEEEEE))
            )
        }

        if (done) {
            Icon(
                Icons.Rounded.CheckCircle,
                contentDescription = "Concluído",
                tint = checkColor,
                modifier = Modifier.size(22.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEEEEEE))
            )
        }
    }
}
