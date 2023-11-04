package com.softwarelinkers.browser.fragment

import android.accounts.Account
import android.accounts.AccountManager
import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.text.SpannableStringBuilder
import android.util.Base64
import android.util.Log
import android.view.*
import android.webkit.*
import android.widget.FrameLayout
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ShareCompat
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat.startActivity
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.softwarelinkers.browser.R
import com.softwarelinkers.browser.activity.MainActivity
import com.softwarelinkers.browser.activity.changeTab
import com.softwarelinkers.browser.databinding.FragmentBrowseBinding
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Proxy
import java.net.URL
import java.util.Random
import kotlin.properties.Delegates

class BrowseFragment(private var urlNew: String) : Fragment() {

    lateinit var binding: FragmentBrowseBinding
    var webIcon: Bitmap? = null
    private lateinit var webViewContainer: FrameLayout
    private lateinit var sessionName: String
    private lateinit var accountManager: AccountManager
    private lateinit var sessionId : String
    lateinit var currentWebView: WebView

    private val sharedPreferences: SharedPreferences by lazy {
        requireContext().getSharedPreferences("MY_PRE", Context.MODE_PRIVATE)
    }

    private val credentialsList = mutableListOf<WebsiteCredentials>()


    private fun loadUserCredentialsForWebsite(websiteUrl: String) {
        val gson = Gson()
        val credentialsJson = sharedPreferences.getString("credentialsList", null)
        credentialsList.clear()

        if (!credentialsJson.isNullOrBlank()) {
            val type = object : TypeToken<List<WebsiteCredentials>>() {}.type
            val savedCredentials = gson.fromJson<List<WebsiteCredentials>>(credentialsJson, type)
            credentialsList.addAll(savedCredentials)
        }

        val websiteCredentials = credentialsList.find { it.websiteUrl == websiteUrl }
        websiteCredentials?.let {
            // Fill in the login form with the saved credentials
            val javascriptCode = """
                document.getElementById('usernameField').value = '${it.username}';
                document.getElementById('passwordField').value = '${it.password}';
            """.trimIndent()
            currentWebView.evaluateJavascript(javascriptCode, null)
        }
    }

    private fun isLoginPageForWebsite(url: String?): Boolean {
        // Implement your logic to identify the login page
        return url != null && url.contains("example.com/login")
    }

    private fun getAccountForWebsite(url: String?): Account? {
        // Implement a logic to retrieve or create an Account based on the website URL
        if (url != null) {
            val accountName = url // Replace with a unique identifier
            return Account(accountName, "WebsiteAccountType")
        }
        return null
    }

    private fun saveUserCredentialsForWebsite(url: String, username: String, password: String) {
        val websiteAccount = getAccountForWebsite(url)
        if (websiteAccount != null) {
            accountManager.addAccountExplicitly(websiteAccount, password, null)
            accountManager.setAuthToken(websiteAccount, "password", password)
            accountManager.setUserData(websiteAccount, "username", username)

            // Set up periodic synchronization if needed
            ContentResolver.setSyncAutomatically(websiteAccount, "authority", true)
            ContentResolver.addPeriodicSync(websiteAccount, "authority", Bundle(), 3600)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_browse, container, false)
        binding = FragmentBrowseBinding.bind(view)
        webViewContainer = binding.webViewLayout
//        registerForContextMenu(currentWebView)




        // Retrieve the session name and create a unique SharedPreferences file for the session
        sessionName = sharedPreferences.getString("SESSION_NAME", "").toString()
        val sessionSharedPreferences = requireContext().getSharedPreferences("SESSION_CACHE_$sessionName", Context.MODE_PRIVATE)
        sessionId = sharedPreferences.getString("SESSION_ID","").toString()
        currentWebView = MainActivity.sessionWebViews[sessionId]!!
        webViewContainer.addView(currentWebView)
        
        // Load session-specific cache data
        val cookies = sessionSharedPreferences.getString("CACHE_KEY", "")
        val cookieManager = CookieManager.getInstance()
        cookieManager.setCookie(urlNew, cookies)

        currentWebView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url.toString()
                view?.loadUrl(url)
                return true
            }

        }



        currentWebView.apply {
            when{
                URLUtil.isValidUrl(urlNew) -> loadUrl(urlNew)
                urlNew.contains(".com", ignoreCase = true) -> loadUrl(urlNew)
                else -> loadUrl("https://www.google.com/search?q=$urlNew")
            }
        }
        return view
    }

    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
    override fun onResume() {
        super.onResume()
        MainActivity.tabsList[MainActivity.myPager.currentItem].name = currentWebView.url.toString()
        MainActivity.tabsBtn.text = MainActivity.tabsList.size.toString()

        //for downloading file using external download manager
        currentWebView.setDownloadListener { url, _, _, _, _ -> startActivity(Intent(Intent.ACTION_VIEW).setData(
            Uri.parse(url))) }

        val mainRef = requireActivity() as MainActivity

        mainRef.binding.refreshBtn.visibility = View.VISIBLE
        mainRef.binding.refreshBtn.setOnClickListener {
            currentWebView.reload()
        }

        currentWebView.apply {
            settings.javaScriptEnabled = true
            settings.setSupportZoom(true)
            settings.builtInZoomControls = true
            settings.displayZoomControls = false
            settings.domStorageEnabled = true

            //next point code
            val userAgentArray = resources.getStringArray(R.array.user_agent_strings)
            val random = Random()
            val randomIndex = random.nextInt(userAgentArray.size)
            val selectedUserAgent = userAgentArray[randomIndex]
            settings.userAgentString = selectedUserAgent


//            settings.userAgentString = "android|Mozilla/5.0 (Android; Android 4.4.1; Nexus5 V6.1 Build/KOT49H) AppleWebKit/535.8 (KHTML, like Gecko)  Chrome/47.0.2757.152 Mobile Safari/536.2"
//            settings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36"
            settings.cacheMode = WebSettings.LOAD_NO_CACHE
            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                cookieManager.setAcceptThirdPartyCookies(this, true)
            }

            webViewClient = object: WebViewClient(){

                override fun onLoadResource(view: WebView?, url: String?) {
                    super.onLoadResource(view, url)
                    if(MainActivity.isDesktopSite)
                        view?.evaluateJavascript("document.querySelector('meta[name=\"viewport\"]').setAttribute('content'," +
                                " 'width=1024px, initial-scale=' + (document.documentElement.clientWidth / 1024));", null)
                }

                override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                    super.doUpdateVisitedHistory(view, url, isReload)
                    mainRef.binding.topSearchBar.text = SpannableStringBuilder(url)
                    MainActivity.tabsList[MainActivity.myPager.currentItem].name = url.toString()
                }

                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    mainRef.binding.progressBar.progress = 0
                    mainRef.binding.progressBar.visibility = View.VISIBLE
                    if(url!!.contains("you", ignoreCase = false)) mainRef.binding.root.transitionToEnd()
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    mainRef.binding.progressBar.visibility = View.GONE
                    currentWebView.zoomOut()

                    CookieManager.getInstance().setAcceptCookie(true);
                    CookieManager.getInstance().acceptCookie();
                    CookieManager.getInstance().flush();

                    if (url != null) {
                        loadUserCredentialsForWebsite(url)
                    }

//                    val cookieStore = CookieManager.getInstance().getCookie(url)
////                    val sharedPreferences = requireActivity().getSharedPreferences("MY_PRE", Context.MODE_PRIVATE)
//                    sharedPreferences.edit().putString(sessionName, cookieStore).apply()
//                    Log.d("cookieStore",sessionName + cookieStore.toString())

                }
            }
            webChromeClient = object: WebChromeClient(){
                //for setting icon to our search bar
                override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
                    super.onReceivedIcon(view, icon)
                    try{
                        mainRef.binding.webIcon.setImageBitmap(icon)
                        webIcon = icon
                        MainActivity.bookmarkIndex = mainRef.isBookmarked(view?.url!!)
                        if(MainActivity.bookmarkIndex != -1){
                            val array = ByteArrayOutputStream()
                            icon!!.compress(Bitmap.CompressFormat.PNG, 100, array)
                            MainActivity.bookmarkList[MainActivity.bookmarkIndex].image = array.toByteArray()
                        }
                    }catch (e: Exception){}
                }

                override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                    super.onShowCustomView(view, callback)
                    currentWebView.visibility = View.GONE
                    binding.customView.visibility = View.VISIBLE
                    binding.customView.addView(view)
                    mainRef.binding.root.transitionToEnd()
                }

                override fun onHideCustomView() {
                    super.onHideCustomView()
                    currentWebView.visibility = View.VISIBLE
                    binding.customView.visibility = View.GONE

                }

                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    super.onProgressChanged(view, newProgress)
                    mainRef.binding.progressBar.progress = newProgress
                }
            }

            currentWebView.setOnTouchListener { _, motionEvent ->
                mainRef.binding.root.onTouchEvent(motionEvent)
                return@setOnTouchListener false
            }

            currentWebView.reload()
        }


    }

    override fun onPause() {
        super.onPause()
        (requireActivity() as MainActivity).saveBookmarks()

        currentWebView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                val cookies = CookieManager.getInstance().getCookie(url)
                val sharedPreferences = requireActivity().getSharedPreferences("MY_PRE", Context.MODE_PRIVATE)
                sharedPreferences.edit().putString(sessionName, cookies).apply()
            }
        }

//        saveCookies()

        //for clearing all webview data
//        currentWebView.apply {
//            clearMatches()
//            clearHistory()
//            clearFormData()
//            clearSslPreferences()
////            clearCache(true)
//
////            CookieManager.getInstance().removeAllCookies(null)
//            WebStorage.getInstance().deleteAllData()
//
//
//        }

    }

//    override fun onDestroy() {
//        super.onDestroy()
////        currentWebView.apply {
////            clearMatches()
////            clearHistory()
////            clearFormData()
////            clearSslPreferences()
////            clearCache(true)
////            CookieManager.getInstance().removeAllCookies(null)
////            WebStorage.getInstance().deleteAllData()
//        }
//    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)

        val result = currentWebView.hitTestResult
        when(result.type){
            WebView.HitTestResult.IMAGE_TYPE -> {
                menu.add("View Image")
                menu.add("Save Image")
                menu.add("Share")
                menu.add("Close")
            }
            WebView.HitTestResult.SRC_ANCHOR_TYPE, WebView.HitTestResult.ANCHOR_TYPE-> {
                menu.add("Open in New Tab")
                menu.add("Open Tab in Background")
                menu.add("Share")
                menu.add("Close")
            }
            WebView.HitTestResult.EDIT_TEXT_TYPE, WebView.HitTestResult.UNKNOWN_TYPE -> {}
            else ->{
                menu.add("Open in New Tab")
                menu.add("Open Tab in Background")
                menu.add("Share")
                menu.add("Close")
            }
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {

        val message = Handler().obtainMessage()
        currentWebView.requestFocusNodeHref(message)
        val url = message.data.getString("url")
        val imgUrl = message.data.getString("src")

        when(item.title){
            "Open in New Tab" -> {
                changeTab(url.toString(), BrowseFragment(url.toString()))
            }
            "Open Tab in Background" ->{
                changeTab(url.toString(), BrowseFragment(url.toString()), isBackground = true)
            }
            "View Image" ->{
                if(imgUrl != null) {
                    if (imgUrl.contains("base64")) {
                        val pureBytes = imgUrl.substring(imgUrl.indexOf(",") + 1)
                        val decodedBytes = Base64.decode(pureBytes, Base64.DEFAULT)
                        val finalImg =
                            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)

                        val imgView = ShapeableImageView(requireContext())
                        imgView.setImageBitmap(finalImg)

                        val imgDialog = MaterialAlertDialogBuilder(requireContext()).setView(imgView).create()
                        imgDialog.show()

                        imgView.layoutParams.width = Resources.getSystem().displayMetrics.widthPixels
                        imgView.layoutParams.height = (Resources.getSystem().displayMetrics.heightPixels * .75).toInt()
                        imgView.requestLayout()

                        imgDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

                    }
                    else changeTab(imgUrl, BrowseFragment(imgUrl))
                }
            }

            "Save Image" ->{
                if(imgUrl != null) {
                    if (imgUrl.contains("base64")) {
                        val pureBytes = imgUrl.substring(imgUrl.indexOf(",") + 1)
                        val decodedBytes = Base64.decode(pureBytes, Base64.DEFAULT)
                        val finalImg =
                            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)

                        MediaStore.Images.Media.insertImage(
                            requireActivity().contentResolver,
                            finalImg, "Image", null
                        )
                        Snackbar.make(binding.root, "Image Saved Successfully", 3000).show()
                    }
                    else startActivity(Intent(Intent.ACTION_VIEW).setData(Uri.parse(imgUrl)))
                }
            }

            "Share" -> {
                val tempUrl = url ?: imgUrl
                if(tempUrl != null){
                    if(tempUrl.contains("base64")){

                        val pureBytes = tempUrl.substring(tempUrl.indexOf(",") + 1)
                        val decodedBytes = Base64.decode(pureBytes, Base64.DEFAULT)
                        val finalImg = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)

                        val path = MediaStore.Images.Media.insertImage(requireActivity().contentResolver,
                            finalImg, "Image", null)

                        ShareCompat.IntentBuilder(requireContext()).setChooserTitle("Sharing Url!")
                            .setType("image/*")
                            .setStream(Uri.parse(path))
                            .startChooser()
                    }
                    else{
                        ShareCompat.IntentBuilder(requireContext()).setChooserTitle("Sharing Url!")
                            .setType("text/plain").setText(tempUrl)
                            .startChooser()
                    }
                }
                else Snackbar.make(binding.root, "Not a Valid Link!", 3000).show()
            }
            "Close" -> {}
        }

        return super.onContextItemSelected(item)
    }


//    fun getPublicIPAddress(): String {
//        var ipAddress = "Unknown"
//
//        try {
//            val url = URL("https://api64.ipify.org?format=json") // You can use any service that provides public IP information
//            val connection = url.openConnection() as HttpURLConnection
//            connection.requestMethod = "GET"
//
//            val responseCode = connection.responseCode
//
//            if (responseCode == HttpURLConnection.HTTP_OK) {
//                val reader = BufferedReader(InputStreamReader(connection.inputStream))
//                val response = StringBuilder()
//                var line: String?
//
//                while (reader.readLine().also { line = it } != null) {
//                    response.append(line)
//                }
//
//                reader.close()
//
//                ipAddress = response.toString()
//            }
//
//            connection.disconnect()
//        } catch (e: Exception) {
//            e.printStackTrace()
//            // Handle exceptions here
//        }
//
//        return ipAddress
//    }

//    fun getIPAddress(): List<String> {
//        val addresses = mutableListOf<String>()
//        try {
//            val interfaces = NetworkInterface.getNetworkInterfaces()
//            while (interfaces.hasMoreElements()) {
//                val networkInterface = interfaces.nextElement()
//                val enumIpAddr = networkInterface.inetAddresses
//                while (enumIpAddr.hasMoreElements()) {
//                    val inetAddress = enumIpAddr.nextElement()
//                    if (!inetAddress.isLoopbackAddress) {
//                        if (inetAddress is Inet4Address) {
//                            addresses.add("IPv4: ${inetAddress.hostAddress}")
//                        } else if (inetAddress is Inet6Address) {
//                            addresses.add("IPv6: ${inetAddress.hostAddress}")
//                        }
//                    }
//                }
//            }
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
//        return addresses
//    }
}


//
//open class WebViewProxy(private val proxyServer: String, private val proxyPort: Int) : WebViewClient(){
//    override fun shouldInterceptRequest(
//        view: WebView?,
//        request: WebResourceRequest?
//    ): WebResourceResponse? {
//        try{
//            val  proxy = Proxy(Proxy.Type.HTTP,InetSocketAddress(proxyServer,proxyPort))
//            val url = request?.url.toString()
//            val connection = URL(url).openConnection(proxy)
//
//            connection.readTimeout = 10000
//            connection.connectTimeout = 15000
//            connection.setRequestProperty("Request-Method","GET")is
//
//            connection.connect()
//
//            val inputStream: InputStream = connection.getInputStream()
//
//            return WebResourceResponse("text/html","UTF-8", inputStream)
//        }catch (e: Exception) {
//            e.printStackTrace()
//            // Handle exceptions here
//        }
//        return null
//    }
//
//}

//class WebViewProxy(private val proxyServer: String, private val proxyPort: Int, private val proxyUsername: String, private val proxyPassword: String) : WebViewClient() {
//    @RequiresApi(Build.VERSION_CODES.O)
//    override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
//        try {
//            val proxy = Proxy(Proxy.Type.HTTP, InetSocketAddress(proxyServer, proxyPort))
//            val url = request?.url.toString()
//            val connection = URL(url).openConnection(proxy) as HttpURLConnection
//
//            // Set up proxy authentication
//            val encodedAuth = java.util.Base64.getEncoder().encodeToString("$proxyUsername:$proxyPassword".toByteArray())
//            connection.setRequestProperty("Proxy-Authorization", "Basic $encodedAuth")
//
//            connection.readTimeout = 10000
//            connection.connectTimeout = 15000
//            connection.requestMethod = "GET"
//
//            connection.connect()
//
//            val responseCode = connection.responseCode
//
//            if (responseCode == HttpURLConnection.HTTP_OK) {
//                val inputStream: InputStream = connection.inputStream
//                return WebResourceResponse("text/html", "UTF-8", inputStream)
//            }
//        } catch (e: Exception) {
//            e.printStackTrace()
//            Log.e("ProxyError", "Error: ${e.message}")
//            // Handle exceptions here
//        }
//        return null
//    }
//}


data class WebsiteCredentials(val websiteUrl: String, val username: String, val password: String)

