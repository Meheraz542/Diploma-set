package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MarketplaceRepository
import com.example.model.SetStatus
import com.example.ui.components.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allUsers by MarketplaceRepository.allUsers.collectAsState()
    val bookSets by MarketplaceRepository.bookSets.collectAsState()
    val megaDeals by MarketplaceRepository.megaDeals.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0: Overview, 1: Users, 2: Book Sets

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = BgCanvas,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassIconButton(
                        onClick = onNavigateBack,
                        icon = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back",
                        tone = GlassIconTone.FROSTED_WHITE,
                        size = 40.dp,
                        testTag = "admin_btn_back"
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = "🛡️ Admin Control Panel",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = PrimaryPurple,
                    divider = {}
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Overview", fontWeight = FontWeight.SemiBold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Users (${allUsers.size})", fontWeight = FontWeight.SemiBold) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Listings (${bookSets.size})", fontWeight = FontWeight.SemiBold) }
                    )
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when (selectedTab) {
                0 -> {
                    // Overview Stats
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            AdminStatCard(
                                label = "Total Listings",
                                value = bookSets.size.toString(),
                                icon = Icons.Outlined.Book,
                                color = PrimaryPurple,
                                modifier = Modifier.weight(1f)
                            )
                            AdminStatCard(
                                label = "Active Sets",
                                value = bookSets.count { it.status == SetStatus.AVAILABLE }.toString(),
                                icon = Icons.Outlined.CheckCircle,
                                color = Color(0xFF10B981),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            AdminStatCard(
                                label = "Sold Sets",
                                value = bookSets.count { it.status == SetStatus.SOLD }.toString(),
                                icon = Icons.Outlined.DoneAll,
                                color = AccentBlue,
                                modifier = Modifier.weight(1f)
                            )
                            AdminStatCard(
                                label = "Mega Deals",
                                value = megaDeals.size.toString(),
                                icon = Icons.Outlined.LocalFireDepartment,
                                color = Color(0xFFF59E0B),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Text("Security Notice", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Admin email mdmaheraz65@gmail.com has full moderation authority over student book listings and physical exchange agreements.",
                                fontSize = 13.sp,
                                color = TextSecondary,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                1 -> {
                    // Users list
                    items(allUsers) { user ->
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(user.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        if (user.role == "ADMIN") {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = PrimaryPurpleLight
                                            ) {
                                                Text("ADMIN", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PrimaryPurple, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(user.email, fontSize = 12.sp, color = TextMuted)
                                    Text("${user.college} • ${user.technology.shortName}", fontSize = 12.sp, color = TextSecondary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Status: ${user.accountStatus}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (user.accountStatus == "ACTIVE") Color(0xFF10B981) else Color(0xFFEF4444)
                                    )
                                }

                                if (user.role != "ADMIN") {
                                    Column(horizontalAlignment = Alignment.End) {
                                        if (user.accountStatus == "ACTIVE") {
                                            FrostedGlassButton(
                                                text = "Suspend",
                                                onClick = { MarketplaceRepository.adminSuspendUser(user.id) },
                                                textColor = Color(0xFFEF4444),
                                                height = 36.dp,
                                                testTag = "admin_suspend_${user.id}"
                                            )
                                        } else {
                                            GlassButton(
                                                text = "Restore",
                                                onClick = { MarketplaceRepository.adminRestoreUser(user.id) },
                                                height = 36.dp,
                                                gradient = androidx.compose.ui.graphics.Brush.linearGradient(
                                                    listOf(Color(0xFF10B981), Color(0xFF059669))
                                                ),
                                                testTag = "admin_restore_${user.id}"
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // Listings Moderation
                    items(bookSets) { set ->
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(set.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                                    Text("${set.semester.displayName} • ${set.technology.displayName}", fontSize = 12.sp, color = TextMuted)
                                    Text("Seller: ${set.sellerName} (Tk ${set.totalPrice})", fontSize = 12.sp, color = TextSecondary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Status: ${set.status.name}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when (set.status) {
                                            SetStatus.AVAILABLE -> Color(0xFF10B981)
                                            SetStatus.SOLD -> Color(0xFF3B82F6)
                                            SetStatus.REMOVED -> Color(0xFFEF4444)
                                        }
                                    )
                                }

                                if (set.status == SetStatus.AVAILABLE) {
                                    FrostedGlassButton(
                                        text = "Remove",
                                        onClick = { MarketplaceRepository.adminRemoveSet(set.id) },
                                        textColor = Color(0xFFEF4444),
                                        height = 36.dp,
                                        testTag = "admin_remove_${set.id}"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminStatCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(100.dp)
            .shadow(elevation = 3.dp, shape = RoundedCornerShape(20.dp), ambientColor = SoftShadowColor),
        shape = RoundedCornerShape(20.dp),
        color = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(label, fontSize = 12.sp, color = TextMuted, fontWeight = FontWeight.Medium)
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            }

            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}
