package com.mibeko.mibeko.ui.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mibeko.mibeko.data.preferences.UserPreferencesRepository
import com.mibeko.mibeko.ui.home.HomeScreen
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Data class representing a single onboarding slide.
 */
data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val description: String
)

/**
 * Onboarding Screen - Shown only on first app launch.
 * Presents 3 slides explaining the app's value proposition.
 */
class OnboardingScreen : Screen {

    private val pages = listOf(
        OnboardingPage(
            icon = Icons.Default.CloudOff,
            title = "Tout le droit congolais",
            description = "Accessible hors-ligne, même sans connexion internet. Téléchargez une fois, consultez partout."
        ),
        OnboardingPage(
            icon = Icons.Default.Search,
            title = "Recherche intelligente",
            description = "Trouvez rapidement par numéro d'article ou par thématique. La loi n'a jamais été aussi accessible."
        ),
        OnboardingPage(
            icon = Icons.Default.Folder,
            title = "Organisez vos dossiers",
            description = "Sauvegardez vos articles favoris et organisez vos recherches juridiques efficacement."
        )
    )

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val userPreferences: UserPreferencesRepository = koinInject()
        val scope = rememberCoroutineScope()

        val pagerState = rememberPagerState(pageCount = { pages.size })
        val isLastPage = pagerState.currentPage == pages.lastIndex

        fun completeOnboarding() {
            userPreferences.setOnboardingCompleted()
            navigator.replace(HomeScreen())
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Skip button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = !isLastPage,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        TextButton(onClick = { completeOnboarding() }) {
                            Text(
                                text = "Passer",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Pager with slides
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) { page ->
                    OnboardingPageContent(pages[page])
                }

                // Bottom section: indicators + button
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Page indicators
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(bottom = 32.dp)
                    ) {
                        pages.forEachIndexed { index, _ ->
                            PageIndicator(isSelected = pagerState.currentPage == index)
                            if (index < pages.lastIndex) {
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                        }
                    }

                    // Action button
                    Button(
                        onClick = {
                            if (isLastPage) {
                                completeOnboarding()
                            } else {
                                scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = if (isLastPage) "Commencer" else "Suivant",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun OnboardingPageContent(page: OnboardingPage) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon container
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = page.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Title
            Text(
                text = page.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Description
            Text(
                text = page.description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )
        }
    }

    @Composable
    private fun PageIndicator(isSelected: Boolean) {
        val width by animateDpAsState(
            targetValue = if (isSelected) 24.dp else 8.dp,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
        )

        Box(
            modifier = Modifier
                .height(8.dp)
                .width(width)
                .clip(CircleShape)
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
        )
    }
}
