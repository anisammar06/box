package com.k650.remote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.k650.remote.ui.MainScreen
import com.k650.remote.ui.SoundbarViewModel
import com.k650.remote.ui.theme.K650Theme

class MainActivity : ComponentActivity() {

    private val viewModel: SoundbarViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            K650Theme {
                MainScreen(viewModel)
            }
        }
    }
}
