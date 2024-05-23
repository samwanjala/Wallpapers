package com.example.socialapp

import android.content.Context
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.net.Network
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import androidx.viewpager2.widget.ViewPager2
import com.example.socialapp.databinding.ActivityMainBinding
import com.example.socialapp.pager.updatePhotosStatus
import com.example.socialapp.pager.updateTopicPhotosStatus
import com.example.socialapp.pager.updateTopicsStatus
import com.example.socialapp.ui.downloads.downloadsImageAdapter
import com.example.socialapp.ui.home.homeImageAdapter
import com.example.socialapp.ui.main.mainPager
import com.example.socialapp.ui.popular.popularPhotosAdapter
import com.example.socialapp.ui.topicphotos.topicPhotosAdapter
import com.example.socialapp.ui.topics.topicAdapter
import com.example.socialapp.viewmodel.DownloadsViewModel
import com.example.socialapp.viewmodel.MainViewModel
import com.example.socialapp.viewmodel.TopicViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber


const val mainViewModelKey = "mainViewModel"
const val topicViewModelKey = "topicViewModel"
const val downloadsViewModelKey = "downloadsViewModel"

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private val mainViewModel by viewModels<MainViewModel>()
    private val topicViewModel by viewModels<TopicViewModel>()
    private val downloadsViewModel = DownloadsViewModel()
    lateinit var pageChangeCallback: ViewPager2.OnPageChangeCallback
    lateinit var networkChangeCallback: ConnectivityManager.NetworkCallback
    lateinit var connectivityManager: ConnectivityManager
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()


        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        var isInitializing = true



        networkChangeCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                if (!isInitializing) {
                    updateTopicsStatus = true
                    updateTopicPhotosStatus = true
                    updatePhotosStatus = true
                    Timber.tag("initializing").d("is not initializing")
                    if (homeImageAdapter?.itemCount == 1) {
                        homeImageAdapter?.refresh()
                    }
                    if (popularPhotosAdapter?.itemCount == 1) {
                        popularPhotosAdapter?.refresh()
                    }
                    if (topicPhotosAdapter?.itemCount == 1) {
                        topicPhotosAdapter?.refresh()
                    }
                    if (topicAdapter?.data?.size == 1) {
                        lifecycleScope.launch {
                            topicAdapter?.data = topicViewModel.getTopics()
                        }
                    }
                } else {
                    Timber.tag("initializing").d("is initializing")
                }
            }
        }

        connectivityManager.registerDefaultNetworkCallback(networkChangeCallback)

        this.viewModelStore.put(mainViewModelKey, mainViewModel)
        this.viewModelStore.put(topicViewModelKey, topicViewModel)
        this.viewModelStore.put(downloadsViewModelKey, downloadsViewModel)

        val navView: BottomNavigationView = binding.navView

        val nightModeFlags: Int = this.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK

        when (nightModeFlags ) {
            Configuration.UI_MODE_NIGHT_YES -> {
                Timber.tag("dark").d("night mode yes")
                navView.setBackgroundColor(resources.getColor(R.color.black, null))
            }

            Configuration.UI_MODE_NIGHT_NO -> {
                Timber.tag("dark").d("night mode no")
                navView.setBackgroundColor(resources.getColor(R.color.white, null))
            }

            Configuration.UI_MODE_NIGHT_UNDEFINED -> {
                Timber.tag("dark").d("night mode undefined")
            }
        }

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        navView.setupWithNavController(navController)

        val navHome = navView.menu.findItem(R.id.navigation_home).apply {
            setOnMenuItemClickListener {
                mainPager?.currentItem = 0
                true
            }
        }

        val navExplore = navView.menu.findItem(R.id.navigation_explore).apply {
            setOnMenuItemClickListener {
                mainPager?.currentItem = 2
                true
            }
        }

        val navDownload = navView.menu.findItem(R.id.downloads).apply {
            setOnMenuItemClickListener {
                mainPager?.currentItem = 3
                true
            }
        }

        pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                when (position) {
                    0 -> {
                        navHome.isChecked = true
                    }

                    1 -> {
                        navHome.isChecked = true
                    }

                    2 -> {
                        navExplore.isChecked = true
                    }

                    3 -> {
                        navDownload.isChecked = true
                    }
                }
            }
        }

        mainPager?.registerOnPageChangeCallback(pageChangeCallback)
        isInitializing = false
    }

    override fun onDestroy() {
        super.onDestroy()
        updateTopicsStatus = true
        updateTopicPhotosStatus = true
        updatePhotosStatus = true
        topicAdapter = null
        homeImageAdapter = null
        topicPhotosAdapter = null
        downloadsImageAdapter = null
        mainPager?.unregisterOnPageChangeCallback(pageChangeCallback)
        mainPager = null
        connectivityManager.unregisterNetworkCallback(networkChangeCallback)
    }
}