package com.paradoxo.amadeus.auth

import android.app.Activity
import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class AuthUiState(
    val uid: String? = null,
    val displayName: String? = null,
    val email: String? = null,
    val photoUrl: String? = null,
    val isAnonymous: Boolean = true,
    val loading: Boolean = false,
    val errorMessage: String? = null
) {
    val isGoogleSignedIn: Boolean
        get() = uid != null && !isAnonymous
}

class AuthManager private constructor(private val context: Context) {

    private val appContext = context.applicationContext
    private val auth = FirebaseAuth.getInstance()
    private val credentialManager = CredentialManager.create(appContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow(auth.currentUser.toAuthUiState())
    val state: StateFlow<AuthUiState> = _state

    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        _state.value = firebaseAuth.currentUser.toAuthUiState()
    }

    init {
        auth.addAuthStateListener(authStateListener)
    }

    suspend fun ensureAnonymousUser(): FirebaseUser {
        auth.currentUser?.let { return it }
        return auth.signInAnonymously().await().user
            ?: error("Não foi possível criar usuário anônimo.")
    }

    suspend fun requireGoogleAccountForPurchase(activity: Activity): Boolean {
        val current = auth.currentUser
        if (current != null && !current.isAnonymous) return true
        return signInWithGoogle(activity)
    }

    suspend fun signInWithGoogle(activity: Activity): Boolean {
        _state.update { it.copy(loading = true, errorMessage = null) }
        return try {
            val serverClientId = getDefaultWebClientId()
            if (serverClientId.isBlank()) {
                _state.update {
                    it.copy(
                        loading = false,
                        errorMessage = "Configure o OAuth Web Client no Firebase e atualize o google-services.json."
                    )
                }
                return false
            }

            val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(serverClientId)
                .build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(signInWithGoogleOption)
                .build()
            val result = credentialManager.getCredential(activity, request)
            val credential = result.credential
            if (credential !is CustomCredential ||
                credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                _state.update {
                    it.copy(loading = false, errorMessage = "Credencial Google inválida.")
                }
                return false
            }

            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val firebaseCredential = GoogleAuthProvider.getCredential(
                googleIdTokenCredential.idToken,
                null
            )

            val current = auth.currentUser
            if (current != null && current.isAnonymous) {
                runCatching {
                    current.linkWithCredential(firebaseCredential).await()
                }.getOrElse {
                    auth.signInWithCredential(firebaseCredential).await()
                }
            } else {
                auth.signInWithCredential(firebaseCredential).await()
            }

            _state.update { auth.currentUser.toAuthUiState() }
            true
        } catch (e: Exception) {
            _state.update {
                it.copy(
                    loading = false,
                    errorMessage = e.message ?: "Não foi possível entrar com Google."
                )
            }
            false
        }
    }

    fun signInWithGoogleAsync(activity: Activity, onResult: ((Boolean) -> Unit)? = null) {
        scope.launch {
            onResult?.invoke(signInWithGoogle(activity))
        }
    }

    fun signOut() {
        scope.launch {
            runCatching {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            }
            auth.signOut()
            _state.value = AuthUiState()
        }
    }

    private fun getDefaultWebClientId(): String {
        val id = appContext.resources.getIdentifier(
            "default_web_client_id",
            "string",
            appContext.packageName
        )
        return if (id != 0) appContext.getString(id) else ""
    }

    companion object {
        @Volatile private var instance: AuthManager? = null

        fun getInstance(context: Context): AuthManager =
            instance ?: synchronized(this) {
                instance ?: AuthManager(context.applicationContext).also { instance = it }
            }
    }
}

private fun FirebaseUser?.toAuthUiState(): AuthUiState {
    if (this == null) return AuthUiState()
    return AuthUiState(
        uid = uid,
        displayName = displayName,
        email = email,
        photoUrl = photoUrl?.toString(),
        isAnonymous = isAnonymous,
        loading = false,
        errorMessage = null
    )
}
