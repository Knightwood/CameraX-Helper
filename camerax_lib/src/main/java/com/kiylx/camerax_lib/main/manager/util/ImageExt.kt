package com.kiylx.camerax_lib.main.manager.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.Image
import java.nio.ByteBuffer

fun Image.imageToBuffer(): ByteBuffer? {
    val buffer: ByteBuffer = this.planes[0].buffer
    val result= cloneByteBuffer(buffer)
    return result
}

fun ByteBuffer.toBitmap():Bitmap?{
    val bytes = ByteArray(remaining())
    get(bytes)
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, null)
}

fun Image.imageToBitmap(): Bitmap? {
    val buffer: ByteBuffer = this.planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, null)
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