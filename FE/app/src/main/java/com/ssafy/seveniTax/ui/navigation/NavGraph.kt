package com.ssafy.seveniTax.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.ssafy.seveniTax.ui.ai.AiScreen
import com.ssafy.seveniTax.ui.main.AiChatFab
import com.ssafy.seveniTax.ui.auth.AuthSuccessScreen
import com.ssafy.seveniTax.ui.auth.IdentityVerificationScreen
import com.ssafy.seveniTax.ui.auth.PinConfirmScreen
import com.ssafy.seveniTax.ui.auth.PinLoginScreen
import com.ssafy.seveniTax.ui.auth.PinSetupScreen
import com.ssafy.seveniTax.ui.auth.SmsAuthScreen
import com.ssafy.seveniTax.ui.auth.SplashScreen
import com.ssafy.seveniTax.ui.card.CardAccountSelectScreen
import com.ssafy.seveniTax.ui.card.CardBusinessInfoScreen
import com.ssafy.seveniTax.ui.card.CardProductSelectScreen
import com.ssafy.seveniTax.ui.card.CardChangeScreen
import com.ssafy.seveniTax.ui.card.CardCompleteScreen
import com.ssafy.seveniTax.ui.card.CardDetailScreen
import com.ssafy.seveniTax.ui.card.CardInputScreen
import com.ssafy.seveniTax.ui.card.CardListScreen
import com.ssafy.seveniTax.ui.card.CardOwnerVerifyScreen
import com.ssafy.seveniTax.ui.card.CardSmsScreen
import com.ssafy.seveniTax.ui.card.CardTypeSelectScreen
import com.ssafy.seveniTax.ui.classification.AutoClassificationScreen
import com.ssafy.seveniTax.ui.classification.CategorySelectScreen
import com.ssafy.seveniTax.ui.classification.ClassificationCompleteScreen
import com.ssafy.seveniTax.ui.classification.ClassificationLoadingScreen
import com.ssafy.seveniTax.ui.classification.ClassificationResultScreen
import com.ssafy.seveniTax.ui.classification.MemoAddScreen
import com.ssafy.seveniTax.ui.classification.BulkClassificationLoadingScreen
import com.ssafy.seveniTax.ui.classification.UnclassifiedListScreen
import com.ssafy.seveniTax.ui.main.MainScreen
import com.ssafy.seveniTax.ui.pay.PayBusinessInfoScreen
import com.ssafy.seveniTax.ui.pay.PayCompleteScreen
import com.ssafy.seveniTax.ui.pay.PayConfirmScreen
import com.ssafy.seveniTax.ui.pay.PayIntroScreen
import com.ssafy.seveniTax.ui.pay.PayTermsScreen
import com.ssafy.seveniTax.ui.pay.PayVerifyScreen
import com.ssafy.seveniTax.ui.payment.PaymentCompleteScreen
import com.ssafy.seveniTax.ui.payment.PaymentProcessingScreen
import com.ssafy.seveniTax.ui.payment.QrPaymentScreen
import com.ssafy.seveniTax.ui.book.BookEntryDetailScreen
import com.ssafy.seveniTax.ui.book.BookEntryListScreen
import com.ssafy.seveniTax.ui.book.BookFilterScreen
import com.ssafy.seveniTax.ui.book.BookMemoAddScreen
import com.ssafy.seveniTax.ui.book.ExportDateRangeScreen
import com.ssafy.seveniTax.ui.book.ExportFormatScreen
import com.ssafy.seveniTax.ui.book.ExportPurposeScreen
import com.ssafy.seveniTax.ui.book.TaxReportScreen
import com.ssafy.seveniTax.ui.book.TaxSavingsDetailScreen
import com.ssafy.seveniTax.ui.calendar.NotificationSettingsScreen
import com.ssafy.seveniTax.ui.calendar.TaxCalendarDetailScreen
import com.ssafy.seveniTax.ui.calendar.TaxCalendarScreen
import com.ssafy.seveniTax.ui.test.ServerTestScreen
import com.ssafy.seveniTax.viewmodel.AuthViewModel
import com.ssafy.seveniTax.viewmodel.BookEntryViewModel
import com.ssafy.seveniTax.viewmodel.BulkEntryInput
import com.ssafy.seveniTax.viewmodel.CardViewModel
import com.ssafy.seveniTax.viewmodel.ClassificationViewModel
import com.ssafy.seveniTax.viewmodel.PaymentViewModel
import com.ssafy.seveniTax.viewmodel.TaxCalendarViewModel

private const val AUTH_GRAPH_ROUTE = "auth_graph"

@Composable
fun NavGraph(navController: NavHostController, pendingNavigateTo: String? = null) {
    val cardViewModel: CardViewModel = hiltViewModel()
    val taxCalendarViewModel: TaxCalendarViewModel = hiltViewModel()
    val bookEntryViewModel: BookEntryViewModel = hiltViewModel()
    val paymentViewModel: PaymentViewModel = hiltViewModel()
    val classificationViewModel: ClassificationViewModel = hiltViewModel()

    // FAB을 auth 화면과 AI 챗봇 화면에서는 숨김
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute by remember {
        derivedStateOf { navBackStackEntry?.destination?.route }
    }
    val hideFabRoutes = setOf(
        // auth
        Route.Splash.path, Route.PinLogin.path, Route.IdentityVerify.path,
        Route.SmsAuth.path, Route.PinSetup.path, Route.PinConfirm.path,
        Route.AuthSuccess.path,
        // AI 챗봇
        Route.AiChat.path,
        // 하단 버튼이 있는 화면
        Route.BookMemoAdd.path,
        Route.PaymentComplete.path,
        Route.ExportFormat.path,
        Route.ExportDateRange.path,
        Route.ExportPurpose.path,
        Route.PayIntro.path, Route.PayTerms.path, Route.PayVerify.path,
        Route.PayConfirm.path, Route.PayComplete.path,
        Route.CardInput.path, Route.CardSms.path, Route.CardComplete.path,
        Route.CardOwnerVerify.path, Route.CardBusinessInfo.path,
        Route.ClassificationLoading.path, Route.ClassificationResult.path,
        Route.ClassificationComplete.path, Route.CategorySelect.path,
        Route.MemoAdd.path, Route.BulkClassificationLoading.path
    )
    val showFab = currentRoute != null && currentRoute !in hideFabRoutes

    Box(modifier = Modifier.fillMaxSize()) {
    NavHost(
        navController = navController,
        startDestination = AUTH_GRAPH_ROUTE
    ) {
        composable(Route.ServerTest.path) {
            ServerTestScreen(navController)
        }

        navigation(
            startDestination = Route.Splash.path,
            route = AUTH_GRAPH_ROUTE
        ) {
            composable(Route.Splash.path) { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(AUTH_GRAPH_ROUTE)
                }
                val viewModel: AuthViewModel = hiltViewModel(parentEntry)
                SplashScreen(navController, viewModel)
            }

            composable(Route.PinLogin.path) { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(AUTH_GRAPH_ROUTE)
                }
                val viewModel: AuthViewModel = hiltViewModel(parentEntry)
                PinLoginScreen(navController, viewModel)
            }

            composable(Route.IdentityVerify.path) { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(AUTH_GRAPH_ROUTE)
                }
                val viewModel: AuthViewModel = hiltViewModel(parentEntry)
                IdentityVerificationScreen(navController, viewModel)
            }

            composable(Route.SmsAuth.path) { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(AUTH_GRAPH_ROUTE)
                }
                val viewModel: AuthViewModel = hiltViewModel(parentEntry)
                SmsAuthScreen(navController, viewModel)
            }

            composable(Route.PinSetup.path) { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(AUTH_GRAPH_ROUTE)
                }
                val viewModel: AuthViewModel = hiltViewModel(parentEntry)
                PinSetupScreen(navController, viewModel)
            }

            composable(Route.PinConfirm.path) { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(AUTH_GRAPH_ROUTE)
                }
                val viewModel: AuthViewModel = hiltViewModel(parentEntry)
                PinConfirmScreen(navController, viewModel)
            }

            composable(Route.AuthSuccess.path) {
                AuthSuccessScreen(navController)
            }
        }

        composable(Route.Main.path) {
            if (pendingNavigateTo != null) {
                LaunchedEffect(Unit) {
                    when (pendingNavigateTo) {
                        "classification_result" -> navController.navigate(Route.ClassificationLoading.path)
                        "tax_calendar" -> navController.navigate(Route.TaxCalendar.path)
                    }
                }
            }
            MainScreen(navController)
        }

        composable(Route.PayIntro.path) {
            PayIntroScreen(navController)
        }

        composable(Route.PayBusinessInfo.path) {
            PayBusinessInfoScreen(navController)
        }

        composable(Route.PayTerms.path) {
            PayTermsScreen(navController)
        }

        composable(Route.PayConfirm.path) {
            PayConfirmScreen(navController)
        }

        composable(Route.PayVerify.path) {
            PayVerifyScreen(
                onBack = { navController.popBackStack() },
                onNext = { navController.navigate(Route.PayComplete.path) }
            )
        }

        composable(Route.PayComplete.path) {
            PayCompleteScreen(navController)
        }

        composable(Route.QrPayment.path) {
            QrPaymentScreen(navController, cardViewModel, paymentViewModel)
        }

        composable(Route.PaymentProcessing.path) {
            PaymentProcessingScreen(navController)
        }

        composable(Route.PaymentComplete.path) {
            PaymentCompleteScreen(navController, paymentViewModel)
        }

        composable(Route.CardList.path) {
            CardListScreen(navController, cardViewModel)
        }

        composable(Route.CardTypeSelect.path) {
            CardTypeSelectScreen(navController, cardViewModel)
        }

        composable(
            route = Route.CardInput.path,
            arguments = listOf(navArgument("cardType") { type = NavType.StringType })
        ) { backStackEntry ->
            val cardType = backStackEntry.arguments?.getString("cardType") ?: "personal"
            CardInputScreen(navController, cardViewModel, cardType)
        }

        composable(Route.CardAccountSelect.path) {
            CardAccountSelectScreen(navController, cardViewModel)
        }

        composable(Route.CardProductSelect.path) {
            CardProductSelectScreen(navController, cardViewModel)
        }

        composable(Route.CardBusinessInfo.path) {
            CardBusinessInfoScreen(navController)
        }

        composable(Route.CardOwnerVerify.path) {
            CardOwnerVerifyScreen(navController)
        }

        composable(Route.CardSms.path) {
            CardSmsScreen(navController, cardViewModel)
        }

        composable(Route.CardComplete.path) {
            CardCompleteScreen(navController, cardViewModel)
        }

        composable(Route.CardChange.path) {
            CardChangeScreen(navController, cardViewModel)
        }

        composable(
            route = Route.CardDetail.path,
            arguments = listOf(navArgument("cardId") { type = NavType.StringType })
        ) { backStackEntry ->
            val cardId = backStackEntry.arguments?.getString("cardId").orEmpty()
            CardDetailScreen(navController, cardViewModel, cardId)
        }

        composable(Route.ClassificationLoading.path) {
            ClassificationLoadingScreen(
                navController = navController,
                classificationViewModel = classificationViewModel,
                onComplete = {
                    navController.navigate(Route.ClassificationResult.path) {
                        popUpTo(Route.ClassificationLoading.path) { inclusive = true }
                    }
                }
            )
        }

        composable(Route.ClassificationResult.path) {
            val uiState by classificationViewModel.uiState.collectAsState()
            ClassificationResultScreen(
                navController = navController,
                onConfirm = {
                    // AI 추천 경비로 확정 → API 호출
                    val result = uiState.result
                    if (result != null) {
                        classificationViewModel.setSelectedCategory(result.taxCategory)
                    }
                    if (uiState.entryId > 0 && result != null) {
                        bookEntryViewModel.updateEntryCategory(uiState.entryId, result.taxCategory)
                    }
                    navController.navigate(Route.ClassificationComplete.create())
                },
                onChangeCategory = {
                    // 세목 변경 → 카테고리 선택 페이지 (entryId 전달)
                    navController.navigate(Route.CategorySelect.create("classify", uiState.entryId))
                }
            )
        }

        composable(
            route = Route.CategorySelect.path,
            arguments = listOf(
                navArgument("returnTo") { type = NavType.StringType; defaultValue = "" },
                navArgument("entryId") { type = NavType.LongType; defaultValue = -1L }
            )
        ) { backStackEntry ->
            val returnTo = backStackEntry.arguments?.getString("returnTo") ?: ""
            val entryId = backStackEntry.arguments?.getLong("entryId") ?: -1L
            // AI 일괄 분석 결과가 있는 상태에서 개별 수정인지 판별
            val hasBulkResults = classificationViewModel.bulkResults.value.any { it.isDone }
            val isUnclassifiedEdit = returnTo == "unclassified" && hasBulkResults

            CategorySelectScreen(
                navController = navController,
                onCategorySelected = { category ->
                    if (entryId > 0 && isUnclassifiedEdit) {
                        // AI 일괄 분석 후 개별 수정: 서버 확정 + bulkResults에서 제거
                        classificationViewModel.removeBulkEntry(entryId)
                        bookEntryViewModel.updateEntryCategory(entryId, category)
                        navController.popBackStack()
                    } else if (entryId > 0) {
                        // 완료 화면에 표시할 정보 세팅
                        val entry = bookEntryViewModel.entries.value.find { it.id == entryId }
                        if (entry != null) {
                            val amt = when (entry.entryType) {
                                "INCOME" -> entry.incomeAmount
                                "EXPENSE" -> entry.expenseAmount
                                "ASSET" -> entry.fixedAssetAmount
                                else -> 0L
                            }
                            classificationViewModel.setEntryInfo(
                                entryId = entryId,
                                merchantName = entry.merchantName ?: entry.description ?: "거래",
                                amount = amt,
                                dateTime = entry.createdAt.take(16).replace("T", " ")
                            )
                        }
                        classificationViewModel.setSelectedCategory(category)
                        bookEntryViewModel.updateEntryCategory(entryId, category)
                        if (returnTo == "book") {
                            navController.navigate(Route.ClassificationComplete.create("book")) {
                                popUpTo(Route.BookEntryList.path) { inclusive = false }
                            }
                        } else {
                            navController.navigate(Route.ClassificationComplete.create("unclassified")) {
                                popUpTo(Route.UnclassifiedList.path) { inclusive = false }
                            }
                        }
                    } else {
                        navController.navigate(Route.MemoAdd.path)
                    }
                }
            )
        }

        composable(Route.MemoAdd.path) {
            MemoAddScreen(
                navController = navController,
                onSave = { navController.navigate(Route.ClassificationComplete.create()) },
                onSkip = { navController.navigate(Route.ClassificationComplete.create()) }
            )
        }

        composable(
            route = Route.ClassificationComplete.path,
            arguments = listOf(navArgument("returnTo") {
                type = NavType.StringType; defaultValue = ""
            })
        ) { backStackEntry ->
            val returnTo = backStackEntry.arguments?.getString("returnTo") ?: ""
            ClassificationCompleteScreen(
                navController = navController,
                classificationViewModel = classificationViewModel,
                onConfirm = {
                    classificationViewModel.reset()
                    bookEntryViewModel.loadEntries()
                    bookEntryViewModel.loadUnconfirmedCount()
                    when (returnTo) {
                        "book" -> {
                            navController.navigate(Route.BookEntryList.path) {
                                popUpTo(Route.BookEntryList.path) { inclusive = false }
                            }
                        }
                        "unclassified" -> {
                            // 미분류 건이 남아있으면 목록으로 복귀, 없으면 홈으로
                            val remaining = bookEntryViewModel.entries.value.count { !it.confirmed }
                            if (remaining > 0) {
                                navController.navigate(Route.UnclassifiedList.path) {
                                    popUpTo(Route.UnclassifiedList.path) { inclusive = true }
                                }
                            } else {
                                navController.navigate(Route.Main.path) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        }
                        else -> {
                            navController.navigate(Route.Main.path) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                }
            )
        }

        composable(Route.UnclassifiedList.path) { backStackEntry ->
            val aiDone = backStackEntry.savedStateHandle.get<Boolean>("aiRecommended") ?: false
            val entries by bookEntryViewModel.entries.collectAsState()
            val bulkResults by classificationViewModel.bulkResults.collectAsState()
            // AI 일괄 분류 결과를 entryId → category 맵으로 변환
            val aiCategoryMap = bulkResults.filter { it.isDone && it.aiCategory != null }
                .associate { it.entryId to it.aiCategory!! }
            val unclassified = entries.filter { !it.confirmed }.map { entry ->
                val fmt = java.text.NumberFormat.getNumberInstance(java.util.Locale.KOREA)
                val amount = when (entry.entryType) {
                    "INCOME" -> entry.incomeAmount
                    "EXPENSE" -> entry.expenseAmount
                    "ASSET" -> entry.fixedAssetAmount
                    else -> 0L
                }
                val dateTime = try {
                    val dt = entry.createdAt.take(16).replace("T", " ")
                    dt
                } catch (_: Exception) { entry.createdAt }
                // AI 일괄 분류 결과가 있으면 우선 사용, 없으면 BE 데이터
                val aiCategory = aiCategoryMap[entry.id] ?: entry.categoryName ?: "미분류"
                com.ssafy.seveniTax.ui.classification.UnclassifiedTransaction(
                    id = entry.id.toString(),
                    merchantName = entry.merchantName ?: entry.description ?: "거래",
                    amount = "${fmt.format(amount)}원",
                    dateTime = dateTime,
                    aiCategory = aiCategory
                )
            }

            // 서버에서 장부 데이터 로드
            LaunchedEffect(Unit) { bookEntryViewModel.loadEntries() }

            // 일괄 확정 완료 시 네비게이션
            val bulkConfirmDone by bookEntryViewModel.bulkConfirmDone.collectAsState()
            LaunchedEffect(bulkConfirmDone) {
                if (bulkConfirmDone) {
                    bookEntryViewModel.clearBulkConfirmDone()
                    classificationViewModel.reset()
                    navController.navigate(Route.ClassificationComplete.create("unclassified")) {
                        popUpTo(Route.UnclassifiedList.path) { inclusive = false }
                    }
                }
            }

            UnclassifiedListScreen(
                navController = navController,
                transactions = unclassified,
                aiRecommendedInitial = aiDone,
                onBulkConfirm = {
                    // 실행 시점에 최신 bulkResults를 직접 읽어서 수동 수정 반영
                    val freshCategoryMap = classificationViewModel.bulkResults.value
                        .filter { it.isDone && it.aiCategory != null }
                        .associate { it.entryId to it.aiCategory!! }
                    val unconfirmedIds = bookEntryViewModel.entries.value
                        .filter { !it.confirmed }.map { it.id }
                    bookEntryViewModel.bulkConfirmEntries(freshCategoryMap, unconfirmedIds)
                },
                onAiRecommend = {
                    // 미분류 항목으로 일괄 AI 분류 시작
                    val fmt = java.text.NumberFormat.getNumberInstance(java.util.Locale.KOREA)
                    val bulkInputs = entries.filter { !it.confirmed }.map { entry ->
                        val amt = when (entry.entryType) {
                            "INCOME" -> entry.incomeAmount
                            "EXPENSE" -> entry.expenseAmount
                            "ASSET" -> entry.fixedAssetAmount
                            else -> 0L
                        }
                        BulkEntryInput(
                            entryId = entry.id,
                            merchantName = entry.merchantName ?: entry.description ?: "거래",
                            amount = "${fmt.format(amt)}원",
                            amountLong = amt,
                            note = entry.note
                        )
                    }
                    classificationViewModel.classifyBulk(bulkInputs)
                    navController.navigate(Route.BulkClassificationLoading.path)
                },
                onReviewAll = {
                    // 첫 번째 미분류 항목으로 개별 분류 시작
                    val firstUnconfirmed = entries.firstOrNull { !it.confirmed }
                    if (firstUnconfirmed != null) {
                        val amt = when (firstUnconfirmed.entryType) {
                            "INCOME" -> firstUnconfirmed.incomeAmount
                            "EXPENSE" -> firstUnconfirmed.expenseAmount
                            "ASSET" -> firstUnconfirmed.fixedAssetAmount
                            else -> 0L
                        }
                        classificationViewModel.setEntryInfo(
                            entryId = firstUnconfirmed.id,
                            merchantName = firstUnconfirmed.merchantName ?: firstUnconfirmed.description ?: "거래",
                            amount = amt,
                            dateTime = firstUnconfirmed.createdAt.take(16).replace("T", " "),
                            note = firstUnconfirmed.note
                        )
                        navController.navigate(Route.ClassificationLoading.path)
                    }
                },
                onTransactionClick = { transactionId ->
                    val entryId = transactionId.toLongOrNull() ?: -1L
                    navController.navigate(Route.CategorySelect.create("unclassified", entryId))
                }
            )
        }

        composable(Route.BulkClassificationLoading.path) {
            BulkClassificationLoadingScreen(
                navController = navController,
                onComplete = {
                    // 서버 업데이트 없이 결과만 전달 — 사용자가 "일괄 확정" 시 반영
                    navController.previousBackStackEntry?.savedStateHandle?.set("aiRecommended", true)
                    navController.popBackStack()
                }
            )
        }

        composable(Route.BookEntryList.path) {
            BookEntryListScreen(navController, viewModel = bookEntryViewModel)
        }

        composable(Route.BookFilter.path) {
            BookFilterScreen(navController, viewModel = bookEntryViewModel)
        }

        composable(
            route = Route.BookMemoAdd.path,
            arguments = listOf(
                navArgument("entryId") { type = NavType.LongType; defaultValue = -1L },
                navArgument("fromPayment") { type = NavType.BoolType; defaultValue = false }
            )
        ) { backStackEntry ->
            val entryId = backStackEntry.arguments?.getLong("entryId") ?: -1L
            val fromPayment = backStackEntry.arguments?.getBoolean("fromPayment") ?: false
            BookMemoAddScreen(
                navController = navController,
                entryId = entryId,
                fromPayment = fromPayment,
                bookEntryRepository = bookEntryViewModel
            )
        }

        composable(Route.TaxReport.path) {
            TaxReportScreen(navController, bookEntryViewModel)
        }

        composable(Route.TaxSavingsDetail.path) {
            TaxSavingsDetailScreen(navController, bookEntryViewModel)
        }

        composable(
            route = Route.BookEntryDetail.path,
            arguments = listOf(navArgument("entryId") { type = NavType.LongType })
        ) { backStackEntry ->
            val entryId = backStackEntry.arguments?.getLong("entryId") ?: 0L
            BookEntryDetailScreen(navController, entryId)
        }

        composable(Route.ExportPurpose.path) {
            ExportPurposeScreen(navController)
        }

        composable(
            route = Route.ExportDateRange.path,
            arguments = listOf(navArgument("purpose") { type = NavType.StringType })
        ) { backStackEntry ->
            val purpose = backStackEntry.arguments?.getString("purpose").orEmpty()
            ExportDateRangeScreen(navController, purpose)
        }

        composable(
            route = Route.ExportFormat.path,
            arguments = listOf(
                navArgument("purpose") { type = NavType.StringType },
                navArgument("startDate") { type = NavType.StringType },
                navArgument("endDate") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val purpose = backStackEntry.arguments?.getString("purpose").orEmpty()
            val startDate = backStackEntry.arguments?.getString("startDate").orEmpty()
            val endDate = backStackEntry.arguments?.getString("endDate").orEmpty()
            ExportFormatScreen(navController, purpose, startDate, endDate)
        }

        composable(Route.TaxCalendar.path) {
            TaxCalendarScreen(navController, viewModel = taxCalendarViewModel)
        }

        composable(Route.NotificationSettings.path) {
            NotificationSettingsScreen(navController, taxCalendarViewModel)
        }

        composable(
            route = Route.TaxCalendarDetail.path,
            arguments = listOf(
                navArgument("taxName") { type = NavType.StringType },
                navArgument("deadline") { type = NavType.StringType },
                navArgument("dDay") { type = NavType.IntType },
                navArgument("description") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val taxName = java.net.URLDecoder.decode(
                backStackEntry.arguments?.getString("taxName").orEmpty(), "UTF-8"
            )
            val deadline = backStackEntry.arguments?.getString("deadline").orEmpty()
            val dDay = backStackEntry.arguments?.getInt("dDay") ?: 0
            val description = java.net.URLDecoder.decode(
                backStackEntry.arguments?.getString("description").orEmpty(), "UTF-8"
            )
            TaxCalendarDetailScreen(navController, taxName, deadline, dDay, description, taxCalendarViewModel)
        }

        composable(Route.AutoClassification.path) {
            AutoClassificationScreen(
                navController = navController,
                onConfirm = { navController.popBackStack() },
                onEditCategory = { navController.navigate(Route.CategorySelect.create()) }
            )
        }

        composable(Route.AiChat.path) {
            AiScreen(navController)
        }
    }

    // AI 챗봇 FAB (모든 비-auth 화면에서 표시)
    if (showFab) {
        AiChatFab(
            onClick = { navController.navigate(Route.AiChat.path) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 20.dp, bottom = 20.dp)
        )
    }
    } // Box end
}

