package com.example.tabisuke.ui.joingroup

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

@Composable
fun JoinGroupScreen(navController: NavController, viewModel: JoinGroupViewModel = viewModel()) {
    val groupId by viewModel.groupId.collectAsState()
    val joinError by viewModel.joinError.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = groupId,
            onValueChange = { viewModel.onGroupIdChange(it) },
            label = { Text("グループID") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                viewModel.joinGroup(
                    onSuccess = { groupId, eventId ->
                        Toast.makeText(context, "グループに参加しました！", Toast.LENGTH_SHORT).show()
                        navController.navigate("home/${groupId}/${eventId}") {
                            popUpTo("event_list") { inclusive = true }
                        }
                    }
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("グループに参加")
        }

        joinError?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = it, color = androidx.compose.material3.MaterialTheme.colorScheme.error)
        }
    }
}
