package com.example.bricklist

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_settings.*
import java.io.File

class SettingsActivity : AppCompatActivity() {
    fun Boolean.toInt() = if (this) 1 else 0

    fun save(v : View) {
        val settingsFile = File(filesDir, "settings.txt")
        settingsFile.delete()
        settingsFile.createNewFile()
        settingsFile.writeText("${urlInput.text.toString()} ${showArchivedSwitch.isChecked.toInt()}")
        refresh()
        startActivity(Intent(this, MainActivity::class.java))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
    }

    override fun onStart() {
        super.onStart()
        refresh()
    }

    fun refresh() {
        val settingsFile = File(filesDir, "settings.txt")
        settingsFile.createNewFile()

        val settingsStr = settingsFile.readText()

        val settings = settingsStr.split(" ")

        if(settings.count() == 2) {
            showArchivedSwitch.isChecked = when(settings[1].toInt()){
                0 -> false
                else -> true
            }
            urlInput.hint = settings[0]
        } else {
            urlInput.hint = "http://fcds.cs.put.poznan.pl/MyWeb/BL/"
            showArchivedSwitch.isChecked = false
        }
    }


}