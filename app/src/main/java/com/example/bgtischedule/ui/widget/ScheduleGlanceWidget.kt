package com.example.bgtischedule.ui.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.*
import androidx.glance.text.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.background

class ScheduleGlanceWidget: GlanceAppWidget() {


    override suspend fun provideGlance(
        context: Context,
        id: GlanceId
    ) {
        provideContent {
            // Простой контейнер с фоном
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .background(color = Color(0xFF1A73E8))
            ) {
                Column(
                    modifier = GlanceModifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Расписание БГТИ"
                    )

                    Text(
                        text = "📅 Загрузка..."
                    )
                }
            }
        }
    }
}