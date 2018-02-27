package com.lanredroidsmith.imagegalleryapp.util

import android.util.SparseArray
import java.util.*

/**
 * Created by Lanre on 1/23/18.
 */

inline fun <E> SparseArray<E>.forEach(action: (Int, E) -> Unit) {
    val size = size()
    for (i in 0 until size) {
        if (size != size()) throw ConcurrentModificationException()
        action(keyAt(i), valueAt(i))
    }
}

val <E> SparseArray<E>.values: MutableCollection<E>
    get() {
        val size = size()
        val values = ArrayList<E>()
        for (i in 0 until size) {
            if (size != size()) throw ConcurrentModificationException()
            values.add(valueAt(i))
        }
        return values
    }