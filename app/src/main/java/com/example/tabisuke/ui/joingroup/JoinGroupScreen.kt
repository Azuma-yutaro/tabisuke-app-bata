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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.Color
import com.example.tabisuke.R

@Composable
fun JoinGroupScreen(navController: NavController, viewModel: JoinGroupViewModel = viewModel()) {
    val groupId by viewModel.groupId.collectAsState()
    val joinError by viewModel.joinError.collectAsState()
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {
        // 背景画像
        Image(
            painter = painterResource(id = R.drawable.back01),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop
        )
        // 画面上部中央に重ねる
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 64.dp, start = 24.dp, end = 24.dp)
                .align(Alignment.TopCenter),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = "共有するたび、絆になる。",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF6A4C93),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "私たちの旅は、ここから始まる",
                fontSize = 14.sp,
                color = Color(0xFF6A4C93).copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            // 入力欄上のラベル
            Text(
                text = "グループを検索",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF333333),
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "グループIDの確認…イベント詳細画面下部などに表示されているIDを共有してもらいましょう",
                fontSize = 10.sp,
                color = Color(0xFF333333).copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            OutlinedTextField(
                value = groupId,
                onValueChange = { viewModel.onGroupIdChange(it) },
                label = { Text("グループID") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(color = Color(0xFF6A4C93))
            )
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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFB8B5FF),
                    contentColor = Color.DarkGray,
                    disabledContainerColor = Color(0xFFE0E0F8),
                    disabledContentColor = Color(0xFF888888)
                ),
                enabled = groupId.isNotBlank()
            ) {
                Text("グループに参加", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            joinError?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = it, color = Color.Red, fontSize = 12.sp)
            }
        }
    }
}
