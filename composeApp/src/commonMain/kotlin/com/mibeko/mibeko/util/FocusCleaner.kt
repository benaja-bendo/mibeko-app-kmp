package com.mibeko.mibeko.util

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Modificateur permettant de masquer le clavier et de retirer le focus
 * lorsqu'on clique en dehors d'un champ de saisie de texte.
 */
fun Modifier.addFocusCleaner(focusManager: FocusManager): Modifier {
    return this.pointerInput(Unit) {
        detectTapGestures(onTap = {
            focusManager.clearFocus()
        })
    }
}
