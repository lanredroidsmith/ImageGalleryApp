package com.lanredroidsmith.imagegalleryapp.ui.photo

import android.Manifest
import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.app.ActivityCompat
import android.support.v4.app.LoaderManager
import android.support.v4.app.SharedElementCallback
import android.support.v4.content.ContextCompat
import android.support.v4.content.CursorLoader
import android.support.v4.content.Loader
import android.support.v4.view.PagerAdapter
import android.support.v4.view.ViewCompat
import android.support.v4.view.ViewPager
import android.util.Log
import android.util.SparseArray
import android.view.*
import android.widget.Toast
import com.lanredroidsmith.imagegalleryapp.R
import com.lanredroidsmith.imagegalleryapp.data.local.model.Photo
import com.lanredroidsmith.imagegalleryapp.data.local.repo.ImageRepository
import com.lanredroidsmith.imagegalleryapp.ui.common.DepthPageTransformer
import kotlinx.android.synthetic.main.activity_single_photo.*
import kotlinx.android.synthetic.main.single_photo_toolbar.*
import org.jetbrains.anko.toast
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.github.chrisbanes.photoview.PhotoView
import com.lanredroidsmith.imagegalleryapp.ui.photo.PhotoAlbumActivity.Companion.EXTRA_CURRENT_POSITION
import com.lanredroidsmith.imagegalleryapp.ui.photo.PhotoAlbumActivity.Companion.EXTRA_CURRENT_TRANSITION_NAME
import com.lanredroidsmith.imagegalleryapp.ui.photo.PhotoAlbumActivity.Companion.EXTRA_ORIGINAL_POSITION
import com.lanredroidsmith.imagegalleryapp.util.getInt
import com.lanredroidsmith.imagegalleryapp.util.imageBounds
import com.lanredroidsmith.imagegalleryapp.util.roughlyEquals
import java.io.File


class SinglePhotoActivity : AppCompatActivity(), LoaderManager.LoaderCallbacks<Cursor> {

    private val _tag = SinglePhotoActivity::class.java.simpleName

    companion object {
        const val PHOTO_POSITION_KEY = "PHOTO_POSITION"
        const val ALBUM_ID_KEY = "ALBUM_ID"
        private const val SYSTEM_BARS_VISIBILITY_KEY = "BARS_KEY"
        private const val CURRENT_TRANSITION_NAME_KEY = "CURRENT_TRANSITION_NAME"
        private const val IS_FROM_IMPLICIT_INTENT_KEY = "IS_FROM_IMPLICIT_INTENT"
        private const val STORAGE_PERMISSION_REQUEST_CODE = 4
        private const val ALBUM_PHOTOS_LOADER_ID = 2
        private const val IMPLICIT_PHOTO_LOADER_ID = 3
        private const val DEFAULT_PHOTO_POSITION = -1
    }

    private lateinit var mPhotoPagerAdapter: PhotoSlidePagerAdapter
    private lateinit var mCursor: Cursor

    private var mIdColumnIndex: Int = 0
    private var mFilePathColumnIndex: Int = 0
    private var mDisplayNameColumnIndex: Int = 0
    private var mDateAddedColumnIndex: Int = 0
    private var mDateModifiedColumnIndex: Int = 0
    private var mSizeColumnIndex: Int = 0
    private var mHeightColumnIndex: Int = 0
    private var mWidthColumnIndex: Int = 0
    private var mCurrentPhotoPosition: Int = DEFAULT_PHOTO_POSITION
    private var mOriginalPosition: Int = 0
    private var mInitialBounds = RectF()
    private var mScaleChanged = false
    private var mSystemBarsShowing: Boolean = true
    private var mIsReturning: Boolean = false
    private var mCurrentTransitionName: String? = null
    private var mAlbumId: Int = 0
    private var mIsFromImplicitIntent: Boolean = false
    private var mImplicitData: Uri? = null

    private val mEnterElementCallback = object : SharedElementCallback() {
        override fun onMapSharedElements(names: MutableList<String>,
                                         sharedElements: MutableMap<String, View>) {
            if (mIsReturning) {
                val sharedElement = mPhotoPagerAdapter.getViewAt(mCurrentPhotoPosition)
                if (sharedElement == null) {
                    names.clear()
                    sharedElements.clear()
                } else if (mOriginalPosition != mCurrentPhotoPosition) {
                    names.clear()
                    names.add(ViewCompat.getTransitionName(sharedElement))

                    sharedElements.clear()
                    sharedElements.put(ViewCompat.getTransitionName(sharedElement), sharedElement)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor = ContextCompat.getColor(this, R.color.translucent_status_bar_dark)
        }
        setContentView(R.layout.activity_single_photo)
        // Get the intent data that started this activity if implicit
        mImplicitData = intent.data
        if (mImplicitData != null) { mIsFromImplicitIntent = true }

        if (!mIsFromImplicitIntent) {
            // We need to stop the loading of this Activity before Glide has finished loading the image
            supportPostponeEnterTransition()
            ActivityCompat.setEnterSharedElementCallback(this, mEnterElementCallback)
        }

        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        window.decorView.setOnSystemUiVisibilityChangeListener {
            visibility ->
            // Note that system bars will only be "visible" if none of the
            // LOW_PROFILE, HIDE_NAVIGATION, or FULLSCREEN flags are set.
            if (visibility and View.SYSTEM_UI_FLAG_FULLSCREEN == 0) {
                // The system bars are visible. Make any desired
                // adjustments to your UI, such as showing the action bar or
                // other navigational controls.
                mSystemBarsShowing = true
                supportActionBar!!.show()
            } else {
                // The system bars are NOT visible. Make any desired
                // adjustments to your UI, such as hiding the action bar or
                // other navigational controls.
                mSystemBarsShowing = false
                supportActionBar!!.hide()
            }
        }

        if (mIsFromImplicitIntent) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                checkWriteStoragePermission()
            } else {
                // for pre-Marshmallow, permission is already granted at install time, so we load
                loadPhoto()
            }
        } else {
            // no need to check for permission here it
            // would have been checked in MainActivity
            mOriginalPosition = intent.extras.getInt(PHOTO_POSITION_KEY)
            mAlbumId = intent.extras.getInt(ALBUM_ID_KEY)
            supportLoaderManager.initLoader(ALBUM_PHOTOS_LOADER_ID, null, this)
        }

        if (savedInstanceState != null) {
            mSystemBarsShowing = savedInstanceState.getBoolean(SYSTEM_BARS_VISIBILITY_KEY)
            mCurrentPhotoPosition = savedInstanceState.getInt(PHOTO_POSITION_KEY)
            mCurrentTransitionName = savedInstanceState.getString(CURRENT_TRANSITION_NAME_KEY)
            mIsFromImplicitIntent = savedInstanceState.getBoolean(IS_FROM_IMPLICIT_INTENT_KEY)
        } else if (!mIsFromImplicitIntent){
            mCurrentPhotoPosition = mOriginalPosition
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private fun checkWriteStoragePermission() {
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    STORAGE_PERMISSION_REQUEST_CODE)
            return
        }
        loadPhoto() // executed ONLY when permission is granted
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
                    loadPhoto()
                } else {
                    Toast.makeText(this, R.string.pls_enable_permission, Toast.LENGTH_LONG).show()
                    finish()
                }
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun loadPhoto() {
        supportLoaderManager.initLoader(IMPLICIT_PHOTO_LOADER_ID, null,
                object : LoaderManager.LoaderCallbacks<Cursor> {
                    override fun onCreateLoader(id: Int, args: Bundle?) =
                            ImageRepository().getImageByUri(this@SinglePhotoActivity,
                                    mImplicitData!!)

                    override fun onLoaderReset(loader: Loader<Cursor>?) {}

                    override fun onLoadFinished(loader: Loader<Cursor>?, data: Cursor?) {
                        if (data != null && data.moveToFirst()) {
                            mAlbumId = data.getInt(MediaStore.Images.Media.BUCKET_ID)
                            supportLoaderManager.initLoader(ALBUM_PHOTOS_LOADER_ID,
                                    null, this@SinglePhotoActivity)
                        } else toast(R.string.error_occurred)
                    }
                })
    }

    override fun onResume() {
        super.onResume()
        if (!mSystemBarsShowing) {
            hideSystemUI()
            supportActionBar?.hide()
        }
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.putInt(PHOTO_POSITION_KEY, mCurrentPhotoPosition)
        outState?.putString(CURRENT_TRANSITION_NAME_KEY, mCurrentTransitionName)
        outState?.putBoolean(SYSTEM_BARS_VISIBILITY_KEY, mSystemBarsShowing)
        outState?.putBoolean(IS_FROM_IMPLICIT_INTENT_KEY, mIsFromImplicitIntent)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            if (!mIsFromImplicitIntent) supportFinishAfterTransition() else finish()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun finishAfterTransition() {
        mIsReturning = true
        val data = Intent()
        data.putExtra(EXTRA_ORIGINAL_POSITION, mOriginalPosition)
        data.putExtra(EXTRA_CURRENT_POSITION, mCurrentPhotoPosition)
        data.putExtra(EXTRA_CURRENT_TRANSITION_NAME, mCurrentTransitionName)
        setResult(Activity.RESULT_OK, data)
        super.finishAfterTransition()
    }

    private fun getColumnIndices() {
        mIdColumnIndex = mCursor.getColumnIndex(MediaStore.Images.Media._ID)
        mFilePathColumnIndex = mCursor.getColumnIndex(MediaStore.Images.Media.DATA)
        mDisplayNameColumnIndex = mCursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
        mDateAddedColumnIndex = mCursor.getColumnIndex(MediaStore.Images.Media.DATE_ADDED)
        mDateModifiedColumnIndex = mCursor.getColumnIndex(MediaStore.Images.Media.DATE_MODIFIED)
        mSizeColumnIndex = mCursor.getColumnIndex(MediaStore.Images.Media.SIZE)
        mHeightColumnIndex = mCursor.getColumnIndex(MediaStore.Images.Media.HEIGHT)
        mWidthColumnIndex = mCursor.getColumnIndex(MediaStore.Images.Media.WIDTH)
    }

    private fun getCurrentPosition(): Int {
        var position = 0
        while (position < mCursor.count) {
            if (mCursor.moveToPosition(position) &&
                    mCursor.getInt(mIdColumnIndex) == intent.data.lastPathSegment.toInt())
                break
            else ++position
        }
        return position
    }

    override fun onLoadFinished(loader: Loader<Cursor>?, data: Cursor?) {
        // note that we set up the PagerAdapter here cos
        // a re-query may happen on the CursorLoader
        if (data != null) {
            mCursor = data
            getColumnIndices()
            if (mCurrentPhotoPosition == DEFAULT_PHOTO_POSITION) {
                // it is yet to be set, most probably cos we're just coming from an implicit intent
                mCurrentPhotoPosition = getCurrentPosition()
            }
            mPhotoPagerAdapter = PhotoSlidePagerAdapter(this)
            pager.adapter = mPhotoPagerAdapter
            val pageChangeListener = object : ViewPager.SimpleOnPageChangeListener() {
                override fun onPageSelected(position: Int) {
                    mCurrentPhotoPosition = position
                    if (mCursor.moveToPosition(position)) {
                        // update the app bar's title
                        mCurrentTransitionName = mCursor.getString(mDisplayNameColumnIndex)
                        toolbar.title = mCurrentTransitionName
                    }
                }
            }
            pager.addOnPageChangeListener(pageChangeListener)
            pager.setPageTransformer(true, DepthPageTransformer())
            pager.currentItem = mCurrentPhotoPosition
            // to be sure that the Activity's title is set correctly when just coming,
            // we do this cos ViewPager does not call onPageSelected for item 0
            if (mCurrentPhotoPosition == 0) {
                // note that we use 'post' to be sure that the ViewPager/Adapter
                // is well initialized before we run the enclosed code
                pager.post { pageChangeListener.onPageSelected(0) }
            }
        } else { toast(R.string.error_occurred) } // highly unlikely though
    }

    override fun onLoaderReset(loader: Loader<Cursor>?) {}

    override fun onCreateLoader(id: Int, args: Bundle?): CursorLoader? =
        ImageRepository().getImagesInAlbum(this, mAlbumId)

    private fun hideSystemUI() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE)
    }

    private fun showSystemUI() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
    }

    // this is used to prevent the ViewPager from swiping - like WhatsApp
    fun onImageZoomed(isImageLarger: Boolean) {
        pager.mIsLocked = isImageLarger
    }


    private inner class PhotoSlidePagerAdapter(private var mContext: Context) : PagerAdapter() {

        private val mViewsArray = SparseArray<View?>(mCursor.count)

        override fun isViewFromObject(view: View, `object`: Any) = view === `object`

        override fun getCount() = mCursor.count

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val photo = if (mCursor.moveToPosition(position)) {
                val id = mCursor.getLong(mIdColumnIndex)
                val filePath = mCursor.getString(mFilePathColumnIndex)
                val displayName = mCursor.getString(mDisplayNameColumnIndex)
                val dateAdded = mCursor.getLong(mDateAddedColumnIndex)
                val dateModified = mCursor.getLong(mDateModifiedColumnIndex)
                val size = mCursor.getLong(mSizeColumnIndex)
                val height = mCursor.getInt(mHeightColumnIndex)
                val width = mCursor.getInt(mWidthColumnIndex)

                Photo(id, filePath, displayName, dateAdded, dateModified, size, height, width)
            } else null

            val photoView = LayoutInflater.from(mContext).inflate(R.layout.sliding_photo,
                    container, false) as PhotoView

            if (photo != null) {
                ViewCompat.setTransitionName(photoView, photo.displayName)
                // don't bother to add if we came from implicit intent
                // after all, there's no return transition to be done
                if (!mIsFromImplicitIntent) { mViewsArray.put(position, photoView) }
                try {
                    Glide.with(mContext)
                            .load(File(photo.filePath))
                            .listener(object : RequestListener<Drawable> {
                                override fun onResourceReady(resource: Drawable?,
                                                             model: Any?,
                                                             target: Target<Drawable>?,
                                                             dataSource: DataSource?,
                                                             isFirstResource: Boolean): Boolean {
                                    photoView.viewTreeObserver.addOnPreDrawListener(
                                            object : ViewTreeObserver.OnPreDrawListener {
                                        override fun onPreDraw(): Boolean {
                                            photoView.viewTreeObserver.removeOnPreDrawListener(this)
                                            supportStartPostponedEnterTransition()
                                            return true
                                        }
                                    })
                                    photoView.post { mInitialBounds = photoView.imageBounds }
                                    return false
                                }

                                override fun onLoadFailed(e: GlideException?,
                                                          model: Any?,
                                                          target: Target<Drawable>?,
                                                          isFirstResource: Boolean): Boolean {
                                    supportStartPostponedEnterTransition()
                                    toast(R.string.error_occurred)
                                    return false
                                }
                            })
                            .into(photoView)
                } catch (e: Exception) {
                    toast(R.string.error_occurred)
                    Log.e(_tag, e.message)
                }

                photoView.setOnViewTapListener {
                    _, _, _ ->
                    val visible = window.decorView.systemUiVisibility and
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION == 0
                    if (visible) {
                        hideSystemUI()
                    } else {
                        showSystemUI()
                    }
                }

                // OnMatrixChangeListener gives us exact RectFs unlike OnScaleChangeListener
                // but the former can get triggered even upon no zoom e.g. when user tries
                // to swipe and the ViewPager is already at the end. The latter however
                // is not called unless there's a (true) zoom, so we depend on it to
                // know when the former should act in order to show system bars.
                photoView.postDelayed(fun (){
                    photoView.setOnMatrixChangeListener {
                        rectF ->
                        if (mScaleChanged) {
                            if (mInitialBounds.roughlyEquals(rectF)) {
                                onImageZoomed(false)
                                // we're back to the initial (no-zoom) state
                                showSystemUI()
                            } else {
                                onImageZoomed(true)
                                // hide system ui as user zooms in/out as long
                                // as image has not returned to original size
                                if (mSystemBarsShowing) { hideSystemUI() }
                            }
                            mScaleChanged = false // return it to its default
                        }
                    }
                }, 500L)

                photoView.postDelayed(fun (){
                    photoView.setOnScaleChangeListener { _, _, _ -> mScaleChanged = true }
                }, 500L)
            } else toast(R.string.photo_not_found)

            container.addView(photoView)
            return photoView
        }

        override fun destroyItem(collection: ViewGroup, position: Int, view: Any) {
            if (!mIsFromImplicitIntent) { mViewsArray.removeAt(position) }
            collection.removeView(view as View)
        }

        fun getViewAt(position: Int): View? = mViewsArray.get(position)
    }
}