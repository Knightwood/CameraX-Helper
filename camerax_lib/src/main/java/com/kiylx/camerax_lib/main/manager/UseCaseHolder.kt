package com.kiylx.camerax_lib.main.manager

import android.util.Size
import android.view.Surface
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.video.Recorder
import com.kiylx.camerax_lib.main.manager.UseCaseHolder.resolutionSelector
import com.kiylx.camerax_lib.main.manager.UseCaseHolder.setInitImpl
import com.kiylx.camerax_lib.main.manager.model.ManagerConfig
import com.kiylx.camerax_lib.main.manager.video.OnceRecorderHelper
import java.util.concurrent.ExecutorService

interface IUseCaseHelper {

    fun initPreView(
        cameraExecutor: ExecutorService,
        screenAspectRatio: Int,
        rotation: Int = Surface.ROTATION_0,
        size: Size,
        cameraConfig: ManagerConfig,
    ): Preview

    fun initVideoCapture(
        cameraExecutor: ExecutorService,
        screenAspectRatio: Int,
        rotation: Int = Surface.ROTATION_0,
        size: Size,
        cameraConfig: ManagerConfig,
    ): androidx.camera.video.VideoCapture<Recorder>

    fun initImageAnalyzer(
        cameraExecutor: ExecutorService,
        screenAspectRatio: Int,
        rotation: Int = Surface.ROTATION_0,
        size: Size,
        cameraConfig: ManagerConfig,
    ): ImageAnalysis

    /** setTargetResolution(size)和setTargetAspectRatio(screenAspectRatio)不能同时使用 */
    fun initImageCapture(
        cameraExecutor: ExecutorService,
        screenAspectRatio: Int,
        rotation: Int = Surface.ROTATION_0,
        size: Size,
        cameraConfig: ManagerConfig,
    ): ImageCapture

//    /**
//     * 初始化自定义的用例组合
//     */
//    fun initCustomUseCaseList(
//        cameraExecutor: ExecutorService,
//        screenAspectRatio: Int,
//        rotation: Int = Surface.ROTATION_0,
//        size: Size,
//        cameraConfig: ManagerConfig,
//        selectAnalyzer: ImageAnalysis.Analyzer,
//    )

//    /**
//     * Provide custom use case list
//     */
//    fun provideCustomUseCaseList(): List<UseCase>
}

/**
 * 在相机初始化之前调用[setInitImpl]方法，以提供自己所需要的初始化
 * 还可以指定[resolutionSelector]，提供预览与拍照所需要的分辨率与纵横比筛选
 */
object UseCaseHolder : IUseCaseHelper {
    var caseHelper: IUseCaseHelper = this
        internal set

    /** 分辨率与纵横比筛选，用于相机预览和拍照 */
    var resolutionSelector = ResolutionSelector.Builder()
        //分辨率筛选
        .setResolutionFilter { supportedSizes, rotationDegrees ->
            supportedSizes
        }
        //纵横比选择策略 16:9 比例
        .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
        //分辨率策略选择最高可用分辨率
        .setResolutionStrategy(ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY)
        //设置允许的分辨率模式。
        .setAllowedResolutionMode(ResolutionSelector.PREFER_CAPTURE_RATE_OVER_HIGHER_RESOLUTION)
        .build()

    /**
     * 相机初始化前调用此方法以替换默认的usecase初始化
     * 或者相机初始化后调用此方法，再调用[CameraXManager.reBindUseCase]重新初始化相机
     */
    fun setInitImpl(caseHelper: IUseCaseHelper) {
        this.caseHelper = caseHelper
    }

    override fun initPreView(
        cameraExecutor: ExecutorService,
        screenAspectRatio: Int,
        rotation: Int,
        size: Size,
        cameraConfig: ManagerConfig
    ): Preview {
        val preview = Preview.Builder()
            // 我们要去宽高比，但是没有分辨率
            //.setTargetAspectRatio(screenAspectRatio)
            //.setTargetResolution(size)
            // 设置初始的旋转
            .setTargetRotation(rotation)
            .setResolutionSelector(resolutionSelector)
            .build()
        return preview
    }

    override fun initVideoCapture(
        cameraExecutor: ExecutorService,
        screenAspectRatio: Int,
        rotation: Int,
        size: Size,
        cameraConfig: ManagerConfig
    ): androidx.camera.video.VideoCapture<Recorder> {
        val videoCapture =
            OnceRecorderHelper.getVideoCapture(cameraExecutor, rotation, cameraConfig.recordConfig)
        return videoCapture
    }

    override fun initImageAnalyzer(
        cameraExecutor: ExecutorService,
        screenAspectRatio: Int,
        rotation: Int,
        size: Size,
        cameraConfig: ManagerConfig
    ): ImageAnalysis {
        val imageAnalyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setTargetRotation(rotation)//设置旋转角度
            .build()

        return imageAnalyzer
    }

    /** setTargetResolution(size)和setTargetAspectRatio(screenAspectRatio)不能同时使用 */
    override fun initImageCapture(
        cameraExecutor: ExecutorService,
        screenAspectRatio: Int,
        rotation: Int,
        size: Size,
        cameraConfig: ManagerConfig
    ): ImageCapture {
        val config = cameraConfig.imageCaptureConfig

        // ImageCapture，用于拍照功能
        val imageCapture = ImageCapture.Builder()
            .setCaptureMode(config.captureMode)
            //设置初始目标旋转，如果旋转改变，我们将不得不再次调用它在此用例的生命周期中
            .setTargetRotation(rotation)
            .setJpegQuality(config.jpegQuality)
//            .setTargetResolution(size)
            // 我们要求长宽比，但没有分辨率匹配预览配置，但让 CameraX优化为任何特定的解决方案，最适合我们的用例
//            .setTargetAspectRatio(screenAspectRatio)
            .setResolutionSelector(resolutionSelector)
            .build()
        return imageCapture
    }

//    override fun initCustomUseCaseList(
//        cameraExecutor: ExecutorService,
//        screenAspectRatio: Int,
//        rotation: Int,
//        size: Size,
//        cameraConfig: ManagerConfig,
//        selectAnalyzer: ImageAnalysis.Analyzer
//    ) {
//        // 自定义usecase组合
//    }

//    override fun provideCustomUseCaseList(): List<UseCase> {
//        //提供自定义usecase组合
//        return emptyList()
//    }
}