package com.pathrift.anonve.android.core.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pathrift.anonve.android.R
import com.pathrift.anonve.android.core.engine.ScoreEngine
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
    onPlayAgain: () -> Unit,
    onMainMenu: () -> Unit
) {
    val rank = ScoreEngine.getRank(score)

    // Score count-up animation — iOS parity
    var displayedScore by remember { mutableStateOf(0L) }
    LaunchedEffect(score) {
        val steps = 30
        val stepValue = if (score > 0) score / steps else 0L
        for (i in 1..steps) {
            delay(40L)
            displayedScore = if (i == steps) score else stepValue * i
        }
    }

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
                    // Left panel (45%): title + animated score + rank
                    Column(
                        modifier = Modifier.fillMaxWidth(0.45f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "RUN OVER",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = PathriftDanger,
                            letterSpacing = 2.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "$displayedScore",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = PathriftNeonBlue,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = stringResource(R.string.run_end_score_label),
                            fontSize = 10.sp,
                            color = PathriftTextSecondary,
                            letterSpacing = 1.sp
                        )
                        Spacer(Modifier.height(16.dp))
                        RankBadgeCompact(rank = rank)
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
                                icon = Icons.Default.Star,
                                label = "ENEMY KILLS",
                                value = "$enemyKills",
                                color = PathriftDanger
                            )
                            StatIconRow(
                                icon = Icons.Default.EmojiEvents,
                                label = "FINAL SCORE",
                                value = "$score",
                                color = PathriftGold
                            )
                        }

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

                        OutlinedButton(
                            onClick = onMainMenu,
                            modifier = Modifier.fillMaxWidth().height(40.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, PathriftSurfaceVariant)
                        ) {
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
                    // Header — iOS: "RUN OVER" title
                    Text(
                        text = "RUN OVER",
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Black,
                        color = PathriftDanger,
                        letterSpacing = 3.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(Modifier.height(24.dp))

                    RankBadge(rank = rank)

                    Spacer(Modifier.height(24.dp))

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
                        // Animated score — iOS parity
                        Text(
                            text = "$displayedScore",
                            fontSize = 48.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = PathriftNeonBlue,
                            fontFamily = FontFamily.Monospace
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
                        StatIconRow(
                            icon = Icons.Default.Star,
                            label = "ENEMY KILLS",
                            value = "$enemyKills",
                            color = PathriftDanger
                        )
                        StatIconRow(
                            icon = Icons.Default.EmojiEvents,
                            label = "FINAL SCORE",
                            value = "$score",
                            color = PathriftGold
                        )
                    }

                    Spacer(Modifier.height(40.dp))

                    Button(
                        onClick = onPlayAgain,
                        modifier = Modifier
                            .width(220.dp)
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

                    OutlinedButton(
                        onClick = onMainMenu,
                        modifier = Modifier
                            .width(220.dp)
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PathriftSurfaceVariant)
                    ) {
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

@Composable
private fun RankBadge(rank: String) {
    val rankColor = when (rank) {
        "S" -> PathriftGold
        "A" -> PathriftSuccess
        "B" -> PathriftNeonBlue
        "C" -> PathriftPurple
        else -> PathriftTextSecondary
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(rankColor.copy(alpha = 0.15f))
            .padding(horizontal = 32.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "RANK",
                fontSize = 10.sp,
                color = PathriftTextSecondary,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = rank,
                fontSize = 64.sp,
                fontWeight = FontWeight.ExtraBold,
                color = rankColor
            )
        }
    }
}

@Composable
private fun RankBadgeCompact(rank: String) {
    val rankColor = when (rank) {
        "S" -> PathriftGold
        "A" -> PathriftSuccess
        "B" -> PathriftNeonBlue
        "C" -> PathriftPurple
        else -> PathriftTextSecondary
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(rankColor.copy(alpha = 0.15f))
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "RANK",
                fontSize = 9.sp,
                color = PathriftTextSecondary,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = rank,
                fontSize = 48.sp,
                fontWeight = FontWeight.ExtraBold,
                color = rankColor
            )
        }
    }
}

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
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = PathriftTextPrimary,
            fontFamily = FontFamily.Monospace
        )
    }
}
