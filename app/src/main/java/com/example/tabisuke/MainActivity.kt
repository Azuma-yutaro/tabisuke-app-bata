package com.example.tabisuke

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.tabisuke.ui.guestaccess.GuestAccessScreen
import com.example.tabisuke.ui.home.HomeScreen
import com.example.tabisuke.ui.login.LoginScreen
import com.example.tabisuke.ui.management.ManagementScreen
import com.example.tabisuke.ui.main.MainScreen
import com.example.tabisuke.ui.scheduleedit.ScheduleEditScreen
import com.example.tabisuke.ui.theme.TabisukeTheme
import com.example.tabisuke.ui.eventlist.EventListScreen
import com.example.tabisuke.ui.grouplist.GroupListScreen
import com.example.tabisuke.ui.creategroup.CreateGroupScreen
import com.example.tabisuke.ui.createevent.CreateEventScreen
import com.example.tabisuke.ui.joingroup.JoinGroupScreen
import com.example.tabisuke.ui.budget.BudgetScreen
import com.example.tabisuke.ui.groupsettings.GroupSettingsScreen
import com.example.tabisuke.ui.membermanagement.MemberManagementScreen
import com.google.firebase.auth.FirebaseAuth
import android.content.Context
import android.content.SharedPreferences
import com.example.tabisuke.ui.welcome.WelcomeScreen
import com.example.tabisuke.ui.terms.TermsScreen
import com.example.tabisuke.utils.NetworkUtils
import com.example.tabisuke.utils.OfflineCache
import com.example.tabisuke.ui.mypage.MyPageScreen

@Composable
fun AppNavigator() {
    val navController = rememberNavController()
    var isLoggedIn by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser != null) }
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("tabisuke_prefs", Context.MODE_PRIVATE) }

    // NavControllerの遷移ごとに現在のルートを保存
    LaunchedEffect(navController) {
        navController.addOnDestinationChangedListener { _, destination, arguments ->
            val route = destination.route
            // パラメータ付きルートの場合はargumentsから埋める
            val fullRoute = if (route != null && arguments != null) {
                var r: String = route
                arguments.keySet().forEach { key ->
                    r = r.replace("{$key}", arguments.getString(key) ?: "")
                }
                r
            } else route ?: ""
            // リスト系・管理系画面は保存しない
            val isListOrManagement = fullRoute.startsWith("group_list") ||
                fullRoute.startsWith("event_list/") ||
                fullRoute.startsWith("group_settings/") ||
                fullRoute.startsWith("management/")
            if (isListOrManagement) {
                prefs.edit().putString("last_route", "").apply()
            } else {
                prefs.edit().putString("last_route", fullRoute).apply()
            }
        }
    }

    LaunchedEffect(Unit) {
        FirebaseAuth.getInstance().addAuthStateListener {
            isLoggedIn = it.currentUser != null
        }
    }

    val allowedStartRoutes = listOf(
        "group_list",
        "home/", "main/", "schedule_edit/", "schedule_edit/", "management/", "budget/", "guest_access/"
    )
    val lastRoute = prefs.getString("last_route", null)
    val isValidLastRoute = lastRoute != null && lastRoute.isNotBlank() && allowedStartRoutes.any { lastRoute.startsWith(it) }
    // 初回起動判定
    val agreedToTerms = prefs.getBoolean("agreed_to_terms", false)
    val completedWelcome = prefs.getBoolean("completed_welcome", false)
    val staticStartDestination = when {
        !agreedToTerms -> "terms"
        !completedWelcome -> "welcome"
        isLoggedIn -> "group_list"
        else -> "login"
    }

    // NavHostの直後にlast_routeが有効ならnavigate（同意済み・welcome済み時のみ）
    LaunchedEffect(isLoggedIn, agreedToTerms, completedWelcome) {
        if (isLoggedIn && agreedToTerms && completedWelcome && isValidLastRoute && lastRoute != "group_list") {
            navController.navigate(lastRoute!!) {
                popUpTo("group_list") { inclusive = false }
                launchSingleTop = true
            }
        }
    }

    NavHost(navController = navController, startDestination = staticStartDestination) {
        composable("terms") { TermsScreen(navController, prefs) }
        composable("welcome") { WelcomeScreen(navController, prefs) }
        composable("login") { LoginScreen(navController) }
        composable("group_list") { GroupListScreen(navController) }
        composable("create_group") { CreateGroupScreen(navController) }
        composable("join_group") { JoinGroupScreen(navController) }
        composable("event_list/{groupId}") { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
            EventListScreen(navController = navController, groupId = groupId)
        }
        composable("mypage") { MyPageScreen(navController) }
        composable("create_event/{groupId}") { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
            CreateEventScreen(navController = navController, groupId = groupId)
        }
        composable("home/{groupId}/{eventId}") { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
            val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
            HomeScreen(navController = navController, groupId = groupId, eventId = eventId)
        }
        composable("main/{groupId}/{eventId}") { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
            val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
            MainScreen(navController = navController, groupId = groupId, eventId = eventId)
        }
        composable("schedule_edit/{groupId}/{eventId}") { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
            val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
            ScheduleEditScreen(navController = navController, groupId = groupId, eventId = eventId)
        }
        composable("schedule_edit/{groupId}/{eventId}/{scheduleId}") { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
            val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
            val scheduleId = backStackEntry.arguments?.getString("scheduleId") ?: ""
            ScheduleEditScreen(navController = navController, groupId = groupId, eventId = eventId, scheduleId = scheduleId)
        }
        composable("management/{groupId}/{eventId}") { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
            val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
            ManagementScreen(navController = navController, groupId = groupId, eventId = eventId)
        }
        composable("budget/{groupId}/{eventId}") { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
            val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
            BudgetScreen(navController = navController, groupId = groupId, eventId = eventId)
        }
        composable("guest_access/{groupId}/{eventId}") { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
            val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
            GuestAccessScreen(navController = navController, groupId = groupId, eventId = eventId)
        }
        composable("group_settings/{groupId}") { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
            GroupSettingsScreen(navController = navController, groupId = groupId)
        }
        composable("member_management/{groupId}") { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
            MemberManagementScreen(navController = navController, groupId = groupId)
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // オフライン対応の初期化
        NetworkUtils.initialize(this)
        OfflineCache.initialize(this)
        
        setContent {
            TabisukeTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppNavigator()
                }
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        NetworkUtils.cleanup()
    }
}