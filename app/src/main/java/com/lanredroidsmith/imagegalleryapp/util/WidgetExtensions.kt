package com.lanredroidsmith.imagegalleryapp.util

import android.graphics.RectF
import android.widget.ImageView

/**
 * Created by Lanre on 1/30/18.
 */

val ImageView.imageBounds: RectF
    get() {
        val bounds = RectF()
        if (drawable != null) {
            imageMatrix.mapRect(bounds, RectF(drawable.bounds))
        }
        return bounds
    }

fun RectF.roughlyEquals(r: RectF): Boolean {
    if (this === r) return true
    return left.round() == r.left.round()
            && top.round() == r.top.round()
            && right.round() == r.right.round()
            && bottom.round() == r.bottom.round()
}