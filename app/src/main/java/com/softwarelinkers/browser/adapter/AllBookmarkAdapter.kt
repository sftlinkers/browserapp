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

class AllBookmarkAdapter(private val context: Context, private val isActivity: Boolean = false): RecyclerView.Adapter<AllBookmarkAdapter.MyHolder>() {

    private val colors = context.resources.getIntArray(R.array.myColors)

    class MyHolder(binding: BookmarkViewBinding? = null, bindingL: LongBookmarkViewBinding? = null)
        :RecyclerView.ViewHolder((binding?.root ?: bindingL?.root)!!) {
        val image = (binding?.bookmarkIcon ?: bindingL?.bookmarkIcon)!!
        val name = (binding?.bookmarkName ?: bindingL?.bookmarkName)!!
        val root = (binding?.root ?: bindingL?.root)!!
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
//        if(isActivity)
            return MyHolder(bindingL = LongBookmarkViewBinding.inflate(LayoutInflater.from(context), parent, false))
//        return MyHolder(binding = BookmarkViewBinding.inflate(LayoutInflater.from(context), parent, false))
    }

    override fun onBindViewHolder(holder: AllBookmarkAdapter.MyHolder, position: Int) {
        try {
            val icon = BitmapFactory.decodeByteArray(
                MainActivity.allBookmarkList[position].image, 0,
                MainActivity.allBookmarkList[position].image!!.size)
            holder.image.background = icon.toDrawable(context.resources)
        }catch (e: Exception){
            holder.image.setBackgroundColor(colors[(colors.indices).random()])
            holder.image.text = MainActivity.allBookmarkList[position].name[0].toString()
        }
        holder.name.text = MainActivity.allBookmarkList[position].name

        holder.root.setOnClickListener{
            when{
                checkForInternet(context) -> {
                    changeTab(
                        MainActivity.allBookmarkList[position].name,
                        BrowseFragment(urlNew = MainActivity.allBookmarkList[position].url)
                    )
                    if(isActivity) (context as Activity).finish()
                }
                else -> Snackbar.make(holder.root, "Internet Not Connected\uD83D\uDE03", 3000).show()
            }

        }    }


    override fun getItemCount(): Int {
        return MainActivity.allBookmarkList.size
    }




















//    private val colors = context.resources.getIntArray(R.array.myColors)
//
//    class MyHolder(view: View): RecyclerView.ViewHolder(view) {
//        val name: TextView = view.findViewById(R.id.bookmarkName2)
//        val image: TextView = view.findViewById(R.id.bookmarkIcon2)
//    }
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
//        return MyHolder(
//            LayoutInflater.from(parent.context).inflate(R.layout.long_bookmark_view2, parent, false)
//        )
//    }
//
//    override fun getItemCount(): Int {
//        return bookmarkList.size
//    }
//
//    override fun onBindViewHolder(holder: MyHolder, position: Int) {
//        try {
//            val icon = BitmapFactory.decodeByteArray(
//                bookmarkList[position].image, 0,
//                bookmarkList[position].image!!.size)
//            holder.image.background = icon.toDrawable(context.resources)
//        }catch (e: Exception){
//            holder.image.setBackgroundColor(colors[(colors.indices).random()])
//            holder.image.text = MainActivity.bookmarkList[position].name[0].toString()
//        }
//        holder.name.text = MainActivity.bookmarkList[position].name
//
//        holder.itemView.setOnClickListener{
//            when{
//                checkForInternet(context) -> {
//                    changeTab(bookmarkList[position].name,
//                        BrowseFragment(urlNew = bookmarkList[position].url)
//                    )
//                    (context as Activity).finish()
//                }
//                else -> Snackbar.make(holder.itemView, "Internet Not Connected\uD83D\uDE03", 3000).show()
//            }
//
//        }
//    }
}