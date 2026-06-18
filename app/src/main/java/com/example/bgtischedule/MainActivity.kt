package com.example.bgtischedule

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.bgtischedule.ui.AppRoot
import com.example.bgtischedule.ui.theme.BGTIScheduleTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    Log.i(TAG, "onCreate start")
    try {
      enableEdgeToEdge()
      setContent {
        BGTIScheduleTheme {
          AppRoot()
        }
      }
      Log.i(TAG, "onCreate setContent done")
    } catch (e: Exception) {
      Log.e(TAG, "onCreate failed", e)
      throw e
    }
  }

  private companion object {
    private const val TAG = "MainActivity"
  }
}