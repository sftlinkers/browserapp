package com.softwarelinkers.browser.adapter

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.softwarelinkers.browser.R
import com.softwarelinkers.browser.activity.MainActivity
import com.softwarelinkers.browser.activity.newbcActivity
import com.softwarelinkers.browser.activity.checkForInternet
import com.softwarelinkers.browser.model.newbc

class newbcAdapter(private val context: Context, private val newbc: List<newbc>): RecyclerView.Adapter<newbcAdapter.MyHolder>(){


    class MyHolder(view: View): RecyclerView.ViewHolder(view) {
        val stitle: TextView = view.findViewById(R.id.stitle)
        val sshortened_url: TextView = view.findViewById(R.id.sshortened_url)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
         return MyHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.newbc_item, parent, false)
            )
    }

    override fun onBindViewHolder(holder: newbcAdapter.MyHolder, position: Int) {

        holder.stitle.text = newbc[position].stitle
        holder.sshortened_url.text = newbc[position].sshortened_url
        holder.stitle.setOnClickListener {
            when{
                checkForInternet(context) -> {

                    val sharedPreferences = context.getSharedPreferences("MY_PRE",Context.MODE_PRIVATE)
                    val editor: SharedPreferences.Editor= sharedPreferences.edit()
                    editor.putString("SESSION_ID", newbc[position].id)
                    editor.apply()

                    val i = Intent(context,MainActivity::class.java)
                    context.startActivity(i)
                    (context as Activity).finish()
                } }
        }
    }

    override fun getItemCount(): Int {
        return newbc.size
    }
}