package com.ssafy.seveniTax.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ssafy.seveniTax.ui.components.TaxButton
import com.ssafy.seveniTax.ui.navigation.Route
import com.ssafy.seveniTax.ui.theme.Background
import com.ssafy.seveniTax.ui.theme.BrandPurple
import com.ssafy.seveniTax.ui.theme.Disabled
import com.ssafy.seveniTax.ui.theme.Divider
import com.ssafy.seveniTax.ui.theme.TextPrimary
import com.ssafy.seveniTax.ui.theme.TextSecondary
import com.ssafy.seveniTax.viewmodel.AuthStep
import com.ssafy.seveniTax.viewmodel.AuthViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdentityVerificationScreen(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showTelecomSheet by remember { mutableStateOf(false) }
    val telecomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val termsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(uiState.currentStep) {
        if (uiState.currentStep == AuthStep.TERMS && uiState.terms.isEmpty()) {
            viewModel.loadTerms()
        }
        if (uiState.currentStep == AuthStep.TELECOM) {
            showTelecomSheet = true
        }
    }

    LaunchedEffect(uiState.currentStep, uiState.phone) {
        if (uiState.currentStep == AuthStep.PHONE && uiState.phone.length == 11) {
            delay(150)
            if (viewModel.uiState.value.currentStep == AuthStep.PHONE) {
                viewModel.nextStep()
            }
        }
    }

    LaunchedEffect(uiState.currentStep, uiState.residentFront, uiState.residentBack) {
        if (uiState.currentStep == AuthStep.SSN && uiState.residentFront.length == 6 && uiState.residentBack.length == 1) {
            delay(150)
            if (viewModel.uiState.value.currentStep == AuthStep.SSN) {
                viewModel.nextStep()
            }
        }
    }

    if (showTelecomSheet) {
        TelecomPickerSheet(
            sheetState = telecomSheetState,
            onDismiss = { showTelecomSheet = false },
            onSelect = {
                viewModel.updateTelecom(it)
                viewModel.nextStep()
                showTelecomSheet = false
            }
        )
    }

    if (uiState.currentStep == AuthStep.TERMS) {
        TermsSheetContent(
            sheetState = termsSheetState,
            onDismiss = { viewModel.previousStep() },
            onApplyAgreements = viewModel::setAgreedTerms,
            onNext = {
                viewModel.nextStep()
                navController.navigate(Route.SmsAuth.path)
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(Background)
    ) {
        IconButton(
            onClick = {
                if (uiState.currentStep == AuthStep.PHONE) {
                    navController.popBackStack()
                } else {
                    viewModel.previousStep()
                }
            },
            modifier = Modifier.padding(top = 16.dp, start = 12.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = TextPrimary
            )
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 24.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "본인인증을 위해\n정보를 입력해 주세요",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(12.dp))
                if (uiState.errorMessage.isNotBlank()) {
                    Text(
                        text = uiState.errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            item {
                if (uiState.currentStep >= AuthStep.NAME) {
                    NameSection(
                        name = uiState.name,
                        isActive = uiState.currentStep == AuthStep.NAME,
                        onNameChange = viewModel::updateName,
                        onNext = { viewModel.nextStep() },
                        onEditClick = { viewModel.goToStep(AuthStep.NAME) }
                    )
                }
            }

            item {
                if (uiState.currentStep >= AuthStep.TELECOM) {
                    TelecomSection(
                        telecom = uiState.telecom,
                        isActive = uiState.currentStep == AuthStep.TELECOM,
                        onClick = { showTelecomSheet = true },
                        onEditClick = {
                            viewModel.goToStep(AuthStep.TELECOM)
                            showTelecomSheet = true
                        }
                    )
                }
            }

            item {
                if (uiState.currentStep >= AuthStep.SSN) {
                    SsnSection(
                        front = uiState.residentFront,
                        back = uiState.residentBack,
                        isActive = uiState.currentStep == AuthStep.SSN,
                        onFrontChange = viewModel::updateResidentFront,
                        onBackChange = viewModel::updateResidentBack,
                        onEditClick = {
                            viewModel.updateResidentBack("")
                            viewModel.goToStep(AuthStep.SSN)
                        }
                    )
                }
            }

            item {
                PhoneSection(
                    phone = uiState.phone,
                    isActive = uiState.currentStep == AuthStep.PHONE,
                    onPhoneChange = viewModel::updatePhone,
                    onEditClick = {
                        viewModel.updatePhone("")
                        viewModel.goToStep(AuthStep.PHONE)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TelecomPickerSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val options = listOf("SKT", "KT", "LG U+", "SKT 알뜰폰", "KT 알뜰폰", "LG U+ 알뜰폰")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Background
    ) {
        LazyColumn(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
            items(options) { option ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(option) }
                        .padding(vertical = 16.dp)
                ) {
                    Text(
                        text = option,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun PhoneSection(
    phone: String,
    isActive: Boolean,
    onPhoneChange: (String) -> Unit,
    onEditClick: () -> Unit = {}
) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        if (!isActive) {
            Column(modifier = Modifier.clickable(onClick = onEditClick)) {
                FormFieldReadOnly("휴대폰 번호", formatPhoneForm(phone))
            }
        } else {
            val focusRequester = remember { FocusRequester() }
            LaunchedEffect(Unit) { focusRequester.requestFocus() }

            Text("휴대폰 번호", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Spacer(modifier = Modifier.height(8.dp))
            BasicTextField(
                value = phone,
                onValueChange = { if (it.length <= 11 && it.all(Char::isDigit)) onPhoneChange(it) },
                textStyle = MaterialTheme.typography.titleLarge.copy(color = TextPrimary),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                cursorBrush = SolidColor(BrandPurple)
            )
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = BrandPurple)
        }
    }
}

@Composable
private fun SsnSection(
    front: String,
    back: String,
    isActive: Boolean,
    onFrontChange: (String) -> Unit,
    onBackChange: (String) -> Unit,
    onEditClick: () -> Unit = {}
) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        if (!isActive) {
            Column(modifier = Modifier.clickable(onClick = onEditClick)) {
                FormFieldReadOnly("주민등록번호", buildSsnDisplay(front, back))
            }
        } else {
            val frontFocusRequester = remember { FocusRequester() }
            val backFocusRequester = remember { FocusRequester() }

            LaunchedEffect(Unit) { frontFocusRequester.requestFocus() }
            LaunchedEffect(front.length) {
                if (front.length == 6) {
                    backFocusRequester.requestFocus()
                }
            }

            Text("주민등록번호", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicTextField(
                    value = front,
                    onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) onFrontChange(it) },
                    textStyle = MaterialTheme.typography.titleLarge.copy(
                        color = TextPrimary,
                        letterSpacing = 6.sp
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .width(136.dp)
                        .focusRequester(frontFocusRequester),
                    cursorBrush = SolidColor(BrandPurple)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text("-", style = MaterialTheme.typography.titleLarge, color = TextSecondary)
                Spacer(modifier = Modifier.width(16.dp))
                BasicTextField(
                    value = back,
                    onValueChange = { if (it.length <= 1 && it.all(Char::isDigit)) onBackChange(it) },
                    textStyle = MaterialTheme.typography.titleLarge.copy(
                        color = TextPrimary,
                        letterSpacing = 6.sp
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .width(28.dp)
                        .focusRequester(backFocusRequester),
                    cursorBrush = SolidColor(BrandPurple)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("* * * * * *", style = MaterialTheme.typography.titleLarge, color = TextSecondary)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(
                    modifier = Modifier.width(150.dp),
                    color = BrandPurple
                )
                Spacer(modifier = Modifier.width(32.dp))
                HorizontalDivider(
                    modifier = Modifier.width(30.dp),
                    color = BrandPurple
                )
            }
        }
    }
}

@Composable
private fun TelecomSection(
    telecom: String,
    isActive: Boolean,
    onClick: () -> Unit,
    onEditClick: () -> Unit = {}
) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        if (!isActive) {
            Column(modifier = Modifier.clickable(onClick = onEditClick)) {
                FormFieldReadOnly("통신사", telecom)
            }
        } else {
            Text("통신사", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (telecom.isBlank()) "통신사를 선택해 주세요" else telecom,
                style = MaterialTheme.typography.titleLarge,
                color = if (telecom.isBlank()) TextSecondary else TextPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
            )
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = BrandPurple)
        }
    }
}

@Composable
private fun NameSection(
    name: String,
    isActive: Boolean,
    onNameChange: (String) -> Unit,
    onNext: () -> Unit = {},
    onEditClick: () -> Unit = {}
) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        if (!isActive) {
            Column(modifier = Modifier.clickable(onClick = onEditClick)) {
                FormFieldReadOnly("이름", name)
            }
        } else {
            val focusRequester = remember { FocusRequester() }
            LaunchedEffect(Unit) { focusRequester.requestFocus() }

            Text("이름", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Spacer(modifier = Modifier.height(8.dp))
            BasicTextField(
                value = name,
                onValueChange = onNameChange,
                textStyle = MaterialTheme.typography.titleLarge.copy(color = TextPrimary),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (name.trim().length >= 2) onNext()
                }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                cursorBrush = SolidColor(BrandPurple)
            )
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = BrandPurple)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TermsSheetContent(
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onApplyAgreements: (Set<String>) -> Unit,
    onNext: () -> Unit
) {
    val groupedTerms = remember {
        listOf(
            TermsGroup(
                id = "membership",
                label = "[필수] 기택스 회원 약관 및 동의사항",
                details = listOf(
                    "서비스 이용약관",
                    "통합 금융정보 및 전자문서 서비스 약관",
                    "개인(신용)정보 수집 및 이용 동의(회원가입)"
                ),
                required = true,
                backendConsentTypes = setOf("SERVICE", "PRIVACY", "FINANCIAL")
            ),
            TermsGroup(
                id = "identity",
                label = "[필수] 본인 확인 서비스 약관 및 동의사항",
                details = listOf(
                    "통신사 이용약관",
                    "본인확인서비스 이용약관",
                    "개인정보 수집, 이용 및 위탁 동의",
                    "고유식별정보 처리 동의"
                ),
                required = true,
                backendConsentTypes = setOf("SERVICE", "PRIVACY", "FINANCIAL")
            ),
            TermsGroup(
                id = "tax_optional",
                label = "[선택] 기택스 세무 서비스 제공 동의",
                details = emptyList(),
                required = false,
                backendConsentTypes = emptySet()
            )
        )
    }
    var expandedConsentType by remember { mutableStateOf<String?>(groupedTerms.first().id) }
    var selectedGroupIds by remember { mutableStateOf(setOf<String>()) }
    val allChecked = groupedTerms.all { selectedGroupIds.contains(it.id) }
    val allRequiredChecked = groupedTerms
        .filter { it.required }
        .all { selectedGroupIds.contains(it.id) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Background
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
            Text(
                text = "서비스 이용에\n꼭 필요한 동의만 추렸어요",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 30.sp,
                    lineHeight = 38.sp
                ),
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(24.dp))

            // 전체 동의
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Divider, RoundedCornerShape(16.dp))
                    .clickable {
                        selectedGroupIds = if (allChecked) {
                            emptySet()
                        } else {
                            groupedTerms.map { it.id }.toSet()
                        }
                    }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = if (allChecked) BrandPurple else Disabled
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "아래 약관에 모두 동의합니다",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = TextPrimary
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            groupedTerms.forEach { group ->
                val isExpanded = expandedConsentType == group.id
                TermsGroupRow(
                    label = group.label,
                    checked = selectedGroupIds.contains(group.id),
                    expanded = isExpanded,
                    details = group.details,
                    onToggleChecked = {
                        selectedGroupIds = if (selectedGroupIds.contains(group.id)) {
                            selectedGroupIds - group.id
                        } else {
                            selectedGroupIds + group.id
                        }
                    },
                    onToggleExpanded = {
                        if (group.details.isNotEmpty()) {
                            expandedConsentType = if (isExpanded) null else group.id
                        }
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))
            TaxButton(
                text = "동의하고 본인 인증하기",
                enabled = allRequiredChecked,
                onClick = {
                    val consentTypes = groupedTerms
                        .filter { selectedGroupIds.contains(it.id) }
                        .flatMap { it.backendConsentTypes }
                        .toSet()
                    onApplyAgreements(consentTypes)
                    onNext()
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun TermsGroupRow(
    label: String,
    checked: Boolean,
    expanded: Boolean,
    details: List<String>,
    onToggleChecked: () -> Unit,
    onToggleExpanded: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Divider, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Box(
                modifier = Modifier.clickable(onClick = onToggleChecked),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = if (checked) BrandPurple else Disabled
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Row(
                modifier = Modifier
                    .weight(1f)
                    .let { base ->
                        if (details.isNotEmpty()) base.clickable(onClick = onToggleExpanded) else base
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                if (details.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    ExpandIcon(expanded = expanded)
                }
            }
        }

        if (expanded && details.isNotEmpty()) {
            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = Divider)
            Spacer(modifier = Modifier.height(14.dp))
            details.forEachIndexed { index, detail ->
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    lineHeight = 20.sp
                )
                if (index != details.lastIndex) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun ExpandIcon(expanded: Boolean) {
    val icon: ImageVector = if (expanded) {
        Icons.Filled.KeyboardArrowDown
    } else {
        Icons.AutoMirrored.Filled.KeyboardArrowRight
    }
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = TextSecondary
    )
}

private data class TermsGroup(
    val id: String,
    val label: String,
    val details: List<String>,
    val required: Boolean,
    val backendConsentTypes: Set<String>
)
