package com.example.bricklist

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.example.DbHelper
import kotlinx.android.synthetic.main.activity_project.*
import java.io.File
import java.net.URL

class ProjectActivity : AppCompatActivity() {
    private var projectId: String? = null
    private var items : ArrayList<Brick> = ArrayList<Brick>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_project)
        projectId = intent.getStringExtra("id")
        projectName.text = projectId
        LoadBrickData().execute()
    }

    override fun onStart() {
        super.onStart()
        refresh()
    }

    fun refresh () {
        val itemAdapter = ItemAdapter(this, items)
        brickList.adapter = itemAdapter
    }

    fun archive (v : View) {
        val myDatabase = DbHelper(applicationContext).readableDatabase
        myDatabase.execSQL("UPDATE Inventories SET Active = 0 WHERE Name = ?", arrayOf(projectId))
        startActivity(Intent(this, MainActivity::class.java))
    }

    fun exportMissing (v : View) {
        val myDatabase = DbHelper(applicationContext).readableDatabase
        val cursor = myDatabase.rawQuery("SELECT TypeID, ItemID, ColorID, QuantityInSet - QuantityInStore as Filled FROM InventoriesParts WHERE InventoryID = (SELECT id FROM Inventories WHERE Name = ?) AND QuantityInSet-QuantityInStore > 0", arrayOf(projectId))

        var xmlString = ""
        (1 .. cursor.count).forEach {
            cursor.moveToNext()
            val itemID = cursor.getString(cursor.getColumnIndex("ItemID"))
            val typeID = cursor.getString(cursor.getColumnIndex("TypeID"))
            val colorID = cursor.getString(cursor.getColumnIndex("ColorID"))
            val filled = cursor.getString(cursor.getColumnIndex("Filled"))
            val itemString = wrap("ITEMTYPE", typeID) +
                    wrap("ITEMID", itemID) +
                    wrap("COLOR", colorID) +
                    wrap("QTYFILLED", filled)
            xmlString = xmlString.plus(wrap("ITEM", itemString))
        }
        xmlString = wrap("INVENTORY", xmlString)

        val dir = File(getExternalFilesDir(null), "brickExport")
        dir.mkdir()
        val exportFile = File(dir, "$projectId.xml")

        exportFile.createNewFile()
        exportFile.writeText(xmlString)
        Toast.makeText(applicationContext, "File exported to /brickExport/$projectId.xml!", Toast.LENGTH_LONG).show()
    }

    fun wrap(tag : String, value : String) : String {
        return "<$tag>$value</$tag>\n"
    }

    private inner class LoadBrickData() : AsyncTask<Void, Void, ArrayList<Brick>>()  {

        override fun doInBackground(vararg params : Void): ArrayList<Brick>  {
            val myDatabase = DbHelper(applicationContext).readableDatabase
            val cursor = myDatabase.rawQuery(
                """
                SELECT 
                    IFNULL(Parts.Name, "Brick name") as PartName, 
                    IFNULL(Colors.Name, "Brick color") as ColorName,
                    IFNULL(Codes.Code, "Brick code") as Code,
                    InventoriesParts.ItemID as ItemID,
                    QuantityInSet, 
                    InventoryID,
                    QuantityInStore,
                    InventoriesParts.ColorID
                FROM InventoriesParts
                LEFT JOIN Parts ON Parts.Code = InventoriesParts.ItemID
                LEFT JOIN Colors ON Colors.id = InventoriesParts.ColorID
                LEFT JOIN Codes ON (Codes.ItemID = Parts.id and Codes.ColorID = Colors.id)
                WHERE InventoryID = (SELECT id FROM Inventories WHERE Name = ?)
                """, arrayOf(projectId))
            val items : ArrayList<Brick> = ArrayList<Brick>()
            (1 .. cursor.count).forEach {
                cursor.moveToNext()
                val partName = cursor.getString(cursor.getColumnIndex("PartName"))
                val colorName = cursor.getString(cursor.getColumnIndex("ColorName"))
                val code = cursor.getString(cursor.getColumnIndex("Code"))
                val itemID = cursor.getString(cursor.getColumnIndex("ItemID"))
                val inventoryID = cursor.getString(cursor.getColumnIndex("InventoryID"))
                val quantityInSet = cursor.getInt(cursor.getColumnIndex("QuantityInSet"))
                val quantityInStore = cursor.getInt(cursor.getColumnIndex("QuantityInStore"))

                var url : URL
                var bmp : Bitmap? = null
                if(code != "Brick code") {
                    try {
                        url = URL("https://www.lego.com/service/bricks/5/2/$code")
                        bmp = BitmapFactory.decodeStream(url.openConnection().getInputStream())
                    } catch(e : Exception) {
                        println(e.message)
                    }
                } else {
                    try {
                        url = URL("https://www.bricklink.com/PL/$itemID.jpg")
                        bmp = BitmapFactory.decodeStream(url.openConnection().getInputStream())
                    } catch(e : Exception) {
                        println(e.message)
                    }
                }

                val item = Brick(partName, colorName, code, itemID, inventoryID, quantityInSet, quantityInStore, bmp)
                items.add(item)
            }
            return items
        }

        override fun onPostExecute(result : ArrayList<Brick>) {
            items = result
            refresh()
        }

    }

    data class Brick(var PartName: String, var ColorName: String, var Code: String, var ItemID: String, var InventoryID : String, var QuantityInSet: Int, var QuantityInStore: Int, var ImageBmp: Bitmap?)
}