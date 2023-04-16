package com.ient.cartracker.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import com.ient.cartracker.R
import com.ient.cartracker.library.baseActivity
import com.ient.cartracker.library.baseApp

class Login : baseActivity() {
    lateinit var userInput: EditText
    lateinit var loginButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        userInput = findViewById(R.id.login_input)
        loginButton = findViewById(R.id.login_button)
        if(!baseApp.mInstance.loadPreferences("user").isNullOrEmpty()){
            goToMain()
        }
        Log.e("USER",baseApp.mInstance.loadPreferences("user").toString())
        loginButton.setOnClickListener(View.OnClickListener {
            verifyUser()
        })
    }

    private fun goToMain(){
        val i = Intent(applicationContext, MainActivity::class.java)
        i.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        i.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(i)
        finish()
    }

    override fun onResume() {
        super.onResume()
    }

    private fun verifyUser(){
        var input = userInput.text.toString().trim()
        if(input.isNotEmpty()){
            baseApp.mInstance.savePreferences("user",input)
            goToMain()
        }else{
            createToast("Zadejte uživatele","red")
        }
    }
}