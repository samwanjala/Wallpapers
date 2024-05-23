package com.example.socialapp

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.MutableLiveData
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.request.CachePolicy
import com.example.socialapp.network.ProgressInterceptor
import dagger.hilt.android.HiltAndroidApp
import okhttp3.OkHttpClient
import timber.log.Timber

//val updateProgress = MutableLiveData<Int>()

@HiltAndroidApp
class SocialApp : Application(), ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader {
//        val progressInterceptor = object : ProgressInterceptor.ProgressListener {
//            override fun onProgress(progress: Int) {
//                Timber.tag("progress").d("$progress")
//                updateProgress.postValue(progress)
//            }
//        }

        return ImageLoader.Builder(this)
            .crossfade(300)
            .diskCachePolicy(CachePolicy.DISABLED)
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}