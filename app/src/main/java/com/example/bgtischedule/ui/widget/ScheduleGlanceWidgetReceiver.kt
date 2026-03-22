package com.example.bgtischedule.ui.widget

import com.example.bgtischedule.R
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

class ScheduleGlanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ScheduleGlanceWidget()
    val metadataResId = R.xml.widget_schedule_info
}