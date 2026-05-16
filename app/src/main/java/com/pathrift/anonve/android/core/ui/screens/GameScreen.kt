package com.pathrift.anonve.android.core.ui.screens

import android.content.res.Configuration
import android.widget.FrameLayout
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlin.math.sqrt
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import com.pathrift.anonve.android.core.engine.EconomyConstants
import com.pathrift.anonve.android.core.ui.ArtilleryTowerColor
import com.pathrift.anonve.android.core.ui.BlastTowerColor
import com.pathrift.anonve.android.core.ui.BoltTowerColor
import com.pathrift.anonve.android.core.ui.CoreTowerColor
import com.pathrift.anonve.android.core.ui.FrostTowerColor
import com.pathrift.anonve.android.core.ui.GameEvent
import com.pathrift.anonve.android.core.ui.InfernoTowerColor
import com.pathrift.anonve.android.core.ui.NovaTowerColor
import com.pathrift.anonve.android.core.ui.PathriftGold
import com.pathrift.anonve.android.core.ui.PathriftTextDisabled
import com.pathrift.anonve.android.core.ui.PierceTowerColor
import com.pathrift.anonve.android.core.ui.SniperTowerColor
import com.pathrift.anonve.android.core.ui.TeslaTowerColor
import com.pathrift.anonve.android.core.ui.GameViewModel
import com.pathrift.anonve.android.core.ui.TowerShapeIcon
import com.pathrift.anonve.android.core.ui.LanguageManager
import com.pathrift.anonve.android.core.ui.PathriftBackground
import com.pathrift.anonve.android.core.ui.PathriftDanger
import com.pathrift.anonve.android.core.ui.PathriftNeonBlue
import com.pathrift.anonve.android.core.ui.PathriftOrange
import com.pathrift.anonve.android.core.ui.PathriftPurple
import com.pathrift.anonve.android.core.ui.PathriftSurface
import com.pathrift.anonve.android.core.ui.PathriftTextPrimary
import com.pathrift.anonve.android.core.ui.PathriftTextSecondary
import com.pathrift.anonve.android.game.GamePhase
import com.pathrift.anonve.android.game.GameRenderer
import com.pathrift.anonve.android.game.GameState
import com.pathrift.anonve.android.game.TowerInfo
import com.pathrift.anonve.android.game.WaveDefinition
import com.pathrift.anonve.android.game.enemies.EnemyType
import com.pathrift.anonve.android.game.towers.TowerType

// ==============================
// Game Screen Entry Point
// ==============================

@Composable
fun GameScreen(
    onRunEnded: (score: Long, wave: Int, kills: Int) -> Unit,
    navController: NavController? = null,
    gameViewModel: GameViewModel = viewModel()
) {
    val state by gameViewModel.state.collectAsState()
    var isPaused by remember { mutableStateOf(false) }
    var showPremiumDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        gameViewModel.events.collect { event ->
            when (event) {
                is GameEvent.RunEnded -> onRunEnded(event.score, event.wave, event.kills)
                is GameEvent.ShowMessage -> { /* silent — no snackbar per iOS parity */ }
                is GameEvent.WaveStarted -> {}
                is GameEvent.WaveCompleted -> {}
                is GameEvent.RiftShift -> {}
                is GameEvent.ShowPremiumPrompt -> showPremiumDialog = true
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PathriftBackground)
    ) {
        GameCanvasView(
            state = state,
            gameViewModel = gameViewModel,
            modifier = Modifier.fillMaxSize()
        )

        CombatHUD(
            state = state,
            nextWaveDefinition = gameViewModel.nextWaveDefinition,
            onNextWave = gameViewModel::startNextWave,
            onPause = { isPaused = true },
            onToggleSpeed = gameViewModel::toggleSpeed,
            onShowNextWaveInfo = { gameViewModel.showNextWaveInfo = true },
            modifier = Modifier.fillMaxSize()
        )

        state.selectedTowerInfo?.let { info ->
            TowerInfoBottomPanel(
                info = info,
                gold = state.gold,
                onUpgrade = { gameViewModel.upgradeSelectedTower() },
                onSell = { gameViewModel.sellSelectedTower() },
                onDismiss = { gameViewModel.clearTowerSelection() }
            )
        }

        // PATHRIFT-157: Next Wave Info Panel
        if (gameViewModel.showNextWaveInfo) {
            NextWaveInfoPanel(
                waveDef = gameViewModel.nextWaveDefinition,
                onDismiss = { gameViewModel.showNextWaveInfo = false }
            )
        }

        val emptySelectedSlot = state.selectedTowerSlotId
        if (emptySelectedSlot != null && state.selectedTowerInfo == null) {
            TowerSelectionPanel(
                state = state,
                slotId = emptySelectedSlot,
                viewModel = gameViewModel,
                onDismiss = { gameViewModel.clearTowerSelection() }
            )
        }

        // Rift Shift animated overlay — iOS parity: pulsing purple full-screen flash + banner
        if (state.riftShiftActive) {
            RiftShiftOverlay()
        }

        if (isPaused) {
            PauseOverlay(
                onResume = { isPaused = false },
                onQuit = {
                    gameViewModel.clearSaveOnQuit()
                    onRunEnded(state.score, state.wave, state.enemyKills)
                }
            )
        }

        // Revive overlay
        if (state.showRevivePrompt) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.75f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(32.dp)
                        .background(Color(0xFF1A1A2E), RoundedCornerShape(20.dp))
                        .padding(28.dp)
                ) {
                    Text("GAME OVER", color = Color(0xFFFF2244), fontSize = 22.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(12.dp))
                    Text("REVIVE? (${state.reviveCountdown}s)", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Text("You have 1 revive (Premium)", color = Color.Gray, fontSize = 12.sp)
                    Spacer(Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { gameViewModel.acceptRevive() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00AA44))
                        ) {
                            Text("REVIVE", fontWeight = FontWeight.Black)
                        }
                        OutlinedButton(
                            onClick = { gameViewModel.declineRevive() },
                            border = BorderStroke(1.dp, Color(0xFFFF2244))
                        ) {
                            Text("Give Up", color = Color(0xFFFF2244))
                        }
                    }
                }
            }
        }

        // Premium prompt dialog
        if (showPremiumDialog) {
            AlertDialog(
                onDismissRequest = { showPremiumDialog = false },
                title = { Text("Premium Feature", color = Color(0xFF00CCFF)) },
                text = { Text("x2 Speed requires Premium membership. Visit the Store to activate Premium.") },
                confirmButton = {
                    TextButton(onClick = {
                        showPremiumDialog = false
                        navController?.navigate("store")
                    }) {
                        Text("Get Premium")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPremiumDialog = false }) { Text("Not Now") }
                },
                containerColor = Color(0xFF1A1A2E)
            )
        }
    }
}

// ==============================
// Canvas View
// ==============================

@Composable
private fun GameCanvasView(
    state: GameState,
    gameViewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val gameSurface = remember { GameRenderer(context) }
    val enemies by gameViewModel.enemies.collectAsState()
    var sizeInitialized by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val systemBarsInsets = WindowInsets.systemBars

    // Wire renderer to ViewModel so projectile callbacks can inject directly
    LaunchedEffect(gameSurface) {
        gameViewModel.renderer = gameSurface
    }

    gameSurface.enemies = enemies
    gameSurface.towerInstances = gameViewModel.game.towers
    gameSurface.slotPositions = gameViewModel.game.grid.slots.map { it.position }
    gameSurface.slotOccupied = gameViewModel.game.grid.slots.associate { it.id to it.state.isOccupied }
    gameSurface.selectedSlotId = state.selectedTowerSlotId
    gameSurface.riftShiftActive = state.riftShiftActive

    Box(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                if (!sizeInitialized && coordinates.size.width > 0 && coordinates.size.height > 0) {
                    val w = coordinates.size.width.toFloat()
                    val h = coordinates.size.height.toFloat()
                    val topPx = with(density) {
                        systemBarsInsets.getTop(density).toFloat() + 90.dp.toPx()
                    }
                    val bottomPx = with(density) {
                        systemBarsInsets.getBottom(density).toFloat() + 80.dp.toPx()
                    }
                    gameViewModel.initLayout(w, h, topInset = topPx, bottomInset = bottomPx)
                    sizeInitialized = true
                }
            }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: continue
                        if (change.pressed && !change.previousPressed) {
                            val tapX = change.position.x
                            val tapY = change.position.y
                            val slots = gameViewModel.game.grid.slots
                            val tapRadiusPx = 48f * density.density  // 48dp → pixels
                            var closestId: Int? = null
                            var closestDist = Float.MAX_VALUE
                            for (slot in slots) {
                                val dx = slot.position.x - tapX
                                val dy = slot.position.y - tapY
                                val dist = sqrt(dx * dx + dy * dy)
                                if (dist < tapRadiusPx && dist < closestDist) {
                                    closestDist = dist
                                    closestId = slot.id
                                }
                            }
                            if (closestId != null) {
                                gameViewModel.tapTowerSlot(closestId)
                                change.consume()
                            }
                        }
                    }
                }
            }
    ) {
        AndroidView(
            factory = {
                FrameLayout(context).apply { addView(gameSurface) }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

// ==============================
// Combat HUD
// ==============================

@Composable
private fun CombatHUD(
    state: GameState,
    nextWaveDefinition: WaveDefinition,
    onNextWave: () -> Unit,
    onPause: () -> Unit,
    onToggleSpeed: () -> Unit = {},
    onShowNextWaveInfo: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopStart)
        ) {
            if (isLandscape) {
                // Three-section landscape top bar: [Wave+Info] [Lives+Gold+Diamond] [Speed+Pause]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.75f),
                                    Color.Black.copy(alpha = 0.45f)
                                )
                            )
                        )
                        .systemBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // LEFT: Wave number + Info button — Build 5.3.5
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.width(90.dp)
                    ) {
                        Text(
                            text = if (state.wave == 0) "W--" else "W${state.wave}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0f, 0.78f, 1f)
                        )
                        IconButton(
                            onClick = { onShowNextWaveInfo() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Info,
                                contentDescription = "Wave info",
                                tint = Color(0f, 0.78f, 1f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // CENTER (weight 1): Lives + Gold + Diamond + Kills
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Lives pill
                        StatPill {
                            for (i in 0 until EconomyConstants.STARTING_LIVES) {
                                Icon(
                                    imageVector = if (i < state.lives) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = null,
                                    tint = if (i < state.lives) PathriftDanger else PathriftTextSecondary.copy(alpha = 0.3f),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        // Gold pill
                        StatPill {
                            Text("💰", fontSize = 11.sp)
                            Text("${state.gold}", color = PathriftGold, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                        Spacer(Modifier.width(8.dp))
                        // Diamond pill
                        StatPill {
                            Text("♦", color = Color(0xFF00CCFF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("${state.diamonds}", color = Color(0xFF00CCFF), fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                        Spacer(Modifier.width(8.dp))
                        // Kills pill
                        StatPill {
                            Text("✕", color = PathriftOrange, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("${state.enemyKills}", color = PathriftOrange, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }

                    // RIGHT: Speed + Pause
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (state.speedMultiplier == 2f) Color(0xFF00CCFF).copy(alpha = 0.2f)
                                    else Color.White.copy(alpha = 0.07f)
                                )
                                .border(
                                    1.dp,
                                    if (state.speedMultiplier == 2f) Color(0xFF00CCFF).copy(alpha = 0.5f)
                                    else Color.White.copy(alpha = 0.1f),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { onToggleSpeed() }
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                Icon(Icons.Default.Speed, contentDescription = null,
                                    tint = if (state.speedMultiplier == 2f) Color(0xFF00CCFF) else Color.Gray,
                                    modifier = Modifier.size(10.dp))
                                Text(
                                    if (state.speedMultiplier == 1f) "×1" else "×2",
                                    color = if (state.speedMultiplier == 2f) Color(0xFF00CCFF) else Color.Gray,
                                    fontWeight = FontWeight.Bold, fontSize = 12.sp
                                )
                            }
                        }
                        IconButton(onClick = onPause, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Pause, contentDescription = null, tint = PathriftTextSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            } else {
                // Portrait HUD — always active (game is portrait-locked)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(PathriftBackground.copy(alpha = 0.9f), Color.Transparent)
                            )
                        )
                        .systemBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HudStatPill(LanguageManager.s("GOLD", "ALTIN"), "${state.gold}", PathriftNeonBlue)
                    Spacer(Modifier.width(14.dp))
                    HudStatPill(LanguageManager.s("DIAMONDS", "ELMAS"), "♦${state.diamonds}", Color(0xFF00CCFF))
                    Spacer(Modifier.weight(1f))
                    // Wave card IS the info button (Build 5.3.4)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(9.dp))
                            .background(Color(0f, 0.78f, 1f, 0.15f))
                            .border(1.dp, Color(0f, 0.78f, 1f, 0.6f), RoundedCornerShape(9.dp))
                            .clickable { onShowNextWaveInfo() }
                            .padding(horizontal = 10.dp, vertical = 3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(0.dp)) {
                            Text(
                                text = if (state.wave == 0) "READY" else "WAVE",
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 2.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = if (state.wave == 0) "--" else "${state.wave}",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0f, 0.78f, 1f),
                                fontFamily = FontFamily.Monospace
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(Icons.Filled.Info, null, tint = Color(0f, 0.78f, 1f, 0.7f), modifier = Modifier.size(9.dp))
                                Text("INFO", fontSize = 9.sp, fontFamily = FontFamily.Monospace, letterSpacing = 0.5.sp, color = Color(0f, 0.78f, 1f, 0.7f))
                            }
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Compact lives stat — Build 5.3.2
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            Icon(
                                imageVector = if (state.lives > 0) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = null,
                                tint = if (state.lives > 0) PathriftDanger else Color.Gray,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "${state.lives}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = if (state.lives > 0) PathriftDanger else Color.Gray
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        // Speed toggle button
                        TextButton(
                            onClick = onToggleSpeed,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.Black.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = if (state.speedMultiplier == 1.0f) "x1" else "x2",
                                color = if (state.speedMultiplier == 2.0f) Color(0xFF00CCFF) else Color.Gray,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                        IconButton(onClick = onPause, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Pause, "pause", tint = PathriftTextSecondary.copy(alpha = 0.8f), modifier = Modifier.size(22.dp))
                        }
                    }
                }
            } // end portrait else
            state.waveCompleteMessage?.let { msg ->
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .background(PathriftNeonBlue.copy(alpha = 0.15f))
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(msg, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PathriftNeonBlue, fontFamily = FontFamily.Monospace)
                }
            }
        }

        if (isLandscape) {
            // Landscape bottom bar: progress/send wave right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .background(
                        Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)))
                    )
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(Modifier.weight(1f))
                when {
                    state.phase == GamePhase.WAVE_ACTIVE -> WaveProgressIndicator(state.waveEnemiesCleared, state.waveEnemyTotal)
                    state.phase != GamePhase.GAME_OVER -> SendWaveButton(onClick = onNextWave)
                }
            }
        } else {
            // Portrait bottom bar — Build 5.3.1: NextWaveBanner removed, START WAVE button shown directly
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, PathriftBackground.copy(alpha = 0.9f))))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HudStatPill(LanguageManager.s("KILLS", "ÖLDÜRME"), "${state.enemyKills}", PathriftOrange)
                    Spacer(Modifier.weight(1f))
                    when {
                        state.phase == GamePhase.WAVE_ACTIVE -> WaveProgressIndicator(state.waveEnemiesCleared, state.waveEnemyTotal)
                        state.phase != GamePhase.GAME_OVER -> SendWaveButton(onClick = onNextWave)
                    }
                }
            }
        }
    }
}

@Composable
private fun HudStatPill(label: String, value: String, valueColor: Color) {
    Column {
        Text(value, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = valueColor, fontFamily = FontFamily.Monospace)
        Text(label, fontSize = 8.sp, fontWeight = FontWeight.SemiBold, color = PathriftTextSecondary, letterSpacing = 1.5.sp, fontFamily = FontFamily.Monospace)
    }
}

/** Subtle pill wrapper for HUD stat clusters in the landscape top bar. */
@Composable
private fun StatPill(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .background(Color.White.copy(alpha = 0.07f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        content = content
    )
}

/** 4dp wave progress strip below the landscape top bar. */
@Composable
private fun WaveProgressStrip(progress: Double) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(4.dp)
            .padding(horizontal = 8.dp)
    ) {
        // Track
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = 0.08f))
        )
        // Fill
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction = progress.toFloat().coerceIn(0f, 1f))
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF00CCFF), Color(0xFF9966FF))
                    )
                )
        )
    }
}

@Composable
private fun WaveProgressIndicator(cleared: Int, total: Int) {
    Row(
        modifier = Modifier
            .background(Color.White.copy(alpha = 0.07f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "$cleared/$total",
            color = PathriftTextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        LinearProgressIndicator(
            progress = { if (total > 0) cleared.toFloat() / total else 0f },
            modifier = Modifier.width(80.dp).height(5.dp).clip(RoundedCornerShape(3.dp)),
            color = Color(0xFF00CCFF),
            trackColor = Color.White.copy(alpha = 0.1f)
        )
    }
}

@Composable
private fun SendWaveButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
        modifier = Modifier.height(36.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        elevation = ButtonDefaults.buttonElevation(0.dp)
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(listOf(Color(0xFF00CCFF), Color(0xFF9966FF))),
                    RoundedCornerShape(18.dp)
                )
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.FastForward, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                Text(
                    LanguageManager.s("NEXT WAVE", "SONRAKI DALGA"),
                    color = Color.Black,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

// ==============================
// Rift Shift Overlay — iOS parity: pulsing purple flash + "RIFT SHIFT!" banner
// ==============================

@Composable
private fun RiftShiftOverlay() {
    val infiniteTransition = rememberInfiniteTransition(label = "riftPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.42f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "riftAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF8B4FFF).copy(alpha = pulseAlpha)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .background(Color(0xFF8B4FFF).copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 24.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "⚡ RIFT SHIFT ⚡",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp
                )
            }
        }
    }
}

// ==============================
// Pause Overlay
// ==============================

@Composable
private fun PauseOverlay(onResume: () -> Unit, onQuit: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.background(PathriftSurface, RoundedCornerShape(20.dp)).padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(LanguageManager.s("PAUSED", "DURAKLATILDI"), fontSize = 22.sp, fontWeight = FontWeight.Black, color = PathriftTextPrimary, letterSpacing = 3.sp, fontFamily = FontFamily.Monospace)
            Spacer(Modifier.height(24.dp))
            Button(onClick = onResume, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = PathriftNeonBlue), shape = RoundedCornerShape(12.dp)) {
                Text(LanguageManager.s("RESUME", "DEVAM ET"), fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onQuit, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp)) {
                Text(LanguageManager.s("QUIT RUN", "OYUNU BIRAK"), fontSize = 14.sp, color = PathriftTextSecondary)
            }
        }
    }
}

// ==============================
// Tower Info Panel
// ==============================

@Composable
private fun TowerInfoBottomPanel(
    info: TowerInfo,
    gold: Int,
    onUpgrade: () -> Unit,
    onSell: () -> Unit,
    onDismiss: () -> Unit
) {
    val canAffordUpgrade = gold >= info.upgradeCost
    val towerColor = towerDisplayColor(info.type)

    Box(
        modifier = Modifier.fillMaxSize()
            .background(Color.Transparent)
            .clickable(onClick = onDismiss)
            .navigationBarsPadding(),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Single-row compact card — 60dp fixed height (no nav bar padding inside row)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .background(PathriftBackground.copy(alpha = 0.94f))
                .clickable(enabled = false) {},
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tower color accent bar
            Box(
                modifier = Modifier
                    .width(4.dp).fillMaxHeight()
                    .padding(vertical = 10.dp)
                    .background(towerColor, RoundedCornerShape(2.dp))
            )
            Spacer(Modifier.width(12.dp))

            // Identity: name + level
            Column(modifier = Modifier.width(90.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(info.type.displayName.uppercase(), fontSize = 11.sp,
                        fontWeight = FontWeight.Black, color = PathriftTextPrimary)
                    Spacer(Modifier.width(4.dp))
                    Box(modifier = Modifier
                        .background(PathriftNeonBlue.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text("Lv.${info.level}", fontSize = 8.sp, fontWeight = FontWeight.Bold,
                            color = PathriftNeonBlue, fontFamily = FontFamily.Monospace)
                    }
                }
                info.type.typeAdvantageHint?.let {
                    Text("⚡ advantage", fontSize = 7.sp, color = PathriftGold, fontFamily = FontFamily.Monospace)
                }
            }

            // Stats block
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
                    .background(Color.Black.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MiniStatItem("DMG", String.format("%.0f", info.damage), PathriftOrange, Modifier.weight(1f))
                Box(Modifier.size(1.dp, 22.dp).background(PathriftTextSecondary.copy(alpha = 0.2f)))
                MiniStatItem("RNG", "${info.range.toInt()}t", PathriftNeonBlue, Modifier.weight(1f))
                Box(Modifier.size(1.dp, 22.dp).background(PathriftTextSecondary.copy(alpha = 0.2f)))
                MiniStatItem("SPD", String.format("%.1f/s", info.attackSpeed), PathriftPurple, Modifier.weight(1f))
            }

            // Buttons
            Row(
                modifier = Modifier.padding(end = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onUpgrade, enabled = canAffordUpgrade,
                    modifier = Modifier.width(72.dp).height(38.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (canAffordUpgrade) PathriftNeonBlue else Color.White.copy(alpha = 0.06f),
                        disabledContainerColor = Color.White.copy(alpha = 0.06f)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("UPGRADE", fontSize = 9.sp, fontWeight = FontWeight.Bold,
                            color = if (canAffordUpgrade) PathriftBackground else PathriftTextSecondary)
                        Text("${info.upgradeCost}g", fontSize = 8.sp,
                            color = if (canAffordUpgrade) PathriftBackground.copy(0.7f) else PathriftTextSecondary.copy(0.5f))
                    }
                }
                Box(
                    modifier = Modifier.width(56.dp).height(38.dp)
                        .background(PathriftDanger.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                        .border(1.dp, PathriftDanger.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                        .clickable(onClick = onSell),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("SELL", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = PathriftDanger)
                        Text("+${info.sellValue}g", fontSize = 8.sp, color = PathriftDanger.copy(0.8f))
                    }
                }
                Box(
                    modifier = Modifier.size(28.dp)
                        .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(8.dp))
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✕", fontSize = 11.sp, color = PathriftTextSecondary)
                }
            }
        }
    }
}

@Composable
private fun MiniStatItem(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PathriftTextPrimary, fontFamily = FontFamily.Monospace)
        Text(label, fontSize = 7.sp, fontWeight = FontWeight.SemiBold, color = color, letterSpacing = 0.8.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun TowerStatItem(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PathriftTextPrimary, fontFamily = FontFamily.Monospace)
        Text(label, fontSize = 8.sp, fontWeight = FontWeight.SemiBold, color = PathriftTextSecondary, letterSpacing = 1.sp, fontFamily = FontFamily.Monospace)
    }
}

// ==============================
// Tower Selection Panel
// ==============================

@Composable
private fun TowerSelectionPanel(
    state: GameState,
    slotId: Int,
    viewModel: GameViewModel,
    onDismiss: () -> Unit
) {
    var selectedType by remember { mutableStateOf<TowerType?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f))
            .clickable(onClick = onDismiss)
            .navigationBarsPadding(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(PathriftSurface, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .clickable(enabled = false, onClick = {})
                .padding(bottom = 8.dp)
        ) {
            // Drag handle
            Box(
                modifier = Modifier
                    .padding(top = 8.dp, bottom = 4.dp)
                    .size(36.dp, 4.dp)
                    .align(Alignment.CenterHorizontally)
                    .background(PathriftTextSecondary.copy(alpha = 0.4f), RoundedCornerShape(2.dp))
            )

            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = LanguageManager.s("PLACE TOWER", "KULE YERLEŞTIR"),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = PathriftTextPrimary,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("💰", fontSize = 10.sp)
                        Text("${state.gold}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PathriftGold, fontFamily = FontFamily.Monospace)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("♦", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00CCFF))
                        Text("${state.diamonds}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00CCFF), fontFamily = FontFamily.Monospace)
                    }
                }
            }

            val sortedTowers = remember {
                TowerType.values().sortedWith(
                    compareByDescending<TowerType> { viewModel.isTowerUnlocked(it) }
                        .thenBy { it.diamondCost }
                )
            }

            // Tower scroll row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(sortedTowers) { type ->
                    val isUnlocked = viewModel.isTowerUnlocked(type)
                    val canAffordGold = state.gold >= towerGoldCost(type)
                    val canAffordDiamonds = state.diamonds >= type.diamondCost || type.diamondCost == 0
                    CompactTowerPickCard(
                        type = type,
                        isSelected = selectedType == type,
                        isUnlocked = isUnlocked,
                        canAffordGold = canAffordGold,
                        canAffordDiamonds = canAffordDiamonds,
                        diamonds = state.diamonds,
                        onTap = { selectedType = if (selectedType == type) null else type }
                    )
                }
            }

            // Detail panel — shows when a tower is selected (iOS parity)
            val sel = selectedType
            if (sel != null) {
                val isUnlocked = viewModel.isTowerUnlocked(sel)
                val canAffordGold = state.gold >= towerGoldCost(sel)
                val canAffordDiamonds = state.diamonds >= sel.diamondCost || sel.diamondCost == 0
                val towerColor = towerDisplayColor(sel)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = sel.displayName.uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = towerColor,
                        fontFamily = FontFamily.Monospace
                    )
                    sel.typeAdvantageHint?.let { hint ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text("⚡", fontSize = 10.sp)
                            Text(hint, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = PathriftGold, fontFamily = FontFamily.Monospace)
                        }
                    }
                }

                // BUILD / UNLOCK button
                if (!isUnlocked) {
                    val hasDiamonds = canAffordDiamonds
                    Button(
                        onClick = { viewModel.unlockTower(sel) },
                        enabled = hasDiamonds,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (hasDiamonds) Color(0xFF00CCFF) else PathriftSurface,
                            disabledContainerColor = PathriftSurface
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (hasDiamonds) "UNLOCK ${sel.displayName.uppercase()} — ${sel.diamondCost}♦"
                                   else "NEED ${sel.diamondCost}♦",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (hasDiamonds) PathriftBackground else PathriftTextSecondary,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                } else {
                    Button(
                        onClick = {
                            if (canAffordGold) {
                                viewModel.placeTower(slotId, sel)
                                onDismiss()
                            }
                        },
                        enabled = canAffordGold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (canAffordGold) PathriftNeonBlue else PathriftSurface,
                            disabledContainerColor = PathriftSurface
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (canAffordGold) "BUILD ${sel.displayName.uppercase()} — ${towerGoldCost(sel)}g"
                                   else "NOT ENOUGH GOLD",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (canAffordGold) PathriftBackground else PathriftTextSecondary,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
            } else {
                // No tower selected yet
                Text(
                    text = LanguageManager.s("Select a tower type above", "Yukarıdan kule seçin"),
                    fontSize = 11.sp,
                    color = PathriftTextSecondary,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun CompactTowerPickCard(
    type: TowerType,
    isSelected: Boolean,
    isUnlocked: Boolean,
    canAffordGold: Boolean,
    canAffordDiamonds: Boolean,
    diamonds: Int,
    onTap: () -> Unit
) {
    val towerColor = towerDisplayColor(type)
    val isAffordable = isUnlocked && canAffordGold
    val alpha = if (isAffordable) 1f else 0.45f

    Box(
        modifier = Modifier
            .width(72.dp)
            .background(
                if (isSelected) towerColor.copy(alpha = 0.12f) else PathriftSurface.copy(alpha = alpha),
                RoundedCornerShape(10.dp)
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) towerColor else if (isAffordable) towerColor.copy(alpha = 0.5f) else PathriftTextDisabled,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onTap)
            .padding(vertical = 6.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        towerColor.copy(alpha = if (isSelected) 0.2f else if (isAffordable) 0.1f else 0.05f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (!isUnlocked) {
                    Text("🔒", fontSize = androidx.compose.ui.unit.TextUnit(14f, androidx.compose.ui.unit.TextUnitType.Sp))
                } else {
                    TowerShapeIcon(
                        type = type,
                        color = if (isSelected) towerColor else towerColor.copy(alpha = alpha),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            Spacer(Modifier.height(3.dp))
            Text(
                text = type.displayName.uppercase().take(6),
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) PathriftTextPrimary else if (isAffordable) PathriftTextPrimary else PathriftTextSecondary.copy(alpha = alpha),
                fontFamily = FontFamily.Monospace
            )
            Spacer(Modifier.height(2.dp))
            if (!isUnlocked) {
                Text(text = "${type.diamondCost}♦", fontSize = 8.sp, fontWeight = FontWeight.Bold,
                    color = if (canAffordDiamonds) Color(0xFF00CCFF) else PathriftDanger, fontFamily = FontFamily.Monospace)
            } else {
                val goldCost = towerGoldCost(type)
                Text(
                    text = "${goldCost}g",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (canAffordGold) PathriftGold else PathriftDanger,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun TowerPickCard(
    type: TowerType,
    isUnlocked: Boolean,
    canAffordGold: Boolean,
    canAffordDiamonds: Boolean,
    diamonds: Int,
    onTap: () -> Unit
) {
    val towerColor = towerDisplayColor(type)
    val isAffordable = isUnlocked && canAffordGold
    val alpha = if (isAffordable) 1f else 0.45f

    Box(
        modifier = Modifier
            .width(76.dp)
            .background(
                PathriftSurface.copy(alpha = alpha),
                RoundedCornerShape(12.dp)
            )
            .border(
                width = 1.5.dp,
                color = if (isAffordable) towerColor.copy(alpha = 0.7f) else PathriftTextDisabled,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onTap)
            .padding(vertical = 10.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Color dot
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .background(towerColor.copy(alpha = alpha), CircleShape)
            )
            Spacer(Modifier.height(5.dp))
            Text(
                text = type.name,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = if (isAffordable) PathriftTextPrimary else PathriftTextSecondary.copy(alpha = alpha),
                letterSpacing = 0.5.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(Modifier.height(3.dp))
            // Cost / lock badge
            if (!isUnlocked) {
                // Premium lock — show diamond cost
                Box(
                    modifier = Modifier
                        .background(
                            if (canAffordDiamonds) Color(0xFF004466) else Color(0xFF330000),
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "🔒 ${type.diamondCost}♦",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (canAffordDiamonds) Color(0xFF00CCFF) else PathriftDanger,
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else {
                // Gold cost
                val goldCost = towerGoldCost(type)
                Text(
                    text = "${goldCost}g",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (canAffordGold) PathriftGold else PathriftDanger,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

// Enemy type indicator colors — DESIGN_SPEC_BUILD5_3 Section 3.5 + PATHRIFT-157
private val EnemyType.indicatorColor: Color get() = when(this) {
    EnemyType.RUNNER   -> Color(0.20f, 0.65f, 1.00f)   // electric blue
    EnemyType.TANK     -> Color(0.55f, 0.55f, 0.60f)   // gray
    EnemyType.BOSS     -> Color(1.00f, 0.18f, 0.08f)   // red
    EnemyType.SHIELD   -> Color(0.56f, 0.18f, 1.00f)   // purple
    EnemyType.SWARM    -> Color(1.00f, 0.82f, 0.10f)   // gold
    EnemyType.GHOST    -> Color(0.85f, 1.00f, 1.00f, 0.70f)  // cyan-white, semi-transparent
    EnemyType.SPLITTER -> Color(1.00f, 0.70f, 0.0f)
    EnemyType.JUMPER   -> Color(0.0f, 0.8f, 0.6f)
    EnemyType.HEALER   -> Color(0.2f, 1.0f, 0.4f)
    EnemyType.PHANTOM  -> Color(0.7f, 0.3f, 1.0f)
}

// Build 5.3: NextWaveBanner — auto info strip above START WAVE button (DESIGN_SPEC_BUILD5_3 Section 3)
@Composable
fun NextWaveBanner(waveDef: WaveDefinition, onTap: () -> Unit) {
    Button(
        onClick = onTap,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, Color(0f, 0.78f, 1f, 0.25f)),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                "NEXT ▸",
                fontSize = 9.sp,
                color = Color.Gray,
                fontFamily = FontFamily.Monospace
            )
            // Show up to 4 enemy type chips
            waveDef.spawnGroups.take(4).forEach { group ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Box(
                        Modifier
                            .size(7.dp)
                            .background(group.type.indicatorColor, CircleShape)
                    )
                    Text(
                        "${group.type.name.take(3)}×${group.count}",
                        fontSize = 9.sp,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            if (waveDef.spawnGroups.size > 4) {
                Text(
                    "+${waveDef.spawnGroups.size - 4}",
                    fontSize = 9.sp,
                    color = Color.Gray,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(Modifier.weight(1f))
            Icon(
                Icons.Filled.Info,
                null,
                tint = Color(0f, 0.78f, 1f, 0.7f),
                modifier = Modifier.size(11.dp)
            )
        }
    }
}

// PATHRIFT-157: Next Wave Info Panel
@Composable
fun NextWaveInfoPanel(
    waveDef: WaveDefinition,
    onDismiss: () -> Unit
) {
    val isBoss = waveDef.spawnGroups.any { it.type == EnemyType.BOSS }
    Box(
        modifier = androidx.compose.ui.Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = androidx.compose.ui.Modifier
                .background(Color(0xFF12121A), RoundedCornerShape(16.dp))
                .padding(20.dp)
                .clickable(enabled = false, onClick = {})
                .width(220.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "WAVE ${waveDef.waveNumber}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF00C8FF),
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp
            )
            Spacer(androidx.compose.ui.Modifier.height(4.dp))
            if (isBoss) {
                Text(
                    text = "⚠ BOSS WAVE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFF2E14),
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(androidx.compose.ui.Modifier.height(10.dp))
            for (group in waveDef.spawnGroups) {
                Row(
                    modifier = androidx.compose.ui.Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = androidx.compose.ui.Modifier
                            .size(10.dp)
                            .background(group.type.indicatorColor, CircleShape)
                    )
                    Text(
                        text = group.type.name.lowercase().replaceFirstChar { it.uppercase() },
                        fontSize = 11.sp,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        modifier = androidx.compose.ui.Modifier.weight(1f)
                    )
                    Text(
                        text = "×${group.count}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = group.type.indicatorColor,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            Spacer(androidx.compose.ui.Modifier.height(12.dp))
            Text(
                text = "tap to close",
                fontSize = 9.sp,
                color = Color.Gray,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

private fun towerDisplayColor(type: TowerType): Color = when (type) {
    TowerType.BOLT      -> BoltTowerColor
    TowerType.BLAST     -> BlastTowerColor
    TowerType.FROST     -> FrostTowerColor
    TowerType.PIERCE    -> PierceTowerColor
    TowerType.CORE      -> CoreTowerColor
    TowerType.INFERNO   -> InfernoTowerColor
    TowerType.TESLA     -> TeslaTowerColor
    TowerType.NOVA      -> NovaTowerColor
    TowerType.SNIPER    -> SniperTowerColor
    TowerType.ARTILLERY -> ArtilleryTowerColor
}

private fun towerGoldCost(type: TowerType): Int = when (type) {
    TowerType.BOLT      -> 50
    TowerType.BLAST     -> 70
    TowerType.FROST     -> 60
    TowerType.PIERCE    -> 130
    TowerType.CORE      -> 180
    TowerType.INFERNO   -> 200
    TowerType.TESLA     -> 300
    TowerType.NOVA      -> 500
    TowerType.SNIPER    -> 220
    TowerType.ARTILLERY -> 160
}
