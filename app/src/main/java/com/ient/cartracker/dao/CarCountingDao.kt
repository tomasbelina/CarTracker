package com.ient.cartracker.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ient.cartracker.objects.CarCounting


@Dao
interface CarCountingDao {
    @Query("SELECT * FROM CarCounting")
    fun getAll(): List<CarCounting>

    @Query("SELECT * FROM CarCounting ORDER BY uid DESC LIMIT 1")
    fun getLast(): CarCounting

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(carCounting: CarCounting): Long

    @Query("SELECT * FROM CarCounting WHERE serverId IS NULL")
    fun loadUnsent(): List<CarCounting>

    @Query("SELECT * FROM CarCounting WHERE uid = :carCountingId")
    fun findById(carCountingId: Int): CarCounting

    @Update
    fun update (carCounting: CarCounting)
}