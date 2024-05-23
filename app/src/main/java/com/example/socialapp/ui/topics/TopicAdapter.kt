package com.example.socialapp.ui.topics

import android.content.Context
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.example.socialapp.R
import com.example.socialapp.network.ListTopic

class TopicAdapter(
    metrics: DisplayMetrics,
    val onBind:(holder: TopicItemView, position: Int, data: ListTopic) -> Unit
) : RecyclerView.Adapter<TopicAdapter.TopicItemView>() {

    val aspectRatio = metrics.heightPixels.toFloat()/metrics.widthPixels.toFloat()
    var data = listOf<Any>()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) 0 else 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TopicItemView {
        val layoutInflater = LayoutInflater.from(parent.context)
        val view = if (viewType == 0){
            layoutInflater.inflate(R.layout.title, parent, false)
        }else{
            layoutInflater.inflate(R.layout.topic_item, parent, false)
        }
        return TopicItemView(view, viewType)
    }

    override fun getItemCount() = data.size

    override fun onBindViewHolder(holder: TopicItemView, position: Int) {
        var item: ListTopic? = null
        if (position != 0) {
            item = data[position] as ListTopic
        }
        holder.title?.text = data[0] as String
        if (item != null) {
            onBind(holder, position, item)
        }
    }

    inner class TopicItemView(view: View, viewType: Int) : RecyclerView.ViewHolder(view) {
        var topicItem: ConstraintLayout? = null
        var coverPhoto: ImageView? = null
        var topicText: TextView? = null
        var title: TextView? = null

        init {
            if(viewType == 1) {
                topicItem = view.findViewById(R.id.header)
                coverPhoto = view.findViewById(R.id.cover_photo)
                topicText = view.findViewById(R.id.topic)

                (coverPhoto!!.layoutParams as ConstraintLayout.LayoutParams).dimensionRatio = aspectRatio.toString()
            }else {
                title = view.findViewById(R.id.pageTitle)
            }
        }
    }
}