package com.ient.cartracker.ui

import android.content.Context
import android.content.DialogInterface
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.text.HtmlCompat
import com.ient.cartracker.R
import com.ient.cartracker.activities.MainActivity
import com.ient.cartracker.activities.countingTimer
import com.ient.cartracker.dao.CarDao
import com.ient.cartracker.library.appDatabase
import com.ient.cartracker.library.baseApp
import com.ient.cartracker.objects.Car
import com.loopj.android.http.AsyncHttpClient
import com.loopj.android.http.TextHttpResponseHandler
import cz.msebera.android.httpclient.Header
import cz.msebera.android.httpclient.entity.StringEntity
import org.json.JSONArray
import org.json.JSONException
import java.util.Date


class carRow:ConstraintLayout {

    var licensePlateTv:TextView
    var descriptionTv:TextView
    var lidlBtn: Button
    var outsideBtn : Button
    var timerTv:TextView
    var goOut:Button
    var carObject : Car
    var timer : Handler
    var carDao : CarDao
    var view : View


    constructor(context: Context, car : Car) : super(context) {
        view = View.inflate(context, R.layout.car_item, null)
        licensePlateTv = view.findViewById(R.id.license_plate)
        descriptionTv = view.findViewById(R.id.description)
        lidlBtn = view.findViewById(R.id.lidl)
        outsideBtn = view.findViewById(R.id.outside)
        timerTv = view.findViewById(R.id.timer)
        goOut = view.findViewById(R.id.go_out)
        carObject = car
        timer = Handler(Looper.getMainLooper())
        carDao = appDatabase.getInstance(context)!!.carDao()

        val scale = resources.displayMetrics.density
        val dp_75 = (75.0f * scale + 0.5f).toInt()
        var params =  ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT,ConstraintLayout.LayoutParams.WRAP_CONTENT)

        val dp_10 = (10.0f * scale + 0.5f).toInt()

        params.setMargins(dp_10,dp_10,dp_10,dp_10)
        view.layoutParams = params
        addView(view)

        licensePlateTv.text = carObject.licensePlate
        descriptionTv.text = carObject.description
        startTimer()
        setupButtons()
    }

    private fun deleteAndSend(){
        (this.parent as ViewGroup).removeView(this)
        (baseApp.currentActivity as MainActivity).calculateParkedCars()
        sendToSever()
    }

    private fun setupButtons() {
        checkButtons()
        lidlBtn.setOnClickListener(OnClickListener {
            carObject.lidl = !carObject.lidl
            carDao.update(carObject)
            checkButtons()
        })
        outsideBtn.setOnClickListener(OnClickListener {
            carObject.outside = !carObject.outside
            carDao.update(carObject)
            checkButtons()
        })

        goOut.setOnClickListener(OnClickListener {
            if(getMinutesDiff(carObject.getparketAtDate()) < 90){
                carObject.leftAt = carObject.getNow()
                carDao.update(carObject)
               deleteAndSend()
            }else{
                AlertDialog.Builder(context)
                    .setTitle("Odjezd auta")
                    .setCancelable(false)
                    .setPositiveButton("Ukončit sledování") {
                            dialog: DialogInterface, _: Int ->
                        dialog.dismiss()
                        carObject.leftAt = carObject.getNow()
                        carDao.update(carObject)
                        deleteAndSend()
                    }
                    .setNegativeButton("Nevšiml jsem si odjezdu") {
                            dialog: DialogInterface, _: Int ->
                        dialog.dismiss()
                        carObject.leftAt = carObject.getNow()
                        carObject.leftType = 2
                        carDao.update(carObject)
                        deleteAndSend()
                    }
                    .show()
            }
        })
    }

    private fun startTimer(){
        timer.post(object : Runnable {
            override fun run() {
                timerTv.text = getFormattedDiff(carObject.getparketAtDate())
                timer.postDelayed(this, 1000)
            }
        })
    }

    private fun checkButtons(){
        if(carObject.lidl){
            lidlBtn.setBackgroundColor(resources.getColor(R.color.checked,null))
        }else{
            lidlBtn.setBackgroundColor(resources.getColor(R.color.unchecked,null))
        }

        if (carObject.outside){
            outsideBtn.setBackgroundColor(resources.getColor(R.color.checked,null))
        }else{
            outsideBtn.setBackgroundColor(resources.getColor(R.color.unchecked,null))
        }
    }

    private fun getFormattedDiff(date2: Date):String{
        val date1 = Date()
        val diff: Long = date1.time - date2.time
        val seconds = diff / 1000
        val minutes = (seconds / 60).toInt()
        val final_sec = seconds.toInt() - (minutes * 60)
        if(minutes in 60..89){
            timerTv.setTextColor(resources.getColor(R.color.orange,null))
        }else if(minutes > 89){
            timerTv.setTextColor(resources.getColor(R.color.red,null))
        }
        return String.format("%02d:%02d", minutes,final_sec)
    }

    private fun getMinutesDiff(date2 : Date):Int{
        val date1 = Date()
        val diff: Long = date1.time - date2.time
        val seconds = diff / 1000
        val minutes = (seconds / 60).toInt()
        return minutes
    }

    private fun sendToSever(){
        val client = AsyncHttpClient()
        client.responseTimeout = 30000
        client.setTimeout(30000)
        client.maxConnections = 100
        val jsonCars = JSONArray()

        for (car in carDao.loadUnsent()) {
            jsonCars.put(car.json())
            Log.e("JSON",car.json().toString())
        }
        val entity = StringEntity(jsonCars.toString(), "UTF-8")

        client.post(
            context,
            "http://coffee.ient.cloud/cars",
            entity,
            "application/json",
            object : TextHttpResponseHandler() {
                override fun onFailure(
                    statusCode: Int,
                    headers: Array<Header?>?,
                    responseString: String?,
                    throwable: Throwable?
                ) {
                }

                override fun onSuccess(
                    statusCode: Int,
                    headers: Array<Header?>?,
                    responseString: String?
                ) {
                    try {
                        val serverCars = JSONArray(responseString)
                        for (i in 0 until serverCars.length()) {
                            val carJSON = serverCars.getJSONObject(i)
                            val car: Car = carDao.findById(carJSON.getInt("id"))
                            car.serverId = carJSON.getInt("server_id")
                            carDao.update(car)
                        }
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                }
            })
    }
}