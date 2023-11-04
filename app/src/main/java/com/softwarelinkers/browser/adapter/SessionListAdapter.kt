package com.softwarelinkers.browser.adapter

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.TextView
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.recyclerview.widget.RecyclerView
import com.softwarelinkers.browser.R
import com.softwarelinkers.browser.activity.MainActivity
import com.softwarelinkers.browser.activity.checkForInternet
import com.softwarelinkers.browser.model.SessionList


class SessionListAdapter( private val context: Context,private val sessionList: List<SessionList>): RecyclerView.Adapter<SessionListAdapter.MyHolder>(){



    class MyHolder(view: View): RecyclerView.ViewHolder(view) {
        val sessionName: TextView = view.findViewById(R.id.session_n)
        val sessionType: TextView = view.findViewById(R.id.session_t)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
         return MyHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.session_rv_item, parent, false)
            )
    }

    override fun onBindViewHolder(holder: SessionListAdapter.MyHolder, position: Int) {
        holder.sessionName.text = sessionList[position].session_name
        holder.sessionType.text = sessionList[position].session_type

        holder.sessionName.setOnClickListener {
            if (checkForInternet(context)) {
                val sessionId = sessionList[position].id
                val sessionType = sessionList[position].session_type
                val sessionName = sessionList[position].session_name

                // Create a unique SharedPreferences file for each session
                val sessionSharedPreferencesName = "SESSION_PREF_$sessionId"

                val sharedPreferences = context.getSharedPreferences(sessionSharedPreferencesName, Context.MODE_PRIVATE)
                val editor: SharedPreferences.Editor = sharedPreferences.edit()
                editor.putString("SESSION_ID", sessionId)
                editor.putString("SESSION_TYPE", sessionType)
                editor.putString("SESSION_NAME", sessionName)
                editor.putInt("SESSION_POSITION",position)
                editor.apply()

                val i = Intent(context, MainActivity::class.java)
                context.startActivity(i)
                (context as Activity).finish()

            }
        }
    }


    override fun getItemCount(): Int {
        return sessionList.size
    }
}