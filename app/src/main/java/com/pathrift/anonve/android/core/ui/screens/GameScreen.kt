package com.pathrift.anonve.android.core.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlin.math.sqrt
import androidx.compose.ui.text.style.TextOverflow
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
import com.pathrift.anonve.android.core.ui.TeslaTowerColor
import com.pathrift.anonve.android.core.ui.GameViewModel
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
import com.pathrift.anonve.android.game.towers.TowerType

// ==============================
// Game Screen Entry Point
// ==============================

@Composable
fun GameScreen(
    onRunEnded: (score: Long, wave: Int) -> Unit,
    navController: NavController? = null,
    gameViewModel: GameViewModel = viewModel()
) {
    val state by gameViewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var isPaused by remember { mutableStateOf(false) }
    var showPremiumDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        gameViewModel.events.collect { event ->
            when (event) {
                is GameEvent.RunEnded -> onRunEnded(event.score, event.wave)
                is GameEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
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
            onNextWave = gameViewModel::startNextWave,
            onPause = { isPaused = true },
            onToggleSpeed = gameViewModel::toggleSpeed,
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

        val emptySelectedSlot = state.selectedTowerSlotId
        if (emptySelectedSlot != null && state.selectedTowerInfo == null) {
            TowerSelectionPanel(
                state = state,
                slotId = emptySelectedSlot,
                viewModel = gameViewModel,
                onDismiss = { gameViewModel.clearTowerSelection() }
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        if (isPaused) {
            PauseOverlay(
                onResume = { isPaused = false },
                onQuit = { onRunEnded(state.score, state.wave) }
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

    gameSurface.enemies = enemies
    gameSurface.towerInstances = gameViewModel.game.towers
    gameSurface.slotPositions = gameViewModel.game.grid.slots.map { it.position }
    gameSurface.slotOccupied = gameViewModel.game.grid.slots.associate { it.id to it.state.isOccupied }
    gameSurface.selectedSlotId = state.selectedTowerSlotId
    gameSurface.riftShiftActive = state.riftShiftActive

    Box(
        modifier = modifier.pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull() ?: continue
                    if (change.pressed && !change.previousPressed) {
                        val tapX = change.position.x
                        val tapY = change.position.y
                        val slots = gameViewModel.game.grid.slots
                        val tapRadiusPx = 60f
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
    onNextWave: () -> Unit,
    onPause: () -> Unit,
    onToggleSpeed: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopStart)
        ) {
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
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (state.wave == 0) LanguageManager.s("READY", "HAZIR") else LanguageManager.s("WAVE", "DALGA"),
                        fontSize = 9.sp, fontWeight = FontWeight.Bold,
                        color = PathriftTextSecondary, letterSpacing = 2.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = if (state.wave == 0) "--" else "${state.wave}",
                        fontSize = 24.sp, fontWeight = FontWeight.Black,
                        color = PathriftNeonBlue, fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row {
                        for (i in 0 until 3) {
                            Icon(
                                imageVector = if (i < state.lives) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = null,
                                tint = if (i < state.lives) PathriftDanger else PathriftTextSecondary.copy(alpha = 0.3f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .background(Brush.verticalGradient(listOf(Color.Transparent, PathriftBackground.copy(alpha = 0.9f))))
                .padding(horizontal = 20.dp, vertical = 14.dp),
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

@Composable
private fun HudStatPill(label: String, value: String, valueColor: Color) {
    Column {
        Text(value, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = valueColor, fontFamily = FontFamily.Monospace)
        Text(label, fontSize = 8.sp, fontWeight = FontWeight.SemiBold, color = PathriftTextSecondary, letterSpacing = 1.5.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun WaveProgressIndicator(cleared: Int, total: Int) {
    val progress = cleared.toFloat() / maxOf(1, total).toFloat()
    Column(horizontalAlignment = Alignment.End) {
        Text(
            text = "$cleared / $total ${LanguageManager.s("CLEARED", "TEMİZLENDİ")}",
            fontSize = 11.sp, color = PathriftNeonBlue, fontFamily = FontFamily.Monospace
        )
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.width(140.dp).height(7.dp).clip(RoundedCornerShape(4.dp)),
            color = PathriftNeonBlue, trackColor = PathriftSurface
        )
    }
}

@Composable
private fun SendWaveButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = PathriftNeonBlue),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.height(48.dp)
    ) {
        Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(11.dp))
        Spacer(Modifier.width(8.dp))
        Text(LanguageManager.s("SEND WAVE", "DALGA GÖNDER"), fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
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
    val towerColor = when (info.type) {
        TowerType.BOLT    -> BoltTowerColor
        TowerType.BLAST   -> BlastTowerColor
        TowerType.FROST   -> FrostTowerColor
        TowerType.PIERCE  -> PierceTowerColor
        TowerType.CORE    -> CoreTowerColor
        TowerType.INFERNO -> InfernoTowerColor
        TowerType.TESLA   -> TeslaTowerColor
        TowerType.NOVA    -> NovaTowerColor
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().background(PathriftSurface, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.padding(top = 10.dp, bottom = 6.dp).size(36.dp, 4.dp).background(PathriftTextSecondary.copy(alpha = 0.4f), RoundedCornerShape(2.dp)))

            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp, top = 8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(12.dp).background(towerColor, CircleShape))
                    Spacer(Modifier.width(8.dp))
                    Text(info.type.name, fontSize = 15.sp, fontWeight = FontWeight.Black, color = PathriftTextPrimary, letterSpacing = 1.sp)
                    Spacer(Modifier.width(8.dp))
                    Box(modifier = Modifier.background(PathriftNeonBlue.copy(alpha = 0.15f), RoundedCornerShape(6.dp)).padding(horizontal = 7.dp, vertical = 3.dp)) {
                        Text("Lv.${info.level}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PathriftNeonBlue, fontFamily = FontFamily.Monospace)
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Text("✕", fontSize = 16.sp, color = PathriftTextSecondary)
                    }
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(10.dp)).padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TowerStatItem("DMG", String.format("%.0f", info.damage), PathriftOrange, Modifier.weight(1f))
                    Box(modifier = Modifier.size(1.dp, 32.dp).background(PathriftTextSecondary.copy(alpha = 0.2f)))
                    TowerStatItem("RNG", "${info.range.toInt()}t", PathriftNeonBlue, Modifier.weight(1f))
                    Box(modifier = Modifier.size(1.dp, 32.dp).background(PathriftTextSecondary.copy(alpha = 0.2f)))
                    TowerStatItem("SPD", String.format("%.1f/s", info.attackSpeed), PathriftPurple, Modifier.weight(1f))
                }

                Spacer(Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = onUpgrade, enabled = canAffordUpgrade,
                        modifier = Modifier.weight(1f).height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (canAffordUpgrade) PathriftNeonBlue else PathriftSurface,
                            disabledContainerColor = PathriftSurface
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(LanguageManager.s("UPGRADE", "GELİŞTİR"), fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                color = if (canAffordUpgrade) PathriftTextPrimary else PathriftTextSecondary)
                            Text("${info.upgradeCost}g", fontSize = 11.sp,
                                color = if (canAffordUpgrade) PathriftTextPrimary.copy(0.7f) else PathriftTextSecondary.copy(0.5f))
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Button(
                        onClick = onSell,
                        modifier = Modifier.width(100.dp).height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PathriftSurface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(LanguageManager.s("SELL", "SAT"), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PathriftOrange)
                            Text("+${info.sellValue}g", fontSize = 11.sp, color = PathriftOrange.copy(0.8f))
                        }
                    }
                }
            }
        }
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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(PathriftSurface, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .clickable(enabled = false, onClick = {}) // consume clicks inside panel
        ) {
            Box(modifier = Modifier.padding(top = 10.dp, bottom = 4.dp).size(36.dp, 4.dp).align(Alignment.CenterHorizontally).background(PathriftTextSecondary.copy(alpha = 0.4f), RoundedCornerShape(2.dp)))

            Text(
                text = LanguageManager.s("SELECT TOWER", "KULE SEÇ"),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = PathriftTextSecondary,
                letterSpacing = 2.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(start = 20.dp, bottom = 8.dp, top = 4.dp)
            )

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 28.dp)
            ) {
                items(TowerType.values().toList()) { type ->
                    val isUnlocked = viewModel.isTowerUnlocked(type)
                    val canAffordGold = state.gold >= towerGoldCost(type)
                    val canAffordDiamonds = state.diamonds >= type.diamondCost || type.diamondCost == 0
                    TowerPickCard(
                        type = type,
                        isUnlocked = isUnlocked,
                        canAffordGold = canAffordGold,
                        canAffordDiamonds = canAffordDiamonds,
                        diamonds = state.diamonds,
                        onTap = {
                            if (isUnlocked) {
                                viewModel.placeTower(slotId, type)
                                onDismiss()
                            } else {
                                viewModel.unlockTower(type)
                            }
                        }
                    )
                    Spacer(Modifier.width(8.dp))
                }
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

private fun towerDisplayColor(type: TowerType): Color = when (type) {
    TowerType.BOLT    -> BoltTowerColor
    TowerType.BLAST   -> BlastTowerColor
    TowerType.FROST   -> FrostTowerColor
    TowerType.PIERCE  -> PierceTowerColor
    TowerType.CORE    -> CoreTowerColor
    TowerType.INFERNO -> InfernoTowerColor
    TowerType.TESLA   -> TeslaTowerColor
    TowerType.NOVA    -> NovaTowerColor
}

private fun towerGoldCost(type: TowerType): Int = when (type) {
    TowerType.BOLT    -> 50
    TowerType.BLAST   -> 70
    TowerType.FROST   -> 60
    TowerType.PIERCE  -> 130
    TowerType.CORE    -> 180
    TowerType.INFERNO -> 200
    TowerType.TESLA   -> 300
    TowerType.NOVA    -> 500
}
