package com.lanredroidsmith.imagegalleryapp.util

import android.support.v4.app.Fragment
import org.jetbrains.anko.toast

/**
 * Created by Lanre on 1/26/18.
 */

inline fun Fragment.toast(message: Int) = activity?.toast(message)