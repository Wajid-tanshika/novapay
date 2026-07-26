package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.example.data.repository.FirebaseManager
import com.example.ui.WalletViewModel
import com.example.ui.WalletViewModelFactory
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.ui.screens.NovaPayAppContent
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    
    // Safely initialize Firebase with offline persistence & fallback support
    FirebaseManager.initialize(applicationContext)
    
    // Create the ViewModel using the application context
    val viewModelFactory = WalletViewModelFactory(application)
    val viewModel = ViewModelProvider(this, viewModelFactory)[WalletViewModel::class.java]

    setContent {
      val themeState by viewModel.themeState.collectAsState()
      MyApplicationTheme(darkTheme = themeState == "black") {
        NovaPayAppContent(viewModel = viewModel)
      }
    }
  }
}
