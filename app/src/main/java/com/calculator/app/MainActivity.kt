package com.calculator.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import com.calculator.app.ui.adaptive.AdaptiveCalculatorLayout
import com.calculator.app.ui.theme.CalculatorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CalculatorTheme {
                val windowAdaptiveInfo = currentWindowAdaptiveInfo()
                AdaptiveCalculatorLayout(windowAdaptiveInfo = windowAdaptiveInfo)
            }
        }
    }
}
