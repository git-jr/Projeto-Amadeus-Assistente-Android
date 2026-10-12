package com.paradoxo.amadeus.onboarding.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BottomBanner(
    onExplore: () -> Unit,
    dontShowAgain: Boolean,
    onDontShowAgainChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
//        Row(
//            modifier = Modifier
//                .fillMaxWidth()
//                .clip(RoundedCornerShape(16.dp))
//                .background(Color(0xFFFFF8E1))
//                .padding(horizontal = 16.dp, vertical = 12.dp),
//            horizontalArrangement = Arrangement.SpaceBetween,
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            Row(
//                verticalAlignment = Alignment.CenterVertically,
//                horizontalArrangement = Arrangement.spacedBy(10.dp),
//                modifier = Modifier.weight(1f)
//            ) {
//                Icon(
//                    Icons.Rounded.CardGiftcard,
//                    contentDescription = null,
//                    tint = Color(0xFFE6A817),
//                    modifier = Modifier.size(22.dp)
//                )
//                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
//                    Text(
//                        "Tem mais!",
//                        fontSize = 13.sp,
//                        fontWeight = FontWeight.SemiBold,
//                        color = Color(0xFF5D4037)
//                    )
//                    Text(
//                        "Dá uma olhada em tudo que o Amadeus pode fazer por você.",
//                        fontSize = 11.sp,
//                        color = Color(0xFF8D6E63),
//                        lineHeight = 14.sp
//                    )
//                }
//            }
//
//            Button(
//                onClick = onExplore,
//                shape = RoundedCornerShape(12.dp),
//                colors = ButtonDefaults.buttonColors(
//                    containerColor = Color(0xFFFFECB3),
//                    contentColor = Color(0xFF5D4037)
//                ),
//                modifier = Modifier.padding(start = 8.dp)
//            ) {
//                Text("Explorar agora →", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
//            }
//        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Checkbox(
                checked = dontShowAgain,
                onCheckedChange = onDontShowAgainChange,
                colors = CheckboxDefaults.colors(
                    checkedColor = Color(0xFF888888),
                    uncheckedColor = Color(0xFFAAAAAA)
                )
            )
            Text(
                "Não exibir mais",
                fontSize = 12.sp,
                color = Color(0xFF888888)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BottomBannerPreview() {
    MaterialTheme {
        BottomBanner(
            onExplore = {},
            dontShowAgain = false,
            onDontShowAgainChange = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun BottomBannerCheckedPreview() {
    MaterialTheme {
        BottomBanner(
            onExplore = {},
            dontShowAgain = true,
            onDontShowAgainChange = {}
        )
    }
}
