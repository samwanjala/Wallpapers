package com.example.socialapp.ui.downloads

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.DisplayMetrics
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil.imageLoader
import coil.load
import coil.request.ImageRequest
import com.example.socialapp.databinding.DownloadsDetailActivityBinding
import com.google.android.renderscript.Toolkit
import timber.log.Timber
import java.io.File


class DownloadsDetailActivity : AppCompatActivity() {
    private var _binding: DownloadsDetailActivityBinding? = null
    private val binding get() = _binding!!

    lateinit var metrics: DisplayMetrics

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }
        hideSystemUI()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = DownloadsDetailActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        metrics = applicationContext.resources.displayMetrics

        val lowQualImage = binding.downloadDetailImageViewLow
        val highQualImage = binding.downloadDetailImageViewHigh

        highQualImage.transitionName = intent.getStringExtra("transitionNameHigh")
        lowQualImage.transitionName = intent.getStringExtra("transitionNameLow")
        hideSystemUI()
        var showUi = false

        val uri = intent.getStringExtra("listphoto")?.toUri()

        supportPostponeEnterTransition()

        val imageReq = ImageRequest.Builder(lowQualImage.context)
            .size(100)
            .data(uri)
            .listener { _, result ->
                val bit = result.drawable.toBitmap()
                val inputBit = bit.copy(Bitmap.Config.ARGB_8888, true)
                val blurredBitmap = Toolkit.blur(inputBit, 25)
                lowQualImage.setImageBitmap(blurredBitmap)
                highQualImage.load(uri){
                    listener{ _, _ ->
                        scheduleStartPostponedTransition(lowQualImage)
                    }
                }
            }
            .crossfade(false)
            .build()

        lowQualImage.context.imageLoader.enqueue(imageReq)


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

        binding.wallpaper.setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_ATTACH_DATA).apply {
                    addCategory(Intent.CATEGORY_DEFAULT)
                    setDataAndType(uri, "image/jpeg")
                    putExtra("mimeType", "image/jpeg")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(intent, "Set As"))
            }catch (e:Exception){
                Throwable(e)
                Timber.tag("wallpaper").d(e)
            }
        }

        binding.delete.setOnClickListener {
            val fileToDelete = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString() + "/" + "MyApp", uri?.lastPathSegment ?: "")
            Timber.tag("file").d("path ${uri?.path}")
            if (fileToDelete.exists()){
                try {
                    fileToDelete.delete()
                    notifyMediaStoreFileDeleted(applicationContext, fileToDelete)
                    Toast.makeText(applicationContext, "image deleted successfully", Toast.LENGTH_LONG).show()
                }catch (e: Exception){
                    Toast.makeText(applicationContext, "error deleting image", Toast.LENGTH_LONG).show()
                }
            }else{
                Timber.tag("file").d("file does not exist")
            }
        }
    }

    private fun notifyMediaStoreFileDeleted(context: Context, file: File) {
        MediaScannerConnection.scanFile(
            context,
            arrayOf(file.toString()),
            null
        ) { uri, mimetype ->
            downloadsImageAdapter?.refresh()
            Timber.tag("scan").d("uri: $uri, mimetype: $mimetype")
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

    private fun scheduleStartPostponedTransition(sharedElement: View) {
        sharedElement.viewTreeObserver.addOnPreDrawListener(
            object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    sharedElement.viewTreeObserver.removeOnPreDrawListener(this)
                    startPostponedEnterTransition()
                    return true
                }
            })
    }
}