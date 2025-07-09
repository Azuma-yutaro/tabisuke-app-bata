package com.example.tabisuke.ui.creategroup

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
fun CreateGroupScreen(navController: NavController, viewModel: CreateGroupViewModel = viewModel()) {
    val groupName by viewModel.groupName.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = groupName,
            onValueChange = { viewModel.onGroupNameChange(it) },
            label = { Text("グループ名") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                viewModel.createGroup(
                    onSuccess = { groupId ->
                        Toast.makeText(context, "グループを作成しました！", Toast.LENGTH_SHORT).show()
                        navController.navigate("group_list") {
                            popUpTo("create_group") { inclusive = true }
                        }
                    },
                    onFailure = { e ->
                        Toast.makeText(context, "グループ作成に失敗しました: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("グループを作成")
        }
    }
}
