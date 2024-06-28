package com.kiylx.camerax_lib.main.manager

import android.annotation.SuppressLint
import android.net.Uri
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.MirrorMode
import androidx.camera.video.Recording
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import com.kiylx.camerax_lib.main.manager.analyer.base.AnalyzerProvider
import com.kiylx.camerax_lib.main.manager.model.CameraManagerEventListener
import com.kiylx.camerax_lib.main.manager.model.CaptureResultListener
import com.kiylx.camerax_lib.main.manager.model.ManagerConfig
import com.kiylx.camerax_lib.main.manager.model.UseCaseHexStatus
import com.kiylx.camerax_lib.main.manager.model.UseCaseMode
import com.kiylx.camerax_lib.main.manager.photo.ImageCaptureHelper
import com.kiylx.camerax_lib.main.manager.video.OnceRecorder
import com.kiylx.camerax_lib.main.manager.video.OnceRecorderHelper
import com.kiylx.camerax_lib.main.store.ImageCaptureConfig
import com.kiylx.camerax_lib.main.store.VideoRecordConfig
import com.permissionx.guolindev.PermissionX
import com.permissionx.guolindev.callback.ExplainReasonCallbackWithBeforeParam
import com.permissionx.guolindev.callback.ForwardToSettingsCallback
import com.permissionx.guolindev.request.ExplainScope
import com.permissionx.guolindev.request.ForwardScope
import java.util.concurrent.Executors

/**
 * 实现拍照，录制等功能
 */
class CameraHolder(
    cameraPreview: PreviewView,
    cameraConfig: ManagerConfig,
    cameraManagerListener: CameraManagerEventListener,
    var captureResultListener: CaptureResultListener? = null,
) : CameraXManager(
    cameraPreview, cameraConfig, cameraManagerListener
) {
    private var analyzer: ImageAnalysis.Analyzer = AnalyzerProvider.emptyAnalyzer

    fun changeAnalyzer(analyzer: ImageAnalysis.Analyzer) {
        this.analyzer = analyzer
    }

    override fun selectAnalyzer(): ImageAnalysis.Analyzer = analyzer

    /**
     * 检查权限，通过后执行block块初始化相机
     */
    override fun checkPerm(block: () -> Unit) {
        PermissionX.init(context).permissions(ManagerUtil.REQUIRED_PERMISSIONS.asList())
            .explainReasonBeforeRequest()//在第一次请求权限之前就先弹出一个对话框向用户解释自己需要哪些权限，然后才会进行权限申请。
            .onExplainRequestReason(object : ExplainReasonCallbackWithBeforeParam {
                //onExplainRequestReason() 方法可以用于监听那些被用户拒绝，而又可以再次去申请的权限。
                //从方法名上也可以看出来了，应该在这个方法中解释申请这些权限的原因。
                override fun onExplainReason(
                    scope: ExplainScope,
                    deniedList: MutableList<String>,
                    beforeRequest: Boolean,
                ) {
                    scope.showRequestReasonDialog(
                        deniedList,
                        "即将重新申请的权限是程序必须依赖的权限",
                        "我已明白",
                        "取消"
                    )
                }
            })
            .onForwardToSettings(object : ForwardToSettingsCallback {
                //onForwardToSettings() 方法，专门用于监听那些被用户永久拒绝的权限。
                //另外从方法名上就可以看出，我们可以在这里提醒用户手动去应用程序设置当中打开权限。
                override fun onForwardToSettings(
                    scope: ForwardScope,
                    deniedList: MutableList<String>,
                ) {
                    scope.showForwardToSettingsDialog(
                        deniedList,
                        "您需要去应用程序设置当中手动开启权限",
                        "我已明白",
                        "取消"
                    )
                }
            })
            .request { allGranted, grantedList, deniedList ->
                if (allGranted) {
                    block()
                } else {
                    throw Exception("缺失权限")
                }
            }
    }

    override fun reBindUseCase() {
        stopRecord()
        super.reBindUseCase()
    }

    /**
     * 可以启用或停用手电筒（手电筒应用） 启用手电筒后，无论闪光灯模式设置如何，手电筒在拍照和拍视频时都会保持开启状态。
     * 仅当手电筒被停用时，ImageCapture 中的 flashMode 才会起作用。
     */
    fun useTorch(b: Boolean) {
        camera?.run {
            val flashCanUse = cameraInfo.hasFlashUnit()
            if (flashCanUse) {
                cameraControl.enableTorch(b)
            }
        }
    }

    /**
     * 拍照处理方法(这里只是拍照，录制视频另外有方法) 图像分析和拍照都绑定了拍照用例，所以，拍照后不需要重新绑定图像分析或拍照 拍照前会检查用例绑定
     */
    fun takePhoto(imageCaptureConfig: ImageCaptureConfig? = null) {
        if (imageCaptureConfig != null) {
            cameraConfig.imageCaptureConfig = imageCaptureConfig
        }
        val captureConfig = cameraConfig.imageCaptureConfig

        //当前，既不是拍照，也不是图像识别的话，要拍照，就得先去绑定拍照的实例
        if (!UseCaseHexStatus.canTakePicture(useCaseBundle)) {
            setCamera(UseCaseMode.takePhoto)
        }

        // 设置拍照的元数据
        val metadata = ImageCapture.Metadata().apply {
            // 用前置摄像头的话要镜像画面
            // Mirror image when using the front camera
            isReversedHorizontal = when (captureConfig.horizontalMirrorMode) {
                MirrorMode.MIRROR_MODE_ON_FRONT_ONLY -> {
                    lensFacing == CameraSelector.LENS_FACING_FRONT
                }

                MirrorMode.MIRROR_MODE_ON -> {
                    true
                }

                MirrorMode.MIRROR_MODE_OFF -> {
                    false
                }

                else -> {
                    throw IllegalArgumentException("未知参数")
                }
            }
            isReversedVertical = when (captureConfig.verticalMirrorMode) {
                MirrorMode.MIRROR_MODE_ON_FRONT_ONLY -> {
                    lensFacing == CameraSelector.LENS_FACING_FRONT
                }

                MirrorMode.MIRROR_MODE_ON -> {
                    true
                }

                MirrorMode.MIRROR_MODE_OFF -> {
                    false
                }

                else -> {
                    throw IllegalArgumentException("未知参数")
                }
            }
            location = captureConfig.location
        }
        // 创建输出选项，包含有图片文件和其中的元数据
        val pair = ImageCaptureHelper.getFileOutputOption(metadata, context)
        val outputOptions = pair.first
        val saveFileData = pair.second

        useCaseRunning = true
        // 设置拍照监听回调，当拍照动作被触发的时候
        // Setup image capture listener which is triggered after photo has been taken
        imageCapture.takePicture(
            outputOptions, cameraExecutor, object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed:-----------------\n\n ${exc.message}", exc)
                    handler.post {
                        captureResultListener?.onPhotoTaken(null)
                    }
                    useCaseRunning = false
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    useCaseRunning = false
                    output.savedUri?.let {
                        saveFileData.uri = it
                    }
                    handler.post {
                        captureResultListener?.onPhotoTaken(saveFileData)
                    }

                }
            })
    }

    /**
     * 为拍摄的图片提供内存缓冲区,自行处理，而不保存为文件
     */
    fun takePhotoInMem(callback: ImageCapture.OnImageCapturedCallback) {
        //当前，既不是拍照，也不是图像识别的话，要拍照，就得先去绑定拍照的实例
        if (!UseCaseHexStatus.canTakePicture(useCaseBundle)) {
            setCamera(UseCaseMode.takePhoto)
        }
        useCaseRunning = true
//        currentStatus = ManagerRunningState.TAKING_PHOTO
        imageCapture.takePicture(cameraExecutor, callback)
//        currentStatus = ManagerRunningState.IDLE
        useCaseRunning = false
    }

    /**
     * 如果绑定了图像识别，录视频后，可以调用它来恢复图像识别实例绑定，其他情况不需要这么做
     *
     * 绑定图像识别，是靠配置参数里的[ManagerConfig.useCaseMode]
     */
    private fun imageAnalyze() {
        if (UseCaseHexStatus.canAnalyze(cameraConfig.useCaseMode)) {
            //几种情况：
            // A：绑定图像识别，再进行拍照或录视频
            // 1.使用了图像识别，此时，是绑定了图像识别和拍照的用例的。点击拍照，是不需要解绑再去绑定拍照用例的；拍照后，不需要恢复图像识别用例绑定
            // 2.依旧使用了图像识别，但此时录制了视频，录制完后，就需要恢复图像识别用例绑定

            // B：没绑定图像识别，进行拍照或录视频
            // 3.仅绑定了拍照，此时，拍照后不需要恢复图像识别用例绑定
            // 4.仅绑定了视频拍摄，此时，录制视频后不需要恢复图像识别用例绑定
            handler.post {
                setCamera(UseCaseMode.imageAnalysis)
            }
        }
    }

    /**
     * 翻转相机时，还需要翻转叠加层
     */
    override fun switchCamera() {
        if (UseCaseHexStatus.canTakeVideo(useCaseBundle) && recording != null && !cameraConfig.recordConfig.asPersistentRecording) {
            //如果未开启持久录制，录制视频时得先停下来
            stopRecord()
        }
        super.switchCamera()
    }

    /**
     * 提供视频录制，暂停，恢复，停止等功能。每一次录制完成，都会置为null
     */
    private var recording: Recording? = null

    /**
     * 开始录制
     *
     * @param videoRecordConfig 将覆盖原有配置
     */
    @SuppressLint("UnsafeOptInUsageError")
    fun startRecord(videoRecordConfig: VideoRecordConfig? = null) {
        if (videoRecordConfig != null) {
            this.cameraConfig.recordConfig = videoRecordConfig //覆盖原有配置
        }
        val recordConfig = cameraConfig.recordConfig

        if (!UseCaseHexStatus.canTakeVideo(useCaseBundle))//如果当前绑定的不是视频捕获实例，绑定他
            setCamera(UseCaseMode.takeVideo)
//        currentStatus = ManagerRunningState.RECORDING
        useCaseRunning = true
        val onceRecorder: OnceRecorder = OnceRecorderHelper.newOnceRecorder(context, recordConfig)
        val videoFile = onceRecorder.saveFileData

        val pendingRecording = onceRecorder.getVideoRecording(videoCapture)
        if (recordConfig.asPersistentRecording) {
            //持久性录制
            pendingRecording.asPersistentRecording()
        }
        recording = pendingRecording.start(Executors.newSingleThreadExecutor(),
            object : androidx.core.util.Consumer<VideoRecordEvent> {
                override fun accept(t: VideoRecordEvent) {
                    if (t is VideoRecordEvent.Finalize) {
                        //t.outputResults.outputUri
//                        currentStatus = ManagerRunningState.IDLE
                        useCaseRunning = false

                        imageAnalyze()
                        if (t.hasError()) {
                            handler.post {
                                captureResultListener?.onVideoRecorded(null)
                            }
                            Log.e(TAG, "accept: what the fuck", t.cause)
                        } else {
                            if (t.outputResults.outputUri != Uri.EMPTY) {
                                videoFile.uri = t.outputResults.outputUri
                            }
                            handler.post {
                                captureResultListener?.onVideoRecorded(videoFile)
                            }
                        }
                    }
                }
            })

    }

    /**
     * 停止录制
     */
    fun stopRecord() {
        //这里是不是会自动的unbind VideoCapture
        if (UseCaseHexStatus.canTakeVideo(useCaseBundle) && recording != null) {
            recording?.stop()
            recording = null
        }
    }

    /**
     * 暂停录制
     */
    fun pauseRecord() {
        recording?.pause()
    }

    /**
     * 恢复录制
     */
    fun resumeRecord() {
        recording?.resume()
    }

    /**
     * 录制时静音
     */
    fun recordMute(mute: Boolean) {
        recording?.mute(mute)
    }

}