package com.calculator.app

import android.app.Application
import androidx.room.Room
import com.calculator.app.data.local.db.CalculatorDatabase
import com.calculator.app.data.local.preferences.UserPreferences

class CalculatorApplication : Application() {

    lateinit var database: CalculatorDatabase
        private set

    lateinit var userPreferences: UserPreferences
        private set

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(
            this,
            CalculatorDatabase::class.java,
            "calculator.db",
        )
            .addMigrations(CalculatorDatabase.MIGRATION_1_2)
            .build()
        userPreferences = UserPreferences(this)
    }
}
