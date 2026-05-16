package com.pathrift.anonve.android.core.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
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
import com.pathrift.anonve.android.core.ui.PathriftPurple
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as PathriftApp
    val diamondStore = app.diamondStore
    val premiumStore = app.premiumStore

    val prefs = remember {
        context.getSharedPreferences("pathrift_store", android.content.Context.MODE_PRIVATE)
    }

    var diamonds by remember { mutableIntStateOf(diamondStore.balance) }
    var isPremium by remember { mutableStateOf(premiumStore.isPremium) }
    var dailyClaimed by remember {
        val todayKey = storeScreenTodayKey()
        mutableStateOf(prefs.getBoolean("daily_claimed_$todayKey", false))
    }

    // Tower detail sheet state
    var selectedTower by remember { mutableStateOf<TowerType?>(null) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    if (selectedTower != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedTower = null },
            sheetState = sheetState,
            containerColor = PathriftBackground
        ) {
            selectedTower?.let { type ->
                TowerDetailSheetContent(
                    type = type,
                    diamonds = diamonds,
                    onUnlock = { t ->
                        if (diamondStore.unlock(t)) {
                            diamonds = diamondStore.balance
                        }
                        scope.launch { sheetState.hide() }.invokeOnCompletion { selectedTower = null }
                    },
                    onClose = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion { selectedTower = null }
                    }
                )
            }
        }
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
                        letterSpacing = 2.sp,
                        fontFamily = FontFamily.Monospace
                    )
                },
                navigationIcon = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable(onClick = onBack).padding(horizontal = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "back",
                            tint = PathriftNeonBlue,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            LanguageManager.s("MAIN MENU", "ANA MENÜ"),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = PathriftNeonBlue
                        )
                    }
                },
                actions = {
                    Row(
                        modifier = Modifier.padding(end = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Diamond, contentDescription = null, tint = PathriftNeonBlue, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("$diamonds", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PathriftTextPrimary, fontFamily = FontFamily.Monospace)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PathriftSurface.copy(alpha = 0.9f))
            )
        },
        containerColor = PathriftBackground
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().background(PathriftBackground).padding(padding)
        ) {
            val isLandscape = maxWidth > maxHeight
            if (isLandscape) {
                Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.Top) {
                    // Left: Premium + Diamonds + Daily Bonus
                    Column(
                        modifier = Modifier.weight(1f).fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        StoreSectionHeader("PREMIUM", Icons.Default.Bolt)
                        PremiumCard(isPremium = isPremium, onActivate = {
                            premiumStore.toggle(); isPremium = premiumStore.isPremium
                        })
                        StoreSectionHeader("DIAMONDS", Icons.Default.Diamond)
                        DiamondBalanceCard(diamonds = diamonds)
                        DailyBonusCard(claimed = dailyClaimed, onClaim = {
                            if (!dailyClaimed) {
                                diamondStore.earn(10); diamonds = diamondStore.balance; dailyClaimed = true
                                prefs.edit().putBoolean("daily_claimed_${storeScreenTodayKey()}", true).apply()
                            }
                        })
                    }
                    Box(Modifier.width(1.dp).fillMaxHeight().background(PathriftSurface))
                    // Right: Towers
                    Column(
                        modifier = Modifier.weight(1f).fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StoreSectionHeader(
                            LanguageManager.s("TOWERS — ALL 10", "KULELER — HEPSİ 10"),
                            Icons.Default.Shield
                        )
                        TowersGrid(unlockedTowers = diamondStore.unlockedTowers, onTap = { selectedTower = it })
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    Spacer(Modifier.height(24.dp))

                    // iOS order: PREMIUM → TOWERS → DIAMONDS → DAILY BONUS
                    StoreSectionHeader("PREMIUM", Icons.Default.Bolt)
                    Spacer(Modifier.height(12.dp))
                    PremiumCard(isPremium = isPremium, onActivate = {
                        premiumStore.toggle(); isPremium = premiumStore.isPremium
                    })

                    Spacer(Modifier.height(24.dp))

                    StoreSectionHeader(
                        LanguageManager.s("TOWERS — ALL 10", "KULELER — HEPSİ 10"),
                        Icons.Default.Shield
                    )
                    Spacer(Modifier.height(12.dp))
                    TowersGrid(unlockedTowers = diamondStore.unlockedTowers, onTap = { selectedTower = it })

                    Spacer(Modifier.height(24.dp))

                    StoreSectionHeader("DIAMONDS", Icons.Default.Diamond)
                    Spacer(Modifier.height(12.dp))
                    DiamondBalanceCard(diamonds = diamonds)

                    Spacer(Modifier.height(24.dp))

                    DailyBonusCard(claimed = dailyClaimed, onClaim = {
                        if (!dailyClaimed) {
                            diamondStore.earn(10); diamonds = diamondStore.balance; dailyClaimed = true
                            prefs.edit().putBoolean("daily_claimed_${storeScreenTodayKey()}", true).apply()
                        }
                    })

                    Spacer(Modifier.height(40.dp))
                }
            }
        }
    }
}

// ---- Towers 2-column grid (iOS parity) ----

@Composable
private fun TowersGrid(unlockedTowers: Set<String>, onTap: (TowerType) -> Unit) {
    val towers = TowerType.values().toList()
    // Render as simple wrapped rows to avoid nested scroll
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        towers.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowItems.forEach { type ->
                    val isUnlocked = unlockedTowers.contains(type.name) || type.diamondCost == 0
                    StoreTowerGridCard(
                        type = type,
                        isUnlocked = isUnlocked,
                        modifier = Modifier.weight(1f),
                        onTap = { onTap(type) }
                    )
                }
                // Fill empty slot if odd number
                if (rowItems.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StoreTowerGridCard(type: TowerType, isUnlocked: Boolean, modifier: Modifier, onTap: () -> Unit) {
    val color = towerColor(type)
    val isPrem = type.isPremium
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.94f else 1.0f, spring(stiffness = 700f), label = "storeCardScale")
    Column(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .background(PathriftSurface, RoundedCornerShape(12.dp))
            .border(
                width = if (isUnlocked) 1.5.dp else 1.dp,
                color = if (isUnlocked) color.copy(alpha = 0.3f) else PathriftTextSecondary.copy(alpha = 0.12f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onTap)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Icon area
        Box(
            modifier = Modifier.fillMaxWidth().height(72.dp)
                .background(color.copy(alpha = if (isPrem) 0.14f else 0.08f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            TowerShapeIcon(type = type, color = color, modifier = Modifier.size(40.dp))
            // Premium lock badge (top-right)
            if (isPrem && !isUnlocked) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.align(Alignment.TopEnd).padding(5.dp).size(12.dp)
                )
            }
        }
        Text(
            text = type.displayName.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = PathriftTextPrimary,
            fontFamily = FontFamily.Monospace,
            maxLines = 1
        )
        // Status badge
        Box(
            modifier = Modifier
                .background(
                    when {
                        type.diamondCost == 0 || isUnlocked -> PathriftSuccess.copy(alpha = 0.15f)
                        else -> PathriftNeonBlue.copy(alpha = 0.12f)
                    },
                    RoundedCornerShape(6.dp)
                )
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text(
                text = when {
                    type.diamondCost == 0 -> "FREE"
                    isUnlocked -> "OWNED ✓"
                    else -> "♦ ${type.diamondCost}"
                },
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = when {
                    type.diamondCost == 0 || isUnlocked -> PathriftSuccess
                    else -> Color(0xFF00CCFF)
                },
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

// ---- Tower Detail Sheet ----

@Composable
private fun TowerDetailSheetContent(
    type: TowerType,
    diamonds: Int,
    onUnlock: (TowerType) -> Unit,
    onClose: () -> Unit
) {
    val color = towerColor(type)
    val isUnlocked = type.diamondCost == 0
    val canAfford = diamonds >= type.diamondCost

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp).padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Tower icon
        Box(
            modifier = Modifier.size(80.dp)
                .background(color.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                .border(1.5.dp, color.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            TowerShapeIcon(type = type, color = color, modifier = Modifier.size(50.dp))
        }

        // Name
        Text(
            text = type.displayName.uppercase(),
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            color = PathriftTextPrimary,
            fontFamily = FontFamily.Monospace
        )

        // Tier + advantage chips
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .background(color.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text("TIER ${type.tier}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color, fontFamily = FontFamily.Monospace)
            }
            type.typeAdvantageHint?.let { hint ->
                Box(
                    modifier = Modifier
                        .background(PathriftSurface, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(hint, fontSize = 10.sp, color = PathriftTextSecondary, fontFamily = FontFamily.Monospace)
                }
            }
        }

        // Stats row
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SheetStatPill("DMG", "${type.baseDamage}", PathriftOrange)
            SheetStatPill("RNG", String.format("%.0ft", type.baseRangeTiles / 64f * 3f), PathriftNeonBlue)
            SheetStatPill("SPD", String.format("%.1f/s", type.baseAttacksPerSecond), PathriftPurple)
        }

        // Description
        Text(
            text = type.description,
            fontSize = 12.sp,
            color = PathriftTextSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            lineHeight = 18.sp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(Modifier.height(4.dp))

        // Action button
        if (type.diamondCost == 0 || isUnlocked) {
            Box(
                modifier = Modifier.fillMaxWidth().height(52.dp)
                    .background(PathriftSuccess.copy(alpha = 0.12f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("✓", fontSize = 18.sp, color = PathriftSuccess)
                    Text("OWNED", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = PathriftSuccess)
                }
            }
        } else {
            Button(
                onClick = { onUnlock(type) },
                enabled = canAfford,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (canAfford) PathriftNeonBlue else PathriftSurface,
                    disabledContainerColor = PathriftSurface
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = if (canAfford) "UNLOCK FOR ${type.diamondCost} ♦" else "NEED ${type.diamondCost - diamonds} MORE ♦",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (canAfford) PathriftBackground else PathriftTextSecondary
                )
            }
        }

        TextButton(onClick = onClose) {
            Text("Close", fontSize = 13.sp, color = PathriftTextSecondary)
        }
    }
}

@Composable
private fun SheetStatPill(label: String, value: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = PathriftTextSecondary,
            fontFamily = FontFamily.Monospace, letterSpacing = 1.sp)
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.Black, color = color, fontFamily = FontFamily.Monospace)
    }
}

// ---- Premium Card (with gradient button) ----

@Composable
private fun PremiumCard(isPremium: Boolean, onActivate: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .background(
                Brush.linearGradient(
                    if (isPremium) listOf(PathriftSurface, PathriftNeonBlue.copy(alpha = 0.08f))
                    else listOf(PathriftSurface, PathriftSurface)
                ),
                RoundedCornerShape(14.dp)
            )
            .border(
                1.dp,
                if (isPremium) PathriftSuccess.copy(0.4f) else PathriftNeonBlue.copy(0.3f),
                RoundedCornerShape(14.dp)
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(52.dp)
                    .background(
                        if (isPremium) PathriftNeonBlue.copy(alpha = 0.15f) else PathriftGold.copy(alpha = 0.1f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPremium) Icons.Default.CheckCircle else Icons.Default.WorkspacePremium,
                    contentDescription = null,
                    tint = if (isPremium) PathriftNeonBlue else PathriftGold,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = if (isPremium) "PREMIUM ACTIVE ✓" else "PREMIUM",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isPremium) PathriftNeonBlue else PathriftTextPrimary,
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
                PremiumBenefitRow(icon = Icons.Default.FastForward, text = "×2 Speed in-game")
                PremiumBenefitRow(icon = Icons.Default.Favorite, text = "1 Revive per run")
                PremiumBenefitRow(icon = Icons.Default.AutoAwesome, text = "More perks coming")
            }
            // Gradient button — iOS parity: NeonBlue → Purple gradient
            Box(
                modifier = Modifier.fillMaxWidth().height(50.dp)
                    .background(
                        Brush.horizontalGradient(listOf(PathriftNeonBlue, PathriftPurple)),
                        RoundedCornerShape(14.dp)
                    )
                    .clickable(onClick = onActivate),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.FastForward, contentDescription = null, tint = PathriftBackground, modifier = Modifier.size(16.dp))
                    Text(
                        text = "GET PREMIUM (FREE – Test Mode)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = PathriftBackground,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                PremiumBenefitRow(icon = Icons.Default.FastForward, text = "×2 Speed active")
                PremiumBenefitRow(icon = Icons.Default.Favorite, text = "1 Revive per run active")
            }
        }
    }
}

@Composable
private fun PremiumBenefitRow(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(imageVector = icon, contentDescription = null, tint = PathriftNeonBlue, modifier = Modifier.size(14.dp))
        Text(text, fontSize = 12.sp, color = PathriftTextPrimary, fontFamily = FontFamily.Monospace)
    }
}

// ---- Diamond Balance Card ----

@Composable
private fun DiamondBalanceCard(diamonds: Int) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .background(
                Brush.linearGradient(listOf(PathriftSurface, PathriftNeonBlue.copy(alpha = 0.08f))),
                RoundedCornerShape(16.dp)
            )
            .border(1.dp, PathriftNeonBlue.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = LanguageManager.s("DIAMONDS", "ELMASLAR"),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = PathriftTextSecondary,
                    letterSpacing = 2.sp,
                    fontFamily = FontFamily.Monospace
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = Icons.Default.Diamond, contentDescription = null, tint = PathriftNeonBlue, modifier = Modifier.size(26.dp))
                    Text("$diamonds", fontSize = 34.sp, fontWeight = FontWeight.Black, color = PathriftTextPrimary)
                }
            }
            Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = PathriftTextSecondary.copy(alpha = 0.3f), modifier = Modifier.size(30.dp))
        }
        Text(
            text = "Buy Diamonds – Coming Soon (Phase 7)",
            fontSize = 11.sp,
            color = PathriftTextSecondary.copy(alpha = 0.6f),
            fontFamily = FontFamily.Monospace
        )
    }
}

// ---- Daily Bonus Card ----

@Composable
private fun DailyBonusCard(claimed: Boolean, onClaim: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed && !claimed) 0.94f else 1.0f, spring(stiffness = 700f), label = "dailyBonusScale")
    Box(
        modifier = Modifier.fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .background(PathriftSurface, RoundedCornerShape(14.dp))
            .border(
                1.dp,
                if (claimed) Color.Transparent else PathriftGold.copy(alpha = 0.3f),
                RoundedCornerShape(14.dp)
            )
            .clickable(interactionSource = interactionSource, indication = null, enabled = !claimed, onClick = onClaim)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(52.dp)
                    .background(if (claimed) PathriftSurface else PathriftGold.copy(alpha = 0.15f), CircleShape),
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
                    Icon(imageVector = Icons.Default.Diamond, contentDescription = null, tint = PathriftGold, modifier = Modifier.size(11.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("+10", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PathriftGold, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

// ---- Section Header ----

@Composable
private fun StoreSectionHeader(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = PathriftNeonBlue, modifier = Modifier.size(14.dp))
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

private fun storeScreenTodayKey(): String {
    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return fmt.format(Date())
}

// towerColor(type) is provided by ArsenalScreen.kt (internal fun, same package)
// TowerType.isPremium, description, tier, damage, range, attackSpeed — provided by Tower.kt
