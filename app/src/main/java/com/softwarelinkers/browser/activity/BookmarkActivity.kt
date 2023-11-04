package com.softwarelinkers.browser.activity

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.softwarelinkers.browser.R
import com.softwarelinkers.browser.adapter.AllBookmarkAdapter
import com.softwarelinkers.browser.databinding.ActivityBookmarkBinding
import com.softwarelinkers.browser.model.Bookmarknew
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class BookmarkActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityBookmarkBinding.inflate(layoutInflater)
        setContentView(binding.root)

        var sharedPreferences: SharedPreferences = getSharedPreferences("MY_PRE", Context.MODE_PRIVATE)
        var sessionId = sharedPreferences.getString("SESSION_ID","")
        var userEmail = sharedPreferences.getString("email","")

        loadBookmarkFromApi(userEmail,sessionId,binding.rvBookmarks)

    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun loadBookmarkFromApi(userEmail: String?, sessionId: String?, recyclerView: RecyclerView) {

        val baseUrl = getString(R.string.allbookmark_url)

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

//                        val bookmarkList = ArrayList<Bookmarknew>()
                        MainActivity.allBookmarkList.clear()
                        for (i in 0 until dataArray.length()) {
                            val item = dataArray.getJSONObject(i)
                            val title = item.getString("title")
                            val id = item.getString("id")
                            val short_url = item.getString("shortened_url")
                            MainActivity.allBookmarkList.add(
                                Bookmarknew(id = id, name = title, url = short_url)
                            )
                        }

                        // Update the RecyclerView on the UI thread
                        launch(Dispatchers.Main) {
                            val recyclerViewAdapter = AllBookmarkAdapter(this@BookmarkActivity, isActivity = true)
                            recyclerView.setHasFixedSize(true)
                            recyclerView.layoutManager = LinearLayoutManager(this@BookmarkActivity)
                            recyclerView.adapter = recyclerViewAdapter

//                            recyclerViewAdapter.setData
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