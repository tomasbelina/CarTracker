package com.ient.cartracker.library

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.Gravity
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.text.HtmlCompat


open class baseActivity : AppCompatActivity() {
    lateinit var toast: Toast

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        baseApp.currentActivity = this
    }

    fun createToast(text : String, color : String = "gray"){
        toast = Toast.makeText(this,
            HtmlCompat.fromHtml("<font color='$color'>$text</font>", HtmlCompat.FROM_HTML_MODE_LEGACY),
            Toast.LENGTH_LONG)
        toast.setGravity(Gravity.CENTER,0,0)
        toast.show()
    }

    fun openDeactivationDialog(){
        AlertDialog.Builder(this)
            .setTitle("Odhlášení aplikace")
            .setMessage("Opravdu chcete odhlásit uživatele?")
            .setCancelable(false)
            .setPositiveButton("Ano") {
                    dialog: DialogInterface, _: Int ->
                dialog.dismiss()
                baseApp.mInstance.clearPreferences()
                finish()
            }
            .setNegativeButton("Ne") {
                    dialog: DialogInterface, _: Int ->
                dialog.dismiss()
            }
            .show()
    }

    fun EditText.showKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
    }

    fun EditText.hideKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(this.windowToken, 0)
    }
}