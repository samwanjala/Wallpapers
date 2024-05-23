package com.example.socialapp.ui.downloads

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PackageManagerCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.util.Pair
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import coil.load
import coil.request.CachePolicy
import coil.result
import com.example.socialapp.R
import com.example.socialapp.databinding.HomeBinding
import com.example.socialapp.downloadsViewModelKey
import com.example.socialapp.ui.home.HomeImageAdapter
import com.example.socialapp.viewmodel.DownloadsViewModel
import com.example.socialapp.viewmodel.Status
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

var downloadsImageAdapter: HomeImageAdapter? = null

class DownloadsFragment : Fragment() {

    private var _binding: HomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var downloadsViewModel: DownloadsViewModel

    private var isPermissionGranted = 0

    override fun onResume() {
        super.onResume()
        if (isPermissionGranted == PackageManager.PERMISSION_GRANTED){
            getData()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkPermissions(requireContext())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = HomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    private fun initView() {
        downloadsViewModel = ViewModelProvider(requireActivity())[downloadsViewModelKey, DownloadsViewModel::class.java]

        binding.status.visibility = View.INVISIBLE

        val statusIcon = binding.icon
        val progressBar = binding.progressBar
        val message = binding.message

        val swipeRefresh = binding.swipeRefresh

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
                    if (!swipeRefresh.isRefreshing) {
                        progressBar.visibility = View.INVISIBLE
                    }
                }

                else -> {
                    val drawable = AppCompatResources.getDrawable(requireContext(), R.drawable.baseline_broken_image_24)
                    statusIcon.setImageDrawable(drawable)
                    message.text = "No downloads"
                    progressBar.visibility = View.INVISIBLE
                    statusIcon.visibility = View.VISIBLE
                }
            }
        }

        downloadsViewModel.status.observe(viewLifecycleOwner, statusObserver)

        val metrics: DisplayMetrics = requireContext().resources.displayMetrics

        downloadsImageAdapter = HomeImageAdapter(metrics) { holder, position, uri ->
            val highQualImage = holder.highQualImage
            val lowQualImage = holder.lowQualImage

            if (highQualImage != null && lowQualImage != null) {
                val item = uri as Uri
                highQualImage.clipToOutline = true
                lowQualImage.clipToOutline = true

                val backGround = Color.GRAY.toDrawable()

                lowQualImage.setImageDrawable(backGround)

                highQualImage.load(item)

                highQualImage.setOnClickListener {
                    lowQualImage.transitionName = "image_low_$position"
                    highQualImage.transitionName = "image_high_$position"

                    val bundle = Bundle().apply {
                        putString("listphoto", item.toString())
                        putString("transitionNameLow", lowQualImage.transitionName)
                        putString("transitionNameHigh", highQualImage.transitionName)
                        putString(
                            "lowPlaceholderCacheKey",
                            lowQualImage.result?.request?.memoryCacheKey?.key
                        )
                        putString(
                            "highPlaceholderCacheKey",
                            highQualImage.result?.request?.memoryCacheKey?.key
                        )
                        putInt("pos", position)
                    }

                    val intent = Intent(activity, DownloadsDetailActivity().javaClass).apply {
                        putExtras(bundle)
                    }

                    val activityOptions = ActivityOptionsCompat.makeSceneTransitionAnimation(
                        requireActivity(),
                        Pair(highQualImage, highQualImage.transitionName),
                        Pair(lowQualImage, lowQualImage.transitionName)
                    )
                    ActivityCompat.startActivity(
                        requireActivity(),
                        intent,
                        activityOptions.toBundle()
                    )
                }
            }
        }

        var isRefreshing = false

        swipeRefresh.setOnRefreshListener {
            isRefreshing = true
            Timber.tag("refresh").d("refreshing...")
            downloadsImageAdapter!!.refresh()
        }

        downloadsImageAdapter!!.addLoadStateListener { loadStates ->
            if (isRefreshing) {
                swipeRefresh.isRefreshing = loadStates.refresh is LoadState.Loading
            }
        }


        val recyclerView = binding.homeRecyclerView
        recyclerView.clipToOutline = true
        recyclerView.layoutManager = setGridLayoutManager(requireContext())
        recyclerView.adapter = downloadsImageAdapter
        recyclerView.itemAnimator = DefaultItemAnimator()
    }

    fun getData() {
        val authority = requireActivity().packageName + ".provider"

        val downloadsPager = downloadsViewModel.getDownloadsPager(requireContext(), authority)

        lifecycleScope.launch {
            downloadsPager.flow.collectLatest {
                downloadsImageAdapter!!.submitData(it)
            }
        }
    }

    fun checkPermissions(context: Context) {
        val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){ permissions ->
            if (permissions[Manifest.permission.READ_EXTERNAL_STORAGE] == true && permissions[Manifest.permission.WRITE_EXTERNAL_STORAGE] == true){
                getData()
            }
            if (permissions[Manifest.permission.READ_MEDIA_IMAGES] == true){
                getData()
            }
        }

        isPermissionGranted = if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2){
            ContextCompat.checkSelfPermission(context, Manifest.permission_group.STORAGE)
        }else{
            ContextCompat.checkSelfPermission(context, Manifest.permission_group.STORAGE)
        }

        if(isPermissionGranted == PackageManager.PERMISSION_DENIED) {
            Timber.tag("perm").d("permission denied")
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
                requestPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                )
            }else{
                requestPermissionLauncher.launch(arrayOf(Manifest.permission.READ_MEDIA_IMAGES))
            }
        }
    }

//    private fun fetchData(): List<Uri> {
//        binding.progressBar.visibility = View.VISIBLE
//        val uris = mutableListOf<Uri>()
////        requireContext().contentResolver.query(
////            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
////            arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.ALBUM),
////            null,
////            null,
////            MediaStore.Images.Media.DATE_ADDED + " DESC"
////        )?.use { cursor ->
////            val idColInd = cursor.getColumnIndex(MediaStore.Images.Media._ID)
////            val i = cursor.getColumnIndex(MediaStore.Images.Media.ALBUM)
////            while (cursor.moveToNext()){
////                val id = cursor.getLong(idColInd)
////                Timber.tag("is download").d(cursor.getString(i))
////                val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
////                uris.add(uri)
////            }
////        }
//
//
//        val wallpapersFile =
//            File(Environment.getExternalStorageDirectory().toString() + "/" + "MyApp")
//
//        wallpapersFile.listFiles()
//            ?.sortedByDescending { it.lastModified() }
//            ?.forEach { file ->
//                uris.add(
//                    FileProvider.getUriForFile(
//                        requireContext(),
//                        requireActivity().packageName + ".provider",
//                        file
//                    )
//                )
//            }
//        binding.progressBar.visibility = View.INVISIBLE
//        if (uris.isEmpty()) {
//            binding.status.visibility = View.VISIBLE
//            val icon = AppCompatResources.getDrawable(requireContext(),R.drawable.baseline_broken_image_24)
//            binding.icon.setImageDrawable(icon)
//            binding.message.text = "Download images to see them here"
//        }
//
//        return uris
//    }
}

fun setGridLayoutManager(context: Context): GridLayoutManager {
    return GridLayoutManager(context, 2, GridLayoutManager.VERTICAL, false).apply {
        spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if (position == 0) 2 else 1
            }
        }
    }
}