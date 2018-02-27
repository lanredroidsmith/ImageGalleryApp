package com.lanredroidsmith.imagegalleryapp

import android.Manifest
import android.annotation.TargetApi
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.app.LoaderManager
import android.support.v4.content.Loader
import android.support.v7.widget.Toolbar
import android.util.SparseArray
import android.widget.Toast
import com.lanredroidsmith.imagegalleryapp.data.local.model.Album
import com.lanredroidsmith.imagegalleryapp.data.local.repo.ImageRepository
import com.lanredroidsmith.imagegalleryapp.ui.photo.PhotoAlbumActivity
import com.lanredroidsmith.imagegalleryapp.ui.common.GridAutoFitLayoutManager
import com.lanredroidsmith.imagegalleryapp.ui.common.GridAutoFitMarginDecoration
import com.lanredroidsmith.imagegalleryapp.ui.photo.adapter.AlbumAdapter
import com.lanredroidsmith.imagegalleryapp.util.values
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity(),
        LoaderManager.LoaderCallbacks<Cursor>,
        AlbumAdapter.OnAlbumSelectedListener {

    lateinit private var mAlbumAdapter: AlbumAdapter
    private var mIsRecreated = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar as Toolbar)

        if (savedInstanceState != null) {
            mIsRecreated = true
        }

        mAlbumAdapter = AlbumAdapter(this, null, this)
        albumsList.adapter = mAlbumAdapter
        albumsList.addItemDecoration(GridAutoFitMarginDecoration(this,
                R.dimen.grid_auto_fit_margin))
        albumsList.setHasFixedSize(true)
        albumsList.layoutManager = GridAutoFitLayoutManager(this,
                resources.getDimension(R.dimen.album_grid_item_width).toInt())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkWriteStoragePermission()
        } else {
            // for pre-Marshmallow, permission is already granted at install time, so we load
            loadAlbums()
        }
    }

    companion object {
        private const val STORAGE_PERMISSION_REQUEST_CODE = 4
        private const val ALL_IMAGES_LOADER_ID = 1
    }

    @TargetApi(Build.VERSION_CODES.M)
    private fun checkWriteStoragePermission() {
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    STORAGE_PERMISSION_REQUEST_CODE)
            return
        }
        loadAlbums() // executed ONLY when permission is granted
    }

    @TargetApi(Build.VERSION_CODES.M)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        when (requestCode) {
            STORAGE_PERMISSION_REQUEST_CODE -> {
                /*
                *  doc says: It is possible that the permissions request interaction with the user
                *  is interrupted. In this case you will receive empty permissions and results
                *  arrays which should be treated as a cancellation.
                *
                *  So, we make the request again.
                * */
                if (grantResults.isEmpty() || permissions.isEmpty()) {
                    requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                            STORAGE_PERMISSION_REQUEST_CODE)
                    return
                }
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    loadAlbums()
                } else {
                    Toast.makeText(this, R.string.pls_enable_permission, Toast.LENGTH_LONG).show()
                    finish()
                }
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun loadAlbums() {
        supportLoaderManager.initLoader(ALL_IMAGES_LOADER_ID, null, this)
    }

    override fun onLoadFinished(loader: Loader<Cursor>?, data: Cursor?) {
        if (data != null) {
            if (data.count == 0) {
                if (!mIsRecreated)
                    Toast.makeText(this, R.string.no_images_found, Toast.LENGTH_SHORT).show()
            } else processImages(data)
        } else {
            Toast.makeText(this, R.string.error_occurred, Toast.LENGTH_SHORT).show()
        }
    }

    private fun processImages(cursor: Cursor) {
        // we init the array so array won't have to be resizing upon expansion - #PerMatters
        val albumsMap = SparseArray<Album>(cursor.count)
        val bucketIdColumnIndex = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_ID)
        val bucketNameColumnIndex = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
        val dataColumnIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA)

        // moveToFirst() is necessary cos of restart due to config change e.g. rotation
        cursor.moveToFirst()
        var bucketId = cursor.getInt(bucketIdColumnIndex)
        var bucketName = cursor.getString(bucketNameColumnIndex)
        var pathToFile = cursor.getString(dataColumnIndex)
        albumsMap.put(bucketId, Album(bucketName, bucketId, 1, pathToFile))

        while (cursor.moveToNext()) {
            bucketId = cursor.getInt(bucketIdColumnIndex)
            bucketName = cursor.getString(bucketNameColumnIndex)
            pathToFile = cursor.getString(dataColumnIndex)

            val album = albumsMap[bucketId]
            if (null != album) {
                album.mediaCount++
            } else {
                // add new album
                albumsMap.put(bucketId, Album(bucketName, bucketId, 1, pathToFile))
            }
        }

        mAlbumAdapter.swapList(ArrayList(albumsMap.values))
    }

    override fun onCreateLoader(id: Int, args: Bundle?) =
        ImageRepository().getAllImages(this)

    override fun onLoaderReset(loader: Loader<Cursor>?) {}

    override fun onAlbumSelected(album: Album) {
        val intent = Intent(this, PhotoAlbumActivity::class.java)
        intent.putExtra(PhotoAlbumActivity.ALBUM_ITEM_KEY, album)
        startActivity(intent)
    }
}