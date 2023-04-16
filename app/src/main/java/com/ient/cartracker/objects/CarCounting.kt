package com.ient.cartracker.objects

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.json.JSONException
import org.json.JSONObject
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date

@Entity
data class CarCounting(
    @PrimaryKey(autoGenerate = true) val uid: Long = 0,
    var tracker : String = "",
    var countedAt : String = "",
    var carCount : Int = 0,
    var serverId : Int? = null,
)

{
    @Throws(JSONException::class)
    fun json(): JSONObject? {
        val json = JSONObject()
        json.put("id", this.uid)
        json.put("counted_at", this.countedAt)
        json.put("car_count", this.carCount)
        json.put("tracker", this.tracker)
        return json
    }

    fun getNow(): String {
        val dateFormat: DateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val date = Date()
        return dateFormat.format(date)
    }

    fun getCountedAtDate(): Date{
        val dateFormat: DateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val dt = dateFormat.parse(this.countedAt)
        return dt
    }
}