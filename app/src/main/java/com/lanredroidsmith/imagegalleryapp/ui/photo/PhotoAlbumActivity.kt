package com.lanredroidsmith.imagegalleryapp.ui.photo

import android.app.ActivityOptions
import android.content.Intent
import android.database.Cursor
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.app.LoaderManager
import android.support.v4.app.SharedElementCallback
import android.support.v4.content.Loader
import android.support.v4.view.ViewCompat
import android.support.v7.widget.Toolbar
import android.view.MenuItem
import android.view.View
import android.view.ViewTreeObserver
import android.widget.ImageView
import com.lanredroidsmith.imagegalleryapp.R
import com.lanredroidsmith.imagegalleryapp.data.local.model.Album
import com.lanredroidsmith.imagegalleryapp.data.local.repo.ImageRepository
import com.lanredroidsmith.imagegalleryapp.ui.common.GridAutoFitLayoutManager
import com.lanredroidsmith.imagegalleryapp.ui.common.GridAutoFitMarginDecoration
import com.lanredroidsmith.imagegalleryapp.ui.photo.adapter.PhotoAdapter
import kotlinx.android.synthetic.main.activity_main.*

class PhotoAlbumActivity : AppCompatActivity(),
        LoaderManager.LoaderCallbacks<Cursor>,
        PhotoAdapter.OnPhotoSelectedListener {

    lateinit private var mPhotoAdapter: PhotoAdapter
    private var mCursor: Cursor? = null
    private lateinit var mAlbum: Album

    companion object {
        const val ALBUM_ITEM_KEY = "ALBUM_ITEM"
        const val EXTRA_CURRENT_POSITION = "CURRENT_POSITION"
        const val EXTRA_ORIGINAL_POSITION = "ORIGINAL_POSITION"
        const val EXTRA_CURRENT_TRANSITION_NAME = "CURRENT_TRANS_NAME"
        const val ALBUM_PHOTOS_LOADER_ID = 2
    }

    private var mReEnterState: Bundle? = null

    private val mExitElementCallback = object : SharedElementCallback() {
        override fun onMapSharedElements(names: MutableList<String>, sharedElements: MutableMap<String, View>) {
            if (mReEnterState != null) {
                val originalPosition = mReEnterState!!.getInt(EXTRA_ORIGINAL_POSITION)
                val currentPosition = mReEnterState!!.getInt(EXTRA_CURRENT_POSITION)
                val newTransitionName = mReEnterState!!.getString(EXTRA_CURRENT_TRANSITION_NAME)
                if (originalPosition != currentPosition) {
                    // Current element has changed, need to override previous exit transitions
                    val newSharedElement = albumsList
                            .findViewWithTag<View>(newTransitionName)
                            .findViewById<ImageView>(R.id.photo)
                    if (newSharedElement != null) {
                        names.clear()
                        names.add(newTransitionName)

                        sharedElements.clear()
                        sharedElements.put(newTransitionName, newSharedElement)
                    }
                }
                mReEnterState = null
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar as Toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        mAlbum = intent.extras.getParcelable(ALBUM_ITEM_KEY)
        supportActionBar!!.title = mAlbum.name

        ActivityCompat.setExitSharedElementCallback(this, mExitElementCallback)

        mPhotoAdapter = PhotoAdapter(this, this)
        albumsList.adapter = mPhotoAdapter
        albumsList.addItemDecoration(GridAutoFitMarginDecoration(this,
                R.dimen.grid_auto_fit_margin))
        albumsList.setHasFixedSize(true)
        albumsList.layoutManager = GridAutoFitLayoutManager(this,
                resources.getDimension(R.dimen.photo_grid_item_width).toInt())

        supportLoaderManager.initLoader(ALBUM_PHOTOS_LOADER_ID, intent.extras, this)
    }

    override fun onResume() {
        super.onResume()
        // restore the status bar so that we have a totally smooth transition
        // particularly if status bar was hidden in the ViewPager Activity
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_VISIBLE
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onLoadFinished(loader: Loader<Cursor>?, data: Cursor?) {
        data?.let {
            mPhotoAdapter.swapCursor(data)
            mCursor = data
        }
    }

    override fun onLoaderReset(loader: Loader<Cursor>?) {
        mPhotoAdapter.swapCursor(null)
        mCursor = null
    }

    override fun onCreateLoader(id: Int, args: Bundle?) =
        ImageRepository().getImagesInAlbum(this, mAlbum.id)

    override fun onPhotoSelected(photoView: View, position: Int) {
        val intent = Intent(this, SinglePhotoActivity::class.java)
        intent.putExtra(SinglePhotoActivity.PHOTO_POSITION_KEY, position)
        intent.putExtra(SinglePhotoActivity.ALBUM_ID_KEY,
                this.intent.extras.getParcelable<Album>(ALBUM_ITEM_KEY).id)
        var options: Bundle? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            options = ActivityOptions.makeSceneTransitionAnimation(this, photoView,
                    ViewCompat.getTransitionName(photoView)).toBundle()
        }
        ActivityCompat.startActivity(this, intent, options)
    }

    override fun onActivityReenter(resultCode: Int, data: Intent) {
        super.onActivityReenter(resultCode, data)
        mReEnterState = data.extras
        mReEnterState?.let {
            val originalPosition = it.getInt(EXTRA_ORIGINAL_POSITION)
            val currentPosition = it.getInt(EXTRA_CURRENT_POSITION)
            // we need to also check that the current position is valid
            if (originalPosition != currentPosition &&
                    mCursor?.moveToPosition(currentPosition) == true) {
                albumsList.scrollToPosition(currentPosition)
            }
            supportPostponeEnterTransition()

            albumsList.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    albumsList.viewTreeObserver.removeOnPreDrawListener(this)
                    supportStartPostponedEnterTransition()
                    return true
                }
            })
        }
    }
}