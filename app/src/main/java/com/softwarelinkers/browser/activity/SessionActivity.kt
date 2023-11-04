package com.softwarelinkers.browser.activity

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import com.google.gson.Gson
import com.softwarelinkers.browser.R
import com.softwarelinkers.browser.activity.MainActivity.Companion.sessionWebViews
import okhttp3.*
import org.json.JSONObject
import java.io.IOException


class SessionActivity : AppCompatActivity() {

    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var fullname: EditText
    private lateinit var phone: EditText
    private lateinit var submitButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.session)

        //loginButton button click

//        val loginButton = findViewById<Button>(R.id.loginButton)
//
//        loginButton.setOnClickListener {
//            val intent = Intent(this, LoginActivity::class.java)
//            startActivity(intent)
//        }
        //loginButton button click

        // Initialize UI elements
        var sharedPreferences: SharedPreferences = getSharedPreferences("MY_PRE", Context.MODE_PRIVATE)
        var userEmail = sharedPreferences.getString("email","")

        val sessionName = findViewById<EditText>(R.id.session_name) // Assuming it's an EditText
        val sessionTypeSpinner = findViewById<Spinner>(R.id.session_type) // Use Spinner here

        val submitButton = findViewById<Button>(R.id.submitButton)

        val adapter = ArrayAdapter.createFromResource(this, R.array.session_types, android.R.layout.simple_spinner_item)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        sessionTypeSpinner.adapter = adapter

        // Set a click listener for the submit button
        submitButton.setOnClickListener {

            val sessionName = sessionName.text.toString()
            val sessionType = sessionTypeSpinner.selectedItem.toString()


//            created_session_id
            val client = OkHttpClient()
            val formBody = FormBody.Builder()
                .add("email", userEmail.toString())
                .add("session_name", sessionName)
                .add("session_type", sessionType)
                .build()

            val baseUrl = getString(R.string.main_url)
            val sessionApiPath = getString(R.string.session_api)
            val fullSessionApiUrl = baseUrl + sessionApiPath

            val request = Request.Builder()
                .url(fullSessionApiUrl)
                .post(formBody)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread {
                        Toast.makeText(this@SessionActivity, "API request failed", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    runOnUiThread {
                        if (response.isSuccessful) {
                            val responseBody = response.body?.string()
                            // Process the response data as needed

                            // Check if the response contains "status":"success"
                            if (responseBody?.contains("\"status\":\"success\"") == true) {
                                // Display a success message
                                val jsonData = JSONObject(responseBody)

                                    // Access the "created_session_id" value
                                val createdSessionId = jsonData.getString("created_session_id")
                                val webView = WebView(applicationContext)
//                                Log.d("id",s)

                                MainActivity.sessionWebViews[createdSessionId] =  webView
                                    Toast.makeText(this@SessionActivity, "Session Added Successfully", Toast.LENGTH_SHORT).show()

                                // Redirect to the login page
                                val intent = Intent(this@SessionActivity,SessionlistActivity::class.java)
                                startActivity(intent)
                                finish() // Close the current activity
                            }  else {
                                // Handle other cases if needed
                                Toast.makeText(this@SessionActivity, "Invalid Error", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(this@SessionActivity, "Invalid Error", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            })
        }
    }


}
