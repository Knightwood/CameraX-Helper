package com.kiylx.camerax_lib.main.manager

import android.annotation.SuppressLint
import android.util.Size
import android.view.Surface
import androidx.camera.core.*
import androidx.camera.video.Recorder
import com.kiylx.camerax_lib.main.manager.video.VideoRecorderHolder
import com.kiylx.camerax_lib.main.store.StorageConfig
import java.util.concurrent.ExecutorService

interface IUseCaseHelper {
    fun initPreView(rotation: Int = Surface.ROTATION_0): Preview

    @Deprecated("不再支持")
    @SuppressLint("RestrictedApi")
    fun initOldVideoCapture(
        screenAspectRatio: Int,
        rotation: Int = Surface.ROTATION_0
    ): VideoCapture

    fun initVideoCapture(
        cameraExecutor: ExecutorService,
    ): androidx.camera.video.VideoCapture<Recorder>

    fun initImageAnalyzer(rotation: Int = Surface.ROTATION_0): ImageAnalysis

    /**
     * setTargetResolution(size)和setTargetAspectRatio(screenAspectRatio)不能同时使用
     */
    fun initImageCapture(
        screenAspectRatio: Int,
        rotation: Int = Surface.ROTATION_0,
        size: Size,
    ): ImageCapture
}

/**
 * 在相机初始化之前调用[setInitImpl]方法，以提供自己所需要的初始化
 */
object UseCaseHolder : IUseCaseHelper {
    var caseHelper: IUseCaseHelper = this
        private set

    fun setInitImpl(caseHelper: IUseCaseHelper) {
        this.caseHelper = caseHelper
    }

    override fun initPreView(rotation: Int): Preview {
        val preview = Preview.Builder()
            // 我们要去宽高比，但是没有分辨率
            //.setTargetAspectRatio(screenAspectRatio)
            //.setTargetResolution(size)
            // 设置初始的旋转
            .setTargetRotation(rotation)
            .build()
        return preview
    }

    @Deprecated("不再支持")
    @SuppressLint("RestrictedApi")
    override fun initOldVideoCapture(
        screenAspectRatio: Int,
        rotation: Int
    ): VideoCapture {
        // 视频的还不是很成熟，不一定都能用
        val videoCapture = VideoCapture.Builder()//录像用例配置
            .setTargetAspectRatio(screenAspectRatio) //设置高宽比「我比较喜欢全屏」
            //视频帧率  越高视频体积越大
            .setVideoFrameRate(60)
            //bit率  越大视频体积越大
            .setBitRate(3 * 1024 * 1024)
            .setTargetRotation(rotation)//设置旋转角度
            //.setAudioRecordSource(MediaRecorder.AudioSource.MIC)//设置音频源麦克风
            .build()
        return videoCapture
    }

    override fun initVideoCapture(
        cameraExecutor: ExecutorService,
    ): androidx.camera.video.VideoCapture<Recorder> {
        val videoCapture =
            VideoRecorderHolder.getVideoCapture(cameraExecutor, StorageConfig.quality)
        return videoCapture
    }

    override fun initImageAnalyzer(rotation: Int): ImageAnalysis {
        val imageAnalyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setTargetRotation(rotation)//设置旋转角度
            .build()

        return imageAnalyzer
    }

    /**
     * setTargetResolution(size)和setTargetAspectRatio(screenAspectRatio)不能同时使用
     */
    override fun initImageCapture(
        screenAspectRatio: Int,
        rotation: Int,
        size: Size,
    ): ImageCapture {
        // ImageCapture，用于拍照功能
        val imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            //设置初始目标旋转，如果旋转改变，我们将不得不再次调用它在此用例的生命周期中
            .setTargetRotation(rotation)
            // 我们要求长宽比，但没有分辨率匹配预览配置，但让 CameraX优化为任何特定的解决方案，最适合我们的用例
            .setTargetAspectRatio(screenAspectRatio)
            //.setTargetResolution(size)
            .build()
        return imageCapture
    }
}