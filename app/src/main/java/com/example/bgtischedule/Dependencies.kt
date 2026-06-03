package com.example.bgtischedule

import android.content.Context
import androidx.room.Room
import com.example.bgtischedule.datebase.AppDatabase

class Dependencies {
    private lateinit var applicationContext: Context

    fun init(context: Context) { applicationContext = context }

    private val appDatabase: AppDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            appDatabase::class.java,
            "database.db"
        )
            .createFromAsset("room_article.db")
            .build()
    }
}