package com.ssafy.seveniTax

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.ssafy.seveniTax.ui.navigation.NavGraph
import com.ssafy.seveniTax.ui.navigation.Route
import com.ssafy.seveniTax.ui.theme.SevenITaxTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var navController: NavHostController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )

        val navigateTo = intent?.getStringExtra("navigate_to")

        setContent {
            SevenITaxTheme {
                val nc = rememberNavController()
                navController = nc

                NavGraph(
                    navController = nc,
                    pendingNavigateTo = navigateTo
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // 앱 실행 중 알림 탭 → PIN 없이 바로 이동
        val navigateTo = intent.getStringExtra("navigate_to") ?: return
        navController?.let { nc ->
            when (navigateTo) {
                "classification_result" -> nc.navigate(Route.ClassificationLoading.path)
                "tax_calendar" -> nc.navigate(Route.TaxCalendar.path)
            }
        }
    }
}
