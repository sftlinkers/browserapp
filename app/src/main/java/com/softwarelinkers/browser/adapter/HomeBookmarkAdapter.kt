package com.softwarelinkers.browser.adapter

import android.app.Activity
import android.content.Context
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.graphics.drawable.toDrawable
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.softwarelinkers.browser.R
import com.softwarelinkers.browser.activity.MainActivity
import com.softwarelinkers.browser.activity.changeTab
import com.softwarelinkers.browser.activity.checkForInternet
import com.softwarelinkers.browser.databinding.BookmarkViewBinding
import com.softwarelinkers.browser.databinding.LongBookmarkViewBinding
import com.softwarelinkers.browser.fragment.BrowseFragment

class HomeBookmarkAdapter(private val context: Context, private val isActivity: Boolean = false): RecyclerView.Adapter<HomeBookmarkAdapter.MyHolder>() {

    class MyHolder(binding: BookmarkViewBinding? = null, bindingL: LongBookmarkViewBinding? = null)
        :RecyclerView.ViewHolder((binding?.root ?: bindingL?.root)!!) {
        val image = (binding?.bookmarkIcon ?: bindingL?.bookmarkIcon)!!
        val name = (binding?.bookmarkName ?: bindingL?.bookmarkName)!!
        val root = (binding?.root ?: bindingL?.root)!!
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
        if(isActivity)
            return MyHolder(bindingL = LongBookmarkViewBinding.inflate(LayoutInflater.from(context), parent, false))
        return MyHolder(binding = BookmarkViewBinding.inflate(LayoutInflater.from(context), parent, false))
    }

    override fun onBindViewHolder(holder: MyHolder, position: Int) {

        holder.image.setBackgroundResource(R.drawable.bookmark_icon_foreground)

        holder.name.text = MainActivity.homeBookmarkList[position].name

        holder.root.setOnClickListener{
            when{
                checkForInternet(context) -> {
                    changeTab(
                        MainActivity.homeBookmarkList[position].name,
                        BrowseFragment(urlNew = MainActivity.homeBookmarkList[position].url)
                    )
                    if(isActivity) (context as Activity).finish()
                }
                else -> Snackbar.make(holder.root, "Internet Not Connected\uD83D\uDE03", 3000).show()
            }

        }
    }

    override fun getItemCount(): Int {
        return MainActivity.homeBookmarkList.size
    }
}