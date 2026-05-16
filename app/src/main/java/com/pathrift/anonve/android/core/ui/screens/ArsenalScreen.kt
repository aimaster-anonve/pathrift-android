package com.pathrift.anonve.android.core.ui.screens

import android.app.Application
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pathrift.anonve.android.app.PathriftApp
import com.pathrift.anonve.android.core.storage.ArsenalStore
import com.pathrift.anonve.android.core.storage.DiamondStore
import com.pathrift.anonve.android.core.ui.ArtilleryTowerColor
import com.pathrift.anonve.android.core.ui.BlastTowerColor
import com.pathrift.anonve.android.core.ui.BoltTowerColor
import com.pathrift.anonve.android.core.ui.CoreTowerColor
import com.pathrift.anonve.android.core.ui.FrostTowerColor
import com.pathrift.anonve.android.core.ui.InfernoTowerColor
import com.pathrift.anonve.android.core.ui.NovaTowerColor
import com.pathrift.anonve.android.core.ui.PierceTowerColor
import com.pathrift.anonve.android.core.ui.SniperTowerColor
import com.pathrift.anonve.android.core.ui.TeslaTowerColor
import com.pathrift.anonve.android.game.towers.TowerType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

// ---- State ----

data class ArsenalUiState(
    val diamonds: Int = 0,
    val unlockedTowers: Set<String> = emptySet(),
    val dmgLevels: Map<TowerType, Int> = emptyMap(),
    val spdLevels: Map<TowerType, Int> = emptyMap(),
    val dmgCosts: Map<TowerType, Int?> = emptyMap(),
    val spdCosts: Map<TowerType, Int?> = emptyMap()
)

// ---- ViewModel ----

class ArsenalViewModel(application: Application) : AndroidViewModel(application) {
    private val diamondStore: DiamondStore = (application as PathriftApp).diamondStore
    private val arsenalStore: ArsenalStore = (application as PathriftApp).arsenalStore

    private val _state = MutableStateFlow(buildState())
    val state = _state.asStateFlow()

    private fun buildState() = ArsenalUiState(
        diamonds = diamondStore.balance,
        unlockedTowers = diamondStore.unlockedTowers,
        dmgLevels = TowerType.values().associateWith { arsenalStore.permDamageLevel(it) },
        spdLevels = TowerType.values().associateWith { arsenalStore.permSpeedLevel(it) },
        dmgCosts = TowerType.values().associateWith { arsenalStore.dmgUpgradeCost(it) },
        spdCosts = TowerType.values().associateWith { arsenalStore.speedUpgradeCost(it) }
    )

    fun upgradeDamage(type: TowerType) {
        val cost = arsenalStore.dmgUpgradeCost(type) ?: return
        if (diamondStore.spend(cost)) {
            arsenalStore.setPermDamageLevel(type, arsenalStore.permDamageLevel(type) + 1)
            _state.value = buildState()
        }
    }

    fun upgradeSpeed(type: TowerType) {
        val cost = arsenalStore.speedUpgradeCost(type) ?: return
        if (diamondStore.spend(cost)) {
            arsenalStore.setPermSpeedLevel(type, arsenalStore.permSpeedLevel(type) + 1)
            _state.value = buildState()
        }
    }
}

// ---- Screen ----

@Composable
fun ArsenalScreen(
    viewModel: ArsenalViewModel = viewModel(),
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().background(Color(0xFF0A0A10))
    ) {
        val isLandscape = maxWidth > maxHeight
        val towerList = TowerType.values().toList()

        Column(Modifier.fillMaxSize()) {
            // Header — same for both orientations
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White)
                }
                Text(
                    "ARSENAL",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    modifier = Modifier.weight(1f).padding(start = 8.dp)
                )
                Text(
                    "◆ ${state.diamonds}",
                    color = Color(0xFF00CCFF),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            if (isLandscape) {
                // Landscape: 2-column grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    gridItems(towerList) { type ->
                        TowerArsenalCard(
                            type = type,
                            isUnlocked = state.unlockedTowers.contains(type.name),
                            dmgLevel = state.dmgLevels[type] ?: 0,
                            spdLevel = state.spdLevels[type] ?: 0,
                            dmgCost = state.dmgCosts[type],
                            spdCost = state.spdCosts[type],
                            diamonds = state.diamonds,
                            onUpgradeDmg = { viewModel.upgradeDamage(type) },
                            onUpgradeSpd = { viewModel.upgradeSpeed(type) }
                        )
                    }
                }
            } else {
                // Portrait: single column
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(towerList) { type ->
                        TowerArsenalCard(
                            type = type,
                            isUnlocked = state.unlockedTowers.contains(type.name),
                            dmgLevel = state.dmgLevels[type] ?: 0,
                            spdLevel = state.spdLevels[type] ?: 0,
                            dmgCost = state.dmgCosts[type],
                            spdCost = state.spdCosts[type],
                            diamonds = state.diamonds,
                            onUpgradeDmg = { viewModel.upgradeDamage(type) },
                            onUpgradeSpd = { viewModel.upgradeSpeed(type) }
                        )
                    }
                }
            }
        }
    }
}

// ---- Tower Card ----

@Composable
fun TowerArsenalCard(
    type: TowerType,
    isUnlocked: Boolean,
    dmgLevel: Int,
    spdLevel: Int,
    dmgCost: Int?,
    spdCost: Int?,
    diamonds: Int,
    onUpgradeDmg: () -> Unit,
    onUpgradeSpd: () -> Unit
) {
    val towerColor = towerColor(type)

    Card(
        modifier = Modifier.fillMaxWidth().alpha(if (isUnlocked) 1f else 0.6f),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF16162A)),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            1.dp,
            if (isUnlocked) towerColor.copy(alpha = 0.25f) else Color.Gray.copy(alpha = 0.15f)
        )
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: identity (40% width)
            Row(
                Modifier.fillMaxWidth(0.4f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // GAP-060: solid fill circle with white center dot — iOS parity
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(towerColor),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .background(Color.White.copy(alpha = 0.9f), CircleShape)
                    )
                }
                Column {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            type.displayName.uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp
                        )
                        Text(
                            "T${type.tier}",
                            color = towerColor,
                            fontSize = 8.sp,
                            modifier = Modifier
                                .background(towerColor.copy(0.15f), RoundedCornerShape(3.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                    Text(
                        type.typeAdvantageHint ?: type.displayName,
                        color = Color.Gray,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (isUnlocked) {
                VerticalDivider(
                    modifier = Modifier.height(40.dp).padding(horizontal = 8.dp),
                    color = Color.White.copy(0.08f)
                )
                upgradeRow(
                    label = "DMG",
                    level = dmgLevel,
                    cost = dmgCost,
                    diamonds = diamonds,
                    color = Color(0xFFFF6B00),
                    onUpgrade = onUpgradeDmg,
                    modifier = Modifier.weight(1f)
                )
                VerticalDivider(
                    modifier = Modifier.height(40.dp).padding(horizontal = 8.dp),
                    color = Color.White.copy(0.08f)
                )
                upgradeRow(
                    label = "SPD",
                    level = spdLevel,
                    cost = spdCost,
                    diamonds = diamonds,
                    color = Color(0xFF9966FF),
                    onUpgrade = onUpgradeSpd,
                    modifier = Modifier.weight(1f)
                )
            } else {
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🔒", fontSize = 14.sp)
                    Text(
                        "${type.diamondCost}♦",
                        color = Color(0xFF00CCFF),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun upgradeRow(
    label: String,
    level: Int,
    cost: Int?,
    diamonds: Int,
    color: Color,
    onUpgrade: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier.height(40.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                label,
                color = Color.Gray,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                repeat(3) { i ->
                    Box(
                        Modifier
                            .size(width = 16.dp, height = 5.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (i < level) color else Color.Gray.copy(alpha = 0.2f))
                    )
                }
            }
        }
        if (cost != null) {
            TextButton(
                onClick = onUpgrade,
                enabled = diamonds >= cost,
                contentPadding = PaddingValues(horizontal = 5.dp, vertical = 2.dp),
                modifier = Modifier.wrapContentWidth().height(28.dp)
            ) {
                Text(
                    text = "+${cost}♦",
                    color = if (diamonds >= cost) color else Color.Gray,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Visible
                )
            }
        } else {
            Text("MAX", color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ---- Tower Color Helper ----

internal fun towerColor(type: TowerType): Color = when (type) {
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
