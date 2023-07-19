package com.kiylx.camerax_lib.main.manager.model

import android.graphics.Bitmap
import androidx.camera.view.PreviewView

/**
 * fragment实现这些接口，其实是把这些功能委托给CameraHolder去做
 */
interface ICameraXF {
    fun setCaptureResultListener(captureListener: CaptureResultListener)

    fun canSwitchCamera(): Boolean
    fun switchCamera()
    fun setFlashMode(mode: Int)
    fun getCurrentStatus(): Int
    fun stopTakeVideo(time: Long = 0)
    fun takeVideo()
    fun takePhoto()
    fun getCameraPreview(): PreviewView
    fun provideBitmap(): Bitmap?
    fun zoom(delta: Float)
    fun zoom2(zoomValue: Float)

    /**
     * @param true:打开手电筒。 false：关闭手电筒
     */
    fun openFlash(open: Boolean)

    /**
     * 设置相机绑定何种实例
     * @param mode 默认： [CaptureMode.takePhoto]
     */
    fun setCameraUseCase(mode: Int = CaptureMode.takePhoto)
    fun refreshBinding()
}