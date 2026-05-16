package com.pathrift.anonve.android.core.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pathrift.anonve.android.R
import com.pathrift.anonve.android.core.ui.PathriftBackground
import com.pathrift.anonve.android.core.ui.PathriftDanger
import com.pathrift.anonve.android.core.ui.PathriftGold
import com.pathrift.anonve.android.core.ui.PathriftNeonBlue
import com.pathrift.anonve.android.core.ui.PathriftOrange
import com.pathrift.anonve.android.core.ui.PathriftPurple
import com.pathrift.anonve.android.core.ui.PathriftSuccess
import com.pathrift.anonve.android.core.ui.PathriftSurface
import com.pathrift.anonve.android.core.ui.PathriftSurfaceVariant
import com.pathrift.anonve.android.core.ui.PathriftTextPrimary
import com.pathrift.anonve.android.core.ui.PathriftTextSecondary
import kotlinx.coroutines.delay

@Composable
fun RunEndScreen(
    score: Long,
    wave: Int,
    enemyKills: Int = 0,
    isVictory: Boolean = false,
    previousBestScore: Long = 0L,
    onPlayAgain: () -> Unit,
    onMainMenu: () -> Unit
) {
    val isNewHighScore = score > previousBestScore && score > 0L

    // Score count-up animation — iOS parity
    var displayedScore by remember { mutableStateOf(0L) }
    var scoreAnimDone by remember { mutableStateOf(false) }
    LaunchedEffect(score) {
        val steps = 30
        val stepValue = if (score > 0) score / steps else 0L
        for (i in 1..steps) {
            delay(40L)
            displayedScore = if (i == steps) score else stepValue * i
        }
        scoreAnimDone = true
    }

    // Digit flip scale animation — iOS parity
    val scoreScale by animateFloatAsState(
        targetValue = if (scoreAnimDone) 1.0f else 0.95f,
        animationSpec = spring(stiffness = 400f),
        label = "scoreScale"
    )

    val titleText = if (isVictory) "VICTORY" else "RUN OVER"
    val titleColor = if (isVictory) PathriftSuccess else PathriftDanger

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PathriftBackground),
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isLandscape = maxWidth > maxHeight
            if (isLandscape) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left panel (45%): title + animated score
                    Column(
                        modifier = Modifier.fillMaxWidth(0.45f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // GAP-047: title glow shadow
                        Text(
                            text = titleText,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = titleColor,
                            letterSpacing = 2.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.drawBehind {
                                drawCircle(
                                    color = titleColor.copy(alpha = 0.3f),
                                    radius = size.minDimension * 1.5f
                                )
                            }
                        )
                        if (isNewHighScore) {
                            NewHighScoreBadge()
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "%,d".format(displayedScore),
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Black,
                            color = PathriftNeonBlue,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.scale(scoreScale)
                        )
                        Text(
                            text = stringResource(R.string.run_end_score_label),
                            fontSize = 10.sp,
                            color = PathriftTextSecondary,
                            letterSpacing = 1.sp
                        )
                    }

                    // Right panel: stats + actions
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Stats card — iOS parity: waves, kills, score
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(PathriftSurface)
                                .padding(horizontal = 24.dp, vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatIconRow(
                                icon = Icons.Default.Flag,
                                label = "WAVES REACHED",
                                value = "$wave",
                                color = PathriftNeonBlue
                            )
                            StatIconRow(
                                icon = Icons.Default.Close,
                                label = "ENEMY KILLS",
                                value = "$enemyKills",
                                color = PathriftDanger
                            )
                            StatIconRow(
                                icon = Icons.Default.EmojiEvents,
                                label = "FINAL SCORE",
                                value = "%,d".format(score),
                                color = PathriftGold
                            )
                        }

                        // GAP-054: full width buttons
                        Button(
                            onClick = onPlayAgain,
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PathriftNeonBlue)
                        ) {
                            Text(
                                text = stringResource(R.string.run_end_play_again),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = PathriftBackground
                            )
                        }

                        // GAP-054 + GAP-055: full width + Home icon
                        OutlinedButton(
                            onClick = onMainMenu,
                            modifier = Modifier.fillMaxWidth().height(40.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, PathriftSurfaceVariant)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = PathriftTextSecondary
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.run_end_main_menu),
                                fontSize = 13.sp,
                                color = PathriftTextSecondary
                            )
                        }
                    }
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize().padding(32.dp)
                ) {
                    // Header — GAP-046: VICTORY vs RUN OVER, GAP-047: glow shadow
                    Text(
                        text = titleText,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Black,
                        color = titleColor,
                        letterSpacing = 3.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.drawBehind {
                            drawCircle(
                                color = titleColor.copy(alpha = 0.3f),
                                radius = size.minDimension * 1.5f
                            )
                        }
                    )

                    // GAP-050: NEW HIGH SCORE badge
                    if (isNewHighScore) {
                        Spacer(Modifier.height(8.dp))
                        NewHighScoreBadge()
                    }

                    Spacer(Modifier.height(24.dp))
                    // GAP-051: RankBadge removed — no iOS equivalent

                    // Stats card — iOS parity: waves, kills, animated score
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(PathriftSurface)
                            .padding(horizontal = 32.dp, vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Animated score — GAP-048: 56sp Black + scale animation
                        Text(
                            text = "%,d".format(displayedScore),
                            fontSize = 56.sp,
                            fontWeight = FontWeight.Black,
                            color = PathriftNeonBlue,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.scale(scoreScale)
                        )
                        Text(
                            text = stringResource(R.string.run_end_score_label),
                            fontSize = 11.sp,
                            color = PathriftTextSecondary,
                            letterSpacing = 1.sp
                        )

                        Spacer(Modifier.height(8.dp))

                        StatIconRow(
                            icon = Icons.Default.Flag,
                            label = "WAVES REACHED",
                            value = "$wave",
                            color = PathriftNeonBlue
                        )
                        // GAP-052: Close icon instead of Star for enemy kills
                        StatIconRow(
                            icon = Icons.Default.Close,
                            label = "ENEMY KILLS",
                            value = "$enemyKills",
                            color = PathriftDanger
                        )
                        StatIconRow(
                            icon = Icons.Default.EmojiEvents,
                            label = "FINAL SCORE",
                            value = "%,d".format(score),
                            color = PathriftGold
                        )
                    }

                    Spacer(Modifier.height(40.dp))

                    // GAP-054: full width
                    Button(
                        onClick = onPlayAgain,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PathriftNeonBlue)
                    ) {
                        Text(
                            text = stringResource(R.string.run_end_play_again),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = PathriftBackground
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    // GAP-054 + GAP-055: full width + Home icon
                    OutlinedButton(
                        onClick = onMainMenu,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PathriftSurfaceVariant)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = PathriftTextSecondary
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.run_end_main_menu),
                            fontSize = 14.sp,
                            color = PathriftTextSecondary
                        )
                    }
                }
            }
        }
    }
}

// GAP-050: NEW HIGH SCORE badge
@Composable
private fun NewHighScoreBadge() {
    Box(
        modifier = Modifier
            .background(PathriftGold.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .border(1.dp, PathriftGold.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "★ NEW HIGH SCORE ★",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = PathriftGold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.5.sp
        )
    }
}

// GAP-051: RankBadge and RankBadgeCompact removed — no iOS equivalent

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = PathriftTextSecondary, fontSize = 13.sp)
        Spacer(Modifier.width(24.dp))
        Text(text = value, color = PathriftTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

// iOS parity: icon + label + value stat row
@Composable
private fun StatIconRow(icon: ImageVector, label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = PathriftTextSecondary,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1f)
        )
        // GAP-053: 18sp iOS parity
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = PathriftTextPrimary,
            fontFamily = FontFamily.Monospace
        )
    }
}
