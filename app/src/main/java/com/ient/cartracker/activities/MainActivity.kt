package com.ient.cartracker.activities



import android.app.Dialog
import android.app.ProgressDialog
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.util.Log
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.ient.cartracker.R
import com.ient.cartracker.dao.CarCountingDao
import com.ient.cartracker.dao.CarDao
import com.ient.cartracker.library.appDatabase
import com.ient.cartracker.library.baseActivity
import com.ient.cartracker.library.baseApp
import com.ient.cartracker.objects.Car
import com.ient.cartracker.objects.CarCounting
import com.ient.cartracker.ui.carRow
import com.loopj.android.http.AsyncHttpClient
import com.loopj.android.http.RequestParams
import com.loopj.android.http.TextHttpResponseHandler
import cz.msebera.android.httpclient.Header
import cz.msebera.android.httpclient.entity.StringEntity
import org.json.JSONArray
import org.json.JSONException
import java.io.FileNotFoundException
import java.util.Date


lateinit var carDao : CarDao
lateinit var carCountDao : CarCountingDao
lateinit var topTimer : Handler
lateinit var countingTimer : TextView
lateinit var currentCars : TextView
lateinit var addCar : Button
lateinit var carsBox : LinearLayout


class MainActivity : baseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        carDao = appDatabase.getInstance(applicationContext)!!.carDao()
        carCountDao = appDatabase.getInstance(applicationContext)!!.carCountingDao()
        countingTimer = findViewById(R.id.cars_time)
        currentCars = findViewById(R.id.current_cars_count)
        addCar = findViewById(R.id.add_car)
        carsBox = findViewById(R.id.cars_box)
        topTimer = Handler(Looper.getMainLooper())

        //start timer
        topTimer.post(object : Runnable {
            override fun run() {
                calculateTopTime()
                topTimer.postDelayed(this, 1000)
            }
        })

        countingTimer.setOnClickListener(View.OnClickListener {
            openTimerModal()
//            var lastCount = carCountDao.getLast()
//            if(lastCount != null){
//                if(getMinutesDiff(lastCount.getCountedAtDate()) > 59){
//                    openTimerModal()
//                }
//            }else{
//                openTimerModal()
//            }
        })

        addCar.setOnClickListener(View.OnClickListener {
            var dialog =  showDialog()
        })

        loadAllCars()

    }

    private fun loadAllCars(){
        var cars = carDao.loadParked()
        for(car in cars){
            carsBox.addView(carRow(this,car))
        }
        calculateParkedCars()
    }

    public fun calculateParkedCars(){
        var count = carsBox.childCount
        currentCars.text = count.toString()
        if(count < 10){
            currentCars.setTextColor(Color.parseColor("#cc2900"))
        }else{
            currentCars.setTextColor(Color.parseColor("#74A83B"))
        }
    }

    override fun onBackPressed() {
        openDeactivationDialog()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.upload_db) {
           sendDatabase()
        } else if (id == R.id.send_all) {
            try {
                sendAllCars()
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun calculateTopTime(){
        var lastCount = carCountDao.getLast()
        if(lastCount != null){
            countingTimer.text = getFormattedDiff(lastCount.getCountedAtDate())
        }
    }

    private fun getFormattedDiff(date2: Date):String{
        val date1 = Date()
        val diff: Long = date1.time - date2.time
        val seconds = diff / 1000
        val minutes = (seconds / 60).toInt()
        val final_sec = seconds.toInt() - (minutes * 60)

        if(minutes>59){
            countingTimer.setTextColor(resources.getColor(R.color.red,null))
        }else{
            countingTimer.setTextColor(resources.getColor(R.color.white,null))
        }

        return String.format("%02d:%02d", minutes,final_sec)
    }

    private fun getMinutesDiff(date2: Date):Int{
        val date1 = Date()
        val diff: Long = date1.time - date2.time
        val seconds = diff / 1000
        val minutes = (seconds / 60).toInt()
       return minutes
    }

    private fun openTimerModal(){

        val count_input = EditText(this)
        count_input.inputType = InputType.TYPE_CLASS_NUMBER
        count_input.textAlignment = View.TEXT_ALIGNMENT_CENTER
        count_input.isScrollContainer = true



        var dialog = AlertDialog.Builder(this)
            .setTitle("Zadejte aktuální počet vozidel na parkovišti")
            .setCancelable(false)
            .setView(count_input)
            .setPositiveButton("Uložit") {
                    dialog: DialogInterface, _: Int ->
                dialog.dismiss()
                var newCount = CarCounting()
                newCount.countedAt = newCount.getNow()
                newCount.tracker = baseApp.mInstance.loadPreferences("user")!!
                newCount.carCount = count_input.text.toString().toInt()
                carCountDao.insert(newCount)
                sendAllCarCountSilent()
                createToast("Zaznamenáno.")
            }
            .setNegativeButton("Zrušit") {
                    dialog: DialogInterface, _: Int ->
                dialog.dismiss()
            }
            .show()

        count_input.setOnKeyListener { _, keyCode, event ->
            if(keyCode == KeyEvent.KEYCODE_ENTER){
                dialog.dismiss()
                var newCount = CarCounting()
                newCount.countedAt = newCount.getNow()
                newCount.tracker = baseApp.mInstance.loadPreferences("user")!!
                newCount.carCount = count_input.text.toString().toInt()
                carCountDao.insert(newCount)
                sendAllCarCountSilent()
                createToast("Zaznamenáno.")
                true
            }
            false
        }

        dialog.window!!.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
    }

    private fun sendAllCarCountSilent(){
        val client = AsyncHttpClient()
        client.responseTimeout = 30000
        client.setTimeout(30000)
        client.maxConnections = 100
        val jsonCarsCount = JSONArray()

        for (carsCount in carCountDao.loadUnsent()) {
            jsonCarsCount.put(carsCount.json())
            Log.e("SEND",carsCount.json().toString())
        }
        val entity = StringEntity(jsonCarsCount.toString(), "UTF-8")

        client.post(
            this@MainActivity,
            "http://coffee.ient.cloud/cars/waves",
            entity,
            "application/json",
            object : TextHttpResponseHandler() {
                override fun onFailure(
                    statusCode: Int,
                    headers: Array<Header?>?,
                    responseString: String?,
                    throwable: Throwable?
                ) {
                    createToast(responseString!!)
                }

                override fun onSuccess(
                    statusCode: Int,
                    headers: Array<Header?>?,
                    responseString: String?
                ) {
                    try {
                        val serverCarsCounting = JSONArray(responseString)
                        for (i in 0 until serverCarsCounting.length()) {
                            val carCountingJSON = serverCarsCounting.getJSONObject(i)
                            val carCounting: CarCounting = carCountDao.findById(carCountingJSON.getInt("id"))
                            carCounting.serverId = carCountingJSON.getInt("server_id")
                            carCountDao.update(carCounting)
                        }
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                }
            })
    }


    fun showDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.add_car_modal)
        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        var button = dialog.findViewById<Button>(R.id.add_car_btn)
        var licensePlate = dialog.findViewById<TextView>(R.id.license_plate_input)
        var description = dialog.findViewById<TextView>(R.id.description_input)

        button.setOnClickListener(View.OnClickListener {
            var licensePlateTrim = licensePlate.text.toString().trim()
            var descriptionTrim = description.text.toString().trim()

            if(licensePlateTrim.isNullOrEmpty() and descriptionTrim.isNullOrEmpty()){
                dialog.dismiss()

            }else{
                var newCar = Car()
                newCar.licensePlate = licensePlateTrim
                newCar.description = descriptionTrim
                newCar.tracker = baseApp.mInstance.loadPreferences("user")!!
                newCar.parkedAt = newCar.getNow()
                newCar.uid = carDao.insert(newCar)
                dialog.dismiss()
                carsBox.addView(carRow(this,newCar))
                calculateParkedCars()
            }
        })
        dialog.show()
    }


    private fun sendAllCarCount(){
        val dialog = ProgressDialog(this)
        dialog.setMessage("Odesílám záznamy. Vyčkejte prosím.")
        dialog.setCancelable(false)
        dialog.setInverseBackgroundForced(false)
        dialog.show()
        val client = AsyncHttpClient()
        client.responseTimeout = 30000
        client.setTimeout(30000)
        client.maxConnections = 100
        val jsonCarsCount = JSONArray()

        for (carsCount in carCountDao.loadUnsent()) {
            jsonCarsCount.put(carsCount.json())
            Log.e("SEND",carsCount.json().toString())
        }
        val entity = StringEntity(jsonCarsCount.toString(), "UTF-8")

        client.post(
            this@MainActivity,
            "http://coffee.ient.cloud/cars/waves",
            entity,
            "application/json",
            object : TextHttpResponseHandler() {
                override fun onFailure(
                    statusCode: Int,
                    headers: Array<Header?>?,
                    responseString: String?,
                    throwable: Throwable?
                ) {
                    createToast("Zkontrolujte připojení k internetu!","red")
                    dialog.dismiss()
                }

                override fun onSuccess(
                    statusCode: Int,
                    headers: Array<Header?>?,
                    responseString: String?
                ) {
                    try {
                        val serverCarsCounting = JSONArray(responseString)
                        for (i in 0 until serverCarsCounting.length()) {
                            val carCountingJSON = serverCarsCounting.getJSONObject(i)
                            val carCounting: CarCounting = carCountDao.findById(carCountingJSON.getInt("id"))
                            carCounting.serverId = carCountingJSON.getInt("server_id")
                            carCountDao.update(carCounting)
                            dialog.dismiss()
                            sendAllCars()
                        }
                    } catch (e: JSONException) {
                        dialog.dismiss()
                        createToast("Došlo k chybě, prosím zkuste znovu.","red")
                        e.printStackTrace()
                    }
                }
            })
    }

    private fun sendAllCars(){
        val dialog = ProgressDialog(this)
        dialog.setMessage("Odesílám záznamy. Vyčkejte prosím.")
        dialog.setCancelable(false)
        dialog.setInverseBackgroundForced(false)
        dialog.show()
        val client = AsyncHttpClient()
        client.responseTimeout = 30000
        client.setTimeout(30000)
        client.maxConnections = 100
        val jsonCars = JSONArray()

        for (car in carDao.loadUnsent()) {
            jsonCars.put(car.json())
        }
        val entity = StringEntity(jsonCars.toString(), "UTF-8")

        client.post(
            this,
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
                    createToast("Zkontrolujte připojení k internetu!","red")
                    dialog.dismiss()
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
                        dialog.dismiss()
                        createToast("Záznamy úspěšně odeslány.")
                    } catch (e: JSONException) {
                        createToast("Došlo k chybě, prosím zkuste znovu.","red")
                        dialog.dismiss()
                        e.printStackTrace()
                    }
                }
            })
    }

    fun sendDatabase() {
        val mDialog = ProgressDialog(this@MainActivity)
        mDialog.setMessage("Prosím vyčkejte. Databáze se odesílá...")
        mDialog.setCancelable(false)
        mDialog.show()
        val db = getDatabasePath("car_tracker.db")
        val shm = getDatabasePath("car_tracker.db-shm")
        val wal = getDatabasePath("car_tracker.db-wal")
        val params = RequestParams()
        try {
            params.put("db", db)
            if (shm.exists()) {
                params.put("db-shm", shm)
            }
            if (wal.exists()) {
                params.put("db-wal", wal)
            }
            params.put("username", baseApp.mInstance.loadPreferences("user"))
            val client = AsyncHttpClient()
            client.responseTimeout = 10000
            client.setTimeout(10000)
            client.post("http://coffee.ient.cloud/db", params, object : TextHttpResponseHandler() {
                override fun onFailure(
                    statusCode: Int,
                    headers: Array<Header>,
                    responseString: String,
                    throwable: Throwable
                ) {
                    mDialog.dismiss()
                    createToast("Zkontrolujte připojení k internetu.","red")
                }

                override fun onSuccess(
                    statusCode: Int,
                    headers: Array<Header>,
                    responseString: String
                ) {
                    mDialog.dismiss()
                    createToast("Databáze byla odeslána")
                }
            })
        } catch (e: FileNotFoundException) {
            mDialog.dismiss()
        }
    }

}