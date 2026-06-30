package com.mibeko.mibeko.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mibeko.mibeko.ui.navigation.LocalNavController
import com.mibeko.mibeko.ui.navigation.Screen
import mibeko.composeapp.generated.resources.Res
import mibeko.composeapp.generated.resources.logo
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen() {
    val navController = LocalNavController.current
    val viewModel: LoginViewModel = koinViewModel()
    val loginState by viewModel.loginState.collectAsState()

    var passwordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(loginState) {
        if (loginState is LoginState.Success) {
            val destination = if ((loginState as LoginState.Success).requiresProfileSetup) {
                Screen.ProfileSetup
            } else {
                Screen.Home
            }
            navController.navigate(destination) {
                popUpTo(Screen.Login) { inclusive = true }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = {
                        if (loginState is LoginState.TwoFactorRequired) {
                            viewModel.cancelTwoFactor()
                        } else {
                            navController.navigateUp()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Image(
                painter = painterResource(Res.drawable.logo),
                contentDescription = null,
                modifier = Modifier.size(88.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            when (val state = loginState) {
                is LoginState.TwoFactorRequired -> TwoFactorStep(
                    code = viewModel.twoFactorCode,
                    onCodeChange = viewModel::onTwoFactorCodeChange,
                    onSubmit = viewModel::loginWithEmail,
                    onCancel = viewModel::cancelTwoFactor,
                    error = state.error
                )

                else -> CredentialsStep(
                    viewModel = viewModel,
                    loginState = loginState,
                    passwordVisible = passwordVisible,
                    onPasswordVisibilityChange = { passwordVisible = it },
                    onForgotPassword = { navController.navigate(Screen.ForgotPassword) },
                    onRegister = { navController.navigate(Screen.Register) }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "En continuant, vous acceptez nos conditions d'utilisation et notre politique de confidentialité.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun CredentialsStep(
    viewModel: LoginViewModel,
    loginState: LoginState,
    passwordVisible: Boolean,
    onPasswordVisibilityChange: (Boolean) -> Unit,
    onForgotPassword: () -> Unit,
    onRegister: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Bienvenue sur Mibeko",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )

        Text(
            text = "L'intelligence juridique à votre portée.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(40.dp))

        val isLoading = loginState is LoginState.Loading

        OutlinedTextField(
            value = viewModel.email,
            onValueChange = viewModel::onEmailChange,
            label = { Text("Adresse email") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = viewModel.password,
            onValueChange = viewModel::onPasswordChange,
            label = { Text("Mot de passe") },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            enabled = !isLoading,
            trailingIcon = {
                val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                val description = if (passwordVisible) "Masquer le mot de passe" else "Afficher le mot de passe"
                IconButton(onClick = { onPasswordVisibilityChange(!passwordVisible) }) {
                    Icon(imageVector = image, contentDescription = description)
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.CenterEnd
        ) {
            TextButton(onClick = onForgotPassword, enabled = !isLoading) {
                Text("Mot de passe oublié ?", style = MaterialTheme.typography.labelLarge)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { viewModel.loginWithEmail() },
            enabled = !isLoading && viewModel.email.isNotBlank() && viewModel.password.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Se connecter")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(onClick = onRegister, enabled = !isLoading) {
            Text("Pas encore de compte ? S'inscrire")
        }

        if (loginState is LoginState.Error) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = loginState.message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Étape de double authentification : le mot de passe a été accepté mais le
 * compte exige un code TOTP de l'application d'authentification.
 */
@Composable
private fun TwoFactorStep(
    code: String,
    onCodeChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onCancel: () -> Unit,
    error: String?
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            Icons.Default.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(40.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Double authentification",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Text(
            text = "Saisissez le code à 6 chiffres de votre application d'authentification.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = code,
            onValueChange = onCodeChange,
            label = { Text("Code de vérification") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            isError = error != null,
            supportingText = error?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onSubmit,
            enabled = code.length == 6,
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text("Vérifier")
        }

        TextButton(onClick = onCancel, modifier = Modifier.padding(top = 8.dp)) {
            Text("Revenir à la connexion")
        }
    }
}
