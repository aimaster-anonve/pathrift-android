package com.pathrift.anonve.android.core.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pathrift.anonve.android.app.PathriftApp
import com.pathrift.anonve.android.core.ui.ArtilleryTowerColor
import com.pathrift.anonve.android.core.ui.BlastTowerColor
import com.pathrift.anonve.android.core.ui.PathriftDanger
import com.pathrift.anonve.android.core.ui.BoltTowerColor
import com.pathrift.anonve.android.core.ui.CoreTowerColor
import com.pathrift.anonve.android.core.ui.FrostTowerColor
import com.pathrift.anonve.android.core.ui.InfernoTowerColor
import com.pathrift.anonve.android.core.ui.LanguageManager
import com.pathrift.anonve.android.core.ui.NovaTowerColor
import com.pathrift.anonve.android.core.ui.PathriftBackground
import com.pathrift.anonve.android.core.ui.PathriftGold
import com.pathrift.anonve.android.core.ui.PathriftNeonBlue
import com.pathrift.anonve.android.core.ui.PathriftOrange
import com.pathrift.anonve.android.core.ui.PathriftSuccess
import com.pathrift.anonve.android.core.ui.PathriftSurface
import com.pathrift.anonve.android.core.ui.PathriftTextPrimary
import com.pathrift.anonve.android.core.ui.PathriftTextSecondary
import com.pathrift.anonve.android.core.ui.PierceTowerColor
import com.pathrift.anonve.android.core.ui.SniperTowerColor
import com.pathrift.anonve.android.core.ui.TeslaTowerColor
import com.pathrift.anonve.android.core.ui.TowerShapeIcon
import com.pathrift.anonve.android.game.towers.TowerType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as PathriftApp
    val diamondStore = app.diamondStore
    val premiumStore = app.premiumStore

    // Local prefs for daily bonus only
    val prefs = remember {
        context.getSharedPreferences("pathrift_store", android.content.Context.MODE_PRIVATE)
    }

    var diamonds by remember { mutableIntStateOf(diamondStore.balance) }
    var isPremium by remember { mutableStateOf(premiumStore.isPremium) }
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
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(PathriftBackground)
                .padding(padding)
        ) {
            val isLandscape = maxWidth > maxHeight
            if (isLandscape) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.Top
                ) {
                    // Left column: Premium + Diamond balance + Daily bonus
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        DiamondBalanceCard(diamonds = diamonds)
                        DailyBonusCard(
                            claimed = dailyClaimed,
                            onClaim = {
                                if (!dailyClaimed) {
                                    diamondStore.earn(10)
                                    diamonds = diamondStore.balance
                                    dailyClaimed = true
                                    val todayKey = todayKey()
                                    prefs.edit().putBoolean("daily_claimed_$todayKey", true).apply()
                                }
                            }
                        )
                        StoreSectionHeader(title = "PREMIUM", icon = Icons.Default.WorkspacePremium)
                        PremiumCard(
                            isPremium = isPremium,
                            onActivate = {
                                premiumStore.toggle()
                                isPremium = premiumStore.isPremium
                            }
                        )
                    }

                    Box(
                        Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(PathriftSurface)
                    )

                    // Right column: Towers + buy diamonds
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        StoreSectionHeader(
                            title = LanguageManager.s("TOWERS", "KULELER"),
                            icon = Icons.Default.Bolt
                        )
                        TowersSection(
                            unlockedTowers = diamondStore.unlockedTowers,
                            diamonds = diamonds,
                            onUnlock = { type ->
                                if (diamondStore.unlock(type)) {
                                    diamonds = diamondStore.balance
                                }
                            }
                        )
                        StoreSectionHeader(title = "DIAMONDS", icon = Icons.Default.Diamond)
                        BuyDiamondsSection()
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    Spacer(Modifier.height(24.dp))

                    DiamondBalanceCard(diamonds = diamonds)

                    Spacer(Modifier.height(16.dp))

                    DailyBonusCard(
                        claimed = dailyClaimed,
                        onClaim = {
                            if (!dailyClaimed) {
                                diamondStore.earn(10)
                                diamonds = diamondStore.balance
                                dailyClaimed = true
                                val todayKey = todayKey()
                                prefs.edit().putBoolean("daily_claimed_$todayKey", true).apply()
                            }
                        }
                    )

                    Spacer(Modifier.height(24.dp))

                    StoreSectionHeader(title = "PREMIUM", icon = Icons.Default.WorkspacePremium)
                    Spacer(Modifier.height(12.dp))
                    PremiumCard(
                        isPremium = isPremium,
                        onActivate = {
                            premiumStore.toggle()
                            isPremium = premiumStore.isPremium
                        }
                    )

                    Spacer(Modifier.height(24.dp))

                    StoreSectionHeader(
                        title = LanguageManager.s("TOWERS", "KULELER"),
                        icon = Icons.Default.Bolt
                    )
                    Spacer(Modifier.height(12.dp))
                    TowersSection(
                        unlockedTowers = diamondStore.unlockedTowers,
                        diamonds = diamonds,
                        onUnlock = { type ->
                            if (diamondStore.unlock(type)) {
                                diamonds = diamondStore.balance
                            }
                        }
                    )

                    Spacer(Modifier.height(24.dp))

                    StoreSectionHeader(title = "DIAMONDS", icon = Icons.Default.Diamond)
                    Spacer(Modifier.height(12.dp))
                    BuyDiamondsSection()

                    Spacer(Modifier.height(40.dp))
                }
            }
        }
    }
}

// ---- Premium Card ----

@Composable
private fun PremiumCard(isPremium: Boolean, onActivate: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isPremium) Color(0xFF0A2A1A) else PathriftSurface,
                RoundedCornerShape(16.dp)
            )
            .border(
                1.dp,
                if (isPremium) PathriftSuccess.copy(0.4f) else PathriftNeonBlue.copy(0.3f),
                RoundedCornerShape(16.dp)
            )
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.WorkspacePremium,
                contentDescription = null,
                tint = if (isPremium) PathriftSuccess else PathriftGold,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = if (isPremium) "PREMIUM ACTIVE" else "PREMIUM",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isPremium) PathriftSuccess else PathriftTextPrimary,
                    letterSpacing = 1.sp
                )
                Text(
                    text = if (isPremium) "All premium features unlocked" else "Unlock speed boost, revives & more",
                    fontSize = 11.sp,
                    color = PathriftTextSecondary,
                    fontFamily = FontFamily.Monospace
                )
            }
            if (isPremium) {
                TextButton(onClick = onActivate, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                    Text("DEACTIVATE", color = PathriftDanger, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (!isPremium) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                PremiumBenefit(text = "x2 Speed in-game")
                PremiumBenefit(text = "1 Revive per run")
                PremiumBenefit(text = "More perks coming")
            }
            Button(
                onClick = onActivate,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = PathriftGold),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "ACTIVATE PREMIUM (Test Mode)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.Black
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                PremiumBenefit(text = "x2 Speed active")
                PremiumBenefit(text = "1 Revive per run active")
            }
        }
    }
}

@Composable
private fun PremiumBenefit(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            Modifier.size(6.dp).clip(CircleShape).background(PathriftGold)
        )
        Text(text, fontSize = 12.sp, color = PathriftTextSecondary, fontFamily = FontFamily.Monospace)
    }
}

// ---- Towers Section ----

@Composable
private fun TowersSection(
    unlockedTowers: Set<String>,
    diamonds: Int,
    onUnlock: (TowerType) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TowerType.values().forEach { type ->
            val isUnlocked = unlockedTowers.contains(type.name) || type.diamondCost == 0
            val canAfford = diamonds >= type.diamondCost
            TowerStoreCard(
                type = type,
                isUnlocked = isUnlocked,
                canAfford = canAfford,
                onUnlock = { onUnlock(type) }
            )
        }
    }
}

@Composable
private fun TowerStoreCard(
    type: TowerType,
    isUnlocked: Boolean,
    canAfford: Boolean,
    onUnlock: () -> Unit
) {
    val color = towerColor(type)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PathriftSurface, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Build 5.2: TowerShapeIcon — unique geometric shape per tower type
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f))
                .border(1.dp, color.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            TowerShapeIcon(
                type = type,
                color = color,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    type.displayName.uppercase(),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    color = PathriftTextPrimary
                )
                Text(
                    "T${type.tier}",
                    color = color,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(color.copy(alpha = 0.15f), RoundedCornerShape(3.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
            Text(
                type.typeAdvantageHint ?: type.displayName,
                fontSize = 10.sp,
                color = PathriftTextSecondary,
                fontFamily = FontFamily.Monospace
            )
        }
        if (isUnlocked) {
            Box(
                Modifier
                    .background(PathriftSuccess.copy(0.15f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text("OWNED", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PathriftSuccess)
            }
        } else if (type.diamondCost == 0) {
            Box(
                Modifier
                    .background(PathriftSuccess.copy(0.15f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text("FREE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PathriftSuccess)
            }
        } else {
            Button(
                onClick = onUnlock,
                enabled = canAfford,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PathriftNeonBlue,
                    disabledContainerColor = PathriftSurface.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Diamond,
                    contentDescription = null,
                    modifier = Modifier.size(10.dp),
                    tint = if (canAfford) Color.Black else PathriftTextSecondary
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "${type.diamondCost}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (canAfford) Color.Black else PathriftTextSecondary
                )
            }
        }
    }
}

// ---- Buy Diamonds Section ----

@Composable
private fun BuyDiamondsSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PathriftSurface.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Diamond,
            contentDescription = null,
            tint = PathriftNeonBlue.copy(alpha = 0.4f),
            modifier = Modifier.size(36.dp)
        )
        Text(
            "Buy Diamonds",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = PathriftTextSecondary
        )
        Text(
            "Coming Soon — v1.1",
            fontSize = 11.sp,
            color = PathriftTextSecondary.copy(alpha = 0.5f),
            fontFamily = FontFamily.Monospace
        )
    }
}

// ---- Reused helpers ----

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
                text = LanguageManager.s("DIAMONDS", "ELMASLAR"),
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
                text = "Earn diamonds by playing — IAP coming in v1.1",
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
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
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

private fun todayKey(): String {
    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return fmt.format(Date())
}
