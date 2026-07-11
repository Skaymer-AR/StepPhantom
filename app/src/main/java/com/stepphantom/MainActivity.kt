package com.stepphantom

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import com.stepphantom.config.DiagnosticsStore
import com.stepphantom.ui.LocalStrings
import com.stepphantom.ui.MainScreen
import com.stepphantom.ui.MainViewModel
import com.stepphantom.ui.stringsFor
import com.stepphantom.ui.theme.StepPhantomTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        DiagnosticsStore.ensure(applicationContext)
        setContent {
            val lang by viewModel.language.collectAsState()
            val dynamic by viewModel.dynamicColor.collectAsState()
            StepPhantomTheme(useDynamicColor = dynamic) {
                CompositionLocalProvider(LocalStrings provides stringsFor(lang)) {
                    MainScreen(viewModel)
                }
            }
        }
    }
}
