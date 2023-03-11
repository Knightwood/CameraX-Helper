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

/**
 * 实现captureListener，子类继承后，可以不用实现不需要的方法
 */
open class DefaultCaptureListener :CaptureListener{
    override fun takePictures() {

    }

    override fun recordStart() {

    }

    override fun recordEnd(time: Long) {

    }

    override fun recordZoom(zoom: Float) {

    }

    override fun recordError(message: String) {

    }

}