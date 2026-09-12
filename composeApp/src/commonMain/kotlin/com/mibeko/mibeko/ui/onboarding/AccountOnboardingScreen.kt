package com.mibeko.mibeko.ui.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mibeko.mibeko.data.remote.OnboardingStep
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AccountOnboardingScreen(
    replay: Boolean,
    onFinished: () -> Unit,
    onOpenLibrary: () -> Unit,
    viewModel: AccountOnboardingViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(replay) { viewModel.initialize(replay) }
    LaunchedEffect(state.finished) { if (state.finished) onFinished() }
    LaunchedEffect(state.currentStep?.key) { viewModel.markViewed() }

    Scaffold { padding ->
        when {
            state.loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.unavailable -> UnavailableOnboarding(
                modifier = Modifier.fillMaxSize().padding(padding),
                onRetry = viewModel::retry,
                onContinue = onFinished
            )
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        "${state.currentIndex + 1} / ${state.steps.size}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = viewModel::postpone) { Text("Plus tard") }
                }

                if (state.offline) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CloudOff, contentDescription = null, modifier = Modifier.size(20.dp))
                            Text(
                                if (state.pendingMutations > 0) "Hors ligne — vos choix seront synchronisés à la reconnexion."
                                else "Hors ligne — reprise depuis la dernière progression connue.",
                                modifier = Modifier.padding(start = 8.dp),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                state.currentStep?.let { step ->
                    StepContent(step, state, viewModel, onOpenLibrary)
                }
            }
        }
    }
}

@Composable
private fun StepContent(
    step: OnboardingStep,
    state: AccountOnboardingUiState,
    viewModel: AccountOnboardingViewModel,
    onOpenLibrary: () -> Unit
) {
    when (step.type) {
        "welcome" -> WelcomeStep(onContinue = { viewModel.answer() })
        "single_choice" -> SingleChoiceStep(step, viewModel::answer)
        "multi_choice" -> MultiChoiceStep(step, state, viewModel::answer, viewModel::skipStep)
        "optional_field" -> OptionalFieldStep(step, viewModel::answer, viewModel::skipStep)
        "guided_action" -> GuidedActionStep(
            onOpenLibrary = { viewModel.answer(after = onOpenLibrary) },
            onSkip = viewModel::skipStep
        )
        "checklist" -> ChecklistStep(step, viewModel::answer, viewModel::skipStep)
    }
}

@Composable
private fun WelcomeStep(onContinue: () -> Unit) {
    Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(72.dp))
    Spacer(Modifier.height(24.dp))
    Text("Bienvenue sur Mibeko", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    Spacer(Modifier.height(12.dp))
    Text(
        "Deux choix facultatifs suffisent pour personnaliser votre découverte. Vous pourrez les modifier plus tard.",
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(32.dp))
    Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) { Text("Commencer") }
}

@Composable
private fun SingleChoiceStep(step: OnboardingStep, onAnswer: (JsonPrimitive) -> Unit) {
    Text("Dans quel cadre utilisez-vous Mibeko ?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(8.dp))
    Text("Ce choix adapte les exemples, sans modifier vos droits.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(24.dp))
    val options = step.config["options"]?.jsonArray.orEmpty()
    options.forEach { option ->
        val obj = option.jsonObject
        val code = obj["code"]?.jsonPrimitive?.contentOrNull ?: return@forEach
        val labelKey = obj["label_key"]?.jsonPrimitive?.contentOrNull.orEmpty()
        OutlinedButton(
            onClick = { onAnswer(JsonPrimitive(code)) },
            modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
            shape = RoundedCornerShape(12.dp)
        ) { Text(onboardingLabel(labelKey, code)) }
    }
}

@Composable
private fun MultiChoiceStep(
    step: OnboardingStep,
    state: AccountOnboardingUiState,
    onAnswer: (JsonArray) -> Unit,
    onSkip: () -> Unit
) {
    Text("Quels sujets vous intéressent ?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Text("Facultatif — choisissez-en autant que vous voulez.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(18.dp))
    val initial = step.progress.value?.let { runCatching { it.jsonArray.map { item -> item.jsonPrimitive.content }.toSet() }.getOrNull() }.orEmpty()
    val selected = remember(step.key) { mutableStateListOf<String>().apply { addAll(initial) } }
    val inlineOptions = step.config["options"]?.jsonArray.orEmpty().mapNotNull { option ->
        val obj = option.jsonObject
        val code = obj["code"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
        code to onboardingLabel(obj["label_key"]?.jsonPrimitive?.contentOrNull.orEmpty(), code)
    }
    val options = if (step.config["source"]?.jsonPrimitive?.contentOrNull == "tags:themes-de-vie") {
        state.themes.map { it.slug to it.name }
    } else {
        inlineOptions
    }
    options.forEach { (code, label) ->
        Row(
            modifier = Modifier.fillMaxWidth().clickable {
                if (code in selected) selected.remove(code) else selected.add(code)
            }.padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = code in selected, onCheckedChange = null)
            Text(label, modifier = Modifier.padding(start = 8.dp))
        }
    }
    if (options.isEmpty()) {
        Text("Les thèmes ne sont pas disponibles pour le moment. Vous pouvez continuer.", style = MaterialTheme.typography.bodyMedium)
    }
    Spacer(Modifier.height(20.dp))
    Button(
        onClick = { onAnswer(JsonArray(selected.map(::JsonPrimitive))) },
        modifier = Modifier.fillMaxWidth(),
        enabled = options.isNotEmpty()
    ) { Text("Continuer") }
    TextButton(onClick = onSkip) { Text("Passer cette étape") }
}

@Composable
private fun OptionalFieldStep(step: OnboardingStep, onAnswer: (JsonPrimitive) -> Unit, onSkip: () -> Unit) {
    var value by remember(step.key) { mutableStateOf(step.progress.value?.jsonPrimitive?.contentOrNull.orEmpty()) }
    Text(onboardingLabel(step.config["title_key"]?.jsonPrimitive?.contentOrNull.orEmpty(), step.key), style = MaterialTheme.typography.headlineSmall)
    Spacer(Modifier.height(18.dp))
    OutlinedTextField(value = value, onValueChange = { value = it }, modifier = Modifier.fillMaxWidth(), singleLine = true)
    Spacer(Modifier.height(20.dp))
    Button(onClick = { onAnswer(JsonPrimitive(value)) }, modifier = Modifier.fillMaxWidth()) { Text("Continuer") }
    TextButton(onClick = onSkip) { Text("Passer cette étape") }
}

@Composable
private fun GuidedActionStep(onOpenLibrary: () -> Unit, onSkip: () -> Unit) {
    Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(72.dp))
    Spacer(Modifier.height(20.dp))
    Text("Gardez un texte disponible hors ligne", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    Spacer(Modifier.height(12.dp))
    Text(
        "Ouvrez la Bibliothèque, choisissez un texte réellement disponible puis téléchargez-le. Vous pourrez ensuite le consulter sans réseau.",
        textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(28.dp))
    Button(onClick = onOpenLibrary, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Default.Explore, contentDescription = null)
        Text("Ouvrir la Bibliothèque", modifier = Modifier.padding(start = 8.dp))
    }
    TextButton(onClick = onSkip) { Text("Je le ferai plus tard") }
}

@Composable
private fun ChecklistStep(step: OnboardingStep, onAnswer: (JsonArray) -> Unit, onSkip: () -> Unit) {
    val items = step.config["items"]?.jsonArray.orEmpty()
    val selected = remember(step.key) { mutableStateListOf<String>() }
    Text("Pour bien démarrer", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    items.forEach { item ->
        val obj = item.jsonObject
        val code = obj["code"]?.jsonPrimitive?.contentOrNull ?: return@forEach
        Row(Modifier.fillMaxWidth().clickable { if (code in selected) selected.remove(code) else selected.add(code) }.padding(8.dp)) {
            Checkbox(code in selected, onCheckedChange = null)
            Text(onboardingLabel(obj["label_key"]?.jsonPrimitive?.contentOrNull.orEmpty(), code), Modifier.padding(start = 8.dp))
        }
    }
    Button(onClick = { onAnswer(JsonArray(selected.map(::JsonPrimitive))) }, modifier = Modifier.fillMaxWidth()) { Text("Terminer") }
    TextButton(onClick = onSkip) { Text("Passer") }
}

@Composable
private fun UnavailableOnboarding(modifier: Modifier, onRetry: () -> Unit, onContinue: () -> Unit) {
    Column(modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(20.dp))
        Text("Mibeko reste disponible", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Le guide ne peut pas être chargé pour le moment. Cela ne bloque aucune fonctionnalité.", textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) { Text("Continuer vers l'application") }
        TextButton(onClick = onRetry) { Text("Réessayer") }
    }
}

private fun onboardingLabel(key: String, fallback: String): String = when (key) {
    "onboarding.usage_context.personal" -> "Pour mes besoins personnels"
    "onboarding.usage_context.studies" -> "Pour mes études"
    "onboarding.usage_context.professional" -> "Pour mon activité professionnelle"
    "onboarding.usage_context.other" -> "Autre"
    else -> fallback.replace('_', ' ').replaceFirstChar { it.uppercase() }
}
