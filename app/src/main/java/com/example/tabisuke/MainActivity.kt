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
import com.google.firebase.auth.FirebaseAuth

@Composable
fun AppNavigator() {
    val navController = rememberNavController()
    var isLoggedIn by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser != null) }

    LaunchedEffect(Unit) {
        FirebaseAuth.getInstance().addAuthStateListener {
            isLoggedIn = it.currentUser != null
        }
    }

    val startDestination = if (isLoggedIn) {
        "group_list" // ログイン済みの場合はグループ一覧から開始
    } else {
        // ディープリンクの処理
        val intent = (LocalContext.current as ComponentActivity).intent
        val appLinkData: Uri? = intent.data
        
        when {
            // ゲストアクセスURL: https://tabisuke/guest_access/{groupId}/{eventId}
            appLinkData != null && appLinkData.pathSegments.size >= 3 && appLinkData.pathSegments[0] == "guest_access" -> {
                val groupId = appLinkData.pathSegments[1]
                val eventId = appLinkData.pathSegments[2]
                "guest_access/${groupId}/${eventId}"
            }
            // イベント直接アクセス: https://tabisuke/{eventId}
            appLinkData != null && appLinkData.pathSegments.size == 1 -> {
                val eventId = appLinkData.pathSegments[0]
                // イベントIDからグループIDを取得する必要があるが、ここでは仮の処理
                "guest_access/temp_group/${eventId}"
            }
            else -> "login"
        }
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable("login") { LoginScreen(navController) }
        composable("group_list") { GroupListScreen(navController) }
        composable("create_group") { CreateGroupScreen(navController) }
        composable("join_group") { JoinGroupScreen(navController) }
        composable("event_list/{groupId}") { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
            EventListScreen(navController = navController, groupId = groupId)
        }
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
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TabisukeTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppNavigator()
                }
            }
        }
    }
}