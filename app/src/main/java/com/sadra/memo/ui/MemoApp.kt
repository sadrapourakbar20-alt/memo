package com.sadra.memo.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sadra.memo.ui.assistant.AssistantScreen
import com.sadra.memo.ui.finance.AccountDetailScreen
import com.sadra.memo.ui.finance.AccountEditScreen
import com.sadra.memo.ui.finance.FinanceScreen
import com.sadra.memo.ui.finance.SmsInboxScreen
import com.sadra.memo.ui.finance.TxEditScreen
import com.sadra.memo.ui.home.HomeScreen
import com.sadra.memo.ui.notes.ArchiveScreen
import com.sadra.memo.ui.notes.NoteEditScreen
import com.sadra.memo.ui.notes.NotesScreen
import com.sadra.memo.ui.notes.TrashScreen
import com.sadra.memo.ui.planner.ClassEditScreen
import com.sadra.memo.ui.planner.PlannerScreen
import com.sadra.memo.ui.planner.TaskEditScreen
import com.sadra.memo.ui.settings.SettingsScreen

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("home", "خانه", Icons.Filled.Home),
    Tab("notes", "یادداشت‌ها", Icons.Filled.Create),
    Tab("planner", "برنامه‌ریز", Icons.Filled.CheckCircle),
    Tab("finance", "مالی", Icons.Filled.ShoppingCart),
    Tab("assistant", "دستیار", Icons.Filled.Star),
    Tab("settings", "تنظیمات", Icons.Filled.Settings)
)

@Composable
fun MemoApp() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val hideChrome = currentRoute != null && tabs.none { it.route == currentRoute }
    val showFab = currentRoute in listOf("home", "notes", "planner", "finance")

    Scaffold(
        floatingActionButton = {
            if (showFab) {
                FloatingActionButton(onClick = {
                    when (currentRoute) {
                        "planner" -> navController.navigate("task_edit/0")
                        "finance" -> navController.navigate("tx_edit/0/0")
                        else -> navController.navigate("note_edit/0")
                    }
                }) {
                    Icon(Icons.Filled.Add, contentDescription = "ثبت سریع")
                }
            }
        },
        bottomBar = {
            if (!hideChrome) NavigationBar {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(padding)
        ) {
            composable("home") {
                HomeScreen(
                    onOpenNote = { id -> navController.navigate("note_edit/$id") },
                    onOpenTask = { id -> navController.navigate("task_edit/$id") }
                )
            }
            composable("notes") {
                NotesScreen(
                    onOpen = { id -> navController.navigate("note_edit/$id") },
                    onOpenTrash = { navController.navigate("trash") },
                    onOpenArchive = { navController.navigate("archive") }
                )
            }
            composable("planner") {
                PlannerScreen(
                    onOpenTask = { id -> navController.navigate("task_edit/$id") },
                    onOpenClass = { id -> navController.navigate("class_edit/$id") }
                )
            }
            composable("finance") {
                FinanceScreen(
                    onAddAccount = { navController.navigate("account_edit/0") },
                    onOpenAccount = { id -> navController.navigate("account/$id") },
                    onAddTx = { type -> navController.navigate("tx_edit/0/$type") },
                    onOpenTx = { id -> navController.navigate("tx_edit/$id/0") },
                    onOpenInbox = { navController.navigate("sms_inbox") }
                )
            }
            composable("sms_inbox") { SmsInboxScreen(onBack = { navController.popBackStack() }) }
            composable("assistant") {
                AssistantScreen(
                    onOpenNote = { id -> navController.navigate("note_edit/$id") },
                    onOpenTask = { id -> navController.navigate("task_edit/$id") }
                )
            }
            composable(
                route = "account/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { entry ->
                val accountId = entry.arguments?.getLong("id") ?: 0L
                AccountDetailScreen(
                    accountId = accountId,
                    onBack = { navController.popBackStack() },
                    onEditAccount = { navController.navigate("account_edit/$accountId") },
                    onOpenTx = { id -> navController.navigate("tx_edit/$id/0") }
                )
            }
            composable(
                route = "account_edit/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { entry ->
                AccountEditScreen(
                    accountId = entry.arguments?.getLong("id") ?: 0L,
                    onClose = { navController.popBackStack() },
                    onDeleted = {
                        // اگه از صفحه جزئیات همین حساب اومده بودیم، یه صفحه بیشتر برمی‌گردیم
                        navController.popBackStack()
                        if (navController.currentDestination?.route == "account/{id}") navController.popBackStack()
                    }
                )
            }
            composable(
                route = "tx_edit/{id}/{type}",
                arguments = listOf(
                    navArgument("id") { type = NavType.LongType },
                    navArgument("type") { type = NavType.IntType }
                )
            ) { entry ->
                TxEditScreen(
                    txId = entry.arguments?.getLong("id") ?: 0L,
                    presetType = entry.arguments?.getInt("type") ?: 0,
                    onClose = { navController.popBackStack() },
                    onAddAccount = { navController.navigate("account_edit/0") }
                )
            }
            composable("settings") { SettingsScreen() }
            composable("trash") { TrashScreen(onBack = { navController.popBackStack() }) }
            composable("archive") {
                ArchiveScreen(
                    onBack = { navController.popBackStack() },
                    onOpen = { id -> navController.navigate("note_edit/$id") }
                )
            }
            composable(
                route = "class_edit/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { entry ->
                ClassEditScreen(
                    classId = entry.arguments?.getLong("id") ?: 0L,
                    onClose = { navController.popBackStack() }
                )
            }
            composable(
                route = "note_edit/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { entry ->
                NoteEditScreen(
                    noteId = entry.arguments?.getLong("id") ?: 0L,
                    onClose = { navController.popBackStack() }
                )
            }
            composable(
                route = "task_edit/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { entry ->
                TaskEditScreen(
                    taskId = entry.arguments?.getLong("id") ?: 0L,
                    onClose = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
private fun ComingSoonScreen(title: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = title + "\nبه‌زودی",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
