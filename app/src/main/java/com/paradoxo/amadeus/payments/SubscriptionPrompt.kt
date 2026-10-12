package com.paradoxo.amadeus.payments

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import com.paradoxo.amadeus.auth.AuthUiState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

@Composable
fun SubscriptionPromptDialog(
    state: EntitlementUiState,
    authState: AuthUiState = AuthUiState(),
    onDismiss: () -> Unit,
    onSubscribe: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Assinatura Amadeus", fontWeight = FontWeight.SemiBold)
        },
        text = {
            Text(
                when {
                    !authState.isGoogleSignedIn -> "Entre com sua Conta Google para assinar e recuperar seu acesso em outro aparelho."
                    authState.loading -> "Conectando sua Conta Google..."
                    state.purchasing -> "Abrindo o pagamento pelo Google Play..."
                    state.errorMessage != null -> state.errorMessage
                    else -> "Assine o plano mensal para acessar modelos de IA mais avançados na seção Inteligência."
                }
            )
        },
        confirmButton = {
            TextButton(
                enabled = !state.purchasing && !authState.loading,
                onClick = {
                    onSubscribe()
                    onDismiss()
                }
            ) {
                Text(
                    if (authState.isGoogleSignedIn) "Assinar" else "Entrar com Google",
                    color = Color(0xFFF50057),
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Agora não")
            }
        }
    )
}

tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
