package com.paradoxo.amadeus.ui.amadeuspro

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.paradoxo.amadeus.payments.findActivity

private val ColorPrimary = Color(0xFFF50057)
private val ColorDarkPurple = Color(0xFF290f3e)
private val ColorSuccess = Color(0xFF4CAF50)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AmadeusProScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val vm: AmadeusProViewModel = viewModel()
    val uiState by vm.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Amadeus Pro", fontWeight = FontWeight.SemiBold, color = Color.White)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ColorDarkPurple)
            )
        },
        bottomBar = {
            ProBottomBar(
                isPremium = uiState.entitlementState.isPremium,
                purchasing = uiState.entitlementState.purchasing,
                authLoading = uiState.authState.loading,
                errorMessage = uiState.entitlementState.errorMessage,
                priceLabel = uiState.priceLabel,
                onSubscribe = {
                    context.findActivity()?.let { vm.subscribe(it) }
                }
            )
        },
        containerColor = Color.White
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(ColorPrimary.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.WorkspacePremium,
                    contentDescription = null,
                    tint = ColorPrimary,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Faça mais com a Amadeus Pro",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = Color(0xFF212121)
            )

            Spacer(Modifier.height(16.dp))

            ProBenefitRow(
                icon = Icons.Rounded.Bolt,
                title = "Use sem limites",
                description = "Envie quantas solicitações quiser à Amadeus, sem o limite de 10 por dia."
            )
            ProBenefitRow(
                icon = Icons.Rounded.History,
                title = "Memória entre sessões",
                description = "Continue tarefas sem precisar explicar tudo novamente."
            )
            ProBenefitRow(
                icon = Icons.Rounded.AutoAwesome,
                title = "Tarefas mais bem executadas",
                description = "A Amadeus entende melhor seus objetivos, planeja os próximos passos e executa tarefas com mais precisão."
            )
        }
    }
}

@Composable
private fun ProBenefitRow(icon: ImageVector, title: String, description: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(ColorDarkPurple.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ColorDarkPurple,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color(0xFF212121))
            Spacer(Modifier.height(2.dp))
            Text(description, fontSize = 13.sp, color = Color(0xFF757575), lineHeight = 18.sp)
        }
    }
    Spacer(Modifier.height(16.dp))
}

@Composable
private fun ProBottomBar(
    isPremium: Boolean,
    purchasing: Boolean,
    authLoading: Boolean,
    errorMessage: String?,
    priceLabel: String?,
    onSubscribe: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isPremium) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = ColorSuccess, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Você já é Amadeus Pro", fontWeight = FontWeight.SemiBold, color = Color(0xFF212121))
                }
            } else {
                Button(
                    onClick = onSubscribe,
                    enabled = !purchasing && !authLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ColorPrimary)
                ) {
                    if (purchasing || authLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Obter Amadeus Pro", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    }
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    text = priceLabel ?: "R$ 15,00 / mês",
                    fontSize = 13.sp,
                    color = Color(0xFF757575)
                )

                if (errorMessage != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = errorMessage,
                        fontSize = 12.sp,
                        color = ColorPrimary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
