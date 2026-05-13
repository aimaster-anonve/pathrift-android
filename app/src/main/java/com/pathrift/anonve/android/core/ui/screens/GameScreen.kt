package com.pathrift.anonve.android.core.ui.screens

import android.widget.FrameLayout
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pathrift.anonve.android.core.ui.GameEvent
import com.pathrift.anonve.android.core.ui.GamePhase
import com.pathrift.anonve.android.core.ui.GameViewModel
import com.pathrift.anonve.android.core.ui.PathriftBackground
import com.pathrift.anonve.android.core.ui.PathriftDanger
import com.pathrift.anonve.android.core.ui.PathriftGold
import com.pathrift.anonve.android.core.ui.PathriftNeonBlue
import com.pathrift.anonve.android.core.ui.PathriftSurface
import com.pathrift.anonve.android.core.ui.PathriftSurfaceVariant
import com.pathrift.anonve.android.core.ui.PathriftTextPrimary
import com.pathrift.anonve.android.core.ui.PathriftTextSecondary
import com.pathrift.anonve.android.core.ui.UiGameState
import com.pathrift.anonve.android.core.ui.BlastTowerColor
import com.pathrift.anonve.android.core.ui.BoltTowerColor
import com.pathrift.anonve.android.core.ui.FrostTowerColor
import com.pathrift.anonve.android.game.GameRenderer
import com.pathrift.anonve.android.game.GridSystem
import com.pathrift.anonve.android.game.TileCoordinate
import com.pathrift.anonve.android.game.towers.BlastTower
import com.pathrift.anonve.android.game.towers.BoltTower
import com.pathrift.anonve.android.game.towers.FrostTower
import com.pathrift.anonve.android.game.towers.Tower
import com.pathrift.anonve.android.game.towers.TowerType

// ==============================
// Game Screen Entry Point
// ==============================

@Composable
fun GameScreen(
    onRunEnded: (score: Long, wave: Int) -> Unit,
    gameViewModel: GameViewModel = viewModel()
) {
    val state by gameViewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        gameViewModel.events.collect { event ->
            when (event) {
                is GameEvent.RunEnded -> onRunEnded(event.score, event.wave)
                is GameEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
                is GameEvent.WaveStarted -> { /* visual feedback handled by HUD state */ }
                is GameEvent.WaveCompleted -> { /* handled by ShowMessage */ }
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
            onTileTapped = { coord -> gameViewModel.selectTile(coord) },
            modifier = Modifier.fillMaxSize()
        )

        CombatHUD(
            state = state,
            onTowerSelect = gameViewModel::selectTowerType,
            onNextWave = gameViewModel::startNextWave,
            onClearSelection = gameViewModel::clearTowerSelection,
            modifier = Modifier.fillMaxSize()
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

// ==============================
// Canvas View
// ==============================

@Composable
private fun GameCanvasView(
    state: UiGameState,
    onTileTapped: (TileCoordinate) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val gameSurface = remember { GameRenderer(context) }

    gameSurface.gridTiles = state.gridTiles
    gameSurface.towers = state.towers
    gameSurface.enemies = state.enemies
    gameSurface.selectedTile = state.selectedTile

    Box(
        modifier = modifier.pointerInput(Unit) {
            detectTapGestures { offset ->
                val tileW = size.width.toFloat() / GridSystem.COLS
                val tileH = size.height.toFloat() / GridSystem.ROWS
                val col = (offset.x / tileW).toInt().coerceIn(0, GridSystem.COLS - 1)
                val row = (offset.y / tileH).toInt().coerceIn(0, GridSystem.ROWS - 1)
                onTileTapped(TileCoordinate(col, row))
            }
        }
    ) {
        AndroidView(
            factory = {
                FrameLayout(context).apply {
                    addView(gameSurface)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

// ==============================
// HUD
// ==============================

@Composable
private fun CombatHUD(
    state: UiGameState,
    onTowerSelect: (TowerType) -> Unit,
    onNextWave: () -> Unit,
    onClearSelection: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        TopStatusBar(
            wave = state.wave,
            score = state.score,
            lives = state.lives,
            gold = state.gold,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopStart)
                .background(PathriftBackground.copy(alpha = 0.85f))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        TowerPanel(
            playerGold = state.gold,
            selectedType = state.selectedTowerType,
            onTowerSelect = onTowerSelect,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 8.dp)
        )

        BottomActionBar(
            phase = state.phase,
            selectedTowerType = state.selectedTowerType,
            wave = state.wave,
            onNextWave = onNextWave,
            onClearSelection = onClearSelection,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .background(PathriftBackground.copy(alpha = 0.85f))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun TopStatusBar(
    wave: Int,
    score: Long,
    lives: Int,
    gold: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatChip(label = "WAVE", value = if (wave == 0) "-" else "$wave", valueColor = PathriftNeonBlue)
        Spacer(Modifier.width(12.dp))
        StatChip(label = "SCORE", value = "$score", valueColor = PathriftTextPrimary)
        Spacer(Modifier.weight(1f))

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(3) { idx ->
                val filled = idx < lives
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(if (filled) PathriftDanger else PathriftSurfaceVariant)
                )
            }
        }

        Spacer(Modifier.width(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "$gold", color = PathriftGold, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(text = " G", color = PathriftGold.copy(alpha = 0.6f), fontSize = 12.sp)
        }
    }
}

@Composable
private fun StatChip(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(PathriftSurface)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "$label ", color = PathriftTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Medium)
        Text(text = value, color = valueColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun BottomActionBar(
    phase: GamePhase,
    selectedTowerType: TowerType?,
    wave: Int,
    onNextWave: () -> Unit,
    onClearSelection: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (selectedTowerType != null) {
            Column {
                Text(
                    text = "Tap grid to place ${selectedTowerType.name} tower",
                    color = PathriftNeonBlue,
                    fontSize = 12.sp
                )
                Text(text = "Tap again to cancel", color = PathriftTextSecondary, fontSize = 10.sp)
            }
        } else {
            Text(text = "Select a tower from the panel", color = PathriftTextSecondary, fontSize = 12.sp)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (selectedTowerType != null) {
                Button(
                    onClick = onClearSelection,
                    colors = ButtonDefaults.buttonColors(containerColor = PathriftSurfaceVariant)
                ) {
                    Text("Cancel", color = PathriftTextSecondary, fontSize = 12.sp)
                }
            }

            val waveButtonEnabled = phase != GamePhase.WAVE_ACTIVE
            val waveButtonLabel = when (phase) {
                GamePhase.PRE_WAVE -> if (wave == 0) "Start" else "Next Wave"
                GamePhase.WAVE_ACTIVE -> "Wave in Progress..."
                GamePhase.BETWEEN_WAVES -> "Next Wave"
                GamePhase.RUN_ENDED -> "Run Ended"
            }

            Button(
                onClick = onNextWave,
                enabled = waveButtonEnabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PathriftNeonBlue,
                    disabledContainerColor = PathriftSurfaceVariant
                )
            ) {
                Text(
                    text = waveButtonLabel,
                    color = if (waveButtonEnabled) PathriftBackground else PathriftTextSecondary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

// ==============================
// Tower Panel
// ==============================

private data class TowerOption(val tower: Tower, val color: Color)

private val TOWER_OPTIONS = listOf(
    TowerOption(BoltTower(), BoltTowerColor),
    TowerOption(BlastTower(), BlastTowerColor),
    TowerOption(FrostTower(), FrostTowerColor)
)

@Composable
private fun TowerPanel(
    playerGold: Int,
    selectedType: TowerType?,
    onTowerSelect: (TowerType) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(PathriftSurface.copy(alpha = 0.92f), RoundedCornerShape(12.dp))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "TOWERS",
            color = PathriftTextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        TOWER_OPTIONS.forEach { option ->
            val canAfford = playerGold >= option.tower.cost
            val isSelected = selectedType == option.tower.type

            TowerCard(
                option = option,
                canAfford = canAfford,
                isSelected = isSelected,
                onClick = { if (canAfford) onTowerSelect(option.tower.type) }
            )
        }
    }
}

@Composable
private fun TowerCard(
    option: TowerOption,
    canAfford: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) option.color else PathriftSurfaceVariant
    val bgColor = when {
        isSelected -> option.color.copy(alpha = 0.15f)
        canAfford -> PathriftSurfaceVariant
        else -> PathriftSurfaceVariant.copy(alpha = 0.4f)
    }
    val textAlpha = if (canAfford) 1f else 0.4f

    Row(
        modifier = Modifier
            .width(160.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(enabled = canAfford) { onClick() }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(modifier = Modifier.size(10.dp)) {
            drawCircle(option.color.copy(alpha = textAlpha))
        }

        Spacer(Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = option.tower.displayName,
                color = PathriftTextPrimary.copy(alpha = textAlpha),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "DMG ${option.tower.damagePerHit} | RNG ${option.tower.rangeTiles}",
                color = PathriftTextSecondary.copy(alpha = textAlpha),
                fontSize = 9.sp
            )
        }

        Spacer(Modifier.width(4.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${option.tower.cost}",
                color = if (canAfford) PathriftGold else PathriftTextSecondary.copy(alpha = 0.4f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "g",
                color = PathriftGold.copy(alpha = if (canAfford) 0.7f else 0.3f),
                fontSize = 9.sp
            )
        }
    }
}
