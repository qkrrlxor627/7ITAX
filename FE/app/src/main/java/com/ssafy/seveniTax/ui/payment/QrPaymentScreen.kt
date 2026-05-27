package com.ssafy.seveniTax.ui.payment

import android.Manifest
import android.graphics.Bitmap
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.ssafy.seveniTax.ui.navigation.Route
import com.ssafy.seveniTax.ui.theme.*
import com.ssafy.seveniTax.viewmodel.CardViewModel
import com.ssafy.seveniTax.viewmodel.PaymentViewModel
import kotlinx.coroutines.delay
import java.util.concurrent.Executors

@Composable
fun QrPaymentScreen(
    navController: NavController,
    cardViewModel: CardViewModel = hiltViewModel(),
    paymentViewModel: PaymentViewModel,
    showBackButton: Boolean = true,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val cardUiState by cardViewModel.uiState.collectAsState()
    val paymentState by paymentViewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedCard by remember { mutableIntStateOf(0) }
    var scannedResult by remember { mutableStateOf<String?>(null) }
    var showPayConfirmDialog by remember { mutableStateOf(false) }
    var scannedToken by remember { mutableStateOf("") }
    var showResultDialog by remember { mutableStateOf(false) }
    var dialogTitle by remember { mutableStateOf("") }
    var dialogMessage by remember { mutableStateOf("") }
    var dialogIsSuccess by remember { mutableStateOf(false) }

    // 가맹점 목록 로드
    LaunchedEffect(Unit) {
        paymentViewModel.loadMerchants()
    }

    // 결제 완료 감지 → 증빙 내역 추가 페이지로 이동
    LaunchedEffect(paymentState.paymentComplete) {
        if (paymentState.paymentComplete) {
            scannedResult = null
            showPayConfirmDialog = false
            showResultDialog = false
            val paymentId = paymentState.paymentId ?: -1L
            paymentViewModel.resetPayment()
            navController.navigate(Route.BookMemoAdd.create(entryId = paymentId, fromPayment = true)) {
                popUpTo(Route.Main.path) { inclusive = false }
            }
        }
    }

    // 에러 감지
    LaunchedEffect(paymentState.errorMessage) {
        if (paymentState.errorMessage.isNotEmpty()) {
            dialogTitle = "결제 실패"
            dialogMessage = paymentState.errorMessage
            dialogIsSuccess = false
            showResultDialog = true
            paymentViewModel.clearError()
        }
    }

    // QR 스캔 결과 팝업
    if (scannedResult != null && !showPayConfirmDialog && !showResultDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = {
                scannedResult = null
                paymentViewModel.resetPayment()
            },
            title = {
                if (paymentState.isLoading) {
                    Text("결제 정보 조회 중", fontWeight = FontWeight.Bold)
                } else {
                    Text("결제 정보", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                if (paymentState.isLoading) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.CircularProgressIndicator(
                            color = BrandPurple,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        Text("조회 중...", fontSize = 14.sp, color = TextSecondary)
                    }
                } else if (paymentState.amount > 0) {
                    Column {
                        Text(paymentState.payerName, fontSize = 14.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "${java.text.NumberFormat.getNumberInstance().format(paymentState.amount)}원",
                            fontSize = 24.sp, fontWeight = FontWeight.Bold, color = BrandPurple
                        )
                    }
                } else {
                    Text("결제 정보를 불러올 수 없습니다.", fontSize = 14.sp, color = TextSecondary)
                }
            },
            confirmButton = {
                if (!paymentState.isLoading && paymentState.amount > 0) {
                    androidx.compose.material3.TextButton(onClick = {
                        showPayConfirmDialog = true
                    }) {
                        Text("결제하기", color = BrandPurple, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = {
                    scannedResult = null
                    paymentViewModel.resetPayment()
                }) {
                    Text(if (paymentState.isLoading) "취소" else "다시 스캔", color = TextSecondary)
                }
            }
        )
    }

    // 결제 확인 다이얼로그
    if (showPayConfirmDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showPayConfirmDialog = false },
            title = { Text("결제 확인", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("아래 결제를 진행하시겠습니까?", fontSize = 14.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("가맹점: ${paymentState.payerName}", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text("금액: ${java.text.NumberFormat.getNumberInstance().format(paymentState.amount)}원", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = BrandPurple)
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    showPayConfirmDialog = false
                    paymentViewModel.confirmQrPayment(scannedToken)
                }) {
                    Text("결제하기", color = BrandPurple, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = {
                    showPayConfirmDialog = false
                    scannedResult = null
                }) {
                    Text("취소", color = TextSecondary)
                }
            }
        )
    }

    if (showResultDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = {
                showResultDialog = false
                scannedResult = null
                paymentViewModel.resetPayment()
            },
            title = {
                Text(
                    dialogTitle,
                    fontWeight = FontWeight.Bold,
                    color = if (dialogIsSuccess) Color(0xFF3DBDA2) else Color(0xFFE8475A)
                )
            },
            text = { Text(dialogMessage) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    showResultDialog = false
                    scannedResult = null
                    paymentViewModel.resetPayment()
                }) {
                    Text("확인", color = BrandPurple)
                }
            }
        )
    }
    var cameraPermissionGranted by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        cameraPermissionGranted = granted
    }

    LaunchedEffect(selectedTab) {
        if (selectedTab == 1 && !cameraPermissionGranted) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onBack?.invoke() ?: navController.popBackStack() }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "뒤로가기",
                    tint = TextPrimary
                )
            }
            Text(
                text = "Pay 결제",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .padding(horizontal = 48.dp)
                .fillMaxWidth()
                .border(1.dp, Divider, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
        ) {
            TabButton("바코드", selectedTab == 0, Modifier.weight(1f)) {
                selectedTab = 0
                scannedResult = null
            }
            TabButton("QR스캔", selectedTab == 1, Modifier.weight(1f)) {
                selectedTab = 1
                scannedResult = null
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, Divider, RoundedCornerShape(16.dp))
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            if (selectedTab == 0) {
                val card = cardUiState.cards.getOrNull(selectedCard)

                // 카드 선택 + 가맹점 로드 완료 시 QR 토큰 생성
                val selectedMerchant = paymentState.selectedMerchant
                LaunchedEffect(selectedCard, card, selectedMerchant) {
                    card?.let {
                        if (selectedMerchant != null) {
                            paymentViewModel.createQrToken(
                                cardId = it.id.toLongOrNull() ?: 0,
                                amount = 1000,
                                merchantId = selectedMerchant.merchantId,
                                merchantName = selectedMerchant.merchantName
                            )
                        }
                    }
                }

                // QR 토큰 생성 후 결제 상태 폴링
                LaunchedEffect(paymentState.qrToken) {
                    if (paymentState.qrToken.isNotEmpty()) {
                        paymentViewModel.startPollingStatus(paymentState.qrToken)
                    }
                }

                var qrTimestamp by remember { mutableLongStateOf(System.currentTimeMillis() / 1000) }
                var remainingSeconds by remember { mutableIntStateOf(60) }
                LaunchedEffect(selectedCard) {
                    qrTimestamp = System.currentTimeMillis() / 1000
                    remainingSeconds = 60
                    while (true) {
                        delay(1_000L)
                        remainingSeconds--
                        if (remainingSeconds <= 0) {
                            qrTimestamp = System.currentTimeMillis() / 1000
                            remainingSeconds = 60
                        }
                    }
                }
                val qrData = if (paymentState.qrToken.isNotEmpty()) paymentState.qrToken
                    else "PAY-${card?.cardNumber?.takeLast(4) ?: "0000"}-$qrTimestamp"
                val qrBitmap = remember(qrData) { generateQrCode(qrData) }

                if (qrBitmap != null) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "QR코드",
                            modifier = Modifier.width(200.dp).height(200.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = card?.cardNumber ?: "카드를 등록해주세요",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = String.format("%02d:%02d", remainingSeconds / 60, remainingSeconds % 60),
                            fontSize = 11.sp,
                            color = if (remainingSeconds <= 10) Color(0xFFE53935) else TextSecondary
                        )
                    }
                } else {
                    Text("QR코드 생성 실패", color = TextSecondary, fontSize = 14.sp)
                }
            } else {
                if (cameraPermissionGranted) {
                    QrScannerView(
                        onQrScanned = { result ->
                            scannedResult = result
                            scannedToken = result
                            // 스캔한 토큰으로 결제 정보 조회
                            paymentViewModel.lookupQrPayment(result)
                        }
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("카메라 권한이 필요합니다", color = TextSecondary, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(BrandPurple)
                                .clickable {
                                    permissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                                .padding(horizontal = 24.dp, vertical = 10.dp)
                        ) {
                            Text("권한 허용", color = Color.White, fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .navigationBarsPadding()
                .padding(bottom = 32.dp)
        ) {
            items(cardUiState.cards.size) { index ->
                val card = cardUiState.cards[index]
                val isSelected = index == selectedCard
                val animatedWidth by animateDpAsState(
                    targetValue = if (isSelected) 116.dp else 100.dp,
                    animationSpec = tween(200),
                    label = "cardWidth"
                )
                val animatedHeight by animateDpAsState(
                    targetValue = if (isSelected) 160.dp else 140.dp,
                    animationSpec = tween(200),
                    label = "cardHeight"
                )
                val animatedElevation by animateDpAsState(
                    targetValue = if (isSelected) 12.dp else 0.dp,
                    animationSpec = tween(200),
                    label = "cardElevation"
                )
                Box(
                    modifier = Modifier
                        .width(animatedWidth)
                        .height(animatedHeight)
                        .shadow(
                            elevation = animatedElevation,
                            shape = RoundedCornerShape(12.dp),
                            clip = false
                        )
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (card.type == "personal") CardGold else CardBlue)
                        .clickable { selectedCard = index }
                        .padding(12.dp),
                    contentAlignment = Alignment.BottomStart
                ) {
                    val typeName = if (card.type == "personal") "개인 카드" else "사업자 카드"
                    Column {
                        Text(typeName, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                        Text(card.cardNumber.takeLast(17), fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f))
                    }
                }
            }

            item {
                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .height(140.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(2.dp, Divider, RoundedCornerShape(12.dp))
                        .clickable { navController.navigate(Route.CardTypeSelect.path) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Light,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun QrScannerView(
    onQrScanned: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    var hasScanned by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                val scanner = BarcodeScanning.getClient()

                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    val mediaImage = imageProxy.image
                    if (mediaImage != null && !hasScanned) {
                        val inputImage = InputImage.fromMediaImage(
                            mediaImage,
                            imageProxy.imageInfo.rotationDegrees
                        )
                        scanner.process(inputImage)
                            .addOnSuccessListener { barcodes ->
                                val barcode = barcodes.firstOrNull()
                                if (barcode != null && !hasScanned) {
                                    hasScanned = true
                                    val value = barcode.rawValue ?: "알 수 없는 데이터"
                                    onQrScanned(value)
                                }
                            }
                            .addOnCompleteListener {
                                imageProxy.close()
                            }
                    } else {
                        imageProxy.close()
                    }
                }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                    )
                } catch (e: Exception) {
                    Log.e("QrScanner", "카메라 바인딩 실패", e)
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun TabButton(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clickable(onClick = onClick)
            .background(if (selected) TextPrimary else Color.Transparent)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) Color.White else TextSecondary,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

private fun generateQrCode(data: String): Bitmap? {
    return try {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(data, BarcodeFormat.QR_CODE, 512, 512)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(
                    x,
                    y,
                    if (bitMatrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                )
            }
        }
        bitmap
    } catch (e: Exception) {
        Log.e("QrCode", "QR코드 생성 실패", e)
        null
    }
}
