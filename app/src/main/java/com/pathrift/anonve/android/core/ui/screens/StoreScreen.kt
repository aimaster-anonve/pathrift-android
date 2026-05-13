package com.pathrift.anonve.android.core.ui.screens

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pathrift.anonve.android.core.ui.BlastTowerColor
import com.pathrift.anonve.android.core.ui.BoltTowerColor
import com.pathrift.anonve.android.core.ui.FrostTowerColor
import com.pathrift.anonve.android.core.ui.LanguageManager
import com.pathrift.anonve.android.core.ui.PathriftBackground
import com.pathrift.anonve.android.core.ui.PathriftGold
import com.pathrift.anonve.android.core.ui.PathriftNeonBlue
import com.pathrift.anonve.android.core.ui.PathriftSuccess
import com.pathrift.anonve.android.core.ui.PathriftSurface
import com.pathrift.anonve.android.core.ui.PathriftTextPrimary
import com.pathrift.anonve.android.core.ui.PathriftTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreScreen(onBack: () -> Unit) {
    val lang by LanguageManager.current.collectAsState()
    val context = LocalContext.current

    val prefs = remember {
        context.getSharedPreferences("pathrift_store", android.content.Context.MODE_PRIVATE)
    }

    var diamonds by remember {
        mutableIntStateOf(prefs.getInt("diamond_balance", 0))
    }
    var dailyClaimed by remember {
        val todayKey = todayKey()
        mutableStateOf(prefs.getBoolean("daily_claimed_$todayKey", false))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = LanguageManager.s("STORE", "MAĞAZA"),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = PathriftTextPrimary,
                        letterSpacing = 2.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "back",
                            tint = PathriftNeonBlue,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                },
                actions = {
                    // Diamond count in nav bar
                    Row(
                        modifier = Modifier.padding(end = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Diamond,
                            contentDescription = null,
                            tint = PathriftNeonBlue,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "$diamonds",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = PathriftTextPrimary,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PathriftSurface.copy(alpha = 0.9f),
                    titleContentColor = PathriftTextPrimary
                )
            )
        },
        containerColor = PathriftBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(PathriftBackground)
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Spacer(Modifier.height(24.dp))

            // Diamond balance card
            DiamondBalanceCard(diamonds = diamonds)

            Spacer(Modifier.height(16.dp))

            // Daily bonus card
            DailyBonusCard(
                claimed = dailyClaimed,
                onClaim = {
                    if (!dailyClaimed) {
                        val newBalance = diamonds + 10
                        diamonds = newBalance
                        dailyClaimed = true
                        val todayKey = todayKey()
                        prefs.edit()
                            .putInt("diamond_balance", newBalance)
                            .putBoolean("daily_claimed_$todayKey", true)
                            .apply()
                    }
                }
            )

            Spacer(Modifier.height(24.dp))

            // Tower skins section header
            StoreSectionHeader(
                title = LanguageManager.s("TOWER SKINS", "KULE KAPLAMALARI"),
                icon = Icons.Default.Palette
            )
            Spacer(Modifier.height(12.dp))

            // Tower skins grid (2 columns, non-scrollable inside scroll)
            val towerSkins = listOf(
                Triple(BoltTowerColor, "BOLT", true),
                Triple(BlastTowerColor, "BLAST", true),
                Triple(FrostTowerColor, "FROST", true)
            )
            val lockedSkins = listOf("Cyber", "Void")

            // Row 1: Bolt + Blast
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                towerSkins.take(2).forEach { (color, name, free) ->
                    SkinCard(
                        color = color,
                        name = name,
                        free = free,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            // Row 2: Frost + Cyber (locked)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SkinCard(
                    color = FrostTowerColor,
                    name = "FROST",
                    free = true,
                    modifier = Modifier.weight(1f)
                )
                LockedSkinCard(name = "Cyber", modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            // Row 3: Void (locked) + placeholder
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LockedSkinCard(name = "Void", modifier = Modifier.weight(1f))
                Spacer(Modifier.weight(1f))
            }

            Spacer(Modifier.height(24.dp))

            // Coming soon section
            StoreSectionHeader(title = "COMING SOON", icon = Icons.Default.Star)
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ComingSoonPill(icon = Icons.Default.Map, label = "Map Themes", modifier = Modifier.weight(1f))
                ComingSoonPill(icon = Icons.Default.Star, label = "Rift Pass", modifier = Modifier.weight(1f))
                ComingSoonPill(icon = Icons.Default.EmojiEvents, label = "Leaderboard", modifier = Modifier.weight(1f))
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun DiamondBalanceCard(diamonds: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PathriftSurface, RoundedCornerShape(16.dp))
            .padding(20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = LanguageManager.s("DIAMONDS", "ELMASlar"),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = PathriftTextSecondary,
                letterSpacing = 2.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Diamond,
                    contentDescription = null,
                    tint = PathriftNeonBlue,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "$diamonds",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black,
                    color = PathriftTextPrimary,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "💎 IAP purchases coming in v1.1 — earn diamonds by playing!",
                fontSize = 11.sp,
                color = PathriftTextSecondary.copy(alpha = 0.6f),
                fontFamily = FontFamily.Monospace
            )
        }
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            tint = PathriftTextSecondary.copy(alpha = 0.3f),
            modifier = Modifier.size(32.dp)
        )
    }
}

@Composable
private fun DailyBonusCard(claimed: Boolean, onClaim: () -> Unit) {
    Button(
        onClick = onClaim,
        enabled = !claimed,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = PathriftSurface,
            disabledContainerColor = PathriftSurface
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(
                        if (claimed) PathriftSurface else PathriftGold.copy(alpha = 0.15f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (claimed) Icons.Default.CheckCircle else Icons.Default.CardGiftcard,
                    contentDescription = null,
                    tint = if (claimed) PathriftSuccess else PathriftGold,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = LanguageManager.s("DAILY BONUS", "GÜNLÜK BONUS"),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (claimed) PathriftTextSecondary else PathriftTextPrimary,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = if (claimed) "Come back tomorrow!" else "Claim 10 free diamonds",
                    fontSize = 11.sp,
                    color = PathriftTextSecondary,
                    fontFamily = FontFamily.Monospace
                )
            }

            if (!claimed) {
                Row(
                    modifier = Modifier
                        .background(PathriftGold.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Diamond,
                        contentDescription = null,
                        tint = PathriftGold,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "+10",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = PathriftGold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
private fun SkinCard(color: Color, name: String, free: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(PathriftSurface, RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp)
                .background(color.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(color, CircleShape)
            )
        }
        Text(
            text = name,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = PathriftTextPrimary
        )
        Box(
            modifier = Modifier
                .background(PathriftSuccess.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text(
                text = LanguageManager.s("FREE", "BEDAVA"),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = PathriftSuccess,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun LockedSkinCard(name: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(PathriftSurface.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp)
                .background(PathriftSurface, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = PathriftTextSecondary.copy(alpha = 0.3f),
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            text = name,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = PathriftTextSecondary
        )
        Box(
            modifier = Modifier
                .background(PathriftSurface, RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text(
                text = LanguageManager.s("Coming Soon", "Yakında"),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = PathriftTextSecondary.copy(alpha = 0.5f),
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun StoreSectionHeader(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PathriftNeonBlue,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = PathriftTextSecondary,
            letterSpacing = 1.5.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun ComingSoonPill(icon: ImageVector, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(PathriftSurface.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PathriftTextSecondary.copy(alpha = 0.4f),
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = PathriftTextSecondary.copy(alpha = 0.5f),
            fontFamily = FontFamily.Monospace
        )
    }
}

private fun todayKey(): String {
    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return fmt.format(Date())
}
