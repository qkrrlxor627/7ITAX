package com.ssafy.seveniTax.ui.classification

import android.net.Uri
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
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.ssafy.seveniTax.ui.theme.*
import java.io.File

@Composable
fun MemoAddScreen(
    navController: NavController,
    merchantName: String = "스타벅스 강남점",
    amount: String = "5,500원",
    category: String = "복리후생비",
    onSave: (String) -> Unit = {},
    onSkip: () -> Unit = {}
) {
    var memoText by rememberSaveable { mutableStateOf("") }
    var isTipExpanded by remember { mutableStateOf(true) }
    var showPhotoDialog by remember { mutableStateOf(false) }
    var photoUriStrings by rememberSaveable { mutableStateOf(listOf<String>()) }
    val attachedPhotos = photoUriStrings.map { Uri.parse(it) }
    val maxLength = 200
    val context = LocalContext.current

    var cameraUriString by rememberSaveable { mutableStateOf<String?>(null) }
    val cameraUri = cameraUriString?.let { Uri.parse(it) }

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
                .background(Background)
        ) {
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
            Text(
                text = "증빙 자료 추가",
                style = Typography.titleLarge,
                color = TextPrimary,
                modifier = Modifier.align(Alignment.Center)
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
                fontWeight = FontWeight.Normal,
                color = Color(0xFF898989)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 증빙 자료 입력 카드
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
                            text = "증빙에 필요한 내용을 자유롭게 입력하세요",
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
                    textStyle = androidx.compose.ui.text.TextStyle(
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
                Text(
                    text = "사진 첨부하기  +",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF898989)
                )
            }

            // 첨부된 사진 목록
            if (attachedPhotos.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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

            Spacer(modifier = Modifier.height(24.dp))

            // 팁 섹션
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isTipExpanded = !isTipExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "💡 이런 증빙 자료가 도움돼요",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF898989)
                )
                Text(
                    text = if (isTipExpanded) "▲" else "▼",
                    fontSize = 12.sp,
                    color = Color(0xFF898989)
                )
            }

            if (isTipExpanded) {
                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Surface, RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TipRow(category = "접대비", example = "거래처 OOO 대표 식사 (3명)")
                    TipRow(category = "여비교통비", example = "서울→부산 출장 택시비")
                    TipRow(category = "복리후생비", example = "팀 회식 (마케팅팀 5명)")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // 하단 버튼
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 28.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onSkip,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(15.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFE0E0E0)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF343434))
            ) {
                Text("건너뛰기", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }

            Button(
                onClick = { onSave(memoText) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandPurple, contentColor = Color.White)
            ) {
                Text("저장", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
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

@Composable
private fun TipRow(category: String, example: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = category,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = BrandPurple,
            modifier = Modifier.width(76.dp)
        )
        Text(
            text = example,
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            color = Color(0xFF898989)
        )
    }
}
