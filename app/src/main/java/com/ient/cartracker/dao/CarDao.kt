package com.ient.cartracker.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ient.cartracker.objects.Car

@Dao
interface CarDao {
    @Query("SELECT * FROM Car")
    fun getAll(): List<Car>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(car: Car): Long

    @Query("SELECT * FROM Car WHERE serverId IS NULL AND leftAt != \"\"")
    fun loadUnsent(): List<Car>

    @Query("SELECT * FROM Car WHERE leftAt = \"\" ORDER BY uid")
    fun loadParked(): List<Car>

    @Query("SELECT * FROM Car WHERE uid = :carId")
    fun findById(carId: Int): Car

    @Update
    fun update (car: Car)
}