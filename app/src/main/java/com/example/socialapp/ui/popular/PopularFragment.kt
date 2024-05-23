package com.example.socialapp.ui.popular

import android.content.Intent
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.toColorInt
import androidx.core.util.Pair
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import coil.load
import coil.size.ViewSizeResolver
import com.example.socialapp.databinding.HomeBinding
import com.example.socialapp.mainViewModelKey
import com.example.socialapp.network.ListPhoto
import com.example.socialapp.ui.detailactivity.DetailActivity
import com.example.socialapp.ui.downloads.setGridLayoutManager
import com.example.socialapp.ui.home.HomeImageAdapter
import com.example.socialapp.viewmodel.MainViewModel
import com.example.socialapp.viewmodel.Status
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

var popularPhotosAdapter: HomeImageAdapter? = null
class PopularFragment : Fragment() {
    private var _binding: HomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = HomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mainViewModel =
            ViewModelProvider(requireActivity())[mainViewModelKey, MainViewModel::class.java]

        val popularPager = mainViewModel.getPhotosPager(
            orderBy = "popular",
            header = "Popular",
            context = requireContext()
        )

        val metrics: DisplayMetrics = requireContext().resources.displayMetrics

        popularPhotosAdapter = HomeImageAdapter(metrics) { holder, position, data ->
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
                    }

                    val intent = Intent(requireContext(), DetailActivity().javaClass).apply {
                        putExtras(bundle)
                    }

                    val activityOptions = ActivityOptionsCompat.makeSceneTransitionAnimation(
                        requireActivity(),
                        Pair(highQualImage, highQualImage.transitionName),
                        Pair(lowQualImage, lowQualImage.transitionName)
                    )

                    ActivityCompat.startActivity(
                        requireContext(),
                        intent,
                        activityOptions.toBundle()
                    )
                }
            }
        }

        val popularRecyclerView = binding.homeRecyclerView

        popularRecyclerView.layoutManager = setGridLayoutManager(requireContext())

        popularRecyclerView.clipToOutline = true

        val swipeRefresh = binding.swipeRefresh

        var isRefreshing = false

        swipeRefresh.setOnRefreshListener {
            isRefreshing = true
            popularPhotosAdapter!!.refresh()
        }

        popularPhotosAdapter!!.addLoadStateListener { loadStates ->
            if (isRefreshing) {
                swipeRefresh.isRefreshing = loadStates.refresh is LoadState.Loading
            }
        }

        val progressBar = binding.progressBar
        val statusIcon = binding.status

        val statusObserver = Observer<Status> { status ->
            when (status) {
                Status.LOADING -> {
                    Timber.tag("popular status").d("loading")
                    statusIcon.visibility = View.INVISIBLE
                    if (!swipeRefresh.isRefreshing) {
                        progressBar.visibility = View.VISIBLE
                    }
                }

                Status.SUCCESS -> {
                    Timber.tag("popular status").d("success")
                    statusIcon.visibility = View.INVISIBLE
                        progressBar.visibility = View.INVISIBLE
                }

                else -> {
                    Timber.tag("popular status").d("fail")
                    progressBar.visibility = View.INVISIBLE
                    statusIcon.visibility = View.VISIBLE
                }
            }
        }

        mainViewModel.status.observe(viewLifecycleOwner, statusObserver)

        Timber.tag("data3").d("view created")
        lifecycleScope.launch {
            popularPager.flow.collectLatest {
                popularPhotosAdapter!!.submitData(it)
            }
        }

        popularRecyclerView.adapter = popularPhotosAdapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}