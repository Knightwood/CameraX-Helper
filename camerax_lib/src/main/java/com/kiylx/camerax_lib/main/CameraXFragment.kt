package com.kiylx.camerax_lib.main

import android.Manifest
import android.annotation.SuppressLint
import android.content.*
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.hardware.display.DisplayManager
import android.net.Uri
import android.os.*
import android.util.DisplayMetrics
import android.util.Log
import android.util.Size
import android.view.*
import android.widget.FrameLayout
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.camera.core.*
import androidx.camera.core.Camera
import androidx.camera.core.ImageCapture.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.common.util.concurrent.ListenableFuture
import com.kiylx.camerax_lib.main.CameraConfig
import com.kiylx.camerax_lib.R
import com.kiylx.camerax_lib.main.manager.imagedetection.face.GraphicOverlay
import com.kiylx.camerax_lib.main.CameraConfig.Companion.CAMERA_FLASH_ALL_ON
import com.kiylx.camerax_lib.main.CameraConfig.Companion.MEDIA_MODE_PHOTO
import com.kiylx.camerax_lib.main.manager.model.CaptureResultListener
import com.kiylx.camerax_lib.main.manager.model.TakeVideoState
import com.kiylx.camerax_lib.utils.ANIMATION_SLOW_MILLIS
import com.kiylx.camerax_lib.utils.CStaticHandler
import com.kiylx.camerax_lib.view.CameraXPreviewViewTouchListener
import kotlinx.android.synthetic.main.fragment_camerax.*
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

const val KEY_CAMERA_EVENT_ACTION = "key_camera_event_action"
const val KEY_CAMERA_EVENT_EXTRA = "key_camera_event_extra"
const val CAMERA_CONFIG = "camera_config"   //相机的配置

/**
 * 1.新版本拍照&拍视频方案，需要适配分区存储TargetSDK>30
 * setUpCamera() --> initCameraUseCases() --> bindCameraUseCase
 */
@Deprecated("使用manager中构建的新工具替代")
class CameraXFragment : Fragment() {
    private lateinit var captureResultListener: CaptureResultListener

    //相机的配置：存储路径，闪光灯模式，
    private lateinit var cameraConfig: CameraConfig

    //CameraFragment 对应的XML布局中最外层的ConstraintLayout
    private lateinit var cameraUIContainer: FrameLayout

    lateinit var cameraPreview: PreviewView
    private lateinit var broadcastManager: LocalBroadcastManager
    var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private lateinit var cameraSelector: CameraSelector

    private var displayId: Int = -1
    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK
    private var lastFaceState = false//记录cameraConfig中有没有人脸识别配置，在拍照和录视频时记录，拍照或录视频后再改回来，以支持持续人脸识别
    var rotation = 0f

    private var preview: Preview? = null

    //图像分析
    private var imageAnalyzer: ImageAnalysis = ImageAnalysis.Builder()
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .build()

    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null

    private var imageCapture: ImageCapture? = null //拍照
    private var videoCapture: VideoCapture? = null //录像用例

    var cameraListener: CameraListener? = null//我定义的接口，用于在这里做某些处理后，通知外界

    private val displayManager by lazy {
        requireContext().getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    }

    var currentStatus: Int = TakeVideoState.none//指示当前状态

    //音量下降按钮接收器用于触发快门
    private val volumeDownReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.getIntExtra(KEY_CAMERA_EVENT_EXTRA, KeyEvent.KEYCODE_UNKNOWN)) {
                // When the volume down button is pressed, simulate a shutter button click
                KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    takePhoto()
                }
            }
        }
    }

    /**
     * 横竖屏切换
     * We need a display listener for orientation changes that do not trigger a configuration
     * change, for example if we choose to override config change in manifest or for 180-degree
     * orientation changes.
     */
    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = Unit
        override fun onDisplayChanged(displayId: Int) = view?.let { view ->
            if (displayId == this@CameraXFragment.displayId) {
                Log.d(TAG, "Rotation changed: ${view.display.rotation}")
                imageCapture?.targetRotation = view.display.rotation
                imageAnalyzer.targetRotation = view.display.rotation
            }
        } ?: Unit
    }

    private var permissionRequestListener: OnPermissionRequestListener? = null

    // This interface can be implemented by the Activity, parent Fragment,
    // or a separate test implementation.
    interface OnPermissionRequestListener {
        fun onBeforePermissionRequest(permissions: Array<String>, requestCode: Int)
        fun onAfterPermissionDeny(permissions: Array<String>, requestCode: Int)
    }

    override fun onAttach(@NonNull context: Context) {
        super.onAttach(context)
        // 要求该 Fragment 所附着的 Activity 必须实现这个方法, 工信部要求申请权限必须要给用户说明申请权限的用途
        // 一般是弹出一个dialog 的形式，具体的样式，相应的开发接入时候可以在接口实现中实现,你可以空实现
        try {
            permissionRequestListener = context as OnPermissionRequestListener
        } catch (e: Exception) {
            Toast.makeText(
                getContext(),
                "你需要在本Fragment 的宿主Activity中实现 OnPermissionRequestListener",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            cameraConfig = it.getParcelable(CAMERA_CONFIG)!!
            if (cameraConfig.mediaMode != MEDIA_MODE_PHOTO) {
                //如果是录像或录音，需要再多申请一个录音权限
                REQUIRED_PERMISSIONS =
                    REQUIRED_PERMISSIONS.plusElement(Manifest.permission.RECORD_AUDIO)
            }
            lastFaceState = cameraConfig.faceDetector//记录人脸识别标志
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_camerax, container, false)
    }

    /**
     * 相机相关的状态初始化
     */
    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //CameraFragment 最外层的ConstraintLayout
        cameraUIContainer = view as FrameLayout
        cameraPreview = cameraUIContainer.findViewById(R.id.camera_preview)
        broadcastManager = LocalBroadcastManager.getInstance(view.context)//用于监测音量键触发

        // 设置意图过滤器，从我们的main activity接收事件
        val filter = IntentFilter().apply { addAction(KEY_CAMERA_EVENT_ACTION) }

        broadcastManager.registerReceiver(volumeDownReceiver, filter)
        // 每当设备的方向改变时，就对用例进行更新旋转
        // Every time the orientation of device changes, update rotation for use cases
        displayManager.registerDisplayListener(displayListener, null)
        handler = CameraXHandler(this)
    }

    /**
     * 有可能回来就取消了权限
     *
     */
    override fun onStart() {
        super.onStart()
        if (!hasAllPermissions(requireContext())) {
            //根据隐私合规要求，需要拦截检测哪些权限没有授权，并给予相应的温馨提示
            permissionRequestListener?.onBeforePermissionRequest(
                REQUIRED_PERMISSIONS,
                PERMISSIONS_REQUEST_CODE
            )

            if (checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
            ) {
                safeSetUpCamera()
            }

        } else {
            permissionRequestListener?.onBeforePermissionRequest(
                arrayOf(),
                PERMISSIONS_REQUEST_CODE
            )
            //等待所有的View 都能正确的显示出
            cameraPreview.post {
                // Keep track of the display in which this view is attached
                displayId = cameraPreview.display.displayId
                // Set up the camera and its use cases
                safeSetUpCamera()
            }
        }
    }

    /**
     * 销毁后各种处理后事啊
     *
     */
    override fun onDestroyView() {
        super.onDestroyView()
        // Shut down our background executor
        cameraExecutor.shutdown()
        // Unregister the broadcast receivers and listeners
        broadcastManager.unregisterReceiver(volumeDownReceiver)
        displayManager.unregisterDisplayListener(displayListener)
        handler.removeCallbacksAndMessages(null)
    }

    /**
     * 需要添加的权限也一起申请了
     */

    fun addRequestPermission(permissions: Array<String>) {
        permissions.forEach {
            if (!REQUIRED_PERMISSIONS.contains(it)) {
                REQUIRED_PERMISSIONS =
                    REQUIRED_PERMISSIONS.plus(permissions)
            }
        }
    }

    fun getOverlay(): GraphicOverlay {
        return graphicOverlay_finder
    }

    /**
     * 初始化摄像头，绑定处理问题
     */
    private fun setUpCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener(Runnable {//相机准备好后，回回调此接口，设置相机相关参数
            //获得CameraProvider
            cameraProvider = cameraProviderFuture.get()

            //设置默认摄像头，默认是后置的摄像头，没有后面的再切换到前面的来
            lensFacing = when {
                hasBackCamera() -> CameraSelector.LENS_FACING_BACK
                hasFrontCamera() -> CameraSelector.LENS_FACING_FRONT
                else -> throw IllegalStateException("Back and front camera are unavailable")
            }
            // Build and bind the camera use cases
            //具体的相机参数设定
            initCameraUseCases()

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun safeSetUpCamera() {
        if (isAdded) {
            setUpCamera()
        } else {
            handler.postDelayed(Runnable { setUpCamera() }, 500)
        }
    }

    /**
     * 初始化一些内容，初始化预览组件，获取拍照和视频拍摄组件，初始化屏幕方向监听器
     * 优化文件传输，剔除废弃代码，调整包级结构，统一依赖版本。学习hilt库的使用
     * 优化相机功能，修人脸识别图层绘制，打包测试。
     */
    @SuppressLint("RestrictedApi")
    private fun initCameraUseCases() {
        // 获得屏幕的尺寸数据来设置全屏的分辨率
        // Get screen metrics used to setup camera for full screen resolution
        val metrics = DisplayMetrics().also { cameraPreview.display.getRealMetrics(it) }
        val screenAspectRatio = aspectRatio(metrics.widthPixels, metrics.heightPixels)

        val size = Size(metrics.widthPixels, metrics.heightPixels)
        val rotation = cameraPreview.display.rotation

        // CameraProvider
        val cameraProvider = cameraProvider ?: throw IllegalStateException("Camera init failed.")

        // CameraSelector
        cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        // 预览 Preview
        preview = Preview.Builder()
            // 我们要去宽高比，但是没有分辨率
//            .setTargetAspectRatio(screenAspectRatio)
////            .setTargetResolution(size)
//            // 设置初始的旋转
//            .setTargetRotation(rotation)
            .build()

        // ImageCapture，用于拍照功能
        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            //设置初始目标旋转，如果旋转改变，我们将不得不再次调用它在此用例的生命周期中
            .setTargetRotation(rotation)
            // 我们要求长宽比，但没有分辨率匹配预览配置，但让 CameraX优化为任何特定的解决方案，最适合我们的用例
            .setTargetAspectRatio(screenAspectRatio)
//            .setTargetResolution(size)
            .build()

        // 视频的还不是很成熟，不一定都能用
        videoCapture = VideoCapture.Builder()//录像用例配置
            .setTargetAspectRatio(screenAspectRatio) //设置高宽比「我比较喜欢全屏」
            //视频帧率  越高视频体积越大
            .setVideoFrameRate(60)
            //bit率  越大视频体积越大
            .setBitRate(3 * 1024 * 1024)
            .setTargetRotation(rotation)//设置旋转角度
//            .setAudioRecordSource(MediaRecorder.AudioSource.MIC)//设置音频源麦克风
            .build()
        val orientationEventListener = object : OrientationEventListener(context) {
            override fun onOrientationChanged(orientation: Int) {
                val rotation: Float = when (orientation) {
                    in 45..134 -> 270f
                    in 135..224 -> 180f
                    in 225..314 -> 90f
                    else -> 0f
                }
                    this@CameraXFragment.rotation = rotation
            }
        }
        orientationEventListener.enable()//屏幕旋转
        bindCameraUseCase(cameraConfig.captureMode)

//        // Attach the viewfinder's surface provider to preview use case
//        preview?.setSurfaceProvider(cameraPreview.surfaceProvider)

        initCameraListener()

        if (cameraConfig.flashMode == CAMERA_FLASH_ALL_ON) {
            setFlashAlwaysOn(true)
        } else {
            imageCapture?.flashMode = cameraConfig.flashMode
        }
        cameraListener?.initCameraFinished()
    }

    /**
     * 基于当前的值再次缩放
     */
    public fun zoom(delta: Float) {
        zoomState?.value?.let {
            val currentZoomRatio = it.zoomRatio
            camera?.cameraControl!!.setZoomRatio(currentZoomRatio * delta)
        }
    }

    private var myZoomValue: Float = 1F

    fun zoom2(zoomValue: Float) {
        myZoomValue = zoomValue
        zoomState?.value?.let {
            camera?.cameraControl!!.setZoomRatio(zoomValue)
        }
    }

    var zoomState: LiveData<ZoomState>? = null

    /**
     * 相机点击等相关操作监听，焦距操作
     *
     */
    private fun initCameraListener() {
        zoomState = camera?.cameraInfo?.zoomState
        val cameraXPreviewViewTouchListener = CameraXPreviewViewTouchListener(this.context)

        cameraXPreviewViewTouchListener.setCustomTouchListener(object :
            CameraXPreviewViewTouchListener.CustomTouchListener {
            // 放大缩小操作
            override fun zoom(delta: Float) {
                zoomState?.value?.let {
                    val currentZoomRatio = it.zoomRatio
                    camera?.cameraControl!!.setZoomRatio(currentZoomRatio * delta)
                }
            }

            // 点击操作
            override fun click(x: Float, y: Float) {
                val factory = camera_preview.meteringPointFactory
                // 设置对焦位置
                val point = factory.createPoint(x, y)
                val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF)
                    // 3秒内自动调用取消对焦
                    .setAutoCancelDuration(3, TimeUnit.SECONDS)
                    .build()
                // 执行对焦
                focus_view!!.startFocus(Point(x.toInt(), y.toInt()))
                val future: ListenableFuture<*> =
                    camera?.cameraControl!!.startFocusAndMetering(action)
                future.addListener({
                    try {
                        // 获取对焦结果
                        val result = future.get() as FocusMeteringResult
                        if (result.isFocusSuccessful) {
                            focus_view!!.onFocusSuccess()
                        } else {
                            focus_view!!.onFocusFailed()
                        }
                    } catch (e: java.lang.Exception) {
                        Log.e(TAG, e.toString())
                    }
                }, ContextCompat.getMainExecutor(this@CameraXFragment.requireContext()))
            }

            // 双击操作
            override fun doubleClick(x: Float, y: Float) {
                // 双击放大缩小
                val currentZoomRatio = zoomState?.value!!.zoomRatio
                if (currentZoomRatio > zoomState?.value!!.minZoomRatio) {
                    camera?.cameraControl!!.setLinearZoom(0f)
                } else {
                    camera?.cameraControl!!.setLinearZoom(0.5f)
                }
            }

            override fun longPress(x: Float, y: Float) {
                Log.d(TAG, "长按")
            }
        })
        // 添加监听事件
        camera_preview.setOnTouchListener(cameraXPreviewViewTouchListener)
    }


    /**
     *  检测是4：3 还是16：9 好点
     *
     *  [androidx.camera.core.ImageAnalysis Config] requires enum value of
     *  [androidx.camera.core.AspectRatio]. Currently it has values of 4:3 & 16:9.
     *
     *  Detecting the most suitable ratio for dimensions provided in @params by counting absolute
     *  of preview ratio to one of the provided values.
     *
     *  @param width - preview width   预览的宽
     *  @param height - preview height 预览的高
     *  @return suitable aspect ratio 合适的比例
     */
    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = max(width, height).toDouble() / min(width, height)
        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }

    var mBitmapFlip: Bitmap? = null

    /**
     * 停止录像
     *
     * 录制的视频的时间
     */
    @SuppressLint("RestrictedApi")
    public fun stopTakeVideo(time: Long) {
        //这里是不是会自动的unbind VideoCapture
        fillPreview()
        if (currentStatus == TakeVideoState.takeVideo)
            videoCapture?.stopRecording()
    }

    //用最后一帧填充画面，避免变成黑色
    private fun fillPreview() {
        mBitmapFlip = cameraPreview.bitmap
        last_preview.visibility = View.VISIBLE
        last_preview.setImageBitmap(mBitmapFlip)
    }

    /**
     * 拍摄视频移动手指控制缩放,支持中...
     *
     */
    @Deprecated("not implemented")
    fun zoomTakeVideo() {
    }

    /**
     * 拍摄视频，目前还没有稳定，先初步的支持吧
     *
     */
    @SuppressLint("RestrictedApi")
    fun takeVideo() {
        lastFaceState = false
        bindCameraUseCase(TAKE_VIDEO_CASE)
        val videoFile = createMediaFile(cameraConfig.MyVideoDir, VIDEO_EXTENSION)

        // 设置视频的元数据，这里需要后期再完善吧
        val metadata = VideoCapture.Metadata().apply {}
        // Create output options object which contains file + metadata
        val outputOptions = VideoCapture.OutputFileOptions.Builder(videoFile)
            .setMetadata(metadata)
            .build()

        //开始录像,需要多一个录音权限
        if (checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissionRequestListener?.onBeforePermissionRequest(
                REQUIRED_PERMISSIONS,
                PERMISSIONS_REQUEST_CODE
            )
        } else {
            permissionRequestListener?.onBeforePermissionRequest(
                arrayOf(),
                PERMISSIONS_REQUEST_CODE
            )
            currentStatus = TakeVideoState.takeVideo
            videoCapture?.startRecording(
                outputOptions,
                Executors.newSingleThreadExecutor(),
                object : VideoCapture.OnVideoSavedCallback {
                    //当视频文件存储后被调用
                    override fun onVideoSaved(outputFileResults: VideoCapture.OutputFileResults) {
                        currentStatus = TakeVideoState.none
                        //flushMedia(outputFileResults.savedUri)
                        lastFaceState = cameraConfig.faceDetector
                        if (lastFaceState)
                            imageAnaly()
                        handler.post(Runnable {
                            bindCameraUseCase(TAKE_PHOTO_CASE)
                        })

                        captureResultListener.onVideoRecorded(outputFileResults.savedUri?.path.toString())
                    }

                    override fun onError(error: Int, message: String, cause: Throwable?) {
                        currentStatus = TakeVideoState.none
                        handler.post(Runnable {
                            bindCameraUseCase(TAKE_PHOTO_CASE)
                        })

                        captureResultListener.onVideoRecorded("")
                    }
                })
        }
    }

    /**
     * 切换前后摄像头
     */
    fun switchCamera() {
        lensFacing = if (CameraSelector.LENS_FACING_FRONT == lensFacing) {
            CameraSelector.LENS_FACING_BACK
        } else {
            CameraSelector.LENS_FACING_FRONT
        }
        // Re-bind use cases to update selected camera
        initCameraUseCases()
        cameraListener?.switchCamera(lensFacing)
    }

    /**
     * 切换闪光模式,  打开，关闭，自动，长亮
     */
    fun setFlashMode(flashMode: Int): Int {
        if (flashMode == CAMERA_FLASH_ALL_ON) {
            cameraConfig.flashMode = CAMERA_FLASH_ALL_ON
            setFlashAlwaysOn(true)
        } else {
            cameraConfig.flashMode = flashMode
            setFlashAlwaysOn(false)
            imageCapture?.setFlashMode(cameraConfig.flashMode)
        }
        return cameraConfig.flashMode
    }


    private fun setFlashAlwaysOn(status: Boolean) {
        camera?.cameraControl?.enableTorch(status)
    }

    /**
     * 把拍照或录制功能绑定到相机，如此才能实现拍照或录视频，
     * 绑定前先解绑。
     * 这里默认是传入0，默认拍照
     * captureMode
     * 0：拍照
     * 1：拍视频
     */
    private fun bindCameraUseCase(captureMode: Int) {
        //再次重新绑定前应该先解绑 , imageAnalyzer
        cameraProvider?.unbindAll()
        try {
            val lifeOwner: LifecycleOwner = requireActivity()

            if (lastFaceState) {//以人脸识别优先，绑定图像分析器
                cameraListener?.let {
                    imageAnalyzer.setAnalyzer(
                        cameraExecutor,
                        it.getAnalyzer()
                    )
                }
                camera = cameraProvider?.bindToLifecycle(
                    lifeOwner,
                    cameraSelector,
                    preview,
                    imageAnalyzer
                )
                currentStatus = TakeVideoState.imageDetection
            } else {
                //目前一次无法绑定拍照和摄像一起
                when (captureMode) {
                    TAKE_PHOTO_CASE -> {
                        camera = cameraProvider?.bindToLifecycle(
                            lifeOwner, cameraSelector, preview, imageCapture
                        )
                        if (myZoomValue != 1F) {//预览时缩放了画面，但拍照时如果不添加缩放，拍出来的图是不会缩放的，所以在这里添加上缩放
                            camera?.cameraControl!!.setZoomRatio(myZoomValue)
                        }
                    }
                    TAKE_VIDEO_CASE -> {
                        camera = cameraProvider?.bindToLifecycle(
                            lifeOwner, cameraSelector, preview, videoCapture
                        )
                    }
                }
            }
            // Attach the viewfinder's surface provider to preview use case
            preview?.setSurfaceProvider(cameraPreview.surfaceProvider)
            cameraPreview.previewStreamState.observe(
                lifeOwner,
                androidx.lifecycle.Observer { streamState ->
                    //有可能会多次调用，那怎么办呢？结合规律，他会多次调用PreviewView.StreamState.IDLE或者PreviewView.StreamState.STREAMING
                    //所以很简单，我们记录上一次的状态如果是IDLE，而当前这一次回调是STREAMING，那么就是成功切换后的第一次调用。就只会执行一次
                    if (lastStreamState == PreviewView.StreamState.IDLE && streamState == PreviewView.StreamState.STREAMING) {
                        flipImageViewRecycler();
                    }
                    lastStreamState = streamState;
                })
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    private var lastStreamState = PreviewView.StreamState.IDLE

    /**
     * 预览数据开始后，去掉假设的画面帧
     */
    private fun flipImageViewRecycler() {
        handler.postDelayed(Runnable {
            last_preview.setImageBitmap(null)
            last_preview.visibility = View.GONE
            if (mBitmapFlip != null) {
                mBitmapFlip?.recycle()
                mBitmapFlip = null
            }
        }, 200)
    }

    /**
     * 获取是前置还是后置模式
     *
     */
    fun getLensFacing(): Int {
        return lensFacing;
    }

    /**
     * 标示拍照触发成功了
     */
    private fun indicateTakePhoto() {
        if (CameraSelector.LENS_FACING_BACK == lensFacing) {
            indicateSuccess(20)
        } else {
            if (cameraConfig.flashMode == CAMERA_FLASH_ALL_ON || cameraConfig.flashMode == FLASH_MODE_ON) {
                indicateSuccess(20)   //先不要柔白补光了 500
            }
        }
    }

    /**
     * provide bitmap from CameraPreview
     */
    fun provideBitmap(): Bitmap? {
        val value = cameraPreview.previewStreamState.value
        return if (value == PreviewView.StreamState.STREAMING)
            cameraPreview.bitmap
        else
            null
    }

    /**
     * 拍照处理方法(这里只是拍照，录制视频另外有方法)
     *
     */
    fun takePhoto() {
        lastFaceState = false
        bindCameraUseCase(TAKE_PHOTO_CASE)
        // Get a stable reference of the modifiable image capture use case
        imageCapture?.let { imageCapture ->
            val photoFile = createMediaFile(cameraConfig.MyPhotoDir, PHOTO_EXTENSION)

            // 设置拍照的元数据
            val metadata = ImageCapture.Metadata().apply {
                // 用前置摄像头的话要镜像画面
                // Mirror image when using the front camera
                isReversedHorizontal = lensFacing == CameraSelector.LENS_FACING_FRONT
            }

            // Create output options object which contains file + metadata
            // 创建输出选项，包含有图片文件和其中的元数据
            val outputOptions = OutputFileOptions.Builder(photoFile)
                .setMetadata(metadata)
                .build()
            currentStatus = TakeVideoState.takePhoto
            // 设置拍照监听回调，当拍照动作被触发的时候
            // Setup image capture listener which is triggered after photo has been taken
            imageCapture.takePicture(
                outputOptions, cameraExecutor, object : OnImageSavedCallback {
                    override fun onError(exc: ImageCaptureException) {
                        Log.e(TAG, "Photo capture failed:-----------------\n\n ${exc.message}", exc)
                        captureResultListener.onPhotoTaken("")
                        currentStatus = TakeVideoState.none
                    }

                    override fun onImageSaved(output: OutputFileResults) {
                        currentStatus = TakeVideoState.none
                        indicateTakePhoto()
                        //我就没有看见 output.savedUri 有过正常的数据
                        val savedUriPath = output.savedUri ?: Uri.fromFile(photoFile)
                        captureResultListener.onPhotoTaken(savedUriPath.path.toString())
                        //flushMedia(savedUriPath)
                        lastFaceState = cameraConfig.faceDetector
                        if (lastFaceState)
                            imageAnaly()
                    }
                })
        }
    }

    class CameraXHandler(ta: CameraXFragment) : CStaticHandler<CameraXFragment>(ta) {
        override fun handle(target: CameraXFragment?, msg: Message?) {
        }
    }

    lateinit var handler: Handler

    //= Handler(Looper.getMainLooper())
    fun imageAnaly() {
        handler.postDelayed(Runnable {
            bindCameraUseCase(TAKE_PHOTO_CASE)
        }, 1000)
    }

    /**
     * 拍照显示成功的提示
     *
     */
    private fun indicateSuccess(durationTime: Long) {
        // 显示一个闪光动画来告知用户照片已经拍好了。在华为等手机上好像有点问题啊 cameraPreview
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            cameraUIContainer.postDelayed({
                cameraUIContainer.foreground = ColorDrawable(Color.WHITE)
                cameraUIContainer.postDelayed(
                    { cameraUIContainer.foreground = null },
                    durationTime
                )
            }, ANIMATION_SLOW_MILLIS)
        }
    }


    /**
     * 是否有两个摄像头可以用来切换
     *
     */
    @Deprecated("")
    fun canSwitchCamera(): Boolean {
        return hasBackCamera() && hasFrontCamera()
    }

    /**
     * 检查设备是否有后置摄像头
     */
    @Deprecated("")
    private fun hasBackCamera(): Boolean {
        return cameraProvider?.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) ?: false
    }

    /**
     * 检查设备是否有前置摄像头
     */
    @Deprecated("")
    private fun hasFrontCamera(): Boolean {
        return cameraProvider?.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) ?: false
    }


    /**
     * 去请求权限
     */
    fun onRequestPermission(permissions: Array<String>, requestCode: Int) {
        requestPermissions(REQUIRED_PERMISSIONS, PERMISSIONS_REQUEST_CODE)
    }

    /**
     *  请求相机权限
     */
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            var permissionsDeny: Array<String> = arrayOf()

            for (index in permissions.indices) {
                if (grantResults[index] == PackageManager.PERMISSION_DENIED) {
                    permissionsDeny = permissionsDeny.plusElement(permissions[index])
                }
                when (permissions[index]) {
                    Manifest.permission.CAMERA -> {
                        if (grantResults[index] == PackageManager.PERMISSION_GRANTED) {
                            //只要有相机权限就可以初始化相机了
                            safeSetUpCamera()
                        }
                    }
                }
            }
            permissionRequestListener?.onAfterPermissionDeny(permissionsDeny, requestCode)
        }
    }

    //这些参数初始化和接口初始化后面再写吧
    fun setCaptureResultListener(listener: CaptureResultListener) {
        this.captureResultListener = listener
    }

    companion object {
        private const val TAG = "CameraXFragment"
        private const val PHOTO_EXTENSION = ".jpg"
        private const val VIDEO_EXTENSION = ".mp4"
        private const val TAKE_PHOTO_CASE = 0
        private const val TAKE_VIDEO_CASE = 1

        private const val PERMISSIONS_REQUEST_CODE = 1619
        const val RATIO_4_3_VALUE = 4.0 / 3.0
        const val RATIO_16_9_VALUE = 16.0 / 9.0


        private var REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        /** Convenience method used to check if all permissions required by this app are granted */
        fun hasAllPermissions(context: Context) = REQUIRED_PERMISSIONS.all {
            checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }

        /**
         * 产生的素材都将统一放在Lebang 文件下，后续需要清楚才好管理
         *
         * @throws IOException
         */
        private fun createMediaFile(baseFolder: String?, format: String): File {
            val timeStamp = SimpleDateFormat("yyyyMMddHHmmss").format(Date())
            createDir(baseFolder)
            return File(baseFolder + timeStamp + format)
        }

        /**
         * 创建图片目录
         *
         * @param dirPath
         * @return
         */
        private fun createDir(dirPath: String?): Boolean {
            //判断为空的目录的情况用默认目录。。。。
            val file = File(dirPath)
            return if (!file.exists() || !file.isDirectory) {
                file.mkdirs()
            } else
                true
        }

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param cameraConfig Parameter 1.
         * @return A new instance of fragment CameraXFragment.
         */
        @JvmStatic
        fun newInstance(cameraConfig: CameraConfig) =
            CameraXFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(CAMERA_CONFIG, cameraConfig)
                }
            }
    }
}