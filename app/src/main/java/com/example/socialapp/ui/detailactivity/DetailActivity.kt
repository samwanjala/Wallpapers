package com.example.socialapp.ui.detailactivity

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Bitmap.createBitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.contentValuesOf
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import coil.ImageLoader
import coil.decode.DataSource
import coil.imageLoader
import coil.load
import coil.request.ImageRequest
import com.example.socialapp.databinding.DetailActivityBinding
import com.example.socialapp.network.ListPhoto
import com.example.socialapp.network.ProgressInterceptor
import com.example.socialapp.ui.downloads.downloadsImageAdapter
import com.example.socialapp.ui.progress.RadialProgressBar
import com.google.android.renderscript.Toolkit
import okhttp3.OkHttpClient
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

const val appName = "My App"

class DetailActivity : AppCompatActivity() {
    private var _binding: DetailActivityBinding? = null

    private var isSaving = false
    private val binding get() = _binding!!

    lateinit var bitmap: Bitmap

    lateinit var item: ListPhoto

    private lateinit var metrics: DisplayMetrics

    private lateinit var uri: Uri

    private lateinit var requestPermissionLauncher: ActivityResultLauncher<Array<String>>

    private lateinit var progressBar: RadialProgressBar

    private lateinit var progressText: TextView
    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }
        hideSystemUI()
        progressBar.visibility = View.INVISIBLE
        progressText.visibility = View.INVISIBLE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = DetailActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                if (permissions[Manifest.permission.READ_EXTERNAL_STORAGE] == true && permissions[Manifest.permission.WRITE_EXTERNAL_STORAGE] == true) {
                    saveImage()
                }
            }

        metrics = applicationContext.resources.displayMetrics

        val lowQualImage = binding.detailImageViewLow
        val highQualImage = binding.detailImageViewHigh
        progressBar = binding.detailImageViewProgressBar
        progressText = binding.progressText

        supportPostponeEnterTransition()

        lowQualImage.transitionName = intent.getStringExtra("low_tran_name")
        highQualImage.transitionName = intent.getStringExtra("high_tran_name")

        WindowCompat.setDecorFitsSystemWindows(window, false)

        var showUi = false

        item = intent.getSerializableExtra("listphoto") as ListPhoto

        val loadProgress = MutableLiveData<Int>()

        val progressObserver = Observer<Int> { progress ->
            progressBar.setProgress(progress, true)
            progressText.text = progress.toString()
        }

        loadProgress.observe(this, progressObserver)

        val progressInterceptor = object : ProgressInterceptor.ProgressListener {
            override fun onProgress(progress: Int) {
                Timber.tag("progress").d("$progress")
                loadProgress.postValue(progress)
            }
        }

        val imageLoader = ImageLoader.Builder(this)
            .crossfade(300)
            .okHttpClient {
                OkHttpClient.Builder()
                    .addInterceptor(ProgressInterceptor(progressInterceptor))
                    .build()
            }
            .build()

        val imageReq = ImageRequest.Builder(lowQualImage.context)
            .size(100)
            .data(item.urls?.thumb)
            .listener { _, result ->
                Timber.tag("blurtask").e("set low")
                val bit = result.drawable.toBitmap()
                val inputBit = bit.copy(Bitmap.Config.ARGB_8888, true)
                val blurredBitmap = Toolkit.blur(inputBit, 25)

                lowQualImage.setImageBitmap(blurredBitmap)

                if (result.dataSource == DataSource.NETWORK) {
                    startPostponedEnterTransition()
                }
            }
            .crossfade(false)
            .build()

        lowQualImage.context.imageLoader.enqueue(imageReq)

        lowQualImage.viewTreeObserver.addOnPreDrawListener(
            object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    lowQualImage.viewTreeObserver.removeOnPreDrawListener(this)

                    val urlHigh =
                        item.urls?.raw + "auto=enhance&fm=jpg&fit=crop&w=${lowQualImage.width}&h=${lowQualImage.height}&q=100"

                    highQualImage.load(urlHigh, imageLoader) {
                        listener(
                            onStart = {
                                Timber.tag("success").d("onstart")
                                progressBar.visibility = View.VISIBLE
                                progressText.visibility = View.VISIBLE
                            },
                            onCancel = {
                                Timber.tag("success").d("oncancel")
                                progressBar.visibility = View.INVISIBLE
                                progressText.visibility = View.INVISIBLE
                            },
                            onError = { _, _ ->
                                Timber.tag("success").d("onerror")
                                progressBar.visibility = View.INVISIBLE
                                progressText.visibility = View.INVISIBLE
                            },
                            onSuccess = { _, result ->
                                progressBar.visibility = View.INVISIBLE
                                progressText.visibility = View.INVISIBLE
                                if (result.dataSource != DataSource.NETWORK) {
                                    startPostponedEnterTransition()
                                }
                                Timber.tag("success").d("onsuccess")
                                bitmap = result.drawable.toBitmap()
                            }
                        )
                    }
                    return true
                }
            })


        highQualImage.setOnClickListener {
            Timber.tag("systemUi").d("hide")
            showUi = if (showUi) {
                Timber.tag("systemUi").d("hide")
                hideSystemUI()
                false
            } else {
                Timber.tag("systemUi").d("show")
                showSystemUI()
                true
            }
        }

        binding.back.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.download.setOnClickListener {
            checkPermissions(applicationContext)
        }

        binding.wallpaper.setOnClickListener {
            saveImage()
            if (::uri.isInitialized) {
                val intent = Intent(Intent.ACTION_ATTACH_DATA).apply {
                    addCategory(Intent.CATEGORY_DEFAULT)
                    setDataAndType(uri, "image/*")
                    putExtra("mimeType", "image/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(intent, "Set As"))
            }
        }
    }

    private fun hideSystemUI() {
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun showSystemUI() {
        WindowInsetsControllerCompat(
            window,
            window.decorView
        ).show(WindowInsetsCompat.Type.systemBars())
    }

    private fun saveImage() {
        if (!::bitmap.isInitialized) return

        val directory = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                .toString() + "/" + "MyApp"
        )

        if (!directory.exists()) {
            directory.mkdir()
        }

        val name = "${item.id}.jpeg"

        val file = File(directory.path, name)

        insertItems(bitmap, name, file)

        downloadsImageAdapter?.refresh()
    }

    private fun insertItems(bitmap: Bitmap, name: String, file: File) {
        val resolver = applicationContext.contentResolver
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
            var outputStream: OutputStream? = null
            val values = contentValuesOf().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, name)
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/MyApp")
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
                put(MediaStore.Images.Media.ALBUM, appName)
            }

            val uri = resolver.insert(
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
                values
            )

            if (uri != null) {
                outputStream = resolver.openOutputStream(uri)
            }
            if (outputStream != null) {
                saveImageToStream(bitmap, outputStream)
            }
        }else{
            if (file.exists()) {
                Toast.makeText(applicationContext, "Image already exists", Toast.LENGTH_LONG).show()
                return
            }

            uri = FileProvider.getUriForFile(
                applicationContext,
                applicationContext.packageName + ".provider",
                file
            )

            val fileOutputStream: FileOutputStream?

            try {
                fileOutputStream = FileOutputStream(file)
            } catch (e: FileNotFoundException){
                Toast.makeText(applicationContext, "Image could not be saved", Toast.LENGTH_LONG).show()
                return
            }

            saveImageToStream(bitmap, fileOutputStream)

            val values = contentValuesOf().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, name)
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/MyApp")
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
                put(MediaStore.Images.Media.ALBUM, appName)
            }

            resolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values
            )
        }
    }

    private fun saveImageToStream(bitmap: Bitmap?, outputStream: OutputStream) {
        if (isSaving) return
        try {
            isSaving = true
            bitmap?.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            outputStream.close()
            Toast.makeText(this, "saved ", Toast.LENGTH_LONG).show()
            isSaving = false
        } catch (e: Exception) {
            isSaving = false
            Toast.makeText(this, "error saving", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    private fun checkPermissions(context: Context) {
        Timber.tag("perm").d("x")
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            val isReadPermissionGranted =
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )

            val isWritePermissionGranted =
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )

            if (isReadPermissionGranted == PackageManager.PERMISSION_DENIED && isWritePermissionGranted == PackageManager.PERMISSION_DENIED) {
                Timber.tag("perm").d("permission denied")
                requestPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                )
            } else {
                Timber.tag("perm").d("permission granted")
                saveImage()
            }
        } else {
            saveImage()
        }
    }
}

