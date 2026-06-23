package com.example.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun AuthScreen(viewModel: PromoViewModel) {
    val classmates by viewModel.classmates.collectAsStateWithLifecycle()
    var isRegistering by remember { mutableStateOf(false) }

    if (isRegistering) {
        RegisterScreen(
            onRegister = { name, nickname, bio ->
                viewModel.registerNewUser(name, nickname, bio)
            },
            onCancel = { isRegistering = false }
        )
    } else {
        LoginScreen(
            classmates = classmates,
            onLogin = { userId ->
                viewModel.setCurrentUser(userId)
            },
            onGoToRegister = { isRegistering = true }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    classmates: List<com.example.data.Classmate>,
    onLogin: (String) -> Unit,
    onGoToRegister: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Selecciona tu cuenta") })
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onGoToRegister,
                icon = { Icon(Icons.Default.Add, "Crear") },
                text = { Text("Nuevo Usuario") }
            )
        }
    ) { padding ->
        if (classmates.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No hay cuentas creadas. ¡Crea una nueva!")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
                items(classmates) { user ->
                    ListItem(
                        headlineContent = { Text(user.name) },
                        supportingContent = { Text("@${user.nickname}") },
                        leadingContent = {
                            Icon(Icons.Default.Person, contentDescription = "User", modifier = Modifier.size(40.dp))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        colors = ListItemDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                    Button(
                        onClick = { onLogin(user.id) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth()
                    ) {
                        Text("Iniciar Sesión como ${user.name}")
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onRegister: (String, String, String) -> Unit,
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var termsAccepted by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }

    if (showTermsDialog) {
        AlertDialog(
            onDismissRequest = { showTermsDialog = false },
            title = { Text("Términos y Condiciones") },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    item {
                        Text("""
                            Al usar esta plataforma, aceptas los siguientes términos:
                            
                            1. YO Jeremy Jimenez Montero no me hago cargo ni responsable de ningún tipo de acoso, bullying, etc., que ocurra dentro de la plataforma provocado por los mismos estudiantes.
                            2. Respeto mutuo y prohibición de lenguaje de odio o discriminación.
                            3. Privacidad y no divulgación de información personal sensible de otros.
                            4. El contenido subido debe ser apropiado (está prohibido subir contenido para adultos o violencia explícita).
                            5. Si es necesario, su cuenta puede ser eliminada/bloqueada temporalmente ante infracciones a estas normas.
                        """.trimIndent(), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { 
                    termsAccepted = true
                    showTermsDialog = false 
                }) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTermsDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Crear Nueva Cuenta") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre Completo") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = nickname,
                onValueChange = { nickname = it },
                label = { Text("Apodo corto (@usuario)") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = bio,
                onValueChange = { bio = it },
                label = { Text("Biografía") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                Checkbox(
                    checked = termsAccepted,
                    onCheckedChange = { termsAccepted = it }
                )
                Text(
                    text = "Acepto los ",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Términos y Condiciones",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline
                    ),
                    modifier = Modifier.clickable { showTermsDialog = true }
                )
            }

            Button(
                onClick = {
                    if (name.isNotBlank() && nickname.isNotBlank() && termsAccepted) {
                        onRegister(name, nickname, bio)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank() && nickname.isNotBlank() && termsAccepted
            ) {
                Text("Registrar e Iniciar Sesión")
            }

            TextButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancelar")
            }
        }
    }
}
