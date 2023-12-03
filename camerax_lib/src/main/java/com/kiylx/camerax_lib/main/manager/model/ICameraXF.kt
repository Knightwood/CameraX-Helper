package com.kiylx.camerax_lib.main.manager.model

import android.graphics.Bitmap
import android.location.Location
import androidx.camera.core.ImageCapture
import androidx.camera.view.PreviewView
import com.kiylx.camerax_lib.main.manager.CameraHolder
import com.kiylx.camerax_lib.main.store.ImageCaptureConfig
import com.kiylx.camerax_lib.main.store.VideoRecordConfig

/**
 * fragment实现这些接口，其实是把这些功能委托给CameraHolder去做
 * 可以方便相机功能调用，若需要复杂功能，可以直接使用cameraHolder
 */
interface ICameraXF {
    fun setCaptureResultListener(captureListener: CaptureResultListener)

    fun canSwitchCamera(): Boolean
    fun switchCamera()
    fun indicateTakePhoto(durationTime: Long = 20)
    fun setFlashMode(mode: Int)
    fun getCurrentStatus(): Int

    //<editor-fold desc="视频录制">
    fun stopRecord(time: Long = 0)

    /**
     * @param recordConfig 录制视频的配置，传入配置将覆盖默认配置
     */
    fun startRecord(recordConfig: VideoRecordConfig? = null)

    fun pauseRecord()
    fun resumeRecord()
    fun recordMute(mute: Boolean)
    //</editor-fold>
    /**
     * 拍照，并保存为文件
     * @param imageCaptureConfig 拍照的配置，传入配置将覆盖默认配置
     */
    fun takePhoto(imageCaptureConfig: ImageCaptureConfig?=null)

    /**
     * 拍照，但不保存为文件，自行处理图像数据
     */
    fun takePhotoInMem(callback: ImageCapture.OnImageCapturedCallback)

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
    fun reBindUseCase()
    fun cameraHolder(): CameraHolder

    fun reSetConfig(func: ManagerConfig.() -> Unit)
}