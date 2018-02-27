package com.lanredroidsmith.imagegalleryapp.util

/**
 * Created by Lanre on 2/1/18.
 */

/**
 * Fast round from float to int. This is faster than Math.round()
 * thought it may return slightly different results. It does not try to
 * handle (in any meaningful way) NaN or infinities.
 *
 * Copied from com.android.internal.util.FastMath
 */
fun Float.round(): Int {
    val lx = (this * (65536 * 256f)).toLong()
    return (lx + 0x800000 shr 24).toInt()
}