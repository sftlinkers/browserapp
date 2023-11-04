package com.softwarelinkers.browser.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.print.PrintAttributes
import android.print.PrintJob
import android.print.PrintManager
import android.provider.Settings
import android.provider.Settings.Secure.getString
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textview.MaterialTextView
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.softwarelinkers.browser.R
import com.softwarelinkers.browser.activity.MainActivity.Companion.allBookmarkList
import com.softwarelinkers.browser.activity.MainActivity.Companion.bookmarkList
import com.softwarelinkers.browser.activity.MainActivity.Companion.historyList
import com.softwarelinkers.browser.activity.MainActivity.Companion.myPager
import com.softwarelinkers.browser.activity.MainActivity.Companion.tabsBtn
import com.softwarelinkers.browser.adapter.AllBookmarkAdapter
import com.softwarelinkers.browser.adapter.HomeBookmarkAdapter
import com.softwarelinkers.browser.adapter.TabAdapter
import com.softwarelinkers.browser.databinding.ActivityMainBinding
import com.softwarelinkers.browser.databinding.MoreFeaturesBinding
import com.softwarelinkers.browser.databinding.TabsViewBinding
import com.softwarelinkers.browser.fragment.BrowseFragment
import com.softwarelinkers.browser.fragment.HomeFragment
import com.softwarelinkers.browser.model.Bookmark
import com.softwarelinkers.browser.model.Bookmarknew

import com.softwarelinkers.browser.model.History
import com.softwarelinkers.browser.model.HomeBookmark
import com.softwarelinkers.browser.model.Tab
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.internal.notifyAll
import java.io.IOException
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding
    private var printJob: PrintJob? = null

    companion object{
        var tabsList: ArrayList<Tab> = ArrayList()
        private var isFullscreen: Boolean = true
        var isDesktopSite: Boolean = false
        var bookmarkList: ArrayList<Bookmark> = ArrayList()
        var historyList: ArrayList<History> = ArrayList()
        var allBookmarkList: ArrayList<Bookmarknew> = ArrayList()
        val sessionWebViews = mutableMapOf<String, WebView>()
        var homeBookmarkList: ArrayList<HomeBookmark> = ArrayList()
        var bookmarkIndex: Int = -1
        lateinit var myPager: ViewPager2
        lateinit var tabsBtn: MaterialTextView
        lateinit var pageUrl: String
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        getAllBookmarks()
        tabsList.clear()
        tabsList.add(Tab("Home", HomeFragment()))
        binding.myPager.adapter = TabsAdapter(supportFragmentManager, lifecycle)
        binding.myPager.isUserInputEnabled = false
        myPager = binding.myPager
        tabsBtn = binding.tabsBtn

        initializeView()

        changeFullscreen(enable = false)



    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onBackPressed() {
        var frag: BrowseFragment? = null
        try {
            frag = tabsList[binding.myPager.currentItem].fragment as BrowseFragment
        }catch (e:Exception){}

        when{
            frag?.currentWebView?.canGoBack() == true -> frag?.currentWebView?.goBack()
            binding.myPager.currentItem != 0 ->{
                tabsList.removeAt(binding.myPager.currentItem)
                binding.myPager.adapter?.notifyDataSetChanged()
                binding.myPager.currentItem = tabsList.size - 1

            }
            else -> super.onBackPressed()
        }
    }




    private inner class TabsAdapter(fa: FragmentManager, lc: Lifecycle) : FragmentStateAdapter(fa, lc) {
        override fun getItemCount(): Int = tabsList.size

        override fun createFragment(position: Int): Fragment = tabsList[position].fragment
    }



    private fun initializeView(){




        var sharedPreferences: SharedPreferences = getSharedPreferences("MY_PRE", Context.MODE_PRIVATE)
        var sessionId = sharedPreferences.getString("SESSION_ID","")
        var userEmail = sharedPreferences.getString("email","")

        loadTopUrlFromApi()

        loadHomeBookmarkFromApi(userEmail, sessionId, binding.bookmarkView)

        binding.bookmarkView.setHasFixedSize(true)
        binding.bookmarkView.setItemViewCacheSize(5)
        binding.bookmarkView.layoutManager= LinearLayoutManager(this)
        binding.bookmarkView.adapter = HomeBookmarkAdapter(this)

        binding.topSearchBar.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                // Not implemented
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                // Not implemented
            }

            override fun afterTextChanged(s: Editable?) {
                val url = s?.toString()?.trim() ?: ""
                pageUrl = url
                if (url.isNotEmpty()) {
                    // Call a function to save the URL to the API
                    Log.d("Url",url)
                    saveHistoryToAPI(url,userEmail,sessionId)
                }
            }
        })

        binding.tabsBtn.setOnClickListener {
            val viewTabs = layoutInflater.inflate(R.layout.tabs_view, binding.root, false)
            val bindingTabs = TabsViewBinding.bind(viewTabs)

            val dialogTabs = MaterialAlertDialogBuilder(this, R.style.roundCornerDialog).setView(viewTabs)
                .setTitle("Select Tab")
                .setPositiveButton("Home"){self, _ ->
                    changeTab("Home", HomeFragment())
                    self.dismiss()
                }
                .setNeutralButton("Google"){self, _ ->
                    changeTab("Google", BrowseFragment(urlNew = "www.google.com"))
                    self.dismiss()
                }
                .create()

            bindingTabs.tabsRV.setHasFixedSize(true)
            bindingTabs.tabsRV.layoutManager = LinearLayoutManager(this)
            bindingTabs.tabsRV.adapter = TabAdapter(this, dialogTabs)

            dialogTabs.show()

            val pBtn = dialogTabs.getButton(AlertDialog.BUTTON_POSITIVE)
            val nBtn = dialogTabs.getButton(AlertDialog.BUTTON_NEUTRAL)

            pBtn.isAllCaps = false
            nBtn.isAllCaps = false

            pBtn.setTextColor(Color.BLACK)
            nBtn.setTextColor(Color.BLACK)

            pBtn.setCompoundDrawablesWithIntrinsicBounds( ResourcesCompat.getDrawable(resources, R.drawable.ic_home, theme)
                , null, null, null)
            nBtn.setCompoundDrawablesWithIntrinsicBounds( ResourcesCompat.getDrawable(resources, R.drawable.ic_add, theme)
                , null, null, null)
        }

        binding.bookmarksaveBtn.setOnClickListener {4

            val dialogView = LayoutInflater.from(this).inflate(R.layout.bookmark_dialog,null)
            val bookmarkTitelEditText = dialogView.findViewById<EditText>(R.id.bookmarkTitle)

            var userId = sharedPreferences.getString("email","")

            val dialog = AlertDialog.Builder(this)
                .setTitle("Add Bookmark")
                .setView(dialogView)
                .setPositiveButton("Save"){_, _ ->
                    val bookmarkTitle = bookmarkTitelEditText.text.toString()
                    saveBookmarkToApi(bookmarkTitle,userId,sessionId)
                }
                .setNegativeButton("Cancel",null)
                .create()

            dialog.show()

        }

        binding.settingBtn.setOnClickListener {

            var frag: BrowseFragment? = null
            try {
                frag = tabsList[binding.myPager.currentItem].fragment as BrowseFragment
            }catch (e:Exception){}

            val view = layoutInflater.inflate(R.layout.more_features, binding.root, false)
            val dialogBinding = MoreFeaturesBinding.bind(view)

            val dialog = MaterialAlertDialogBuilder(this).setView(view).create()

            dialog.window?.apply {
                attributes.gravity = Gravity.BOTTOM
                attributes.y = 50
                setBackgroundDrawable(ColorDrawable(0xFFFFFFFF.toInt()))
            }
            dialog.show()

            if(isFullscreen){
                dialogBinding.fullscreenBtn.apply {
                    setIconTintResource(R.color.cool_blue)
                    setTextColor(ContextCompat.getColor(this@MainActivity, R.color.cool_blue))
                }
            }

            frag?.let {
                bookmarkIndex = isBookmarked(it.currentWebView.url!!)
                if(bookmarkIndex != -1){

//                dialogBinding.bookmarkBtn.apply {
//                    setIconTintResource(R.color.cool_blue)
//                    setTextColor(ContextCompat.getColor(this@MainActivity, R.color.cool_blue))
//                }
            } }

            if(isDesktopSite){
                dialogBinding.desktopBtn.apply {
                    setIconTintResource(R.color.cool_blue)
                    setTextColor(ContextCompat.getColor(this@MainActivity, R.color.cool_blue))
                }
            }



            dialogBinding.backBtn.setOnClickListener {
                onBackPressed()
            }

            dialogBinding.forwardBtn.setOnClickListener {
                frag?.apply {
                    if(currentWebView.canGoForward())
                        currentWebView.goForward()
                }
            }

            dialogBinding.saveBtn.setOnClickListener {
                dialog.dismiss()
                if(frag != null)
                    saveAsPdf(web = frag.currentWebView)
                else Snackbar.make(binding.root, "First Open A WebPage\uD83D\uDE03", 3000).show()
            }

            dialogBinding.fullscreenBtn.setOnClickListener {
                it as MaterialButton

                isFullscreen = if (isFullscreen) {
                    changeFullscreen(enable = false)
                    it.setIconTintResource(R.color.black)
                    it.setTextColor(ContextCompat.getColor(this, R.color.black))
                    false
                }
                else {
                    changeFullscreen(enable = true)
                    it.setIconTintResource(R.color.cool_blue)
                    it.setTextColor(ContextCompat.getColor(this, R.color.cool_blue))
                    true
                }
            }

//            dialogBinding.desktopBtn.setOnClickListener {
//                it as MaterialButton
//
//                frag?.currentWebView?.rootView.apply {
//                    isDesktopSite = if (isDesktopSite) {
//                        settings.userAgentString = null
//                        it.setIconTintResource(R.color.black)
//                        it.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.black))
//                        false
//                    }
//                    else {
//                        settings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:99.0) Gecko/20100101 Firefox/99.0"
//                        settings.useWideViewPort = true
//                        evaluateJavascript("document.querySelector('meta[name=\"viewport\"]').setAttribute('content'," +
//                                " 'width=1024px, initial-scale=' + (document.documentElement.clientWidth / 1024));", null)
//                        it.setIconTintResource(R.color.cool_blue)
//                        it.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.cool_blue))
//                        true
//                    }
//                    reload()
//                    dialog.dismiss()
//                }
//
//            }

            dialogBinding.HistoryBtn.setOnClickListener {
                var i = Intent(this@MainActivity,HistoryActivity::class.java)
                startActivity(i)
                dialog.dismiss()
            }

            dialogBinding.allbookmarkBtn.setOnClickListener {
                var i = Intent(this@MainActivity,BookmarkActivity::class.java)
                startActivity(i)
                dialog.dismiss()
            }

            dialogBinding.sessionlistnav.setOnClickListener {
                var i = Intent(this@MainActivity,SessionlistActivity::class.java)
                startActivity(i)
                dialog.dismiss()
//                BrowseFragment.currentWebView.apply {
//                                        clearMatches()
//                                        clearHistory()
//                                        clearFormData()
//                                        clearSslPreferences()
//                                        clearCache(true)
//
//                                        CookieManager.getInstance().removeAllCookies(null)
//                                        WebStorage.getInstance().deleteAllData()
//
//
//        }
                finish()
            }

            dialogBinding.logout.setOnClickListener {
                var i = Intent(this@MainActivity,LoginActivity::class.java)
                startActivity(i)
                dialog.dismiss()
                finish()
            }

//            dialogBinding.SessionBtn.setOnClickListener {
//                var userEmail = sharedPreferences.getString("email","")
//                loadsessionToApi(userEmail)
//                var i = Intent(this@MainActivity,SessionlistActivity::class.java)
//                startActivity(i)
//                dialog.dismiss()
//            }

//            dialogBinding.bookmarkBtn.setOnClickListener {
//                frag?.let{
//                    if(bookmarkIndex == -1){
//                        val viewB = layoutInflater.inflate(R.layout.bookmark_dialog, binding.root, false)
//                        val bBinding = BookmarkDialogBinding.bind(viewB)
//                        val dialogB = MaterialAlertDialogBuilder(this)
//                            .setTitle("Add Bookmark")
//                            .setMessage("Url:${it.currentWebView.url}")
//                            .setPositiveButton("Add"){self, _ ->
//                                try{
//                                    val array = ByteArrayOutputStream()
//                                    it.webIcon?.compress(Bitmap.CompressFormat.PNG, 100, array)
//                                    bookmarkList.add(
//                                        Bookmark(name = bBinding.bookmarkTitle.text.toString(), url = it.currentWebView.url!!, array.toByteArray()))
//                                }
//                                catch(e: Exception){
//                                    bookmarkList.add(
//                                        Bookmark(name = bBinding.bookmarkTitle.text.toString(), url = it.currentWebView.url!!))
//                                }
//                                self.dismiss()}
//                            .setNegativeButton("Cancel"){self, _ -> self.dismiss()}
//                            .setView(viewB).create()
//                        dialogB.show()
//                        bBinding.bookmarkTitle.setText(it.currentWebView.title)
//                    }else{
//                        val dialogB = MaterialAlertDialogBuilder(this)
//                            .setTitle("Remove Bookmark")
//                            .setMessage("Url:${it.currentWebView.url}")
//                            .setPositiveButton("Remove"){self, _ ->
//                                bookmarkList.removeAt(bookmarkIndex)
//                                self.dismiss()}
//                            .setNegativeButton("Cancel"){self, _ -> self.dismiss()}
//                            .create()
//                        dialogB.show()
//                    }
//                }
//
//                dialog.dismiss()
//            }
        }

    }


    override fun onResume() {
        super.onResume()
        printJob?.let {
            when{
                it.isCompleted -> Snackbar.make(binding.root, "Successful -> ${it.info.label}", 4000).show()
                it.isFailed -> Snackbar.make(binding.root, "Failed -> ${it.info.label}", 4000).show()
            }
        }
    }

    private fun saveAsPdf(web: WebView){
        val pm = getSystemService(Context.PRINT_SERVICE) as PrintManager

        val jobName = "${URL(web.url).host}_${SimpleDateFormat("HH:mm d_MMM_yy", Locale.ENGLISH)
            .format(Calendar.getInstance().time)}"
        val printAdapter = web.createPrintDocumentAdapter(jobName)
        val printAttributes = PrintAttributes.Builder()
        printJob = pm.print(jobName, printAdapter, printAttributes.build())
    }

    private fun changeFullscreen(enable: Boolean){
        if(enable){
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowInsetsControllerCompat(window, binding.root).let { controller ->
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }else{
            WindowCompat.setDecorFitsSystemWindows(window, true)
            WindowInsetsControllerCompat(window, binding.root).show(WindowInsetsCompat.Type.systemBars())
        }
    }

    fun isBookmarked(url: String): Int{
        bookmarkList.forEachIndexed { index, bookmark ->
            if(bookmark.url == url) return index
        }
        return -1
    }

    fun saveBookmarks(){
        //for storing bookmarks data using shared preferences
        val editor = getSharedPreferences("BOOKMARKS", MODE_PRIVATE).edit()

        val data = GsonBuilder().create().toJson(bookmarkList)
        editor.putString("bookmarkList", data)

        editor.apply()
    }

    private fun getAllBookmarks(){
        //for getting bookmarks data using shared preferences from storage
        bookmarkList = ArrayList()
        val editor = getSharedPreferences("BOOKMARKS", MODE_PRIVATE)
        val data = editor.getString("bookmarkList", null)

        if(data != null){
            val list: ArrayList<Bookmark> = GsonBuilder().create().fromJson(data, object: TypeToken<ArrayList<Bookmark>>(){}.type)
            bookmarkList.addAll(list)
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun loadHomeBookmarkFromApi(userEmail: String?, sessionId: String?, recyclerView: RecyclerView) {

        val baseUrl = getString(R.string.homebookmark_url)

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
                        homeBookmarkList.clear()
                        for (i in 0 until dataArray.length()) {
                            val item = dataArray.getJSONObject(i)
                            val title = item.getString("title")
                            val id = item.getString("id")
                            val short_url = item.getString("shortened_url")
                            homeBookmarkList.add(
                                HomeBookmark(id = id, name = title, url = short_url)
                            )
                        }

                        // Update the RecyclerView on the UI thread
                        launch(Dispatchers.Main) {
                            val recyclerViewAdapter = HomeBookmarkAdapter(this@MainActivity, isActivity = false)
                            recyclerView.setHasFixedSize(true)
                            recyclerView.layoutManager = LinearLayoutManager(this@MainActivity)
                            recyclerView.adapter = recyclerViewAdapter

//                            recyclerViewAdapter.setData(bookmarkList)
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

@SuppressLint("NotifyDataSetChanged")
fun changeTab(url: String, fragment: Fragment, isBackground: Boolean = false){
    MainActivity.tabsList.add(Tab(name = url, fragment = fragment))
    myPager.adapter?.notifyDataSetChanged()
    tabsBtn.text = MainActivity.tabsList.size.toString()

    if(!isBackground) myPager.currentItem = MainActivity.tabsList.size - 1
}

fun checkForInternet(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false

        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            else -> false
        }
    } else {
        @Suppress("DEPRECATION") val networkInfo =
            connectivityManager.activeNetworkInfo ?: return false
        @Suppress("DEPRECATION")
        return networkInfo.isConnected
    }
}
private fun saveHistoryToAPI(historyData: String, userEmail: String?, sessionId: String?) {

    val formBody = FormBody.Builder()
        .add("session_id", sessionId.toString())//add here
        .add("session_url", historyData)
        .add("email", userEmail.toString())
        .build()

    val fullHistoryApi = "https://development.softwarelinkers.in/browser/api/history.php"
    val request = Request.Builder()
        .url(fullHistoryApi)
        .post(formBody)
        .build()

    val client = OkHttpClient()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {

        }

        override fun onResponse(call: Call, response: Response) {
            if (response.isSuccessful) {
                val responseBody = response.body?.string()

                if (responseBody?.contains("\"status\":\"success\"") == true) {

                } else {

                 }
            } else {

            }
        }
    })
}

fun saveBookmarkToApi(bookmarkTitle: String, userId: String?, sessionId: String?) {

    val formBody = FormBody.Builder()
        .add("title",bookmarkTitle)
        .add("session_id", sessionId.toString())
        .add("session_url", MainActivity.pageUrl)
        .add("user_id", userId.toString())
        .build()

    val fullBookmarkApi = "https://development.softwarelinkers.in/browser/api/bookmark.php"
    val request = Request.Builder()
        .url(fullBookmarkApi)
        .post(formBody)
        .build()

    val client = OkHttpClient()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {

        }

        override fun onResponse(call: Call, response: Response) {
            if (response.isSuccessful) {
                val responseBody = response.body?.string()

                if (responseBody?.contains("\"status\":\"success\"") == true) {

                } else {

                 }
            } else {

          }
        }
    })
}

fun loadTopUrlFromApi(){
    val client = OkHttpClient()
    val url = "https://development.softwarelinkers.in/browser/api/ft_toplinks.php"

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

                if (responseBody?.contains("\"status\":\"success\"") == true) {
                    val jsonData = JSONObject(responseBody) // Assuming you're using a JSON library
                    val dataArray = jsonData.getJSONArray("data")

                    // Loop through the data and display it
                    bookmarkList.clear()
                    for (i in 0 until dataArray.length()) {
                        val item = dataArray.getJSONObject(i)
                        val id = item.getString("id")
                        val title = item.getString("title")
                        val image = item.getString("image")
                        val tpUrl = item.getString("tp_url")

                        bookmarkList.add(
                            Bookmark(id = id, name = title, url = tpUrl)
                        )
                    }

                } else {

                }
            } else {}
        }
    })
}

//private fun loadBookmarkFromApi(userEmail: String?, sessionId: String?) {
//    val baseUrl = "https://development.softwarelinkers.in/browser/api/get_bookmark.php"
//    val url = "$baseUrl?email=$userEmail&session_id=$sessionId"
//
//            val client = OkHttpClient()
//            val request = Request.Builder()
//                .url(url)
//                .get()
//                .build()
//
//            val response = client.newCall(request).execute()
//
//            if (response.isSuccessful) {
//                val responseBody = response.body?.string()
//
//                if (responseBody?.contains("\"status\":\"success\"") == true) {
//                    val jsonData = JSONObject(responseBody) // Assuming you're using a JSON library
//                    val dataArray = jsonData.getJSONArray("data")
//
//                    MainActivity.homeBookmarkList.clear()
//                    for (i in 0 until dataArray.length()) {
//                        val item = dataArray.getJSONObject(i)
//                        val title = item.getString("title")
//                        val id = item.getString("id")
//                        val short_url = item.getString("shortened_url")
//                        MainActivity.homeBookmarkList.add(
//                            HomeBookmark(id = id, name = title, url = short_url)
//                        )
//                    }
//                } else {
//                    // Handle the case where the API response indicates an error
//                }
//            } else {
//                // Handle the case where the network call was not successful
//            }
//        }
//
