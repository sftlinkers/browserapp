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
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import android.content.SharedPreferences
import android.content.Context

class LoginActivity : AppCompatActivity() {

    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var submitButton: Button

    private lateinit var sharedPreferences: SharedPreferences

    private var succesfulLogin = false;

    private var userEmail= ""
    private var userPass = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login)
        //register button click
        Log.d("bool",succesfulLogin.toString())
        val registerButton = findViewById<Button>(R.id.registerButton)

        registerButton.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
        //register button click

        // Initialize UI elements
        usernameEditText = findViewById(R.id.usernameEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        submitButton = findViewById(R.id.submitButton)

        sharedPreferences = getSharedPreferences("MY_PRE",Context.MODE_PRIVATE)
        if(sharedPreferences.getBoolean("LOGGED_IN",false)){
            val i = Intent(this,SessionlistActivity::class.java)
            startActivity(i)
            updateLoginSharedCache()
            finish()
        }

        val getusername = sharedPreferences.getString("USERNAME","")
        val getpassword = sharedPreferences.getString("PASSWORD","")


        // Set a click listener for the submit button
        submitButton.setOnClickListener {

            val username = usernameEditText.text.toString()
            val password = passwordEditText.text.toString()

//            Log.d("User",username)
//            Log.d("pass",password)

            val client = OkHttpClient()
            val formBody = FormBody.Builder()
                .add("email", username)
                .add("password", password)
                .add("user_type", "User")
                .add("user_status", "Active")
                .build()

            val fullloginapi = getString(R.string.user_login_api)
            val request = Request.Builder()
                .url(fullloginapi)
                .post(formBody)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread {
                        Toast.makeText(this@LoginActivity, "API request failed", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    runOnUiThread {
                        if (response.isSuccessful) {
                            val responseBody = response.body?.string()

                            // Check if the response contains "status":"success"
                            if (responseBody?.contains("\"status\":\"success\"") == true) {
                                try {
                                    val jsonResponse = JSONObject(responseBody)
                                    val status = jsonResponse.getString("status")
                                    val message = jsonResponse.getString("message")
                                    val userData = jsonResponse.getJSONObject("data")

                                    if (status == "success") {
                                        val email = userData.getString("email")
                                        val userType = userData.getString("user_type")

                                        // Save the login session locally
                                        succesfulLogin = true;
                                        userEmail = email
                                        userPass = password
                                        updateLoginSharedCache()
                                        userLoginSharedCache()

                                        // Display a success message
                                        Toast.makeText(this@LoginActivity, "User Login Successfully", Toast.LENGTH_SHORT).show()

                                        // Redirect to the SessionActivity
                                        val intent = Intent(this@LoginActivity, SessionlistActivity::class.java)
                                        startActivity(intent)
                                        finish() // Close the current activity
                                    } else {
                                        // Handle other cases if needed
                                        Toast.makeText(this@LoginActivity, "Invalid Error", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: JSONException) {
                                    e.printStackTrace()
                                }
                            } else {
                                // Handle other non-successful response cases
                                Toast.makeText(this@LoginActivity, "Invalid Error", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            // Handle non-successful HTTP response (e.g., network issues)
                            Toast.makeText(this@LoginActivity, "Network Error", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

            })
        }

    }

//    private fun saveLoginSession(email: String, userType: String) {
//        val sharedPreferences: SharedPreferences = getSharedPreferences("LoginSession", Context.MODE_PRIVATE)
//        val editor: SharedPreferences.Editor = sharedPreferences.edit()
//
//        editor.putString("email", email)
//        editor.putString("userType", userType)
//
//        editor.apply()
//    }
    private fun isSessionExist(): Boolean {
        // Implement your logic to check if a session exists (e.g., user is already logged in)
        // Return true if a session exists, otherwise return false
        return false // Change to true if a session exists
    }

    private fun updateLoginSharedCache(){
        val editor: SharedPreferences.Editor= sharedPreferences.edit()
        editor.putBoolean("LOGGED_IN",succesfulLogin)
        editor.apply()
    }

    private fun userLoginSharedCache(){
        val editor: SharedPreferences.Editor = sharedPreferences.edit()
        editor.putString("email", userEmail)
        editor.putString("userType", userPass)
        editor.apply()
    }

}


