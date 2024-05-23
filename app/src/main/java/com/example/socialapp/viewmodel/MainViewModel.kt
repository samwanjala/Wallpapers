package com.example.socialapp.viewmodel

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.paging.Pager
import androidx.paging.PagingConfig
import com.example.socialapp.network.UnsplashApi
import com.example.socialapp.pager.PhotosPagingSource
import dagger.hilt.android.lifecycle.HiltViewModel
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val unsplashApi: UnsplashApi
) : ViewModel() {
    val status = MutableLiveData<Status>()
    private val pagingConfig = PagingConfig(
        pageSize = 30,
        enablePlaceholders = false
    )

    fun getPhotosPager(orderBy: String = "latest", header: String, context: Context): Pager<Int, Any> {
        return Pager(pagingConfig) {
            PhotosPagingSource(
                unsplashApi = unsplashApi,
                orderBy = orderBy,
                header = header,
                context = context,
                onSuccess = {
                    Timber.tag("status").d("success")
                    status.postValue(Status.SUCCESS)
                },
                onError = {
                    Timber.tag("status").d("error")
                    status.postValue(Status.ERROR)
                },
                onLoading = {
                    Timber.tag("status").d("loading")
                    status.postValue(Status.LOADING)
                }
            )
        }
    }
}

enum class Status{
    LOADING,
    ERROR,
    SUCCESS
}