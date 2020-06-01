package com.example.bricklist

import android.app.Activity
import android.graphics.drawable.ColorDrawable
import android.os.AsyncTask
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.example.example.DbHelper

class ItemAdapter(private val context: Activity, private var items: ArrayList<ProjectActivity.Brick>) : ArrayAdapter<ProjectActivity.Brick>(context, R.layout.brick_list_item) {
    override fun getCount() : Int {
        return items.size
    }
    override fun getView(position: Int, view: View?, parent: ViewGroup): View {
        val inflater = context.layoutInflater
        val rowView = inflater.inflate(R.layout.brick_list_item, null, true)
        val item = items[position]
        val titleText = rowView.findViewById(R.id.title) as TextView
        val subtitleText = rowView.findViewById(R.id.description) as TextView
        val imageView = rowView.findViewById(R.id.icon) as ImageView
        val addButton = rowView.findViewById(R.id.add) as Button
        val subButton = rowView.findViewById(R.id.sub) as Button

        addButton.setOnClickListener {
            if(item.QuantityInSet > item.QuantityInStore) {
                ChangeQuantity(item.InventoryID, item.ItemID, 1).execute()
                item.QuantityInStore += 1
                subtitleText.text = "${item.ColorName}\nParts: ${item.QuantityInStore} of ${item.QuantityInSet}"
                if(item.QuantityInStore == item.QuantityInSet) {
                    rowView.background = ColorDrawable(0xFFEEFFEE.toInt())
                }
            }
        }

        subButton.setOnClickListener {
            if(item.QuantityInStore > 0) {
                ChangeQuantity(item.InventoryID, item.ItemID, -1).execute()
                item.QuantityInStore -= 1
                subtitleText.text = "${item.ColorName}\nParts: ${item.QuantityInStore} of ${item.QuantityInSet}"
                rowView.background = ColorDrawable(0x00FFFFFF.toInt())
            }
        }

        if(item.QuantityInStore == item.QuantityInSet) {
            rowView.background = ColorDrawable(0xFFEEFFEE.toInt())
        } else {
            rowView.background = ColorDrawable(0x00FFFFFF.toInt())
        }

        titleText.text = "${item.PartName} [${item.ItemID}]"
        subtitleText.text = "${item.ColorName}\nParts: ${item.QuantityInStore} of ${item.QuantityInSet}"

        if(item.ImageBmp != null) {
            imageView.setImageBitmap(item.ImageBmp)
        }

        return rowView
    }

    private inner class ChangeQuantity(val InventoryID : String, val ItemID : String, val amount : Int) : AsyncTask<Void, Void, Void?>()  {

        override fun doInBackground(vararg result : Void) : Void? {
            val myDatabase = DbHelper(context).readableDatabase
            myDatabase.execSQL("UPDATE InventoriesParts SET QuantityInStore = QuantityInStore + ? WHERE InventoryID = ? AND ItemID = ?", arrayOf(amount, InventoryID, ItemID))
            return null
        }

    }
}

