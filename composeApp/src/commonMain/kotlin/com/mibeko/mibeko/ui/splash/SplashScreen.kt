package com.mibeko.mibeko.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import com.mibeko.mibeko.data.preferences.UserPreferencesRepository
import com.mibeko.mibeko.ui.home.HomeScreen
import com.mibeko.mibeko.ui.onboarding.OnboardingScreen
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import mibeko.composeapp.generated.resources.Res
import mibeko.composeapp.generated.resources.logo
import org.koin.compose.koinInject

/**
 * Splash Screen - First screen shown when app launches.
 * Displays animated Mibeko logo and baseline, then navigates to
 * Onboarding (first use) or Home (returning users).
 */
class SplashScreen : Screen {

    @Composable
    override fun Content() {
        val navController = com.mibeko.mibeko.ui.navigation.LocalNavController.current
        val userPreferences: UserPreferencesRepository = koinInject()

        // Animation states
        val alpha = remember { Animatable(0f) }
        val scale = remember { Animatable(0.8f) }

        LaunchedEffect(Unit) {
            // Animate logo appearance
            alpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 800)
            )
            scale.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 600)
            )

            // Wait for a moment
            delay(1200)

            // Navigate based on status
            // Flow: Onboarding -> Disclaimer -> Home
            if (!userPreferences.hasCompletedOnboarding()) {
                navController.navigate(com.mibeko.mibeko.ui.navigation.Screen.Onboarding) {
                    popUpTo(com.mibeko.mibeko.ui.navigation.Screen.Splash) { inclusive = true }
                }
            } else if (!userPreferences.hasAcceptedDisclaimer()) {
                navController.navigate(com.mibeko.mibeko.ui.navigation.Screen.Disclaimer) {
                    popUpTo(com.mibeko.mibeko.ui.navigation.Screen.Splash) { inclusive = true }
                }
            } else {
                navController.navigate(com.mibeko.mibeko.ui.navigation.Screen.Home) {
                    popUpTo(com.mibeko.mibeko.ui.navigation.Screen.Splash) { inclusive = true }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .alpha(alpha.value)
                    .scale(scale.value)
                    .padding(32.dp)
            ) {
                // Logo Icon
                Box(
                    modifier = Modifier
                        .size(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.logo),
                        contentDescription = "Mibeko Logo",
                        tint = Color.Unspecified,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // App Name
                Text(
                    text = "Mibeko",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Baseline
                Text(
                    text = "Le droit congolais dans votre poche",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    fontSize = 16.sp
                )
            }
        }
    }
}
