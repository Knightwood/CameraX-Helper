package com.kiylx.camerax_lib.main.buttons

/**
 * 按钮用的接口
 */
interface CaptureListener {

    fun takePictures()

    /**
     * 回调此方法以通知可以开始录制
     */
    fun recordStart()

    /**
     * 到达设定的录制时长时，将回调此方法通知可以结束录制了
     */
    fun recordShouldEnd(time: Long)

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

    override fun recordShouldEnd(time: Long) {

    }

    override fun recordZoom(zoom: Float) {

    }

    override fun recordError(message: String) {

    }

}