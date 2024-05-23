package com.example.socialapp.ui.topicphotos

import android.content.Intent
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.View
import android.view.ViewTreeObserver
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.toColorInt
import androidx.core.util.Pair
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import coil.load
import coil.result
import coil.size.ViewSizeResolver
import com.example.socialapp.databinding.HomeBinding
import com.example.socialapp.network.ListPhoto
import com.example.socialapp.ui.detailactivity.DetailActivity
import com.example.socialapp.ui.downloads.setGridLayoutManager
import com.example.socialapp.ui.home.HomeImageAdapter
import com.example.socialapp.viewmodel.Status
import com.example.socialapp.viewmodel.TopicViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

var topicPhotosAdapter: HomeImageAdapter? = null
var slug: String? = null

@AndroidEntryPoint
class TopicPhotosActivity : AppCompatActivity() {
    private var _binding: HomeBinding? = null
    private val binding get() = _binding!!

    private val topicViewModel by viewModels<TopicViewModel>()

    override fun onResume() {
        super.onResume()
//        val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
//        windowInsetsController.isAppearanceLightStatusBars = true
//        windowInsetsController.isAppearanceLightNavigationBars = true
//        window.statusBarColor = ContextCompat.getColor(applicationContext, R.color.off_white)
//        window.navigationBarColor = ContextCompat.getColor(applicationContext, R.color.off_white)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = HomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        slug = intent.getStringExtra("slug")
        val title = intent.getStringExtra("title")

        val topicPhotosPager = topicViewModel.getTopicPhotosPager(slug!!, header = title ?: "")

        val metrics: DisplayMetrics = applicationContext.resources.displayMetrics

        topicPhotosAdapter = HomeImageAdapter(metrics) { holder, position, data ->
            val highQualImage = holder.highQualImage
            val lowQualImage = holder.lowQualImage

            if (highQualImage != null && lowQualImage != null) {
                val item = data as ListPhoto
                highQualImage.clipToOutline = true
                lowQualImage.clipToOutline = true

                val intCol = item.color?.toColorInt()

                Timber.tag("color Int").d("$intCol")

                if (intCol != null) {
                    lowQualImage.setImageDrawable(intCol.toDrawable())
                }

                lowQualImage.viewTreeObserver.addOnPreDrawListener(
                    object : ViewTreeObserver.OnPreDrawListener {
                        override fun onPreDraw(): Boolean {
                            lowQualImage.viewTreeObserver.removeOnPreDrawListener(this)
                            val url = item.urls?.regular
                            highQualImage.load(url) {
                                size(ViewSizeResolver(lowQualImage))
                            }
                            return true
                        }

                    }
                )


                highQualImage.setOnClickListener {
                    highQualImage.transitionName = "image_high_$position"
                    lowQualImage.transitionName = "image_low_$position"

                    val bundle = Bundle().apply {
                        putSerializable("listphoto", item)
                        putString("low_tran_name", lowQualImage.transitionName)
                        putString("high_tran_name", highQualImage.transitionName)
                        putString(
                            "placeHolderKey",
                            lowQualImage.result?.request?.memoryCacheKey?.key
                        )
                    }

                    val intent = Intent(this, DetailActivity().javaClass).apply {
                        putExtras(bundle)
                    }

                    val activityOptions = ActivityOptionsCompat.makeSceneTransitionAnimation(
                        this,
                        Pair(highQualImage, highQualImage.transitionName),
                        Pair(lowQualImage, lowQualImage.transitionName)
                    )

                    ActivityCompat.startActivity(this, intent, activityOptions.toBundle())
                }
            }
        }

        val recyclerView = binding.homeRecyclerView

        recyclerView.layoutManager = setGridLayoutManager(applicationContext)

        recyclerView.clipToOutline = true

        val swipeRefresh = binding.swipeRefresh

        var isRefreshing = false

        swipeRefresh.setOnRefreshListener {
            isRefreshing = true
            Timber.tag("refresh").d("refreshing...")
            topicPhotosAdapter!!.refresh()
        }

        topicPhotosAdapter!!.addLoadStateListener { loadStates ->
            if (isRefreshing) {
                swipeRefresh.isRefreshing = loadStates.refresh is LoadState.Loading
            }
        }

        val progressBar = binding.progressBar
        val statusIcon = binding.status

        val statusObserver = Observer<Status> { status ->
            when (status) {
                Status.LOADING -> {
                    statusIcon.visibility = View.INVISIBLE
                    if (!swipeRefresh.isRefreshing) {
                        progressBar.visibility = View.VISIBLE
                    }
                }

                Status.SUCCESS -> {
                    statusIcon.visibility = View.INVISIBLE
                    progressBar.visibility = View.INVISIBLE
                }

                else -> {
                    progressBar.visibility = View.INVISIBLE
                    statusIcon.visibility = View.VISIBLE
                }
            }
        }

        topicViewModel.topicPhotosStatus.observe(this, statusObserver)

        Timber.tag("data3").d("view created")
        lifecycleScope.launch {
            topicPhotosPager.flow.collectLatest {
                topicPhotosAdapter!!.submitData(it)
            }
        }

        recyclerView.adapter = topicPhotosAdapter
    }
}