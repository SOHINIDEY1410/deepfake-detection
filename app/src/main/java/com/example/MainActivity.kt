package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ai.configureAppCheck
import com.example.ui.VeriMedApp
import com.example.ui.VeriMedViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure Firebase App Check with launch intent before any UI, ViewModel, or Firebase operations.
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            val viewModel: VeriMedViewModel = viewModel()
            VeriMedApp(viewModel = viewModel)
        }
    }
}
