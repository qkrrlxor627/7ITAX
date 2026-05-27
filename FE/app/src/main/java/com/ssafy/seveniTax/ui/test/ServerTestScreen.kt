package com.ssafy.seveniTax.ui.test

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ssafy.seveniTax.ui.theme.*
import com.ssafy.seveniTax.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class TestResult(
    val endpoint: String,
    val method: String,
    val status: String,
    val responseTime: Long,
    val statusCode: Int?,
    val body: String?,
    val isSuccess: Boolean
)

@Composable
fun ServerTestScreen(navController: NavController) {
    val scope = rememberCoroutineScope()
    var baseUrl by remember { mutableStateOf(Constants.API_BASE_URL) }
    var isTesting by remember { mutableStateOf(false) }

    var connectResult by remember { mutableStateOf<TestResult?>(null) }
    var publicResults by remember { mutableStateOf<List<TestResult>>(emptyList()) }
    var authToken by remember { mutableStateOf<String?>(null) }
    var userId by remember { mutableStateOf<Long?>(null) }
    var authResults by remember { mutableStateOf<List<TestResult>>(emptyList()) }
    var protectedResults by remember { mutableStateOf<List<TestResult>>(emptyList()) }

    val client = remember {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    val JSON_TYPE = "application/json; charset=utf-8".toMediaType()

    fun buildUrl(path: String): String =
        baseUrl.trimEnd('/') + "/" + path.trimStart('/')

    fun httpGet(url: String, token: String? = null): Pair<Int?, String?> {
        val reqBuilder = Request.Builder().url(url).get()
        token?.let { reqBuilder.addHeader("Authorization", "Bearer $it") }
        val response = client.newCall(reqBuilder.build()).execute()
        return response.code to response.body?.string()?.take(1000)
    }

    fun httpPost(url: String, jsonBody: String, token: String? = null, verifyToken: String? = null): Pair<Int?, String?> {
        val body = jsonBody.toRequestBody(JSON_TYPE)
        val reqBuilder = Request.Builder().url(url).post(body)
        token?.let { reqBuilder.addHeader("Authorization", "Bearer $it") }
        verifyToken?.let { reqBuilder.addHeader("X-Verify-Token", it) }
        val response = client.newCall(reqBuilder.build()).execute()
        return response.code to response.body?.string()?.take(1000)
    }

    fun runSingle(
        name: String,
        method: String,
        action: () -> Pair<Int?, String?>
    ): TestResult {
        val start = System.currentTimeMillis()
        return try {
            val (code, body) = action()
            val elapsed = System.currentTimeMillis() - start
            TestResult(
                endpoint = name,
                method = method,
                status = "HTTP $code",
                responseTime = elapsed,
                statusCode = code,
                body = body,
                isSuccess = code != null && code in 200..499
            )
        } catch (e: Exception) {
            val elapsed = System.currentTimeMillis() - start
            TestResult(
                endpoint = name,
                method = method,
                status = e.javaClass.simpleName,
                responseTime = elapsed,
                statusCode = null,
                body = e.message,
                isSuccess = false
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(Background)
            .padding(20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("서버 연동 테스트", style = Typography.headlineMedium)
        Spacer(modifier = Modifier.height(4.dp))
        Text("배포 서버와의 연결 상태를 단계별로 확인합니다", style = Typography.bodySmall)

        Spacer(modifier = Modifier.height(16.dp))

        Text("Base URL", style = Typography.labelLarge)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = baseUrl,
            onValueChange = { baseUrl = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = Typography.bodySmall.copy(fontFamily = FontFamily.Monospace, color = TextPrimary)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // ── 1단계: 서버 연결 확인 ──
        SectionHeader("1단계: 서버 연결 확인")
        Button(
            onClick = {
                isTesting = true
                connectResult = null
                scope.launch {
                    connectResult = withContext(Dispatchers.IO) {
                        runSingle("GET /", "GET") {
                            httpGet(baseUrl.trimEnd('/').removeSuffix("/api"))
                        }
                    }
                    isTesting = false
                }
            },
            enabled = !isTesting,
            colors = ButtonDefaults.buttonColors(containerColor = BrandPurple),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isTesting && connectResult == null) LoadingIndicator()
            Text("연결 테스트", color = Background)
        }
        connectResult?.let { ResultCard(it) }

        Spacer(modifier = Modifier.height(24.dp))

        // ── 2단계: 공개 API ──
        SectionHeader("2단계: 공개 API 테스트 (토큰 불필요)")
        Button(
            onClick = {
                isTesting = true
                publicResults = emptyList()
                scope.launch {
                    publicResults = withContext(Dispatchers.IO) {
                        listOf(
                            runSingle("POST /auth/verify-identity", "POST") {
                                httpPost(
                                    buildUrl("auth/verify-identity"),
                                    """{"name":"홍길동","birthDate":"1990-01-01","gender":"M","phoneNumber":"01012345678"}"""
                                )
                            },
                            runSingle("GET /tax-calendar/deadlines", "GET") {
                                httpGet(buildUrl("tax-calendar/deadlines"))
                            },
                            runSingle("POST /payments/authorize", "POST") {
                                httpPost(
                                    buildUrl("payments/authorize"),
                                    """{"cardId":1,"amount":50000,"merchantName":"테스트","paymentMethod":"OFFLINE","purpose":"BUSINESS"}"""
                                )
                            }
                        )
                    }
                    isTesting = false
                }
            },
            enabled = !isTesting,
            colors = ButtonDefaults.buttonColors(containerColor = BrandPurple),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isTesting && publicResults.isEmpty()) LoadingIndicator()
            Text("공개 API 테스트", color = Background)
        }
        publicResults.forEach { ResultCard(it) }

        Spacer(modifier = Modifier.height(24.dp))

        // ── 3단계: 인증 플로우 ──
        SectionHeader("3단계: 인증 플로우 (토큰 획득)")
        Text("본인인증 → PIN 설정 → 토큰 획득", style = Typography.bodySmall)
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                isTesting = true
                authResults = emptyList()
                authToken = null
                userId = null
                scope.launch {
                    val results = mutableListOf<TestResult>()
                    withContext(Dispatchers.IO) {
                        val verifyResult = runSingle("POST /auth/verify-identity", "POST") {
                            httpPost(
                                buildUrl("auth/verify-identity"),
                                """{"name":"홍길동","birthDate":"1990-01-01","gender":"M","phoneNumber":"01012345678"}"""
                            )
                        }
                        results.add(verifyResult)

                        val parsedUserId = try {
                            val json = JSONObject(verifyResult.body ?: "")
                            json.optJSONObject("data")?.optLong("userId")
                        } catch (_: Exception) { null }
                        val parsedVerifyToken = try {
                            val json = JSONObject(verifyResult.body ?: "")
                            json.optJSONObject("data")?.optString("verifyToken")
                        } catch (_: Exception) { null }
                        userId = parsedUserId

                        if (parsedUserId != null && !parsedVerifyToken.isNullOrBlank()) {
                            val pinResult = runSingle("POST /auth/setup-pin", "POST") {
                                httpPost(
                                    buildUrl("auth/setup-pin"),
                                    """{"pin":"123456"}""",
                                    verifyToken = parsedVerifyToken
                                )
                            }
                            results.add(pinResult)

                            val token = try {
                                val json = JSONObject(pinResult.body ?: "")
                                json.optJSONObject("data")?.optString("accessToken")
                            } catch (_: Exception) { null }

                            if (token.isNullOrBlank()) {
                                val loginResult = runSingle("POST /auth/login (fallback)", "POST") {
                                    httpPost(
                                        buildUrl("auth/login"),
                                        """{"phoneNumber":"01012345678","pin":"123456"}"""
                                    )
                                }
                                results.add(loginResult)

                                authToken = try {
                                    val json = JSONObject(loginResult.body ?: "")
                                    json.optJSONObject("data")?.optString("accessToken")
                                } catch (_: Exception) { null }
                            } else {
                                authToken = token
                            }
                        }
                    }
                    authResults = results
                    isTesting = false
                }
            },
            enabled = !isTesting,
            colors = ButtonDefaults.buttonColors(containerColor = BrandPurple),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isTesting && authResults.isEmpty()) LoadingIndicator()
            Text("인증 플로우 실행", color = Background)
        }
        authResults.forEach { ResultCard(it) }

        if (authToken != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Success.copy(alpha = 0.1f))
                    .padding(12.dp)
            ) {
                Text("Token 획득 성공", style = Typography.labelLarge, color = Success)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = authToken!!.take(40) + "...",
                    style = Typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = TextSecondary
                )
            }
        } else if (authResults.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Token 획득 실패 — 응답 본문을 확인하세요", style = Typography.bodySmall, color = Error)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── 4단계: 인증 필요 API ──
        SectionHeader("4단계: 인증 필요 API 테스트")
        Button(
            onClick = {
                val token = authToken ?: return@Button
                isTesting = true
                protectedResults = emptyList()
                scope.launch {
                    protectedResults = withContext(Dispatchers.IO) {
                        listOf(
                            runSingle("POST /cards (카드 등록)", "POST") {
                                httpPost(
                                    buildUrl("cards"),
                                    """{"cardName":"테스트카드","cardType":"CREDIT","last4Digits":"1234"}""",
                                    token
                                )
                            },
                            runSingle("GET /cards (카드 목록)", "GET") {
                                httpGet(buildUrl("cards"), token)
                            },
                            runSingle("GET /book-entries (장부 목록)", "GET") {
                                httpGet(buildUrl("book-entries"), token)
                            },
                            runSingle("GET /tax-estimation (세금 추정)", "GET") {
                                httpGet(buildUrl("tax-estimation"), token)
                            },
                            runSingle("POST /auth/consents (동의 저장)", "POST") {
                                httpPost(
                                    buildUrl("auth/consents"),
                                    """{"consents":["TERMS","PRIVACY","FINANCIAL"]}""",
                                    token
                                )
                            },
                            runSingle("POST /classification (세목 분류)", "POST") {
                                httpPost(
                                    buildUrl("classification"),
                                    """{"description":"사무실 복합기 렌탈"}""",
                                    token
                                )
                            }
                        )
                    }
                    isTesting = false
                }
            },
            enabled = !isTesting && authToken != null,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (authToken != null) BrandPurple else Disabled
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isTesting && protectedResults.isEmpty()) LoadingIndicator()
            Text(
                if (authToken != null) "인증 API 테스트" else "먼저 3단계에서 토큰을 획득하세요",
                color = Background
            )
        }
        protectedResults.forEach { ResultCard(it) }

        Spacer(modifier = Modifier.height(24.dp))

        // ── 전체 요약 ──
        val allResults = listOfNotNull(connectResult) + publicResults + authResults + protectedResults
        if (allResults.isNotEmpty()) {
            val successCount = allResults.count { it.isSuccess }
            val totalCount = allResults.size
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (successCount == totalCount) Success.copy(alpha = 0.1f)
                        else Error.copy(alpha = 0.1f)
                    )
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (successCount == totalCount) "ALL PASS" else "PARTIAL",
                    style = Typography.titleLarge,
                    color = if (successCount == totalCount) Success else Error
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "$successCount / $totalCount 성공",
                    style = Typography.bodyLarge,
                    color = if (successCount == totalCount) Success else Error
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = Divider)
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = {
                    connectResult = null
                    publicResults = emptyList()
                    authResults = emptyList()
                    protectedResults = emptyList()
                    authToken = null
                    userId = null
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("전체 초기화")
            }
            Button(
                onClick = {
                    navController.navigate("auth_graph") {
                        popUpTo("server_test") { inclusive = true }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Accent),
                modifier = Modifier.weight(1f)
            ) {
                Text("앱으로 이동", color = Background)
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = BrandPurple
    )
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun LoadingIndicator() {
    CircularProgressIndicator(
        modifier = Modifier.size(16.dp),
        color = Background,
        strokeWidth = 2.dp
    )
    Spacer(modifier = Modifier.width(8.dp))
}

@Composable
private fun ResultCard(result: TestResult) {
    Spacer(modifier = Modifier.height(6.dp))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Surface)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = result.endpoint,
                style = Typography.labelLarge,
                modifier = Modifier.weight(1f),
                fontSize = 13.sp
            )
            Text(
                text = if (result.isSuccess) "OK" else "FAIL",
                style = Typography.labelLarge,
                color = if (result.isSuccess) Success else Error,
                fontSize = 13.sp
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${result.method}  |  ${result.status}  |  ${result.responseTime}ms",
            style = Typography.bodySmall,
            fontFamily = FontFamily.Monospace
        )
        if (result.body != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = result.body,
                style = Typography.bodySmall.copy(fontSize = 10.sp),
                fontFamily = FontFamily.Monospace,
                maxLines = 6,
                color = TextSecondary
            )
        }
    }
}
