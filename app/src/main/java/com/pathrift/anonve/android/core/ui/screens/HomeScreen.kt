package com.pathrift.anonve.android.core.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pathrift.anonve.android.core.storage.LocalProgressStore
import com.pathrift.anonve.android.core.ui.ArtilleryTowerColor
import com.pathrift.anonve.android.core.ui.BlastTowerColor
import com.pathrift.anonve.android.core.ui.BoltTowerColor
import com.pathrift.anonve.android.core.ui.CoreTowerColor
import com.pathrift.anonve.android.core.ui.FrostTowerColor
import com.pathrift.anonve.android.core.ui.InfernoTowerColor
import com.pathrift.anonve.android.core.ui.LanguageManager
import com.pathrift.anonve.android.core.ui.NovaTowerColor
import com.pathrift.anonve.android.core.ui.PierceTowerColor
import com.pathrift.anonve.android.core.ui.SniperTowerColor
import com.pathrift.anonve.android.core.ui.TeslaTowerColor
import com.pathrift.anonve.android.game.towers.TowerType
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import com.pathrift.anonve.android.core.ui.PathriftBackground
import com.pathrift.anonve.android.core.ui.PathriftGold
import com.pathrift.anonve.android.core.ui.PathriftNeonBlue
import com.pathrift.anonve.android.core.ui.PathriftOrange
import com.pathrift.anonve.android.core.ui.PathriftPurple
import com.pathrift.anonve.android.core.ui.PathriftSurface
import com.pathrift.anonve.android.core.ui.PathriftTextPrimary
import com.pathrift.anonve.android.core.ui.PathriftTextSecondary
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import com.pathrift.anonve.android.app.PathriftApp
import kotlinx.coroutines.flow.flowOf

@Composable
fun HomeScreen(
    onStartGame: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenStore: () -> Unit,
    onOpenHowToPlay: () -> Unit,
    onOpenArsenal: () -> Unit = {},
    storage: LocalProgressStore? = null
) {
    val lang by LanguageManager.current.collectAsState()
    val bestScore by (storage?.bestScore ?: flowOf(0L)).collectAsState(initial = 0L)
    val context = LocalContext.current
    val gameSaveStore = remember { (context.applicationContext as PathriftApp).gameSaveStore }
    val hasSave = remember { gameSaveStore.hasSave() }
    val savedWave = remember { gameSaveStore.savedWave }

    val infiniteTransition = rememberInfiniteTransition(label = "titlePulse")
    // GAP-003: Start from 1.0 (iOS), not 0.97
    val titleScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "titleScale"
    )
    // GAP-002: Animated glow alpha 0.3→0.8
    val titleGlow by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "titleGlow"
    )

    Box(modifier = Modifier.fillMaxSize().background(PathriftBackground)) {
        // Animated grid background — GAP-013: opacity 0.035f (iOS parity)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val spacing = 44.dp.toPx()
            val lineColor = Color.White.copy(alpha = 0.035f)
            var x = 0f
            while (x <= size.width) {
                drawLine(lineColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 0.5f)
                x += spacing
            }
            var y = 0f
            while (y <= size.height) {
                drawLine(lineColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 0.5f)
                y += spacing
            }
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isLandscape = maxWidth > maxHeight
            if (isLandscape) {
                LandscapeHomeContent(
                    titleScale = titleScale,
                    titleGlow = titleGlow,
                    bestScore = bestScore,
                    onPlay = { gameSaveStore.clear(); onStartGame() },
                    onContinue = onStartGame,
                    hasSave = hasSave,
                    savedWave = savedWave,
                    onHowToPlay = onOpenHowToPlay,
                    onSettings = onOpenSettings,
                    onStore = onOpenStore,
                    onArsenal = onOpenArsenal
                )
            } else {
                PortraitHomeContent(
                    titleScale = titleScale,
                    titleGlow = titleGlow,
                    bestScore = bestScore,
                    onStartGame = { gameSaveStore.clear(); onStartGame() },
                    onContinue = onStartGame,
                    hasSave = hasSave,
                    savedWave = savedWave,
                    onOpenHowToPlay = onOpenHowToPlay,
                    onOpenSettings = onOpenSettings,
                    onOpenStore = onOpenStore,
                    onOpenArsenal = onOpenArsenal
                )
            }
        }
    }
}

@Composable
private fun LandscapeHomeContent(
    titleScale: Float,
    titleGlow: Float,
    bestScore: Long,
    onPlay: () -> Unit,
    onContinue: () -> Unit = {},
    hasSave: Boolean = false,
    savedWave: Int = 0,
    onHowToPlay: () -> Unit,
    onSettings: () -> Unit,
    onStore: () -> Unit,
    onArsenal: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // LEFT PANEL (38%)
        Column(
            modifier = Modifier
                .fillMaxWidth(0.38f)
                .fillMaxHeight()
                .padding(end = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "PATHRIFT",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = PathriftNeonBlue,
                letterSpacing = 2.sp,
                modifier = Modifier
                    .graphicsLayer { scaleX = titleScale; scaleY = titleScale }
                    .drawBehind {
                        drawCircle(
                            color = PathriftNeonBlue.copy(alpha = titleGlow * 0.25f),
                            radius = size.minDimension * 1.4f
                        )
                    }
            )
            Text(
                text = LanguageManager.s("ENDLESS TOWER DEFENSE", "SONSUZ KULE SAVUNMASI"),
                fontSize = 9.sp,
                color = PathriftTextSecondary,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Canvas(modifier = Modifier.width(10.dp).height(2.dp)) {
                    drawRoundRect(
                        color = PathriftPurple.copy(alpha = 0.7f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.dp.toPx())
                    )
                }
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "THE MAP ALWAYS SHIFTS",
                    fontSize = 8.sp,
                    color = PathriftPurple.copy(alpha = 0.8f),
                    letterSpacing = 1.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(Modifier.width(4.dp))
                Canvas(modifier = Modifier.width(10.dp).height(2.dp)) {
                    drawRoundRect(
                        color = PathriftPurple.copy(alpha = 0.7f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.dp.toPx())
                    )
                }
            }

            if (bestScore > 0L) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = "trophy",
                        tint = PathriftGold,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "${LanguageManager.s("BEST", "EN İYİ")}: $bestScore",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PathriftGold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Box(
            Modifier
                .fillMaxHeight()
                .width(1.dp)
                .background(PathriftTextSecondary.copy(0.12f))
        )

        // RIGHT PANEL
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(start = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(1f))

            if (hasSave) {
                val contInteraction = remember { MutableInteractionSource() }
                val contIsPressed by contInteraction.collectIsPressedAsState()
                val contScale by animateFloatAsState(if (contIsPressed) 0.94f else 1.0f, spring(stiffness = 700f), label = "contScale")
                Button(
                    onClick = onContinue,
                    modifier = Modifier.fillMaxWidth().height(44.dp).graphicsLayer { scaleX = contScale; scaleY = contScale },
                    colors = ButtonDefaults.buttonColors(containerColor = PathriftOrange),
                    shape = RoundedCornerShape(14.dp),
                    interactionSource = contInteraction
                ) {
                    Text("↩  CONTINUE — WAVE $savedWave",
                        fontSize = 14.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                }
                Spacer(Modifier.height(4.dp))
            }

            Button(
                onClick = onPlay,
                modifier = Modifier.fillMaxWidth().height(44.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PathriftNeonBlue),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = LanguageManager.s("PLAY", "OYNA"),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp
                )
            }

            OutlinedButton(
                onClick = onHowToPlay,
                modifier = Modifier.fillMaxWidth().height(40.dp),
                border = BorderStroke(1.dp, PathriftTextSecondary.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PathriftTextSecondary)
            ) {
                Icon(Icons.Default.HelpOutline, contentDescription = null, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(6.dp))
                Text(LanguageManager.s("HOW TO PLAY", "NASIL OYNANIR"), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onSettings,
                    modifier = Modifier.weight(1f).height(40.dp),
                    border = BorderStroke(1.dp, PathriftTextSecondary.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PathriftTextSecondary)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(LanguageManager.s("SETTINGS", "AYARLAR"), fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = onStore,
                    modifier = Modifier.weight(1f).height(40.dp),
                    border = BorderStroke(1.dp, PathriftNeonBlue.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PathriftNeonBlue)
                ) {
                    Icon(Icons.Default.Diamond, contentDescription = null, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(LanguageManager.s("STORE", "MAĞAZA"), fontSize = 12.sp)
                }
            }

            OutlinedButton(
                onClick = onArsenal,
                modifier = Modifier.fillMaxWidth().height(40.dp),
                border = BorderStroke(1.dp, PathriftOrange.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PathriftOrange)
            ) {
                Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(5.dp))
                Text(LanguageManager.s("ARSENAL", "CEPHANE"), fontSize = 12.sp)
            }

            TowerLegendCompact(Modifier.fillMaxWidth())

            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun PortraitHomeContent(
    titleScale: Float,
    titleGlow: Float,
    bestScore: Long,
    onStartGame: () -> Unit,
    onContinue: () -> Unit = {},
    hasSave: Boolean = false,
    savedWave: Int = 0,
    onOpenHowToPlay: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenStore: () -> Unit,
    onOpenArsenal: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(Modifier.weight(1f))

        // GAP-001: 56sp, GAP-002: animated glow, GAP-003: scale 1.0→1.03
        Text(
            text = "PATHRIFT",
            fontSize = 56.sp,
            fontWeight = FontWeight.Black,
            color = PathriftNeonBlue,
            modifier = Modifier
                .graphicsLayer { scaleX = titleScale; scaleY = titleScale }
                .drawBehind {
                    drawCircle(
                        color = PathriftNeonBlue.copy(alpha = titleGlow * 0.25f),
                        radius = size.minDimension * 1.4f
                    )
                }
        )
        Text(
            text = LanguageManager.s("ENDLESS TOWER DEFENSE", "SONSUZ KULE SAVUNMASI"),
            fontSize = 12.sp,
            color = PathriftTextSecondary,
            letterSpacing = 3.sp,
            fontFamily = FontFamily.Monospace
        )
        // iOS parity: decorative purple line accents flanking the tagline
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(top = 4.dp)
        ) {
            Canvas(modifier = Modifier.width(20.dp).height(2.dp)) {
                drawRoundRect(
                    color = PathriftPurple.copy(alpha = 0.7f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.dp.toPx())
                )
            }
            Spacer(Modifier.width(6.dp))
            Text(
                text = "THE MAP ALWAYS SHIFTS",
                fontSize = 10.sp,
                color = PathriftPurple.copy(alpha = 0.8f),
                letterSpacing = 1.5.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(Modifier.width(6.dp))
            Canvas(modifier = Modifier.width(20.dp).height(2.dp)) {
                drawRoundRect(
                    color = PathriftPurple.copy(alpha = 0.7f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.dp.toPx())
                )
            }
        }

        if (bestScore > 0L) {
            // GAP-007: Styled high score badge box (FIX 1: Gold background, iOS parity)
            Box(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .background(PathriftGold.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                    .border(1.dp, PathriftGold.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = "trophy",
                        tint = PathriftGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "${LanguageManager.s("BEST", "EN İYİ")}: $bestScore",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = PathriftGold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(Modifier.height(52.dp))

        // GAP-010: height 58dp + orange→gold gradient; GAP-011: Refresh icon
        if (hasSave) {
            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth().height(58.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                shape = RoundedCornerShape(16.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize()
                        .background(
                            Brush.horizontalGradient(listOf(PathriftOrange, PathriftGold)),
                            RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp), tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("CONTINUE — WAVE $savedWave",
                            fontSize = 15.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, color = Color.White)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        // GAP-004: gradient PLAY button + GAP-005: shadow + GAP-012: scale press animation
        val playInteractionSource = remember { MutableInteractionSource() }
        val playIsPressed by playInteractionSource.collectIsPressedAsState()
        val playScale by animateFloatAsState(
            targetValue = if (playIsPressed) 0.94f else 1.0f,
            animationSpec = spring(stiffness = 700f),
            label = "playBtnScale"
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .scale(playScale)
                .shadow(elevation = 12.dp, shape = RoundedCornerShape(16.dp), spotColor = PathriftNeonBlue.copy(alpha = 0.5f))
        ) {
            Button(
                onClick = onStartGame,
                modifier = Modifier.fillMaxSize(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                shape = RoundedCornerShape(16.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                interactionSource = playInteractionSource
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(listOf(PathriftNeonBlue, PathriftPurple)),
                            RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "play", modifier = Modifier.size(18.dp), tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = LanguageManager.s("PLAY", "OYNA"),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = onOpenHowToPlay,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            border = BorderStroke(1.dp, PathriftTextSecondary.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = PathriftTextSecondary)
        ) {
            Icon(Icons.Default.HelpOutline, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = LanguageManager.s("HOW TO PLAY", "NASIL OYNANIR"),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(Modifier.height(12.dp))

        // iOS parity: Settings + Store + Arsenal in one 3-equal-width row (portrait)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onOpenSettings,
                modifier = Modifier.weight(1f).height(44.dp),
                border = BorderStroke(1.dp, PathriftTextSecondary.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PathriftTextSecondary),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 0.dp)
            ) {
                Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(4.dp))
                Text(LanguageManager.s("SETTINGS", "AYARLAR"), fontSize = 11.sp)
            }

            OutlinedButton(
                onClick = onOpenStore,
                modifier = Modifier.weight(1f).height(44.dp),
                border = BorderStroke(1.dp, PathriftNeonBlue.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PathriftNeonBlue),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 0.dp)
            ) {
                Icon(Icons.Default.Diamond, contentDescription = null, modifier = Modifier.size(13.dp), tint = PathriftNeonBlue)
                Spacer(Modifier.width(4.dp))
                Text(LanguageManager.s("STORE", "MAĞAZA"), fontSize = 11.sp)
            }

            OutlinedButton(
                onClick = onOpenArsenal,
                modifier = Modifier.weight(1f).height(44.dp),
                border = BorderStroke(1.dp, PathriftOrange.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PathriftOrange),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 0.dp)
            ) {
                Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(4.dp))
                Text(LanguageManager.s("ARSENAL", "CEPHANE"), fontSize = 11.sp)
            }
        }

        Spacer(Modifier.height(12.dp))

        TowerLegend()

        Spacer(Modifier.weight(1f))

        Text(
            text = "Harita değişiyor • Strateji sürekli evriliyor",
            fontSize = 11.sp,
            color = PathriftTextSecondary.copy(alpha = 0.5f),
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 12.dp)
        )
    }
}

@Composable
private fun towerLegendColor(type: TowerType): Color = when (type) {
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

@Composable
private fun TowerLegend() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PathriftSurface.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
            .padding(vertical = 10.dp, horizontal = 8.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        TowerType.values().forEach { type ->
            val c = towerLegendColor(type)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // GAP-009: circle with shadow/glow
                Box(
                    Modifier.size(10.dp)
                        .shadow(elevation = 4.dp, shape = CircleShape, spotColor = c.copy(alpha = 0.6f))
                        .background(c, CircleShape)
                )
                Text(
                    text = type.displayName.uppercase(),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Medium,
                    color = PathriftTextSecondary,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun TowerLegendCompact(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(PathriftSurface.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
            .padding(vertical = 6.dp, horizontal = 8.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TowerType.values().forEach { type ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Box(Modifier.size(7.dp).background(towerLegendColor(type), CircleShape))
                Text(
                    text = type.displayName.uppercase(),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Medium,
                    color = PathriftTextSecondary,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
