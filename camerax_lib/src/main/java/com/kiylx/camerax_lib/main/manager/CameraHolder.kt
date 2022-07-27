package com.kiylx.camerax_lib.main.manager

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.video.Recording
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import com.permissionx.guolindev.PermissionX
import com.permissionx.guolindev.callback.ExplainReasonCallback
import com.permissionx.guolindev.request.ExplainScope
import com.kiylx.camerax_lib.R
import com.kiylx.camerax_lib.main.manager.imagedetection.base.AnalyzeResultListener
import com.kiylx.camerax_lib.main.manager.imagedetection.face.FaceContourDetectionProcessor
import com.kiylx.camerax_lib.main.manager.imagedetection.face.GraphicOverlay
import com.kiylx.camerax_lib.main.manager.model.*
import com.kiylx.camerax_lib.main.manager.video.OnceRecorder
import com.kiylx.camerax_lib.main.manager.video.getFileOutputOption
import com.kiylx.camerax_lib.utils.ANIMATION_SLOW_MILLIS
import java.util.concurrent.Executors

/**
 * @param content 根布局，跟布局里面要包含预览、对焦、遮盖预览的图像视图等内容
 */
class CameraHolder(
    cameraPreview: PreviewView,
    var graphicOverlay: GraphicOverlay,
    cameraConfig: ManagerConfig,
    content: View,
    private var captureResultListener: CaptureResultListener? = null,
) : CameraXManager(
    cameraPreview, cameraConfig) {
    /**
     * 外界提供此实例后，人脸分析功能将会改为根据visionType取得不同的图像分析器
     */
    var analyzerProvider: AnalyzerProvider? = null
    private var visionType: VisionType = VisionType.Face//默认的图片分析器是人脸分析器
    var analyzerResultListener: AnalyzeResultListener? = null

    init {
        view = content
        lastPreview = content.findViewById(R.id.last_preview)
        focusView = content.findViewById(R.id.focus_view)
    }

    //提供人脸识别
    private val faceProcess by lazy {
        FaceContourDetectionProcessor(graphicOverlay, analyzerResultListener)
    }

    fun changeAnalyzer(visionType: VisionType) {
        this.visionType = visionType
    }

    /**
     * 默认提供人脸分析器，
     * 更多种类型的analyzer由[AnalyzerProvider]提供
     */
    override fun selectAnalyzer(): ImageAnalysis.Analyzer {
        return if (analyzerProvider == null) {
            faceProcess
        } else {
            analyzerProvider!!.provider(visionType)
        }
    }

    override fun checkPerms(): Boolean {
        var b = false
        PermissionX.init(context).permissions(ManagerUtil.REQUIRED_PERMISSIONS.asList())
            .explainReasonBeforeRequest()
            .onExplainRequestReason(object : ExplainReasonCallback {
                override fun onExplainReason(scope: ExplainScope, deniedList: MutableList<String>) {

                }
            })
            .onForwardToSettings(null)
            .request { allGranted, grantedList, deniedList ->
                if (allGranted) {
                    b = true
                }
            }
        return b
    }

    /**
     * 拍照处理方法(这里只是拍照，录制视频另外有方法)
     * 图像分析和拍照都绑定了拍照用例，所以，拍照后不需要重新绑定图像分析或拍照
     * 拍照前会检查用例绑定
     */
    fun takePhoto() {
        //当前，既不是拍照，也不是图像识别的话，要拍照，就得先去绑定拍照的实例
        if (whichInstance != WhichInstanceBind.PICTURE && whichInstance != WhichInstanceBind.IMAGE_DETECTION) {
            setCamera(CaptureMode.takePhoto)
        }
        // TODO: 这里还得改啊，怎么才能不写死呢？
        val photoFile = ManagerUtil.createMediaFile(cameraConfig.MyPhotoDir,
            ManagerUtil.PHOTO_EXTENSION)

        // 设置拍照的元数据
        val metadata = ImageCapture.Metadata().apply {
            // 用前置摄像头的话要镜像画面
            // Mirror image when using the front camera
            isReversedHorizontal = lensFacing == CameraSelector.LENS_FACING_FRONT
        }
        // 创建输出选项，包含有图片文件和其中的元数据
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile)
            .setMetadata(metadata)
            .build()
        currentStatus = TakeVideoState.takePhoto
        // 设置拍照监听回调，当拍照动作被触发的时候
        // Setup image capture listener which is triggered after photo has been taken
        imageCapture.takePicture(
            outputOptions, cameraExecutor, object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed:-----------------\n\n ${exc.message}", exc)
                    captureResultListener?.onPhotoTaken("")
                    currentStatus = TakeVideoState.none
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    currentStatus = TakeVideoState.none
                    indicateTakePhoto()
                    //我就没有看见 output.savedUri 有过正常的数据
                    val savedUriPath = output.savedUri ?: Uri.fromFile(photoFile)
                    captureResultListener?.onPhotoTaken(savedUriPath.path.toString())
                }
            })

    }

    fun takeVideo() {
        if (cameraConfig.useNewVideoCapture) {
            anotherCaptureVideo()
        } else {
            captureVideo()
        }
    }

    fun stopTakeVideo() {
        //这里是不是会自动的unbind VideoCapture
        if (currentStatus == TakeVideoState.takeVideo) {
            if (cameraConfig.useNewVideoCapture) {
                anotherStopCaptureVideo()
            } else {
                stopCaptureVideo()
            }
        }
    }


    /**
     * 拍摄视频，目前还没有稳定，先初步的支持吧
     *
     */
    @SuppressLint("RestrictedApi")
    @Deprecated("不再受支持")
    fun captureVideo() {
        setCamera(ManagerUtil.TAKE_VIDEO_CASE)
        val videoFile = ManagerUtil.createMediaFile(cameraConfig.MyVideoDir,
            ManagerUtil.VIDEO_EXTENSION)

        // 设置视频的元数据，这里需要后期再完善吧
        val metadata = VideoCapture.Metadata().apply {}
        // Create output options object which contains file + metadata
        val outputOptions = VideoCapture.OutputFileOptions.Builder(videoFile)
            .setMetadata(metadata)
            .build()

        currentStatus = TakeVideoState.takeVideo
        if (ActivityCompat.checkSelfPermission(context,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
        ) {
            PermissionX.init(context)
                .permissions(ManagerUtil.REQUIRED_PERMISSIONS.asList())
                .request { allGranted, grantedList, deniedList ->
                    if (!allGranted)
                        throw Exception("缺少权限",
                            Throwable(deniedList.toStr("缺少权限")))
                }
        }
        videoCapture.startRecording(
            outputOptions,
            Executors.newSingleThreadExecutor(),
            object : VideoCapture.OnVideoSavedCallback {
                //当视频文件存储后被调用
                override fun onVideoSaved(outputFileResults: VideoCapture.OutputFileResults) {
                    currentStatus = TakeVideoState.none
                    imageAnalyze()//如果因为拍视频而解绑了图像分析，则重新绑定图像分析
                    captureResultListener?.onVideoRecorded(outputFileResults.savedUri?.path.toString())
                }

                override fun onError(error: Int, message: String, cause: Throwable?) {
                    currentStatus = TakeVideoState.none
                    handler.post(Runnable {
                        setCamera(ManagerUtil.TAKE_PHOTO_CASE)
                    })

                    captureResultListener?.onVideoRecorded("")
                }
            })
    }

    /**
     * 如果使用了图像识别，在拍照或录视频后，可以调用它来恢复图像识别实例绑定
     *
     * 实际上，绑定图像识别，是靠配置参数里的[ManagerConfig.captureMode]
     */
    private fun imageAnalyze() {
        if (cameraConfig.captureMode == CaptureMode.imageAnalysis) {
            //几种情况：
            // A：绑定图像识别，再进行拍照或录视频
            // 1.使用了图像识别，此时，是绑定了图像识别和拍照的用例的。点击拍照，是不需要解绑再去绑定拍照用例的；拍照后，不需要恢复图像识别用例绑定
            // 2.依旧使用了图像识别，但此时录制了视频，录制完后，就需要恢复图像识别用例绑定

            // B：没绑定图像识别，进行拍照或录视频
            // 3.仅绑定了拍照，此时，拍照后不需要恢复图像识别用例绑定
            // 4.仅绑定了视频拍摄，此时，录制视频后不需要恢复图像识别用例绑定
            handler.post {
                setCamera(CaptureMode.imageAnalysis)
            }
        }
    }

    /**
     * 停止录像
     *
     * 录制的视频的时间
     */
    @SuppressLint("RestrictedApi")
    @Deprecated("不再受支持")
    fun stopCaptureVideo() {
        fillPreview()
        videoCapture.stopRecording()
    }

    /**
     * 标示拍照触发成功了
     */
    private fun indicateTakePhoto() {
        if (CameraSelector.LENS_FACING_BACK == lensFacing) {
            indicateSuccess(20)
        } else {
            if (cameraConfig.flashMode == FlashModel.CAMERA_FLASH_ALL_ON || cameraConfig.flashMode == FlashModel.CAMERA_FLASH_ON) {
                indicateSuccess(20)   //先不要柔白补光了 500
            }
        }
    }

    /**
     * 拍照显示成功的提示
     */
    private fun indicateSuccess(durationTime: Long) {
        // 显示一个闪光动画来告知用户照片已经拍好了。在华为等手机上好像有点问题啊 cameraPreview
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            view?.let { cameraUIContainer ->
                cameraUIContainer.postDelayed({
                    cameraUIContainer.foreground = ColorDrawable(Color.WHITE)
                    cameraUIContainer.postDelayed(
                        { cameraUIContainer.foreground = null },
                        durationTime
                    )
                }, ANIMATION_SLOW_MILLIS)
            }
        }
    }

    /**
     * 翻转相机时，还需要翻转叠加层
     */
    override fun switchCamera() {
        if (currentStatus == TakeVideoState.takeVideo) {//录制视频时得先停下来
            stopTakeVideo()
        }
        super.switchCamera()
        graphicOverlay.toggleSelector()
    }

    /**
     *提供视频录制，暂停，恢复，停止等功能。每一次录制完成，都会置为null
     */
    private var recording: Recording? = null

    /**
     * 使用新方式实现的录制视频
     */
    fun anotherCaptureVideo() {
        if (whichInstance != WhichInstanceBind.VIDEO)//如果当前绑定的不是视频捕获实例，绑定他
            setCamera(ManagerUtil.TAKE_VIDEO_CASE)
        currentStatus = TakeVideoState.takeVideo

        // TODO: 这里还得改啊，怎么才能不写死呢？
        //如果是mediastore，也很头疼
        val videoFile = ManagerUtil.createMediaFile(cameraConfig.MyVideoDir,
            ManagerUtil.VIDEO_EXTENSION)
        val onceRecorder: OnceRecorder = OnceRecorder(context).getFileOutputOption(videoFile)

        recording = onceRecorder.getVideoRecording()
            ?.start(Executors.newSingleThreadExecutor(),
                object : androidx.core.util.Consumer<VideoRecordEvent> {
                    override fun accept(t: VideoRecordEvent) {
                        if (t is VideoRecordEvent.Finalize) {
                            currentStatus = TakeVideoState.none
                            imageAnalyze()
                            if (t.error != VideoRecordEvent.Finalize.ERROR_NONE) {
                                captureResultListener?.onVideoRecorded("")
                            } else {
                                captureResultListener?.onVideoRecorded(videoFile.absolutePath)
                            }
                        }
                    }
                })

    }

    fun anotherStopCaptureVideo() {
        recording?.stop()
        recording = null
    }
}