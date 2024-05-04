package com.kiylx.camera.camerax_analyzer.bitmap

import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.RestrictTo
import androidx.camera.core.ImageProxy
import com.kiylx.camerax_lib.main.manager.util.saveToGallery
import java.nio.ByteBuffer

/**
 * 从ImageProxy中获取bitmap，存储到本地
 * 测试用。
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@SuppressLint("UnsafeOptInUsageError")
fun savePhoto(imageProxy: ImageProxy, context: Context) {
    val bitmap = BitmapUtils.getBitmap(imageProxy)
    bitmap?.saveToGallery(context)
}

/**
 * 复制byteBuffer
 */
fun cloneByteBuffer(original: ByteBuffer): ByteBuffer? {
    // Create clone with same capacity as original.
    val clone =
        if (original.isDirect) ByteBuffer.allocateDirect(original.capacity()) else ByteBuffer.allocate(
            original.capacity())
    // Create a read-only copy of the original.
    // This allows reading from the original without modifying it.
    val readOnlyCopy = original.asReadOnlyBuffer()
    // Flip and read from the original.
    readOnlyCopy.flip()

    /*clone.put(readOnlyCopy)
    clone.position(original.position());
    clone.limit(original.limit());
    clone.order(original.order());*/
    return clone
}