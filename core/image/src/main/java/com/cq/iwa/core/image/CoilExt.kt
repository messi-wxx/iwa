package com.cq.iwa.core.image

import android.widget.ImageView
import coil.ImageLoader
import coil.load
import coil.transform.RoundedCornersTransformation

fun ImageView.loadUrl(
    url: String?,
    imageLoader: ImageLoader,
    placeholderRes: Int = R.drawable.ic_image_placeholder,
    errorRes: Int = R.drawable.ic_image_placeholder,
    cornerRadiusDp: Float = 0f,
) {
    load(url, imageLoader) {
        placeholder(placeholderRes)
        error(errorRes)
        if (cornerRadiusDp > 0f) {
            transformations(RoundedCornersTransformation(cornerRadiusDp))
        }
    }
}
