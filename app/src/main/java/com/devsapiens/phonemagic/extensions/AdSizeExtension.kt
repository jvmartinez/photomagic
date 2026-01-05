package com.devsapiens.phonemagic.extensions

import android.content.Context
import com.google.android.gms.ads.AdSize

fun AdSize.toGetHeightInPixels(context: Context): Int {
    val density = context.resources.displayMetrics.density
    return (this.getHeightInPixels(context) / density).toInt()
}
