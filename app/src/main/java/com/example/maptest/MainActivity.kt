package com.example.maptest

import android.Manifest
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.SnapshotReadyCallback
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.*


@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {

    lateinit var buttonScreen: FloatingActionButton
    lateinit var buttonShare: FloatingActionButton
    lateinit var container: View
    lateinit var iv: ImageView
    var imagePath: File? = null
    var mSnapshot: Bitmap? = null
    var uri: Uri? = null
//    private var mSnapshot: Bitmap? = null

    lateinit var mMap: GoogleMap

    private val callback = OnMapReadyCallback {
        mMap = it
        val location = LatLng(41.298360799920665, 69.27301298813875)

        it.moveCamera(CameraUpdateFactory.newLatLng(location))
        it.setMaxZoomPreference(24f)
        it.setMinZoomPreference(12f)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(com.example.maptest.R.layout.activity_main)

        val mapFragment = supportFragmentManager
            .findFragmentById(com.example.maptest.R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(callback)

        buttonScreen = findViewById(com.example.maptest.R.id.buttonScreen)
        buttonShare = findViewById(com.example.maptest.R.id.buttonShare)
        container = findViewById(com.example.maptest.R.id.container)
        iv = findViewById(com.example.maptest.R.id.iv)

        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
            1
        )
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
            1
        )

        buttonScreen.setOnClickListener {
            buttonShare.visibility = View.INVISIBLE
            iv.visibility = View.INVISIBLE
//            val b: Bitmap = takeScreenshotOfRootView(iv)
            onScreenshot()
            Handler().postDelayed({
                onScreenshot()
            }, 100)

            Handler().postDelayed({
                iv.visibility = View.VISIBLE
                buttonShare.visibility = View.VISIBLE
            }, 500)


            container.setBackgroundColor(Color.parseColor("#999999"))

        }

        buttonShare.setOnClickListener {
            Log.d("buttonShare", "$imagePath")
            if (imagePath != null) {
                shareIt()
            }

        }

        //Long click
        buttonShare.setOnLongClickListener {
            iv.setImageDrawable(null)
            buttonShare.visibility = View.INVISIBLE
            return@setOnLongClickListener true
        }
    }

    private fun shareIt() {
        val uri = Uri.fromFile(imagePath)
        val sharingIntent = Intent(Intent.ACTION_SEND)
        sharingIntent.type = "image/*"
        val shareBody = "Map Screenshot photo"
        sharingIntent.putExtra(Intent.EXTRA_SUBJECT, "My Tweecher score")
        sharingIntent.putExtra(Intent.EXTRA_TEXT, shareBody)
        sharingIntent.putExtra(Intent.EXTRA_STREAM, uri)
        startActivity(Intent.createChooser(sharingIntent, "Share via"))
    }

    companion object Screenshot {
        private fun takeScreenshot(view: View): Bitmap {
            view.isDrawingCacheEnabled = true
            view.buildDrawingCache(true)
            val b = Bitmap.createBitmap(view.drawingCache)
            view.isDrawingCacheEnabled = false
            return b
        }

        fun takeScreenshotOfRootView(v: View): Bitmap {
            return takeScreenshot(v.rootView)
        }
    }

    private fun saveMediaToStorage(bitmap: Bitmap) {

        val contentResolver = applicationContext.contentResolver


        // Generating a file name
        val filename = "${System.currentTimeMillis()}.jpg"

        // Output stream
        var fos: OutputStream? = null

        // For devices running android >= Q
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // getting the contentResolver
            this.contentResolver?.also { resolver ->

                // Content resolver will process the contentvalues
                val contentValues = ContentValues().apply {

                    // putting file information in content values
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                }

                val imageUri: Uri? =
                    resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

                fos = imageUri.let { resolver.openOutputStream(it!!) }
            }
        } else {

            val imagesDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val image = File(imagesDir, filename)
            Log.d("buttonShare", "$image")
            imagePath = image

            fos = FileOutputStream(image)
        }

        fos?.use {
            // Finally writing the bitmap to the output stream that we opened
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
            // Toast.makeText(requireContext() , "Captured View and saved to Gallery" , Toast.LENGTH_SHORT).show()
        }
    }

    private fun onScreenshot() {
        if (mMap == null) {
            return
        }
        val callback =
            SnapshotReadyCallback { snapshot -> // Callback is called from the main thread, so we can modify the ImageView safely.
                iv.setImageBitmap(snapshot)
                // Use the same bitmap for the following snapshots.
                mSnapshot = snapshot

                saveMediaToStorage(snapshot)
                Log.d("saveMediaToStorage", "$snapshot")
            }

        // mSnapshot is null on the first call. It is then set in the callback to reuse the same
        // Bitmap object for all the following snapshots thus avoiding creating a new bitmap for
        // every snapshot.
        mMap.snapshot(callback, mSnapshot)
    }



}