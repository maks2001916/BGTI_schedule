package com.example.bgtischedule

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import com.example.bgtischedule.ui.AppRoot
import com.example.bgtischedule.ui.theme.BGTIScheduleTheme

class MainActivity : ComponentActivity() {
  @RequiresApi(Build.VERSION_CODES.P)
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