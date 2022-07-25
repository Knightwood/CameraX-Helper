package com.kiylx.camerax_lib.main.buttons

/**
 * 按钮用的接口
 */
interface CaptureListener {

    fun takePictures()

    fun recordStart()

    fun recordEnd(time: Long)

    fun recordZoom(zoom: Float)

    fun recordError(message:String)

}