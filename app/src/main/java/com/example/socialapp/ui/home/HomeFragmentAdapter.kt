package com.example.socialapp.ui.home

import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.socialapp.R
import com.example.socialapp.network.ListPhoto
import timber.log.Timber

class HomeImageAdapter(
    val metrics: DisplayMetrics,
    val onBind: (holder: ViewHolder, position: Int, data: Any) -> Unit
) : PagingDataAdapter<Any, HomeImageAdapter.ViewHolder>(MyDiffUtil()) {

    val aspectRatio = metrics.widthPixels.toFloat()/metrics.heightPixels.toFloat()

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) 0 else 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val view = if (viewType == 1) {
            layoutInflater
                .inflate(R.layout.image_item, parent, false)
        } else {
            layoutInflater
                .inflate(R.layout.title, parent, false)
        }

        return ViewHolder(view, viewType)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        Timber.tag("adapter").d("bind")
        var item: Any? = null
        if (position != 0) {
            item = getItem(position)
        }else{
            holder.textView?.text = getItem(position) as String
        }
        if (item != null) {
            onBind(holder, position, item)
        }
    }

    class MyDiffUtil : DiffUtil.ItemCallback<Any>() {
        override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
            return true
        }

        override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
            return if (oldItem is ListPhoto && newItem is ListPhoto)
                oldItem.id == newItem.id
            else oldItem == newItem
        }
    }

    inner class ViewHolder(view: View, viewType: Int) : RecyclerView.ViewHolder(view) {
        var highQualImage: ImageView? = null
        var lowQualImage: ImageView? = null
        var textView: TextView? = null
        var darkModeIcon: ImageView? = null
        init {
            if (viewType == 1) {
                highQualImage = view.findViewById(R.id.imageViewHigh)
                lowQualImage = view.findViewById(R.id.imageViewLow)
                Timber.tag("aspect ration").d("$aspectRatio")
                Timber.tag("aspect ration").d("height ${metrics.heightPixels}")
                Timber.tag("aspect ration").d("width ${metrics.widthPixels}")
                (lowQualImage!!.layoutParams as ConstraintLayout.LayoutParams).dimensionRatio = aspectRatio.toString()
                (highQualImage!!.layoutParams as ConstraintLayout.LayoutParams).dimensionRatio = aspectRatio.toString()
            } else {
                textView = view.findViewById(R.id.pageTitle)
                darkModeIcon = view.findViewById(R.id.dark_mode)
            }
        }
    }
}