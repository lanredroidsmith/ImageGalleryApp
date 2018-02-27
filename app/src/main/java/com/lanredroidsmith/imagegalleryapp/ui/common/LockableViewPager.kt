package com.lanredroidsmith.imagegalleryapp.ui.common

import android.content.Context
import android.support.v4.view.ViewPager
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.MotionEvent

/**
 * Created by Lanre on 1/27/18.
 */

class LockableViewPager : ViewPager {

    var mIsLocked: Boolean = false

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        return try {
                !mIsLocked && super.onInterceptTouchEvent(ev)
        } catch (e: IllegalArgumentException) {
            //uncomment if you really want to see these errors
            //e.printStackTrace();
            false
        }
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        return !mIsLocked && super.onTouchEvent(ev)
    }

    // prevent page swiping when using the keyboard left/right keys
    override fun executeKeyEvent(event: KeyEvent): Boolean {
        return if (!mIsLocked) super.executeKeyEvent(event) else false
    }
}