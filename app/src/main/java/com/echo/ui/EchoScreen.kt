package com.echo.ui

import android.Manifest
import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.echo.core.EchoSessionManager
import com.echo.core.EchoState
import com.echo.data.EchoSettingsRepository
import kotlinx.coroutines.launch

private val Night = Color(0xFF09131A)
private val Panel = Color(0xFF10232B)
private val Cyan = Color(0xFF41E2D0)
private val Coral = Color(0xFFFF8A70)
private val Muted = Color(0xFF9FB4BB)

@Composable
fun EchoScreen(onStartService: () -> Unit, onEnableOverlay: () -> Unit) {
    val context = LocalContext.current
    val session = remember { EchoSessionManager.get(context) }
    val state by session.state.collectAsState()
    val response by session.lastResponse.collectAsState()
    val transcript by session.transcript.collectAsState()
    val permissionRequest by session.permissionRequest.collectAsState()
    var showSettings by remember { mutableStateOf(false) }
    var micExplanation by remember { mutableStateOf(false) }
    var serviceAfterMic by remember { mutableStateOf(false) }
    var overlayAfterMic by remember { mutableStateOf(false) }
    var overlayEnabled by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        val granted = results.values.all { it }
        if (permissionRequest.isNotEmpty()) session.onPermissionsResult(granted)
        if (granted && serviceAfterMic) {
            if (overlayAfterMic) onEnableOverlay() else onStartService()
        }
        serviceAfterMic = false
        overlayAfterMic = false
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) overlayEnabled = Settings.canDrawOverlays(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(permissionRequest) {
        if (permissionRequest.isNotEmpty()) micExplanation = true
    }
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            micExplanation = true
        }
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Cyan,
            onPrimary = Night,
            secondary = Coral,
            onSecondary = Night,
            background = Night,
            onBackground = Color.White,
            surface = Panel,
            onSurface = Color.White,
            outline = Muted
        )
    ) {
        if (showSettings) {
            SettingsScreen(onBack = { showSettings = false }, onVoicePreference = session::updateFemaleVoice)
        } else {
            HomeScreen(
                state = state,
                response = response,
                transcript = transcript,
                overlayEnabled = overlayEnabled,
                context = context,
                onListen = session::listen,
                onStartService = {
                    val hasMic = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    if (hasMic) onStartService() else {
                        serviceAfterMic = true
                        micExplanation = true
                    }
                },
                onEnableOverlay = {
                    val hasMic = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    if (hasMic) onEnableOverlay() else {
                        serviceAfterMic = true
                        overlayAfterMic = true
                        micExplanation = true
                    }
                },
                onSettings = { showSettings = true }
            )
        }
    }

    if (micExplanation) {
        AlertDialog(
            onDismissRequest = { micExplanation = false },
            title = { Text("Autoriser le microphone") },
            text = {
                Text(
                    if (permissionRequest.isNotEmpty() && Manifest.permission.RECORD_AUDIO !in permissionRequest) {
                        "Cette commande nécessite les autorisations suivantes : ${permissionRequest.joinToString { it.substringAfterLast('.') }}."
                    } else {
                        "ECHO a besoin du microphone pour reconnaître tes commandes vocales. L'écoute ne démarre qu'après une action de ta part."
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    micExplanation = false
                    if (permissionRequest.isNotEmpty()) permissionLauncher.launch(permissionRequest.toTypedArray())
                    else permissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
                }) { Text("Continuer") }
            },
            dismissButton = {
                TextButton(onClick = {
                    micExplanation = false
                    if (permissionRequest.isNotEmpty()) session.onPermissionsResult(false)
                    serviceAfterMic = false
                    overlayAfterMic = false
                }) { Text("Plus tard") }
            }
        )
    }
}

@Composable
private fun HomeScreen(
    state: EchoState,
    response: String,
    transcript: String,
    overlayEnabled: Boolean,
    context: Context,
    onListen: () -> Unit,
    onStartService: () -> Unit,
    onEnableOverlay: () -> Unit,
    onSettings: () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "echo-pulse")
    val pulse by transition.animateFloat(
        initialValue = 0.94f,
        targetValue = if (state == EchoState.IDLE) 1f else 1.08f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulse"
    )
    val active = state != EchoState.IDLE
    Scaffold(containerColor = Night) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(Modifier.fillMaxWidth().padding(top = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("ECHO", color = Cyan, fontSize = 25.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onSettings) { Text("Réglages", color = Muted) }
            }
            Spacer(Modifier.height(40.dp))
            Text(
                when (state) {
                    EchoState.IDLE -> "PRÊTE À ÉCOUTER"
                    EchoState.LISTENING -> "JE T'ÉCOUTE"
                    EchoState.THINKING -> "JE RÉFLÉCHIS"
                    EchoState.SPEAKING -> "JE RÉPONDS"
                },
                color = if (active) Coral else Muted,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.5.sp
            )
            Spacer(Modifier.height(24.dp))
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(250.dp)) {
                Canvas(Modifier.fillMaxSize().scale(if (active) pulse else 1f).alpha(if (active) 0.7f else 0.35f)) {
                    drawCircle(Cyan, style = Stroke(width = 1.5.dp.toPx()))
                    drawCircle(Cyan.copy(alpha = 0.3f), radius = size.minDimension * 0.42f, style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round))
                }
                Surface(
                    onClick = onListen,
                    modifier = Modifier.size(158.dp),
                    shape = CircleShape,
                    color = if (active) Color(0xFF16414A) else Panel,
                    border = BorderStroke(2.dp, if (active) Coral else Cyan),
                    shadowElevation = 18.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("E", color = Cyan, fontSize = 64.sp, fontWeight = FontWeight.Light)
                    }
                }
            }
            Spacer(Modifier.height(22.dp))
            Text(transcript.ifBlank { "Appuie pour parler" }, color = Muted, fontSize = 15.sp, modifier = Modifier.alpha(if (transcript.isBlank()) 0.7f else 1f))
            Spacer(Modifier.height(22.dp))
            Surface(
                modifier = Modifier.fillMaxWidth().height(96.dp),
                shape = RoundedCornerShape(8.dp),
                color = Panel
            ) {
                Box(Modifier.padding(16.dp), contentAlignment = Alignment.CenterStart) {
                    Text(response.ifBlank { "En attente de ta prochaine instruction." }, color = Color(0xFFE4F0F1), fontSize = 16.sp)
                }
            }
            Spacer(Modifier.height(22.dp))
            Button(
                onClick = onListen,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = Night)
            ) {
                Icon(Icons.Filled.Mic, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Parler à ECHO", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onStartService, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
                Text("Démarrer le service", color = Color.White)
            }
            OutlinedButton(onClick = onEnableOverlay, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
                Text(if (overlayEnabled) "Bulle flottante activée" else "Activer la bulle flottante", color = if (overlayEnabled) Cyan else Color.White)
            }
            if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Text("Les notifications système peuvent être masquées par Android.", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp))
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SettingsScreen(onBack: () -> Unit, onVoicePreference: (Boolean) -> Unit) {
    val context = LocalContext.current
    val repository = remember { EchoSettingsRepository(context) }
    val scope = rememberCoroutineScope()
    var userName by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf("") }
    var femaleVoice by remember { mutableStateOf(true) }
    var saved by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        repository.settings.collect { settings ->
            userName = settings.userName
            apiKey = settings.llmApiKey
            femaleVoice = settings.femaleVoice
            onVoicePreference(settings.femaleVoice)
        }
    }
    Column(
        Modifier.fillMaxSize().background(Night).verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("‹  Retour", color = Cyan) }
            Spacer(Modifier.width(12.dp))
            Text("Réglages", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        }
        OutlinedTextField(userName, { userName = it; saved = false }, label = { Text("Prénom") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(
            apiKey,
            { apiKey = it; saved = false },
            label = { Text("Clé API OpenAI (optionnelle)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Voix féminine", color = Color.White, modifier = Modifier.weight(1f))
            Switch(checked = femaleVoice, onCheckedChange = { femaleVoice = it; onVoicePreference(it); saved = false })
        }
        Button(
            onClick = { scope.launch { repository.update(userName.trim(), apiKey.trim(), femaleVoice); saved = true } },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = Night)
        ) { Text(if (saved) "Enregistré" else "Enregistrer") }
        Text("La clé est conservée localement dans DataStore. Une clé OpenAI valide et une connexion sont nécessaires au chat cloud.", color = Muted, fontSize = 13.sp)
    }
}