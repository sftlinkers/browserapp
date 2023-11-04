package com.softwarelinkers.browser.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.softwarelinkers.browser.R
import okhttp3.*
import java.io.IOException


class RegisterActivity : AppCompatActivity() {

    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var fullname: EditText
    private lateinit var phone: EditText
    private lateinit var submitButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.register)

        //loginButton button click

        val loginButton = findViewById<Button>(R.id.loginButton)

        loginButton.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
        //loginButton button click

        // Initialize UI elements
        usernameEditText = findViewById(R.id.usernameEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        fullname = findViewById(R.id.fullname)
        phone = findViewById(R.id.phone)
        submitButton = findViewById(R.id.submitButton)

        // Set a click listener for the submit button
        submitButton.setOnClickListener {

            val username = usernameEditText.text.toString()
            val password = passwordEditText.text.toString()
            val fullname_value = fullname.text.toString()
            val phone_value = phone.text.toString()
//            Log.d("User",username)
//            Log.d("pass",password)

            val client = OkHttpClient()
            val formBody = FormBody.Builder()
                .add("email", username)
                .add("password", password)
                .add("name", fullname_value)
                .add("phone", phone_value)
                .build()
            val baseUrl = getString(R.string.main_url)
            val user_register_api = getString(R.string.user_register_api)
            val fulluserregisterapi = baseUrl + user_register_api

            val request = Request.Builder()
                .url(fulluserregisterapi)
                .post(formBody)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread {
                        Toast.makeText(this@RegisterActivity, "API request failed", Toast.LENGTH_SHORT).show()
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
                                Toast.makeText(this@RegisterActivity, "User Created Successfully", Toast.LENGTH_SHORT).show()

                                // Redirect to the login page
                                val intent = Intent(this@RegisterActivity, LoginActivity::class.java)
                                startActivity(intent)
                                finish() // Close the current activity
                            } else if (responseBody?.contains("\"status\":\"exist\"") == true) {
                                // Display a message indicating that the email already exists
                                Toast.makeText(this@RegisterActivity, "Email Already Exists", Toast.LENGTH_SHORT).show()
                                // Reload the current activity
                                val intent = Intent(this@RegisterActivity, RegisterActivity::class.java)
                                startActivity(intent)
                                finish() // Close the current activity
                            } else {
                                // Handle other cases if needed
                                Toast.makeText(this@RegisterActivity, "User registration was not successful", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(this@RegisterActivity, "Invalid Error", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            })
        }
    }


}
