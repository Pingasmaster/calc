package com.calculator.app

import android.app.Application
import androidx.room.Room
import com.calculator.app.data.local.db.CalculatorDatabase

class CalculatorApplication : Application() {

    lateinit var database: CalculatorDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(
            this,
            CalculatorDatabase::class.java,
            "calculator.db",
        ).build()
    }
}
