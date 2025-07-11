package com.example.tabisuke.ui.qrscanner

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.journeyapps.barcodescanner.DefaultDecoderFactory
import androidx.compose.ui.viewinterop.AndroidView
import com.example.tabisuke.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.background

class QRScannerActivity : ComponentActivity() {
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // 権限が許可された場合の処理は不要（Composeで処理）
        } else {
            Toast.makeText(this, "カメラ権限が必要です", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    QRScannerScreen(
                        onBackPressed = { finish() },
                        onGroupIdScanned = { groupId ->
                            setResult(RESULT_OK, intent.putExtra("group_id", groupId))
                            finish()
                        },
                        requestPermission = { permission ->
                            requestPermissionLauncher.launch(permission)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QRScannerScreen(
    onBackPressed: () -> Unit,
    onGroupIdScanned: (String) -> Unit,
    requestPermission: (String) -> Unit
) {
    val context = LocalContext.current
    var hasCameraPermission by remember { mutableStateOf(false) }
    var barcodeView by remember { mutableStateOf<DecoratedBarcodeView?>(null) }
    
    // カメラ権限チェック
    LaunchedEffect(Unit) {
        val permission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        hasCameraPermission = permission == PackageManager.PERMISSION_GRANTED
        
        if (!hasCameraPermission) {
            requestPermission(Manifest.permission.CAMERA)
        }
    }
    
    // 権限が変更されたときの処理
    LaunchedEffect(hasCameraPermission) {
        if (hasCameraPermission && barcodeView != null) {
            barcodeView?.resume()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "QRコード読み取り",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "戻る"
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (hasCameraPermission) {
            // カメラ権限がある場合、ZXingビューを表示
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                AndroidView(
                    factory = { context ->
                        DecoratedBarcodeView(context).apply {
                            decoderFactory = DefaultDecoderFactory()
                            initializeFromIntent(android.content.Intent())
                            decodeContinuous(object : BarcodeCallback {
                                override fun barcodeResult(result: BarcodeResult) {
                                    val scannedText = result.text
                                    if (scannedText.isNotEmpty()) {
                                        onGroupIdScanned(scannedText)
                                    }
                                }
                            })
                            barcodeView = this
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = { view ->
                        // ビューの更新処理
                    }
                )
                
                // オーバーレイ表示
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "QRコードをカメラに向けてください",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.background(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                        ).padding(16.dp)
                    )
                }
            }
        } else {
            // カメラ権限がない場合、説明画面を表示
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "カメラ権限が必要です",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "QRコードを読み取るためにカメラ権限を許可してください",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
    
    // ライフサイクル管理
    DisposableEffect(Unit) {
        onDispose {
            barcodeView?.pause()
        }
    }
} 