package com.example.tabisuke.ui.home

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.tabisuke.ui.main.MainScreen
import com.example.tabisuke.ui.main.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController, groupId: String, eventId: String) {
    val mainViewModel: MainViewModel = viewModel()
    val event by mainViewModel.event.collectAsState()
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current

    // Fetch event details for map URL
    mainViewModel.fetchEvent(groupId, eventId)

    Scaffold { padding ->
        MainScreen(
            navController = navController,
            groupId = groupId,
            eventId = eventId,
            modifier = Modifier.padding(padding)
        )
    }
}
