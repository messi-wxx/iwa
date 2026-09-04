package com.cq.iwa.core.dialog

import android.app.Activity
import android.content.Context
import android.widget.ImageView
import coil.load
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.core.ImageViewerPopupView
import com.lxj.xpopup.interfaces.OnSrcViewUpdateListener
import com.lxj.xpopup.interfaces.XPopupImageLoader
import java.io.File

fun Activity.showImageViewer(
    srcView: ImageView,
    position: Int,
    images: List<Any>,
    srcViewAt: (Int) -> ImageView?,
) {
    if (images.isEmpty()) return
    XPopup.Builder(this)
        .asImageViewer(
            srcView,
            position.coerceIn(0, images.lastIndex),
            images,
            OnSrcViewUpdateListener { popupView: ImageViewerPopupView, pos: Int ->
                srcViewAt(pos)?.let { popupView.updateSrcView(it) }
            },
            CoilPopupImageLoader(),
        )
        .isShowSaveButton(false)
        .show()
}

private class CoilPopupImageLoader : XPopupImageLoader {
    override fun loadImage(position: Int, uri: Any, imageView: ImageView) {
        imageView.load(uri) {
            crossfade(true)
            placeholder(com.cq.iwa.core.image.R.drawable.ic_image_placeholder)
            error(com.cq.iwa.core.image.R.drawable.ic_image_placeholder)
        }
    }

    override fun getImageFile(context: Context, uri: Any): File? {
        return (uri as? File)?.takeIf { it.exists() }
    }
}
