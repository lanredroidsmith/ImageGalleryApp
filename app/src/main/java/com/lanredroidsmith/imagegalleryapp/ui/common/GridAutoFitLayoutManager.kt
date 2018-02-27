package com.lanredroidsmith.imagegalleryapp.ui.common

import android.content.Context
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.TypedValue


/**
 * Created by Lanre on 1/4/18.
 */

class GridAutoFitLayoutManager : GridLayoutManager {
    private var mColumnWidth: Int = 0
    private var mColumnWidthChanged = true

    constructor(context: Context, columnWidth: Int) : super(context, 1) {
        setColumnWidth(checkColumnWidth(context, columnWidth))
    }

    constructor(
            context: Context,
            columnWidth: Int,
            orientation: Int,
            reverseLayout: Boolean
    ) : super(context, 1, orientation, reverseLayout) {
        /* Initially set spanCount to 1, will be changed automatically later. */
        setColumnWidth(checkColumnWidth(context, columnWidth))
    }

    private fun checkColumnWidth(context: Context, columnWidth: Int): Int {
        var columnWidth = columnWidth
        if (columnWidth <= 0) {
            // Set default columnWidth value (48dp here).
            columnWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48f,
                    context.resources.displayMetrics).toInt()
        }
        return columnWidth
    }

    private fun setColumnWidth(newColumnWidth: Int) {
        if (newColumnWidth > 0 && newColumnWidth != mColumnWidth) {
            mColumnWidth = newColumnWidth
            mColumnWidthChanged = true
        }
    }

    override fun onLayoutChildren(recycler: RecyclerView.Recycler?, state: RecyclerView.State) {
        if (mColumnWidthChanged && mColumnWidth > 0) {
            val totalSpace =
                    if (orientation == VERTICAL) width - paddingRight - paddingLeft
                    else height - paddingTop - paddingBottom
            spanCount = Math.max(1, totalSpace / mColumnWidth)
            mColumnWidthChanged = false
        }
        super.onLayoutChildren(recycler, state)
    }
}