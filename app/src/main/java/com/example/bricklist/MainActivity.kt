package com.example.bricklist

import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.example.DbHelper
import kotlinx.android.synthetic.main.activity_main.*
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.URL


class MainActivity : AppCompatActivity() {

    var url : String = "http://fcds.cs.put.poznan.pl/MyWeb/BL/"
    var showArchived : Int = 0

    fun handleProjectAdd(v : View) {
        val projectNumber = projectInput.text.toString()
        val projectName = nameInput.text.toString()
        GetAndSaveProject(projectNumber, projectName).execute()
    }

    fun goToSettings(v : View) {
        startActivity(Intent(this, SettingsActivity::class.java))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        projectList.setOnItemClickListener { _, _, position, _ ->
            val element = projectList.adapter.getItem(position)
            val intent = Intent(this, ProjectActivity::class.java)
            val extraValue = element.toString()
            intent.putExtra("id", extraValue as String)
            UpdateLastAccessed(extraValue).execute()
            startActivity(intent)
        }
    }

    override fun onStart() {
        super.onStart()

        val settingsFile = File(filesDir, "settings.txt")
        settingsFile.createNewFile()
        val settingsStr = settingsFile.readText()

        val settings = settingsStr.split(" ")

        if(settings.count() == 2) {
            url = settings[0]
            showArchived = settings[1].toInt()
        } else {
            url = "http://fcds.cs.put.poznan.pl/MyWeb/BL/"
            showArchived = 0
        }

        refresh()
    }

    fun refresh () {
        val myDatabase = DbHelper(applicationContext).readableDatabase
        val cursor = when(showArchived) {
            0 -> myDatabase.rawQuery("SELECT Name FROM Inventories WHERE Active = 1 ORDER BY LastAccessed", arrayOf())
            else -> myDatabase.rawQuery("SELECT Name FROM Inventories ORDER BY LastAccessed", arrayOf())
        }

        val projects = (1 .. cursor.count).map {
            cursor.moveToNext()
            cursor.getString(cursor.getColumnIndex("Name"))
        }
        val adapter = ArrayAdapter<String>(
            applicationContext,
            android.R.layout.simple_spinner_item,
            projects
        )
        projectList.adapter = adapter
    }

    private inner class UpdateLastAccessed(private val projectNumber: String) : AsyncTask<Void, Void, Void>() {

        override fun doInBackground(vararg params: Void): Void? {
            val myDatabase = DbHelper(applicationContext).readableDatabase
            val insertProjectSQL = ("UPDATE Inventories SET LastAccessed = strftime('%s', 'now') WHERE Name = ?")
            myDatabase.execSQL(insertProjectSQL, arrayOf(projectNumber))
            return null
        }
    }


    private inner class GetAndSaveProject(private val projectNumberRef : String, private val projectNameRef : String) : AsyncTask<Void, Void, Int>() {

        val projectNumber : String = projectNumberRef
        val projectName : String = projectNameRef

        override fun doInBackground(vararg params: Void) : Int {
            val url = URL("$url${projectNumber}.xml")
            val conn : HttpURLConnection
            val rdr : BufferedReader
            try {
                conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.doInput = true
                conn.readTimeout = 10000
                conn.connectTimeout = 15000
                conn.connect()
                rdr = BufferedReader(InputStreamReader(conn.inputStream))
            } catch (e : Exception) {
                return 0
            }

            val fileContent = rdr.readLines().joinToString("\n")
            val projectFile = File(filesDir, "$projectNumber.xml")
            projectFile.createNewFile()
            projectFile.writeText(fileContent)

            val inventory = parseAs<Inventory>(projectFile)

            val myDatabase = DbHelper(applicationContext).readableDatabase
            //myDatabase.execSQL("DELETE FROM InventoriesParts WHERE 1 == 1")
            //myDatabase.execSQL("DELETE FROM Inventories WHERE 1 == 1")
            val insertProjectSQL = ("INSERT INTO Inventories (Name, LastAccessed) VALUES (?, strftime('%s', 'now'))")
            myDatabase.execSQL(insertProjectSQL, arrayOf(projectName))

            val projectIdCursor = myDatabase.rawQuery("SELECT id FROM Inventories WHERE Name = ?", arrayOf(projectName))
            projectIdCursor.moveToFirst()
            val projectId = projectIdCursor.getString(projectIdCursor.getColumnIndex("id"))
            val insertPartSQL = ("INSERT INTO InventoriesParts (InventoryID, TypeID, ItemID, QuantityInSet, QuantityInStore, ColorID, Extra) VALUES (?, ?, ?, ?, ?, ?, ?)")
            inventory.items.forEach {
                myDatabase.execSQL(insertPartSQL, arrayOf(
                    projectId,
                    it.type,
                    it.id,
                    it.quantity,
                    0,
                    it.color,
                    it.extra
                ))
            }
            return 1
        }

        override fun onPostExecute(result: Int) {
            if(result == 0) {
                Toast.makeText(applicationContext, "Project not found!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(applicationContext, "Project added!", Toast.LENGTH_LONG).show()
            }
            refresh()
        }
    }
}

