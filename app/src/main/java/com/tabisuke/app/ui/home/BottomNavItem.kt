package com.tabisuke.app.ui.home
import com.tabisuke.app.R

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.ui.graphics.vector.ImageVector

data class BottomNavItem(
    val name: String,
    val route: String,
    val icon: ImageVector,
)

val bottomNavItems = listOf(
    BottomNavItem("イベント一覧", "event_list", Icons.Default.List),
    BottomNavItem("メイン", "main_home", Icons.Default.DateRange),
    BottomNavItem("マップ", "map_list", Icons.Default.LocationOn)
)
