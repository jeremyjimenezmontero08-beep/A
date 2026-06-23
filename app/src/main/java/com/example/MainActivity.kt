package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.AuthScreen
import com.example.ui.PromoMainScreen
import com.example.ui.PromoViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        val viewModel: PromoViewModel = viewModel()
        val isUserLoggedIn by viewModel.isUserLoggedIn.collectAsStateWithLifecycle()
        
        if (isUserLoggedIn) {
            PromoMainScreen(viewModel = viewModel)
        } else {
            AuthScreen(viewModel = viewModel)
        }
      }
    }
  }
}
