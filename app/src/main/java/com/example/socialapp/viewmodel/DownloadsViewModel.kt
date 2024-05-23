package com.example.socialapp.viewmodel

import android.app.Activity
import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.example.socialapp.pager.DownloadsPagingSource
import kotlinx.coroutines.flow.Flow

class DownloadsViewModel : ViewModel() {
    val status = MutableLiveData<Status>()
    private val pagingConfig = PagingConfig(
        pageSize = 30,
        enablePlaceholders = false
    )

    fun getDownloadsPager(context: Context, authority: String): Pager<Int, Any> {
        return Pager(pagingConfig) {
            DownloadsPagingSource(
                context = context,
                authority = authority,
                onLoading = {
                    status.postValue(Status.LOADING)
                },
                onSuccess = {
                    status.postValue(Status.SUCCESS)
                },
                onError = {
                    status.postValue(Status.ERROR)
                }
            )
        }
    }
}