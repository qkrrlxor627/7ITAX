package com.ssafy.seveniTax.ui.book

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.ssafy.seveniTax.ui.navigation.Route
import com.ssafy.seveniTax.ui.theme.*
import com.ssafy.seveniTax.viewmodel.BookEntryViewModel
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun BookMemoAddScreen(
    navController: NavController,
    entryId: Long = -1,
    fromPayment: Boolean = false,
    bookEntryRepository: BookEntryViewModel? = null
) {
    var memoText by rememberSaveable { mutableStateOf("") }
    var showSuccess by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var showPhotoDialog by remember { mutableStateOf(false) }
    var photoUriStrings by rememberSaveable { mutableStateOf(listOf<String>()) }
    val attachedPhotos = photoUriStrings.map { Uri.parse(it) }
    val maxLength = 200
    val context = LocalContext.current

    var cameraUriString by rememberSaveable { mutableStateOf<String?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraUriString != null) {
            photoUriStrings = photoUriStrings + cameraUriString!!
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            photoUriStrings = photoUriStrings + uri.toString()
        }
    }

    LaunchedEffect(showSuccess) {
        if (showSuccess) {
            delay(1500L)
            if (fromPayment) {
                navController.navigate(Route.Main.path) {
                    popUpTo(0) { inclusive = true }
                }
            } else {
                navController.popBackStack()
            }
        }
    }

    fun saveNote() {
        if (entryId > 0 && bookEntryRepository != null && memoText.isNotBlank()) {
            isSaving = true
            bookEntryRepository.updateNote(entryId, memoText.trim()) { success ->
                isSaving = false
                if (success) {
                    showSuccess = true
                } else {
                    Toast.makeText(context, "저장에 실패했습니다", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            // entryId 없으면 로컬만 성공 처리
            showSuccess = true
        }
    }

    if (showSuccess) {
        MemoSavedScreen()
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .background(Background)
        ) {
            // 헤더
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(Background),
                contentAlignment = Alignment.Center
            ) {
                if (!fromPayment) {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기",
                            tint = TextPrimary
                        )
                    }
                }
                Text(
                    text = "증빙 내역 추가",
                    style = Typography.titleLarge,
                    color = TextPrimary
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 28.dp)
            ) {
                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "증빙 자료를 추가하시겠어요?",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF343434)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "세금 신고 시 증빙자료로 활용됩니다",
                    fontSize = 13.sp,
                    color = Color(0xFF898989)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 메모 입력
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, Color(0xFFE0E0E0), RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    TextField(
                        value = memoText,
                        onValueChange = { if (it.length <= maxLength) memoText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 80.dp),
                        placeholder = {
                            Text(
                                "증빙에 필요한 내용을 자유롭게 입력하세요",
                                fontSize = 13.sp,
                                color = Color(0xFFCACACA)
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        textStyle = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF343434)
                        )
                    )

                    HorizontalDivider(color = Surface, thickness = 1.dp)

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = buildAnnotatedString {
                                withStyle(SpanStyle(color = LogoPurple)) {
                                    append("${memoText.length}")
                                }
                                append("/$maxLength")
                            },
                            fontSize = 12.sp,
                            color = Color(0xFFCACACA)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 사진 첨부 버튼
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.5.dp, Color(0xFFE0E0E0), RoundedCornerShape(12.dp))
                        .clickable { showPhotoDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("\uD83D\uDCF7", fontSize = 16.sp)
                        Text("사진 첨부하기", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF898989))
                    }
                }

                // 첨부된 사진 목록
                if (attachedPhotos.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        attachedPhotos.forEachIndexed { index, uri ->
                            Box(modifier = Modifier.size(80.dp)) {
                                AsyncImage(
                                    model = uri,
                                    contentDescription = "첨부 사진",
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .align(Alignment.TopEnd)
                                        .offset(x = 4.dp, y = (-4).dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF343434))
                                        .clickable {
                                            photoUriStrings = photoUriStrings.toMutableList().also {
                                                it.removeAt(index)
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "삭제",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 하단 버튼
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 28.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { saveNote() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(15.dp),
                    enabled = !isSaving && (memoText.isNotBlank() || attachedPhotos.isNotEmpty()),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandPurple,
                        contentColor = Color.White,
                        disabledContainerColor = Disabled,
                        disabledContentColor = Color.White
                    )
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("저장", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }

                if (fromPayment) {
                    OutlinedButton(
                        onClick = {
                            navController.navigate(Route.Main.path) {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(15.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                    ) {
                        Text("건너뛰기", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // 사진 선택 다이얼로그
        if (showPhotoDialog) {
            AlertDialog(
                onDismissRequest = { showPhotoDialog = false },
                title = { Text("사진 첨부", fontWeight = FontWeight.Bold) },
                text = { Text("사진을 어디서 가져올까요?") },
                confirmButton = {
                    TextButton(onClick = {
                        showPhotoDialog = false
                        galleryLauncher.launch("image/*")
                    }) {
                        Text("갤러리", color = BrandPurple)
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showPhotoDialog = false
                        val file = File(context.cacheDir, "photo_${System.currentTimeMillis()}.jpg")
                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file
                        )
                        cameraUriString = uri.toString()
                        cameraLauncher.launch(uri)
                    }) {
                        Text("카메라", color = BrandPurple)
                    }
                }
            )
        }
    }
}

@Composable
private fun MemoSavedScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(Background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(BrandPurple),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "저장되었습니다",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "증빙 내역이 거래에 추가되었어요",
            fontSize = 14.sp,
            color = TextSecondary
        )
    }
}
