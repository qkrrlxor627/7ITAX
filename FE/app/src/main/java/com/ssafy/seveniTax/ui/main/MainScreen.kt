package com.ssafy.seveniTax.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ssafy.seveniTax.ui.home.HomeScreen
import com.ssafy.seveniTax.ui.navigation.Route
import com.ssafy.seveniTax.ui.payment.QrPaymentScreen
import com.ssafy.seveniTax.ui.settings.SettingsScreen
import com.ssafy.seveniTax.ui.theme.BrandPurple
import com.ssafy.seveniTax.ui.theme.TextPrimary
import com.ssafy.seveniTax.ui.theme.TextSecondary
import com.ssafy.seveniTax.viewmodel.MainViewModel

private data class NotificationItem(
    val title: String,
    val message: String,
    val time: String,
    val route: String? = null
)

private data class DrawerItem(
    val title: String,
    val subtitle: String? = null,
    val onClick: () -> Unit
)

private data class DrawerSection(
    val title: String,
    val items: List<DrawerItem>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navController: NavController,
    viewModel: MainViewModel = hiltViewModel()
) {
    var selectedTab by remember { mutableStateOf(BottomTab.HOME) }
    var isSideMenuOpen by remember { mutableStateOf(false) }
    var isNotificationSheetOpen by remember { mutableStateOf(false) }

    // 실제 데이터 기반 알림 생성
    val bookEntryVm: com.ssafy.seveniTax.viewmodel.BookEntryViewModel = hiltViewModel()
    val taxCalendarVm: com.ssafy.seveniTax.viewmodel.TaxCalendarViewModel = hiltViewModel()
    val unconfirmedCount by bookEntryVm.unconfirmedCount.collectAsState()
    val deadlines by taxCalendarVm.deadlines.collectAsState()

    LaunchedEffect(Unit) { bookEntryVm.loadUnconfirmedCount() }

    val notifications = remember(unconfirmedCount, deadlines) {
        buildList {
            if (unconfirmedCount > 0) {
                add(NotificationItem("미분류 거래 ${unconfirmedCount}건", "거래를 분류하면 장부 정확도가 올라갑니다.", "", Route.UnclassifiedList.path))
            }
            deadlines.filter { it.dDay in 0..7 }.sortedBy { it.dDay }.forEach { d ->
                val dText = if (d.dDay == 0) "D-Day" else "D-${d.dDay}"
                add(NotificationItem("${d.taxName} $dText", d.description, "", Route.TaxCalendar.path))
            }
        }
    }

    fun openQrPayment() {
        if (viewModel.isPayEnrolled()) {
            selectedTab = BottomTab.QR_PAYMENT
        } else {
            navController.navigate(Route.PayIntro.path)
        }
    }

    val drawerSections = remember {
        listOf(
            DrawerSection(
                title = "홈",
                items = listOf(
                    DrawerItem("메인 홈") {
                        selectedTab = BottomTab.HOME
                        isSideMenuOpen = false
                    },
                    DrawerItem("AI 세무 도우미") {
                        isSideMenuOpen = false
                        navController.navigate(Route.AiChat.path)
                    },
                    DrawerItem("설정") {
                        selectedTab = BottomTab.SETTINGS
                        isSideMenuOpen = false
                    }
                )
            ),
            DrawerSection(
                title = "세금 일정",
                items = listOf(
                    DrawerItem("전체 세금 일정") {
                        isSideMenuOpen = false
                        navController.navigate(Route.TaxCalendar.path)
                    },
                    DrawerItem("알림 설정") {
                        isSideMenuOpen = false
                        navController.navigate(Route.NotificationSettings.path)
                    }
                )
            ),
            DrawerSection(
                title = "장부",
                items = listOf(
                    DrawerItem("장부 전체 내역") {
                        isSideMenuOpen = false
                        navController.navigate(Route.BookEntryList.path)
                    },
                    DrawerItem("미분류 거래") {
                        isSideMenuOpen = false
                        navController.navigate(Route.UnclassifiedList.path)
                    }
                )
            ),
            DrawerSection(
                title = "세무 리포트",
                items = listOf(
                    DrawerItem("월간 세무리포트", "현재는 동일 리포트 화면으로 연결됩니다.") {
                        isSideMenuOpen = false
                        navController.navigate(Route.TaxReport.path)
                    },
                    DrawerItem("연간 세무리포트", "현재는 동일 리포트 화면으로 연결됩니다.") {
                        isSideMenuOpen = false
                        navController.navigate(Route.TaxReport.path)
                    },
                    DrawerItem("절세 포인트") {
                        isSideMenuOpen = false
                        navController.navigate(Route.TaxSavingsDetail.path)
                    }
                )
            ),
            DrawerSection(
                title = "결제와 카드",
                items = listOf(
                    DrawerItem("QR 결제") {
                        isSideMenuOpen = false
                        openQrPayment()
                    },
                    DrawerItem("카드 관리") {
                        isSideMenuOpen = false
                        navController.navigate(Route.CardList.path)
                    }
                )
            ),
            DrawerSection(
                title = "기타",
                items = listOf(
                    DrawerItem("송금 테스트") {
                        isSideMenuOpen = false
                        navController.navigate(Route.ServerTest.path)
                    }
                )
            )
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                if (!isSideMenuOpen) {
                    BottomTabBar(
                        selectedTab = selectedTab,
                        onTabSelected = { tab ->
                            if (tab == BottomTab.QR_PAYMENT && !viewModel.isPayEnrolled()) {
                                navController.navigate(Route.PayIntro.path)
                            } else {
                                selectedTab = tab
                            }
                        }
                    )
                }
            }
        ) { innerPadding ->
            val modifier = Modifier.padding(innerPadding)
            when (selectedTab) {
                BottomTab.HOME -> HomeScreen(
                    navController = navController,
                    modifier = modifier,
                    onNotificationClick = { isNotificationSheetOpen = true },
                    onMenuClick = { isSideMenuOpen = true },
                    onQrPaymentClick = { openQrPayment() },
                    notificationCount = notifications.size
                )
                BottomTab.SETTINGS -> SettingsScreen(navController, modifier)
                BottomTab.QR_PAYMENT -> QrPaymentScreen(
                    navController = navController,
                    paymentViewModel = hiltViewModel(),
                    onBack = { selectedTab = BottomTab.HOME },
                    modifier = modifier
                )
            }
        }

        AnimatedVisibility(
            visible = isSideMenuOpen,
            enter = fadeIn(animationSpec = tween(180)),
            exit = fadeOut(animationSpec = tween(180))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.28f))
                    .clickable { isSideMenuOpen = false }
            )
        }

        AnimatedVisibility(
            visible = isSideMenuOpen,
            enter = slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(260)
            ) + fadeIn(animationSpec = tween(220)),
            exit = slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(220)
            ) + fadeOut(animationSpec = tween(180)),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            SideMenuSheet(
                sections = drawerSections,
                onClose = { isSideMenuOpen = false }
            )
        }
    }

    if (isNotificationSheetOpen) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { isNotificationSheetOpen = false },
            sheetState = sheetState,
            containerColor = Color.White
        ) {
            NotificationSheetContent(
                notifications = notifications,
                onItemClick = { route ->
                    isNotificationSheetOpen = false
                    navController.navigate(route)
                }
            )
        }
    }
}

@Composable
private fun NotificationSheetContent(notifications: List<NotificationItem>, onItemClick: (String) -> Unit = {}) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .navigationBarsPadding()
    ) {
        Text(
            text = "알림",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "최근 알림과 리마인더를 확인할 수 있습니다.",
            fontSize = 13.sp,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(20.dp))
        notifications.forEachIndexed { index, item ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFFF8F8FB))
                    .then(if (item.route != null) Modifier.clickable { onItemClick(item.route) } else Modifier)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = item.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = item.message,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = item.time,
                        fontSize = 11.sp,
                        color = BrandPurple
                    )
                }
            }
            if (index != notifications.lastIndex) {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun SideMenuSheet(
    sections: List<DrawerSection>,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(320.dp)
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 18.dp)
            .clickable(enabled = false) {}
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "전체 메뉴",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "닫기",
                tint = TextPrimary,
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onClose() }
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        sections.forEachIndexed { sectionIndex, section ->
            Text(
                text = section.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = BrandPurple
            )
            Spacer(modifier = Modifier.height(10.dp))
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F8FB)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    section.items.forEachIndexed { index, item ->
                        DrawerMenuRow(item = item)
                        if (index != section.items.lastIndex) {
                            HorizontalDivider(color = Color(0xFFEAEAF0))
                        }
                    }
                }
            }
            if (sectionIndex != sections.lastIndex) {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun DrawerMenuRow(item: DrawerItem) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { item.onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text(
            text = item.title,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        item.subtitle?.let {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = it,
                fontSize = 12.sp,
                color = TextSecondary
            )
        }
    }
}
