package com.softwarelinkers.browser.activity

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.softwarelinkers.browser.adapter.AllBookmarkAdapter
import com.softwarelinkers.browser.adapter.HistoryAdapter
import com.softwarelinkers.browser.databinding.ActivityHistoryBinding
import com.softwarelinkers.browser.model.Bookmarknew
import com.softwarelinkers.browser.model.History
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

class HistoryActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        var sharedPreferences: SharedPreferences = getSharedPreferences("MY_PRE", Context.MODE_PRIVATE)
        var sessionId = sharedPreferences.getString("SESSION_ID","")
        var userEmail = sharedPreferences.getString("email","")

        loadHistoryToApi(userEmail,sessionId,binding.rvHistorys)

    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun loadHistoryToApi(userEmail: String?, sessionId: String?, recyclerView: RecyclerView) {
        val baseUrl = "https://softwarelinkers.xyz/api/api/get_history.php"
        val url = "$baseUrl?email=$userEmail&session_id=$sessionId"

        GlobalScope.launch(Dispatchers.IO) {
            try {
                val client = OkHttpClient()
                val request = Request.Builder()
                    .url(url)
                    .get()
                    .build()

                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    val responseBody = response.body?.string()

                    if (responseBody?.contains("\"status\":\"success\"") == true) {
                        val jsonData = JSONObject(responseBody) // Assuming you're using a JSON library
                        val dataArray = jsonData.getJSONArray("data")

                        MainActivity.historyList.clear()
                        for (i in 0 until dataArray.length()) {
                            val item = dataArray.getJSONObject(i)
                            val id = item.getString("id")
                            val session_url = item.getString("session_url")
                            MainActivity.historyList.add(
                                History(id = id, name = session_url,url=session_url)
                            )

                        }

                        // Update the RecyclerView on the UI thread
                        launch(Dispatchers.Main) {
                            val recyclerViewAdapter = HistoryAdapter(this@HistoryActivity, isActivity = true)
                            recyclerView.setHasFixedSize(true)
                            recyclerView.layoutManager = LinearLayoutManager(this@HistoryActivity)
                            recyclerView.adapter = recyclerViewAdapter

                            recyclerViewAdapter.notifyDataSetChanged()
                        }
                    } else {
                        // Handle the case where the API response indicates an error
                    }
                } else {
                    // Handle the case where the network call was not successful
                }
            } catch (e: Exception) {
                // Handle exceptions, e.g., network errors
            }
        }
    }

}