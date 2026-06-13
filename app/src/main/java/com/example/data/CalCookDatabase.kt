package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        CalendarEvent::class,
        Recipe::class,
        MealPlan::class,
        GroceryItem::class,
        Substitution::class,
        UserProfile::class,
        BudgetCap::class
    ],
    version = 1,
    exportSchema = false
)
abstract class CalCookDatabase : RoomDatabase() {
    abstract val dao: CalCookDao

    companion object {
        @Volatile
        private var INSTANCE: CalCookDatabase? = null

        fun getInstance(context: Context): CalCookDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CalCookDatabase::class.java,
                    "cal_cook_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
