package com.ient.cartracker.library

import android.app.Application
import android.content.Context
import android.content.SharedPreferences

class baseApp : Application() {

    companion object {
        lateinit var mInstance: baseApp
        var currentActivity : baseActivity? = null
    }

    override fun onCreate() {
        super.onCreate()
        mInstance = this

    }

    fun loadPreferences(name:String) : String?{
        var sharedPreferences = getSharedPreferences("settings", Context.MODE_PRIVATE)
        return if(sharedPreferences.contains(name)){
            sharedPreferences.getString(name,null)
        }else{
            null
        }
    }

    fun savePreferences(name : String, value : String){
        var sharedPreferences = getSharedPreferences("settings", Context.MODE_PRIVATE)
        var editor = sharedPreferences.edit()
        editor.putString(name,value)
        editor.commit()
    }

    fun clearPreferences(){
        var sharedPreferences = getSharedPreferences("settings", Context.MODE_PRIVATE)
        var editor = sharedPreferences.edit()
        editor.clear()
        editor.commit()
    }
}