package com.example

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.ui.JarvisViewModel
import com.example.ui.MainScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val jarvisViewModel: JarvisViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle app exit command ("exit", "goodbye jarvis", etc.)
        lifecycleScope.launch {
            jarvisViewModel.shouldExitApp.collect { shouldExit ->
                if (shouldExit) {
                    finish()
                }
            }
        }

        setContent {
            MyApplicationTheme {
                MainScreen(
                    viewModel = jarvisViewModel,
                    onLaunchIntent = { intent ->
                        try {
                            startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(this, "Could not launch: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }
}
