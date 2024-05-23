package com.example.socialapp.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.paging.Pager
import androidx.paging.PagingConfig
import com.example.socialapp.network.ListTopic
import com.example.socialapp.network.UnsplashApi
import com.example.socialapp.pager.TopicPhotoPagingSource
import com.example.socialapp.pager.updateTopicsStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class TopicViewModel @Inject constructor(
    private val unsplashApi: UnsplashApi
) : ViewModel() {
    val topicPhotosStatus = MutableLiveData<Status>()
    val topicsStatus = MutableLiveData<Status>()

    private val pagingConfig = PagingConfig(
        pageSize = 30,
        enablePlaceholders = false
    )

    fun getTopicPhotosPager(slug: String, header: String): Pager<Int, Any> {
        return Pager(pagingConfig) {
            TopicPhotoPagingSource(
                unsplashApi,
                slug = slug,
                header = header,
                onSuccess = {
                    topicPhotosStatus.postValue(Status.SUCCESS)
                },
                onError = {
                    topicPhotosStatus.postValue(Status.ERROR)
                },
                onLoading = {
                    topicPhotosStatus.postValue(Status.LOADING)
                }
            )
        }
    }

    suspend fun getTopics(): List<Any> {
        if (updateTopicsStatus) topicsStatus.postValue(Status.LOADING)
        val items = mutableListOf<Any>("Categories")
        try {
            val listTopicItems = unsplashApi.getTopics().body()?.filter {
                it.title != "Wallpapers" && it.title != "Current Events"
            } ?: emptyList()
            items.addAll(1, listTopicItems)
            if (updateTopicsStatus) topicsStatus.postValue(Status.SUCCESS)
        } catch (e: Exception) {
            if (updateTopicsStatus) topicsStatus.postValue(Status.ERROR)
        }
        updateTopicsStatus = false
        return items
    }
}


