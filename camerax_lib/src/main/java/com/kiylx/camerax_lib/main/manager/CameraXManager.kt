package com.kiylx.camerax_lib.main.manager

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Point
import android.hardware.display.DisplayManager
import android.os.Handler
import android.util.DisplayMetrics
import android.util.Log
import android.util.Size
import android.view.OrientationEventListener
import android.view.View
import android.widget.ImageView
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.Event.ON_CREATE
import androidx.lifecycle.Lifecycle.Event.ON_DESTROY
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import com.google.common.util.concurrent.ListenableFuture
import com.yeyupiaoling.cameraxapp.view.FocusImageView
import com.kiylx.camerax_lib.main.manager.model.TakeVideoState
import com.kiylx.camerax_lib.main.manager.ManagerUtil.Companion.hasBackCamera
import com.kiylx.camerax_lib.main.manager.ManagerUtil.Companion.hasFrontCamera
import com.kiylx.camerax_lib.main.manager.model.CameraEventListener
import com.kiylx.camerax_lib.main.manager.model.FlashModel
import com.kiylx.camerax_lib.main.manager.model.ManagerConfig
import com.kiylx.camerax_lib.view.CameraXPreviewViewTouchListener
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

abstract class CameraXManager(
    internal val cameraPreview: PreviewView,
    var cameraConfig: ManagerConfig,
) : LifecycleEventObserver {
    internal lateinit var context: FragmentActivity

    //覆盖在预览上面的图片，用于显示预览的最后一帧，这样可以避免预览在某些时候是黑色的情况
    //可选，由子类提供
    internal var lastPreview: ImageView? = null

    //点击画面时，用来对焦的视图
    //可选，由子类提供
    internal var focusView: FocusImageView? = null

    //fragment的布局
    internal var view: View? = null

    //相机相关
    private var preview: Preview? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    //三种实例
    lateinit var imageAnalyzer: ImageAnalysis
    lateinit var imageCapture: ImageCapture
    lateinit var videoCapture: VideoCapture //录像用例

    //状态
    lateinit var metrics: DisplayMetrics
    var rotation = 0f
    private var displayId: Int = -1
    private lateinit var cameraSelector: CameraSelector
    var lensFacing = CameraSelector.LENS_FACING_BACK
    internal var lastImageDetectionState =
        false//记录cameraConfig中有没有图像识别配置，在拍照和录视频时记录，拍照或录视频后再改回来，以支持持续图像识别
    var currentStatus: Int = TakeVideoState.none//指示当前状态

    //接口
    var cameraListener: CameraEventListener? = null//我定义的接口，用于在这里做某些处理后，通知外界
    private val displayManager by lazy {
        context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    }

    /**
     * 横竖屏切换时，设置
     */
    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = Unit
        override fun onDisplayChanged(displayId: Int) {
            view?.let { view ->
                if (displayId == this@CameraXManager.displayId) {
                    Log.d(TAG, "Rotation changed: ${view.display.rotation}")
                    imageCapture.targetRotation = view.display.rotation
                    imageAnalyzer.targetRotation = view.display.rotation
                }
            } ?: Unit
        }
    }


    //其他
    lateinit var handler: Handler
    private var myZoomValue: Float = 1F//缩放值
    var zoomState: LiveData<ZoomState>? = null
    private var lastStreamState = PreviewView.StreamState.IDLE//预览画面的状态
    var mBitmapFlip: Bitmap? = null//临时存储bitmap

    fun bindLifecycle(fragmentActivity: FragmentActivity) {
        this.context = fragmentActivity
        handler = Handler(context.mainLooper)
        context.lifecycle.addObserver(this)
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            ON_CREATE -> {
                initManager()
            }
            ON_DESTROY -> {
                destroy()
            }
            else -> {}
        }
    }

    private fun initManager() {
        lastImageDetectionState = cameraConfig.imageDetector//记录是否使用图像识别的标志
        if (!checkPerms()) {
            throw Exception("没有权限")
        }
        initCamera()
        //display的方向监听
        cameraPreview.post {
            displayId = cameraPreview.display.displayId
        }
        displayManager.registerDisplayListener(displayListener, null)
    }

    private fun destroy() {
        displayManager.unregisterDisplayListener(displayListener)
        cameraExecutor.shutdown()
        handler.removeCallbacksAndMessages(null)
    }

    /**
     * 初始化相机参数
     */
    @SuppressLint("RestrictedApi")
    private fun initCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener(
            Runnable {
                cameraProvider = cameraProviderFuture.get()
                //设置默认摄像头，默认是后置的摄像头，没有后面的再切换到前面的来
                val cameraProviderTmp = cameraProvider
                parseCameraSelector(cameraProviderTmp)

                metrics = DisplayMetrics().also {
                    cameraPreview.display.getRealMetrics(it)
                }
                val screenAspectRatio =
                    ManagerUtil.aspectRatio(metrics.widthPixels, metrics.heightPixels)
                val size = Size(metrics.widthPixels, metrics.heightPixels)
                val rotation = cameraPreview.display.rotation

                 preview = Preview.Builder()
                    // 我们要去宽高比，但是没有分辨率
                    .setTargetAspectRatio(screenAspectRatio)
                    //.setTargetResolution(size)
                    // 设置初始的旋转
                    .setTargetRotation(rotation)
                    .build()

                initImageAnalyzer()
                initImageCapture(screenAspectRatio, rotation, size)
                initVideoCapture(screenAspectRatio, rotation)

                //屏幕方向监听
                val orientationEventListener = object : OrientationEventListener(context) {
                    override fun onOrientationChanged(orientation: Int) {
                        val rotation: Float = when (orientation) {
                            in 45..134 -> 270f
                            in 135..224 -> 180f
                            in 225..314 -> 90f
                            else -> 0f
                        }
                        this@CameraXManager.rotation = rotation
                    }
                }
                orientationEventListener.enable()//屏幕旋转

                //setUpPinchToZoom()
                setCamera(cameraConfig.captureMode)//绑定实例

                initCameraListener()

                if (cameraConfig.flashMode == FlashModel.CAMERA_FLASH_ALL_ON) {
                    setFlashAlwaysOn(true)
                } else {
                    imageCapture.flashMode = cameraConfig.flashMode
                }
                cameraListener?.initCameraFinished()

            }, ContextCompat.getMainExecutor(context)
        )
    }

    /**
     * setTargetResolution(size)和setTargetAspectRatio(screenAspectRatio)不能同时使用
     */
    private fun initImageCapture(screenAspectRatio: Int, rotation: Int, size: Size) {
        // ImageCapture，用于拍照功能
        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            //设置初始目标旋转，如果旋转改变，我们将不得不再次调用它在此用例的生命周期中
            .setTargetRotation(rotation)
            // 我们要求长宽比，但没有分辨率匹配预览配置，但让 CameraX优化为任何特定的解决方案，最适合我们的用例
            .setTargetAspectRatio(screenAspectRatio)
            //.setTargetResolution(size)
            .build()
    }

    @SuppressLint("RestrictedApi")
    private fun initVideoCapture(screenAspectRatio: Int, rotation: Int) {
        // 视频的还不是很成熟，不一定都能用
        videoCapture = VideoCapture.Builder()//录像用例配置
            .setTargetAspectRatio(screenAspectRatio) //设置高宽比「我比较喜欢全屏」
            //视频帧率  越高视频体积越大
            .setVideoFrameRate(60)
            //bit率  越大视频体积越大
            .setBitRate(3 * 1024 * 1024)
            .setTargetRotation(rotation)//设置旋转角度
            //.setAudioRecordSource(MediaRecorder.AudioSource.MIC)//设置音频源麦克风
            .build()
    }

    private fun initImageAnalyzer() {
        imageAnalyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
    }

    /**
     * 绑定相机实例之类的
     */
    internal fun setCamera(captureMode: Int) {
        //再次重新绑定前应该先解绑 , imageAnalyzer
        cameraProvider?.unbindAll()
        try {
            val lifeOwner = context
            if (lastImageDetectionState) {//以人脸识别优先，绑定图像分析器
                imageAnalyzer.setAnalyzer(cameraExecutor, selectAnalyzer())
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
                    ManagerUtil.TAKE_PHOTO_CASE -> {
                        camera = cameraProvider?.bindToLifecycle(
                            lifeOwner, cameraSelector, preview, imageCapture
                        )
                        if (myZoomValue != 1F) {//预览时缩放了画面，但拍照时如果不添加缩放，拍出来的图是不会缩放的，所以在这里添加上缩放
                            camera?.cameraControl!!.setZoomRatio(myZoomValue)
                        }
                    }
                    ManagerUtil.TAKE_VIDEO_CASE -> {
                        camera = cameraProvider?.bindToLifecycle(
                            lifeOwner, cameraSelector, preview, videoCapture
                        )
                    }
                }
            }
            // Attach the viewfinder's surface provider to preview use case
            preview?.setSurfaceProvider(cameraPreview.surfaceProvider)
            cameraPreview.previewStreamState.observe(
                lifeOwner
            ) { streamState ->
                //有可能会多次调用，那怎么办呢？结合规律，他会多次调用PreviewView.StreamState.IDLE或者PreviewView.StreamState.STREAMING
                //所以很简单，我们记录上一次的状态如果是IDLE，而当前这一次回调是STREAMING，那么就是成功切换后的第一次调用。就只会执行一次
                if (lastStreamState == PreviewView.StreamState.IDLE && streamState == PreviewView.StreamState.STREAMING) {
                    flipImageViewRecycler()
                }
                lastStreamState = streamState
            }
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

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
                val factory = cameraPreview.meteringPointFactory
                // 设置对焦位置
                val point = factory.createPoint(x, y)
                val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF)
                    // 3秒内自动调用取消对焦
                    .setAutoCancelDuration(3, TimeUnit.SECONDS)
                    .build()
                // 执行对焦
                focusView!!.startFocus(Point(x.toInt(), y.toInt()))
                val future: ListenableFuture<*> =
                    camera?.cameraControl!!.startFocusAndMetering(action)
                future.addListener({
                    try {
                        // 获取对焦结果
                        val result = future.get() as FocusMeteringResult
                        if (result.isFocusSuccessful) {
                            focusView!!.onFocusSuccess()
                        } else {
                            focusView!!.onFocusFailed()
                        }
                    } catch (e: java.lang.Exception) {
                        Log.e(TAG, e.toString())
                    }
                }, ContextCompat.getMainExecutor(context))
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
        cameraPreview.setOnTouchListener(cameraXPreviewViewTouchListener)
    }

    /**
     * 切换闪光模式,  打开，关闭，自动，长亮
     */
    fun setFlashMode(flashMode: Int): Int {
        if (flashMode == FlashModel.CAMERA_FLASH_ALL_ON) {
            cameraConfig.flashMode = FlashModel.CAMERA_FLASH_ALL_ON
            setFlashAlwaysOn(true)
        } else {
            cameraConfig.flashMode = flashMode
            setFlashAlwaysOn(false)
            imageCapture.setFlashMode(cameraConfig.flashMode)
        }
        return cameraConfig.flashMode
    }

    private fun setFlashAlwaysOn(status: Boolean) {
        camera?.cameraControl?.enableTorch(status)
    }

    /**
     * 基于当前的值再次缩放
     * 这里只是设置了预览画面的缩放，拍照绑定时还得设置给相机才能拍照的时候缩放
     */
    fun zoom(delta: Float) {
        zoomState?.value?.let {
            val currentZoomRatio = it.zoomRatio
            camera?.cameraControl!!.setZoomRatio(currentZoomRatio * delta)
        }
    }

    /**
     * 直接按照输入的参数进行缩放。
     * 这里只是设置了预览画面的缩放，拍照绑定时还得设置给相机才能拍照的时候缩放
     */
    fun zoom2(zoomValue: Float) {
        myZoomValue = zoomValue
        zoomState?.value?.let {
            camera?.cameraControl!!.setZoomRatio(zoomValue)
        }
    }

    /**
     * 预览数据开始后，去掉假设的画面帧
     */
    private fun flipImageViewRecycler() {
        lastPreview?.let {
            handler.postDelayed(Runnable {
                it.setImageBitmap(null)
                it.visibility = View.GONE
                if (mBitmapFlip != null) {
                    mBitmapFlip?.recycle()
                    mBitmapFlip = null
                }
            }, 200)
        }

    }

    //用最后一帧填充画面，避免变成黑色
    internal fun fillPreview() {
        lastPreview?.let {
            mBitmapFlip = cameraPreview.bitmap
            it.visibility = View.VISIBLE
            it.setImageBitmap(mBitmapFlip)
        }
    }

    /**
     * provide bitmap from CameraPreview
     * 实际上，从预览图象里拿到的bitmap，有时候是缺损的
     */
    fun provideBitmap(): Bitmap? {
        val value = cameraPreview.previewStreamState.value
        return if (value == PreviewView.StreamState.STREAMING)
            cameraPreview.bitmap
        else
            null
    }

    /**
     * 获取当前使用的摄像头，并设置变量
     */
    private fun parseCameraSelector(cameraProviderTmp: ProcessCameraProvider?) {
        lensFacing = when {
            cameraProviderTmp!!.hasBackCamera() -> CameraSelector.LENS_FACING_BACK
            cameraProviderTmp.hasFrontCamera() -> CameraSelector.LENS_FACING_FRONT
            else -> throw IllegalStateException("Back and front camera are unavailable")
        }
        setCameraSelector(lensFacing)
    }

    private inline fun setCameraSelector(lensFacing: Int) {
        cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()
    }

    fun canSwitchCamera(): Boolean {
        val tmp = cameraProvider
        return if (tmp == null)
            false
        else
            tmp.hasBackCamera() && tmp.hasFrontCamera()
    }

    /**
     * 切换前后摄像头
     */
    open fun switchCamera() {
        lensFacing = if (CameraSelector.LENS_FACING_FRONT == lensFacing) {
            CameraSelector.LENS_FACING_BACK
        } else {
            CameraSelector.LENS_FACING_FRONT
        }
        setCameraSelector(lensFacing)
        setCamera(cameraConfig.captureMode)//绑定实例
        cameraListener?.switchCamera(lensFacing)
    }

    /**
     * 由子类提供分析器
     */
    abstract fun selectAnalyzer(): ImageAnalysis.Analyzer

    abstract fun checkPerms(): Boolean

    companion object {
        const val TAG = "相机管理器"
    }
}
