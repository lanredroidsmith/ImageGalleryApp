package com.lanredroidsmith.imagegalleryapp.ui.common

import android.content.Context
import android.graphics.Rect
import android.support.v7.widget.RecyclerView
import android.view.View

/**
 * Created by Lanre on 1/6/18.
 */

class GridAutoFitMarginDecoration(context: Context, margin: Int) : RecyclerView.ItemDecoration() {

    private val mMargin = context.resources.getDimensionPixelSize(margin)

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView,
                                state: RecyclerView.State?) {
        outRect.set(mMargin, mMargin, mMargin, mMargin)
    }
}