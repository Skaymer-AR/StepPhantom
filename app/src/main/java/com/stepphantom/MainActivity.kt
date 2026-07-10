package com.stepphantom

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.stepphantom.config.DiagnosticsStore
import com.stepphantom.ui.MainScreen
import com.stepphantom.ui.MainViewModel
import com.stepphantom.ui.theme.StepPhantomTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DiagnosticsStore.ensure(applicationContext)
        setContent {
            StepPhantomTheme { MainScreen(viewModel) }
        }
    }
}
