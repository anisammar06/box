package com.k650.remote.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.k650.remote.data.CpService
import com.k650.remote.ui.theme.Amber
import com.k650.remote.ui.theme.OnInkFaint
import com.k650.remote.ui.theme.Warn

@Composable
fun UrlDialog(onDismiss: () -> Unit, onPlay: (String) -> Unit) {
    var url by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { if (url.isNotBlank()) onPlay(url) }) { Text("Lire", color = Amber) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler", color = Amber) } },
        title = { Text("Lire une URL audio") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("http://… (NAS, webradio, TTS)") },
                    singleLine = true,
                )
                Text(
                    "Flux audio HTTP direct. N'injectez pas de flux YouTube extraits (CGU).",
                    color = OnInkFaint,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        },
    )
}

@Composable
fun LoginDialog(service: CpService, onDismiss: () -> Unit, onLogin: (String, String) -> Unit) {
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { if (user.isNotBlank()) onLogin(user, pass) }) { Text("Se connecter", color = Amber) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler", color = Amber) } },
        title = { Text("Connexion ${service.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = user, onValueChange = { user = it }, label = { Text("Identifiant / e-mail") }, singleLine = true)
                OutlinedTextField(
                    value = pass, onValueChange = { pass = it }, label = { Text("Mot de passe") },
                    singleLine = true, visualTransformation = PasswordVisualTransformation(),
                )
                Text(
                    "⚠ La barre reçoit vos identifiants en clair sur le réseau local (HTTP, sans " +
                        "chiffrement). Le firmware n'offre pas d'autre méthode. À n'utiliser que sur un réseau de confiance.",
                    color = Warn,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        },
    )
}

@Composable
fun RenameDialog(initial: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { if (name.isNotBlank()) onSave(name) }) { Text("Enregistrer", color = Amber) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler", color = Amber) } },
        title = { Text("Renommer la barre") },
        text = { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nom") }, singleLine = true) },
    )
}
