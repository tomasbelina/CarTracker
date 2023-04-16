package com.ient.cartracker.library

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.ient.cartracker.dao.CarCountingDao
import com.ient.cartracker.dao.CarDao
import com.ient.cartracker.objects.Car
import com.ient.cartracker.objects.CarCounting


@Database(entities = [Car::class, CarCounting::class], version = 1)
abstract class appDatabase protected constructor() : RoomDatabase() {
    abstract fun carDao(): CarDao
    abstract fun carCountingDao(): CarCountingDao

    companion object {
        @Volatile
        private var instance: appDatabase? = null
        @Synchronized
        fun getInstance(context: Context): appDatabase? {
            if (instance == null) {
                instance = create(context)
            }
            return instance
        }

        private fun create(context: Context): appDatabase {
            return Room.databaseBuilder(
                context,
                appDatabase::class.java,
                "car_tracker.db"
            ).allowMainThreadQueries().build()
        }
    }
}