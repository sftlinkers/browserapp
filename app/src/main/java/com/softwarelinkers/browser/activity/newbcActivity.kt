package com.softwarelinkers.browser.activity

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.softwarelinkers.browser.R
import com.softwarelinkers.browser.adapter.newbcAdapter
import com.softwarelinkers.browser.databinding.ActivitySessionBinding
import com.softwarelinkers.browser.model.newbc
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.ArrayList

class newbcActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySessionBinding
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: newbcAdapter

    companion object{
        var newbc: ArrayList<newbc> = ArrayList()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySessionBinding.inflate(layoutInflater)
        setContentView(R.layout.newbc)

        var sharedPreferences: SharedPreferences = getSharedPreferences("MY_PRE", MODE_PRIVATE)
        var userEmail = sharedPreferences.getString("email","")
        var sessionId = sharedPreferences.getString("SESSION_ID","")


        recyclerView = findViewById(R.id.rv_newbc)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = newbcAdapter(this, newbc)
        recyclerView.adapter = adapter

        loadSessionToApi(userEmail,sessionId)


//        binding.addSession.setOnClickListener {
//            val intent = Intent(this@newbcActivity,newbcActivity::class.java)
//            startActivity(intent)
//        }

    }


    fun loadSessionToApi(userEmail: String?, sessionId: String?) {
        val client = OkHttpClient()
        val baseUrl = "https://softwarelinkers.xyz/api/api/get_bookmark.php"

        val url = "$baseUrl?email=$userEmail&session_id=$sessionId"


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
                        newbc.clear()
                        for (i in 0 until dataArray.length()) {
                            val item = dataArray.getJSONObject(i)
                            val id = item.getString("id")
                            val stitle = item.getString("title")
                            val sshortened_url = item.getString("shortened_url")
                            newbc.add(
                                newbc(id = id, stitle = stitle,sshortened_url=sshortened_url)
                            )
                            Log.d("data", newbc.toString())
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



