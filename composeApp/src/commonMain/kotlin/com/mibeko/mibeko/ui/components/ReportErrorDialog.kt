package com.mibeko.mibeko.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportErrorDialog(
    onDismiss: () -> Unit,
    onSubmit: (type: String, description: String) -> Unit
) {
    var selectedType by remember { mutableStateOf("Erreur de texte") }
    var description by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    val errorTypes = listOf(
        "Erreur de texte",
        "Problème de structure",
        "Doublon",
        "Texte obsolète",
        "Autre"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Signaler une erreur") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Aidez-nous à améliorer la base de données juridique en signalant les erreurs que vous rencontrez.",
                    style = MaterialTheme.typography.bodySmall
                )

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Type d'erreur") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier
                            .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        errorTypes.forEach { selectionOption ->
                            DropdownMenuItem(
                                text = { Text(selectionOption) },
                                onClick = {
                                    selectedType = selectionOption
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    placeholder = { Text("Que devrait dire ce texte ?") },
                    supportingText = {
                        Text(
                            if (description.isBlank()) {
                                "Décrivez l'erreur : sans détail, une équipe réduite ne peut pas la corriger."
                            } else {
                                "${description.trim().length} caractère(s)"
                            }
                        )
                    },
                    isError = description.isBlank(),
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    maxLines = 4
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSubmit(selectedType, description.trim())
                    onDismiss()
                },
                enabled = description.trim().length >= 10
            ) {
                Text("Envoyer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}
