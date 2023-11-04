package com.softwarelinkers.browser.activity

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.softwarelinkers.browser.activity.SessionlistActivity.Companion.sessionList
import com.softwarelinkers.browser.adapter.SessionListAdapter
import com.softwarelinkers.browser.databinding.ActivitySessionBinding
import com.softwarelinkers.browser.model.SessionList
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.ArrayList

class SessionlistActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySessionBinding
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SessionListAdapter

    companion object{
        var sessionList: ArrayList<SessionList> = ArrayList()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySessionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        var sharedPreferences: SharedPreferences = getSharedPreferences("MY_PRE", Context.MODE_PRIVATE)
        var userEmail = sharedPreferences.getString("email","")

        recyclerView = binding.rvSession
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = SessionListAdapter(this, sessionList)
        recyclerView.adapter = adapter

        loadSessionToApi(userEmail)


        binding.addSession.setOnClickListener {
            val intent = Intent(this@SessionlistActivity,SessionActivity::class.java)
            startActivity(intent)
        }

    }


    fun loadSessionToApi(userEmail: String?) {
        val client = OkHttpClient()
        val baseUrl = "https://development.softwarelinkers.in/browser/api/get_session.php"

        val url = "$baseUrl?email=$userEmail"


        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {

            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
//                Toast.makeText(sessionlistActivity,"Api Successfully loaded",Toast.LENGTH_SHORT).show()

                    if (responseBody?.contains("\"status\":\"success\"") == true) {
                        val jsonData = JSONObject(responseBody) // Assuming you're using a JSON library
                        val dataArray = jsonData.getJSONArray("data")

                        // Loop through the data and display it
                        sessionList.clear()
                        for (i in 0 until dataArray.length()) {
                            val item = dataArray.getJSONObject(i)
                            val id = item.getString("id")
                            val session_name = item.getString("session_name")
                            val session_type = item.getString("session_type")
                            sessionList.add(
                                SessionList(id = id, session_name = session_name,session_type=session_type)
                            )
                            Log.d("data", sessionList.toString())
                        }

                        runOnUiThread {
                            adapter.notifyDataSetChanged()
                        }


                    } else {

                    }
                } else {}
            }
        })
    }

}



