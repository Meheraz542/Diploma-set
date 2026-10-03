package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.data.MarketplaceRepository
import com.example.model.BookSet
import com.example.model.Conversation
import com.example.model.Semester
import com.example.model.Technology
import com.example.ui.components.BottomNavTab
import com.example.ui.components.FloatingBottomNavBar
import com.example.ui.components.LiquidGlassBackdrop
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme

sealed class AppDestination {
    data class MainTab(val tab: BottomNavTab) : AppDestination()
    data class SetDetails(val set: BookSet) : AppDestination()
    data class Chat(val conversationId: String) : AppDestination()
    data object AdminPanel : AppDestination()
    data object Notifications : AppDestination()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainApp()
            }
        }
    }
}

@Composable
fun MainApp() {
    val conversations by MarketplaceRepository.conversations.collectAsState()
    val bookSets by MarketplaceRepository.bookSets.collectAsState()
    val notifications by MarketplaceRepository.notifications.collectAsState()

    val totalUnreadInbox = remember(conversations) {
        conversations.sumOf { it.unreadCount }
    }

    // Navigation state stack
    var currentDestination by remember { mutableStateOf<AppDestination>(AppDestination.MainTab(BottomNavTab.HOME)) }
    var currentTab by remember { mutableStateOf(BottomNavTab.HOME) }

    // Pre-filter state for search screen
    var searchTechFilter by remember { mutableStateOf<Technology?>(null) }
    var searchSemFilter by remember { mutableStateOf<Semester?>(null) }

    // Back handling
    BackHandler(enabled = currentDestination !is AppDestination.MainTab || currentTab != BottomNavTab.HOME) {
        when (currentDestination) {
            is AppDestination.SetDetails,
            is AppDestination.Chat,
            is AppDestination.AdminPanel,
            is AppDestination.Notifications -> {
                currentDestination = AppDestination.MainTab(currentTab)
            }
            is AppDestination.MainTab -> {
                if (currentTab != BottomNavTab.HOME) {
                    currentTab = BottomNavTab.HOME
                    currentDestination = AppDestination.MainTab(BottomNavTab.HOME)
                }
            }
        }
    }

    val showBottomBar = currentDestination is AppDestination.MainTab

    LiquidGlassBackdrop(modifier = Modifier.fillMaxSize()) {
        // Content Area
        when (val dest = currentDestination) {
            is AppDestination.MainTab -> {
                when (dest.tab) {
                    BottomNavTab.HOME -> {
                        HomeScreen(
                            onNavigateToSearch = { tech, sem ->
                                searchTechFilter = tech
                                searchSemFilter = sem
                                currentTab = BottomNavTab.SEARCH
                                currentDestination = AppDestination.MainTab(BottomNavTab.SEARCH)
                            },
                            onNavigateToSetDetails = { set ->
                                currentDestination = AppDestination.SetDetails(set)
                            },
                            onNavigateToProfile = {
                                currentTab = BottomNavTab.PROFILE
                                currentDestination = AppDestination.MainTab(BottomNavTab.PROFILE)
                            },
                            onOpenNotifications = {
                                currentDestination = AppDestination.Notifications
                            },
                            onNavigateToSell = {
                                currentTab = BottomNavTab.SELL
                                currentDestination = AppDestination.MainTab(BottomNavTab.SELL)
                            }
                        )
                    }
                    BottomNavTab.SEARCH -> {
                        SearchScreen(
                            initialTechnology = searchTechFilter,
                            initialSemester = searchSemFilter,
                            onNavigateBack = {
                                currentTab = BottomNavTab.HOME
                                currentDestination = AppDestination.MainTab(BottomNavTab.HOME)
                            },
                            onNavigateToSetDetails = { set ->
                                currentDestination = AppDestination.SetDetails(set)
                            }
                        )
                    }
                    BottomNavTab.SELL -> {
                        SellScreen(
                            onPublishSuccess = { newSet ->
                                currentDestination = AppDestination.SetDetails(newSet)
                            }
                        )
                    }
                    BottomNavTab.INBOX -> {
                        InboxScreen(
                            onNavigateToChat = { conv ->
                                currentDestination = AppDestination.Chat(conv.id)
                            },
                            onNavigateToProfile = {
                                currentTab = BottomNavTab.PROFILE
                                currentDestination = AppDestination.MainTab(BottomNavTab.PROFILE)
                            }
                        )
                    }
                    BottomNavTab.PROFILE -> {
                        ProfileScreen(
                            onNavigateToSetDetails = { set ->
                                currentDestination = AppDestination.SetDetails(set)
                            },
                            onNavigateToAdminPanel = {
                                currentDestination = AppDestination.AdminPanel
                            }
                        )
                    }
                }
            }
            is AppDestination.SetDetails -> {
                // Ensure fresh reference
                val liveSet = bookSets.find { it.id == dest.set.id } ?: dest.set
                SetDetailsScreen(
                    bookSet = liveSet,
                    onNavigateBack = {
                        currentDestination = AppDestination.MainTab(currentTab)
                    },
                    onMessageSeller = { set ->
                        val convId = MarketplaceRepository.startOrGetConversation(set)
                        currentDestination = AppDestination.Chat(convId)
                    },
                    onMarkAsSold = { set ->
                        MarketplaceRepository.markSetAsSold(null, set.id)
                    }
                )
            }
            is AppDestination.Chat -> {
                ChatScreen(
                    conversationId = dest.conversationId,
                    onNavigateBack = {
                        currentDestination = AppDestination.MainTab(BottomNavTab.INBOX)
                    },
                    onNavigateToSetDetails = { set ->
                        currentDestination = AppDestination.SetDetails(set)
                    }
                )
            }
            is AppDestination.AdminPanel -> {
                AdminPanelScreen(
                    onNavigateBack = {
                        currentDestination = AppDestination.MainTab(BottomNavTab.PROFILE)
                    }
                )
            }
            is AppDestination.Notifications -> {
                NotificationScreen(
                    onNavigateBack = {
                        currentDestination = AppDestination.MainTab(BottomNavTab.HOME)
                    },
                    onNavigateToSetDetails = { setId ->
                        val set = bookSets.find { it.id == setId }
                        if (set != null) {
                            currentDestination = AppDestination.SetDetails(set)
                        }
                    }
                )
            }
        }

        // Floating Neumorphic Bottom Navigation Bar
        if (showBottomBar) {
            FloatingBottomNavBar(
                currentTab = currentTab,
                onTabSelected = { tab ->
                    currentTab = tab
                    currentDestination = AppDestination.MainTab(tab)
                    if (tab != BottomNavTab.SEARCH) {
                        searchTechFilter = null
                        searchSemFilter = null
                    }
                },
                inboxUnreadCount = totalUnreadInbox,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}
