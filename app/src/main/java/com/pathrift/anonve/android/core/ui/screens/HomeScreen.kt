package com.pathrift.anonve.android.core.ui.screens

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
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pathrift.anonve.android.core.storage.LocalProgressStore
import com.pathrift.anonve.android.core.ui.BlastTowerColor
import com.pathrift.anonve.android.core.ui.BoltTowerColor
import com.pathrift.anonve.android.core.ui.FrostTowerColor
import com.pathrift.anonve.android.core.ui.LanguageManager
import com.pathrift.anonve.android.core.ui.PathriftBackground
import com.pathrift.anonve.android.core.ui.PathriftGold
import com.pathrift.anonve.android.core.ui.PathriftNeonBlue
import com.pathrift.anonve.android.core.ui.PathriftOrange
import com.pathrift.anonve.android.core.ui.PathriftPurple
import com.pathrift.anonve.android.core.ui.PathriftSurface
import com.pathrift.anonve.android.core.ui.PathriftTextPrimary
import com.pathrift.anonve.android.core.ui.PathriftTextSecondary
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

    val infiniteTransition = rememberInfiniteTransition(label = "titlePulse")
    val titleScale by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "titleScale"
    )

    Box(modifier = Modifier.fillMaxSize().background(PathriftBackground)) {
        // Animated grid background
        Canvas(modifier = Modifier.fillMaxSize()) {
            val spacing = 44.dp.toPx()
            val lineColor = Color(0x0AFFFFFF)
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
                    bestScore = bestScore,
                    onPlay = onStartGame,
                    onHowToPlay = onOpenHowToPlay,
                    onSettings = onOpenSettings,
                    onStore = onOpenStore,
                    onArsenal = onOpenArsenal
                )
            } else {
                PortraitHomeContent(
                    titleScale = titleScale,
                    bestScore = bestScore,
                    onStartGame = onStartGame,
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
    bestScore: Long,
    onPlay: () -> Unit,
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
                modifier = Modifier.graphicsLayer {
                    scaleX = titleScale
                    scaleY = titleScale
                }
            )
            Text(
                text = LanguageManager.s("ENDLESS TOWER DEFENSE", "SONSUZ KULE SAVUNMASI"),
                fontSize = 9.sp,
                color = PathriftTextSecondary,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center
            )
            Text(
                text = "THE MAP ALWAYS SHIFTS",
                fontSize = 8.sp,
                color = PathriftPurple.copy(alpha = 0.8f),
                letterSpacing = 1.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = 2.dp)
            )

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
                Icon(Icons.Default.MilitaryTech, contentDescription = null, modifier = Modifier.size(13.dp))
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
    bestScore: Long,
    onStartGame: () -> Unit,
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

        // Title area
        Text(
            text = "PATHRIFT",
            fontSize = 52.sp,
            fontWeight = FontWeight.Black,
            color = PathriftNeonBlue,
            modifier = Modifier.graphicsLayer {
                scaleX = titleScale
                scaleY = titleScale
            }
        )
        Text(
            text = LanguageManager.s("ENDLESS TOWER DEFENSE", "SONSUZ KULE SAVUNMASI"),
            fontSize = 12.sp,
            color = PathriftTextSecondary,
            letterSpacing = 3.sp,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = "THE MAP ALWAYS SHIFTS",
            fontSize = 10.sp,
            color = PathriftPurple.copy(alpha = 0.8f),
            letterSpacing = 1.5.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(top = 4.dp)
        )

        if (bestScore > 0L) {
            Row(
                modifier = Modifier.padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
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

        Spacer(Modifier.height(52.dp))

        Button(
            onClick = onStartGame,
            modifier = Modifier.fillMaxWidth().height(58.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PathriftNeonBlue),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = "play", modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = LanguageManager.s("PLAY", "OYNA"),
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onOpenSettings,
                modifier = Modifier.weight(1f).height(44.dp),
                border = BorderStroke(1.dp, PathriftTextSecondary.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PathriftTextSecondary)
            ) {
                Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(LanguageManager.s("SETTINGS", "AYARLAR"), fontSize = 13.sp)
            }

            OutlinedButton(
                onClick = onOpenStore,
                modifier = Modifier.weight(1f).height(44.dp),
                border = BorderStroke(1.dp, PathriftNeonBlue.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PathriftNeonBlue)
            ) {
                Icon(Icons.Default.Diamond, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(LanguageManager.s("STORE", "MAĞAZA"), fontSize = 13.sp)
            }
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = onOpenArsenal,
            modifier = Modifier.fillMaxWidth().height(44.dp),
            border = BorderStroke(1.dp, PathriftOrange.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = PathriftOrange)
        ) {
            Icon(Icons.Default.MilitaryTech, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text(LanguageManager.s("ARSENAL", "CEPHANE"), fontSize = 13.sp)
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
private fun TowerLegend() {
    val towers = listOf(
        Triple(BoltTowerColor, "BOLT", "Fast"),
        Triple(BlastTowerColor, "BLAST", "Area"),
        Triple(FrostTowerColor, "FROST", "Slow")
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PathriftSurface.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        towers.forEach { (color, name, _) ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Canvas(modifier = Modifier.size(10.dp)) {
                    drawCircle(color)
                }
                Text(
                    text = name,
                    fontSize = 9.sp,
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
    val towers = listOf(
        Pair(BoltTowerColor, "BOLT"),
        Pair(BlastTowerColor, "BLAST"),
        Pair(FrostTowerColor, "FROST")
    )

    Row(
        modifier = modifier
            .background(PathriftSurface.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
            .padding(vertical = 6.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        towers.forEach { (color, name) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Canvas(modifier = Modifier.size(7.dp)) {
                    drawCircle(color)
                }
                Text(
                    text = name,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Medium,
                    color = PathriftTextSecondary,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
