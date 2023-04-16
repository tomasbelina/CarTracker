package com.ient.cartracker.objects


import androidx.room.Entity
import androidx.room.PrimaryKey
import org.json.JSONException
import org.json.JSONObject
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date


@Entity
data class Car(
    @PrimaryKey(autoGenerate = true) var uid: Long = 0,
    var tracker : String = "",
    var parkedAt : String = "",
    var leftAt : String = "",
    var leftType : Int = 1,
    var lidl : Boolean = false,
    var outside : Boolean = false,
    var licensePlate : String = "",
    var description : String = "",
    var serverId : Int? = null,
    )
{
    @Throws(JSONException::class)
    fun json(): JSONObject? {
        val json = JSONObject()
        json.put("id", this.uid)
        json.put("parked_at", this.parkedAt)
        json.put("left_at", this.leftAt)
        json.put("left_type", this.leftType)
        json.put("lidl", this.lidl)
        json.put("outside", this.outside)
        json.put("license_plate", this.licensePlate)
        json.put("description", this.description)
        json.put("tracker",this.tracker)
        return json
    }

    fun getNow(): String {
        val dateFormat: DateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val date = Date()
        return dateFormat.format(date)
    }

    fun getparketAtDate(): Date{
        val dateFormat: DateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val dt = dateFormat.parse(this.parkedAt)
        return dt
    }
}
