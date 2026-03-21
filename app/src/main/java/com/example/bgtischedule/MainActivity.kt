package com.example.bgtischedule

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.bgtischedule.ui.AppRoot
import com.example.bgtischedule.ui.theme.BGTIScheduleTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BGTIScheduleTheme {
                AppRoot()
            }
        }
    }
}