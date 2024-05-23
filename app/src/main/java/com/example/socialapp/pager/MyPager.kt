package com.example.socialapp.pager

import android.accounts.NetworkErrorException
import android.content.Context
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.socialapp.network.UnsplashApi
import timber.log.Timber
import java.io.File

var updatePhotosStatus = true
var updateTopicPhotosStatus = true
var updateTopicsStatus = true

class PhotosPagingSource(
    private val unsplashApi: UnsplashApi,
    private val orderBy: String = "latest",
    private val header: String,
    private val context: Context,
    val onLoading: () -> Unit,
    val onSuccess: () -> Unit,
    val onError: () -> Unit
) : PagingSource<Int, Any>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Any> {
        Timber.tag("paging").d("loading..")
        val pageNumber = params.key ?: 0

        val totalData = mutableListOf<Any>()

        if (updatePhotosStatus) onLoading()

        if (pageNumber == 1) {
            return LoadResult.Page(emptyList(), 0, 2)
        }

        return try {
            if (pageNumber == 0) {
                totalData.add(0, header)
            }
             val data = unsplashApi.getPhotos(
                page = pageNumber,
                pageSize = params.loadSize,
                orderBy = orderBy
            ).body() ?: emptyList()

            val nextKey = if (data.isEmpty()) null else pageNumber + 1
            val previousKey = if (pageNumber > 0) pageNumber - 1 else null

            Timber.tag("success").d("adding...")

            totalData.addAll(data)

            if (updatePhotosStatus) onSuccess()
            updatePhotosStatus = false
            LoadResult.Page(totalData, previousKey, nextKey)
        } catch (e: Exception) {
            Toast.makeText(context, "No connection", Toast.LENGTH_LONG).show()
            Timber.tag("error").d(e)
            if (updatePhotosStatus) onError()
            updatePhotosStatus = false
            if (pageNumber == 0){
                LoadResult.Page(totalData, null, null)
            }else {
                LoadResult.Error(e)
            }
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Any>): Int? {
        val refreshKey = state.anchorPosition?.let {
            state.closestPageToPosition(it)?.prevKey?.plus(1)
        }

        return refreshKey
    }
}

class TopicPhotoPagingSource(
    private val unsplashApi: UnsplashApi,
    private val slug: String,
    private val orderBy: String = "latest",
    private val header: String,
    val onLoading: () -> Unit,
    val onSuccess: () -> Unit,
    val onError: () -> Unit
) : PagingSource<Int, Any>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Any> {
        Timber.tag("paging").d("loading..")
        val pageNumber = params.key ?: 0
        val totalData = mutableListOf<Any>()

        if (updateTopicPhotosStatus) onLoading()

        if (pageNumber == 1) {
            Timber.tag("paging").d("list photo page key 1")
            return LoadResult.Page(emptyList(), 0, 2)
        }

        return try {
            if (pageNumber == 0) {
                totalData.add(0, header)
            }
            val data = unsplashApi.getTopicPhotos(
                topicSlug = slug,
                pageSize = params.loadSize,
                page = pageNumber
            ).body() ?: emptyList()

            totalData.addAll(data)
            val nextKey = if (data.isEmpty()) null else pageNumber + 1
            val previousKey = if (pageNumber > 0) pageNumber - 1 else null
            if (updateTopicPhotosStatus) onSuccess()
            updateTopicPhotosStatus = false
            LoadResult.Page(totalData, previousKey, nextKey)
        } catch (e: Exception) {
            if (updateTopicPhotosStatus) onError()
            updateTopicPhotosStatus = false
            LoadResult.Page(totalData, null, null)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Any>): Int? {
        val refreshKey = state.anchorPosition?.let {
            state.closestPageToPosition(it)?.prevKey?.plus(1)
        }

        return refreshKey
    }
}

class DownloadsPagingSource(
    val context: Context,
    val authority: String,
    val onLoading: () -> Unit,
    val onSuccess: () -> Unit,
    val onError: () -> Unit
) : PagingSource<Int, Any>() {
    private val directory = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            .toString() + "/MyApp"
    )

    private val files: Array<File>? = directory.listFiles()

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Any> {
        val uris = mutableListOf<Any>("Downloads")

        Timber.tag("downloads").d("is loading downloads")

        val pageNumber = params.key ?: 0

        val previousKey = if (pageNumber > 0) pageNumber - 1 else null

        val nextKey = pageNumber + params.loadSize

        return try {
            onLoading()
            files
                ?.sortedByDescending { it.lastModified() }
                ?.forEach { file ->
                    uris.add(
                        FileProvider.getUriForFile(
                            context,
                            authority,
                            file
                        )
                    )
                }
            if (uris.isEmpty()) {
                onError()
            }
            onSuccess()
            LoadResult.Page(uris, null, null)
        } catch (e: IndexOutOfBoundsException) {
            LoadResult.Error(e)
        } catch (e: Exception) {
            Timber.tag("error").d("error")
            onError()
            LoadResult.Page(uris, null, null)
        }

//        try {
//            onLoading()
//            val uris = mutableListOf<Any>("Downloads")
//            val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
//            val projection = arrayOf(MediaStore.Images.Media.DATA)
//            context.contentResolver
//                .query(
//                    uri,
//                    projection,
//                    MediaStore.Images.Media.DATA + " LIKE ?",
//                    arrayOf("%${directory.path}%"),
//                    MediaStore.Images.Media.DATE_ADDED + " DESC"
//                )?.use { cursor ->
//                    val dataColumn = cursor.getColumnIndex(MediaStore.Images.Media.DATA)
//                    while (cursor.moveToNext()) {
//                        val data = cursor.getString(dataColumn)
//                        Timber.tag("data").d(data)
//                        uris.add(data.toUri())
//                    }
//                }
//            if (uris.size == 1) onError()
//            Timber.tag("data").d("data size: ${uris.size}")
//            onSuccess()
//            return LoadResult.Page(uris, null, null)
//        }catch (e:Exception){
//            onError()
//            return  LoadResult.Error(e)
//        }
    }

    override fun getRefreshKey(state: PagingState<Int, Any>): Int? {
        return state.anchorPosition?.let {
            state.closestPageToPosition(it)?.prevKey?.plus(1)
        }
    }
}