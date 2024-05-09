# CameraXlib

集成了拍照，录制视频，人脸识别等的camerax库。

适配了Android10以上的分区存储，可以将图片和视频存储到app私有目录，相册和相册下文件夹，其他SAF能授予文件夹权限的位置。
Android10 以下，大家都很熟悉。

- 内置人脸检测，图像绘制，并预留出来了改变分析器使用其他图像分析的方法。
- 内置人脸识别，使用tensorflow进行检测，并输出特征点。请查看TestFileDecActivity文件
- 可以拍照，录制，暂停/继续录制，双指缩放，点按对焦，闪光灯，手电筒。
- 自定义配置相机功能，例如拍照时水平翻转或垂直翻转，分辨率和宽高比；视频的镜像翻转，文件或时长限制，视频清晰度等。

* 示例代码在app目录下。

2024-05-09
- 将mlkit和tensorflow拆分出来，新增`BaseCameraXFragment`实现相机，提供自定义布局功能等。
- 下一步计划：1.文档已经过于陈旧，需要大改。2.实现在compose中使用相机

# 截图

<img src="screenshots/1.jpg" width="50%"/><img src="screenshots/2.jpg" width="50%"/>

# 用法
`camerax_lib` module 中提供了`BaseCameraXActivity`和`CameraXFragment`类，后者持有`cameraHolder`实现相机功能，
前者则持有`CameraXFragment`,提供更进一步的封装。

整体的相机实现示例可以看app module下的`CameraExampleActivity`类。
'CameraX'版本：1.3.0
_长期维护中_

- 使用时可直接将`camerax_lib` module集成到项目，进行修改，**不建议dependencies中引入打包好的aar**
~~版本号~~ [![Tag](https://jitpack.io/v/Knightwood/SimpleCameraX.svg)](https://jitpack.io/#Knightwood/SimpleCameraX)
```
 dependencies {
	        implementation 'com.github.Knightwood:SimpleCameraX:Tag'
	}
```


## 配置
以`CameraExampleActivity`为例
### 屏幕方向
```html
//首先是配置：
//屏幕方向这个可选，可以固定竖屏、横屏、不设置。
//需要在清单文件的相机activity中添加如下配置，另持有相机的activity在旋转屏幕时不被销毁重建
android:configChanges="orientation|screenSize"
```
### `CameraExampleActivity` 相机示例，配置存储，拍照，录像，分析器等
#### 相机配置：
例如可以从`MainActivity`启动`CameraExampleActivity`完成拍照和录制
所以，相机的配置可以通过intent传入。
一共有三种模式：拍照，录制，图像分析。

示例：
```kotlin
class CameraExampleActivity : BaseCameraXActivity() {
    // CameraExampleActivity中通过重写configAll 可配置相机一些内容，intent中的键值对为自定义的内容，与库无关
    // 接收到intent，对相机进行配置
  override fun configAll(intent: Intent): ManagerConfig {
    //视频录制配置(可选)
    val videoRecordConfig = VideoRecordConfig(
      quality = CameraRecordQuality.HD,//设置视频拍摄质量
//            fileSizeLimit=100.mb, //文件大小限制。
//            durationLimitMillis =1000*15, //录制时长限制，单位毫秒
      //...省略
    )
    //拍照配置(可选)
    val imageCaptureConfig =ImageCaptureConfig(
      horizontalMirrorMode= MirrorMode.MIRROR_MODE_ON_FRONT_ONLY, //水平翻转
      verticalMirrorMode = MirrorMode.MIRROR_MODE_ON_FRONT_ONLY, //垂直翻转
      //...省略
    )
    //整体的配置
    val useImageDetection = intent.getBooleanExtra(ImageDetection, false)    //是否使用图像分析
    return ManagerConfig().apply {
      this.recordConfig = videoRecordConfig 
//    这里制定了打开相机时的默认模式，图像分析、拍照、录像等。
      this.captureMode =
        if (useImageDetection) CaptureMode.imageAnalysis else CaptureMode.takePhoto
      this.flashMode = FlashModel.CAMERA_FLASH_AUTO
      this.size = Size(1920, 1080)//拍照，预览的分辨率，期望值，不一定会用这个值
    }
  }
}
```

#### 存储配置：
全局的统一配置类: `CameraXStoreConfig`，若不进行配置，则默认存储到相册`CameraX`文件夹下
1. 调用CameraXStoreConfig.configPhoto()配置图片存储位置
2. 调用CameraXStoreConfig.configVideo()配置录制存储位置，使用方式与配置图片没有区别，仅方法名称不同

一共有三种存储方式：file，MediaStore，saf访问框架

```kotlin
class CameraExampleActivity : BaseCameraXActivity() {
    
    //对于拍摄和录制，可以分别配置存储位置，如果不进行配置，则默认存储到相册文件夹。
    //例如：对于拍照的存储配置
  fun initPhotoStore() {
    val relativePath = "fff"
    //使用file绝对路径存储
    CameraXStoreConfig.configPhoto(
      IStore.FileStoreConfig(
        application.cacheDir.absolutePath,
        relativePath
      )
    )
    //使用MediaStore存储
    CameraXStoreConfig.configPhoto(
      IStore.MediaStoreConfig(
        saveCollection = FileLocate.IMAGE.uri,
        mediaFolder = Environment.DIRECTORY_DCIM,
        targetFolder = relativePath
      )
    )
    //使用SAF框架存储到任意文件夹
    StoreX.with(this).safHelper.requestOneFolder { it ->
      //使用SAF框架获取某一个文件夹的授权和uri，然后配置存储
      CameraXStoreConfig.configPhoto(
        IStore.SAFStoreConfig(it)
      )
    }
  }

//视频的存储配置同拍照的存储配置相同，不过是把名称从`configPhoto`换成`configVideo`.可以参考app示例中的MainActivity

}
```
完成配置即可使用相机功能。


##  `UseCaseHolder`
* `UseCaseHolder` 类初始化预览，拍照用例，录像用例，图像分析用例
* 提供设置初始化用例方法
* 提供分辨率和纵横比筛选方式

### 指定预览与拍照所需要的分辨率与纵横比筛选
  指定[resolutionSelector]，提供预览与拍照所需要的分辨率与纵横比筛选
```kotlin
    //不提供自己的实现，仅指定筛选条件用于预览和拍照
    UseCaseHolder.resolutionSelector = ResolutionSelector.Builder()
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
```
### 提供自己所需要的初始化UseCase
如默认的初始化不满足需要，可以调用[setInitImpl]方法，提供自己所需要的初始化
示例：使某个类继承自IUseCaseHelper，并实现初始化预览，拍照用例，录像用例，图像分析用例的方法
  ```
  class IMPL:IUseCaseHelper{
  	........省略
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
    ........省略
    
  }
  val impl =IMPL()
  //在相机初始化之前调用，提供自己的实现
  UseCaseHolder.setInitImpl(impl)
  
  ```

2. 直接继承自`BaseCameraXActivity`就可以自定义相机
3. 或者可以自己实现一个activity,内部放置一个`CameraXFragment`实现相机功能

## 示例相机
CameraExampleActivity 继承自 BaseCameraXActivity，
后者在内部维护了一个cameraxFragment，此类持有了cameraholder实现相机功能，
BaseCameraXActivity还生成了一个CameraXF类实现ICameraXF接口，功能委托给CameraXFragment，
如此可屏蔽fragment相关实现，方便相机操作，还可以获取cameraholder，此类可提供更多的相机操作
```
class CameraXF(private val cameraXF: CameraXFragment) : ICameraXF by cameraXF

abstract class BaseCameraXActivity : BasicActivity(),
   CameraXFragmentEventListener, CaptureResultListener {

    internal lateinit var cameraXFragment: CameraXFragment//相机功能实现者

    /**
     * 简化功能调用，复杂功能直接使用cameraHolder或cameraXFragment
     */
    val cameraXF: CameraXF by lazy { CameraXF(cameraXFragment) }//
}

class CameraExampleActivity : BaseCameraXActivity() {

    /**
     * 这里直接构建了配置，是否使用人脸检测使用了使用intent传入boolean值。
     */
    override fun configAll(intent: Intent): ManagerConfig {
        val useImageDetection = intent.getBooleanExtra(ImageDetection, false)
        //视频录制配置(可选)
        val videoRecordConfig = VideoRecordConfig(
            quality = CameraRecordQuality.HD,//设置视频拍摄质量
//            asPersistentRecording = true,//实验特性，保持长时间录制
//            fileSizeLimit=5.mb, //文件大限制,单位bytes
//            durationLimitMillis =1000*15, //录制时长限制，单位毫秒
        )
        //拍照配置(可选)
        val imageCaptureConfig = ImageCaptureConfig()
        //整体的配置
        return ManagerConfig().apply {
            this.recordConfig = videoRecordConfig
            this.captureMode =
                if (useImageDetection) CaptureMode.imageAnalysis else CaptureMode.takePhoto
            this.flashMode = FlashModel.CAMERA_FLASH_AUTO
            this.size = Size(1920, 1080)//拍照，预览的分辨率，期望值，不一定会用这个值
        }
    }

    override fun closeActivity(shouldInvokeFinish: Boolean) {
        cameraXF.stopTakeVideo(0)

        if (shouldInvokeFinish) {
            mBaseHandler.postDelayed(Runnable {
                this.finish()
            }, 500)
        }
    }

    override fun cameraHolderInitStart(cameraHolder: CameraHolder) {
        super.cameraHolderInitStart(cameraHolder)
        val cameraPreview=cameraHolder.cameraPreview
        //生成图像分析器
        val analyzer = FaceContourDetectionProcessor(
            cameraPreview,
            page.graphicOverlayFinder,
        ).also {
            cameraHolder.changeAnalyzer(it)//设置图像分析器
        }
        //监听分析结果
        (analyzer as FaceContourDetectionProcessor).analyzeListener =
            AnalyzeResultListener {
                // when analyze success
            }
    }

    override fun cameraHolderInitFinish(cameraHolder: CameraHolder) {
        super.cameraHolderInitFinish(cameraHolder)
        if (cameraConfig.isUsingImageAnalyzer()) {//使用了图像分析，因此一些界面
            page.cameraControlLayout.visibility = View.INVISIBLE
        }
    }

    /**
     * 调用相机拍照或录像
     */
    fun capture() {
        if (cameraConfig.captureMode == CaptureMode.takePhoto) {
            //拍照
            mBaseHandler.postDelayed(Runnable {
                cameraXF.takePhoto()
            }, 300)
        } else if (cameraConfig.captureMode == CaptureMode.takeVideo) {
            cameraXF.takeVideo()
        }
    }

    /**
     * 调用相机拍摄照片
     */
    fun captureFace() {
            cameraXF.takePhoto()
        /*
        //还可以使用预览画面里的bitmap存储为图片，而不拍照。
        //但这种方式得到的照片不清晰甚至残缺
        mBaseHandler.post {
            val bitmap = cameraXF.provideBitmap()
            if (bitmap != null) {
                // TODO: 存储bitmap
            }
        }*/

    }

    /**
     * 拍完照片
     */
    override fun onPhotoTaken(saveFileData: SaveFileData?) {
        super.onPhotoTaken(saveFileData)
        Log.d("CameraXFragment", "onPhotoTaken： $saveFileData")
        cameraXF.indicateTakePhoto()//拍照闪光
    }

    /**
     * 录完视频
     */
    override fun onVideoRecorded(saveFileData: SaveFileData?) {
        super.onVideoRecorded(saveFileData)
        saveFileData?.let {
            Log.d(TAG, "onVideoRecorded: $it")
        }
    }
    
    //注：SaveFileData类
 * 描述文件存储位置
 * 若使用了mediastore，path为空，uri为content开头
 * 若使用文件，path不为空，uri为file开头
 * 若使用SAF，path为空，uri为content开头
}
```

## 其他介绍

* `CameraXFragment`实现` ICameraXF`接口，对外提供各种相机方法，实际上各类相机操作实现是由内部的`cameraHolder`实现。
* `ICameraXF`接口则是为了屏蔽fragment的相关方法
* `CameraXFragment`内部创建`CameraHolder`

```
class CameraXFragment : Fragment(), ICameraXF {
........

//相机  
 cameraHolder = CameraHolder(
            page.cameraPreview,
            cameraConfig,
            cameraManagerListener = this,
        ).apply {
            eventListener?.cameraHolderInitStart(this)//相机初始化开始
            //拍照或录像结果监听接口
            this@CameraXFragment.captureResultListener?.let {
                this.captureResultListener=it
            }
            bindLifecycle(requireActivity())//非常重要，绝对不能漏了绑定生命周期
        }
```

* `BaseCameraXActivity`

  持有`CameraXFragment`实现相机功能，并提供了额外的一些功能。

```kotlin
abstract class BaseCameraXActivity : BasicActivity(),
    CameraXFragmentEventListener, CaptureResultListener {
    //.....
    //初始化CameraFragment,提供相机操作
    private fun setCameraFragment() {
        cameraXFragment = CameraXFragment.newInstance(
            cameraConfig,
            eventListener = this, //1. 设置相机事件监听
            captureResultListener = this//2. 拍照录视频操作结果通知回调
        )
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, cameraXFragment).commit()
    }
    //.....
}
```

上方代码1处，CameraXFragmentEventListener 中包含了关于相机的一些回调，

````kotlin
interface CameraXFragmentEventListener {
    /**
     * 开始初始化CameraHolder，此时处于绑定生命周期之前. 触发时机早于[CameraManagerEventListener.initCameraStart]
     */
    fun cameraHolderInitStart(cameraHolder: CameraHolder)

    /**
     * cameraXHolder初始化完成 触发时机晚于[CameraManagerEventListener.initCameraFinished]
     */
    fun cameraHolderInitFinish(cameraHolder: CameraHolder)

    /**
     * 相机预览数据开始
     */
    fun cameraPreviewStreamStart() {}

    /**
     * 切换前置或后置摄像头
     */
    fun switchCamera(lensFacing: Int) {}

    /**
     * 设备旋转，对坐标做转换
     */
    fun cameraRotationChanged(rotation: Int, angle: Int) {}
}
````

上方代码2处,CaptureResultListener 拍照录视频操作结果通知回调

```kotlin
interface CaptureResultListener {

    //Called when the video record is finished and saved
    fun onVideoRecorded(fileMetaData: FileMetaData?)

    //called when the photo is taken and saved
    fun onPhotoTaken(filePath: Uri?)

}
```

## 人脸检测

### google ml-kit教程：
#### ml-kit的检测器
* 定义一个类，在类里加载ml-kit的检测器，然后，提供图像即可进行处理，本示例不包含连接到相机分析流部分

````kotlin
val process=BaseImageAnalyzer()
//传入bitmap调用分析
process.processBitmap(bitmap){list->
    
}
//这个类里维护了ml-kit 分析器
class BaseImageAnalyzer {
    private val executor = ScopedExecutor(TaskExecutors.MAIN_THREAD)
    //ml-kit的人脸检测器
    private val detector: FaceDetector

    init {
        //初始化检测器
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)//在检测人脸时更注重速度还是准确性，精确模式会检测到比快速模式更少的人脸
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)//轮廓检测
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)//面部特征点
            //.setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)//是否将人脸分为不同类别（例如“微笑”和“眼睛睁开”）。
            .setMinFaceSize(1.0f)//人脸最小占图片的百分比
            //.enableTracking() //disable when contour is enable https://developers.google.com/ml-kit/vision/face-detection/android
            .build()

        detector = FaceDetection.getClient(options)
        Log.v(TAG, "Face detector options: $options")

    }

    fun stop() {
        detector.close()
    }
    //使用检测器开始处理图片
    fun processBitmap(bitmap: Bitmap, listener: OnSuccessListener<List<Face>>) {
      detector.process(InputImage.fromBitmap(bitmap, 0))
            .addOnSuccessListener(executor, listener)
            .addOnFailureListener(
                executor,
                OnFailureListener { e: Exception ->
                    val error = "Failed to process. Error: " + e.localizedMessage
                    Log.d(TAG, error)
                    e.printStackTrace()
                }
            )
    }
}

````
#### 连接到相机分析流
需要使类继承自ImageAnalysis.Analyzer，重写ImageAnalysis.Analyzer 的analyze方法，将此类作为analyzer usecase 绑定到相机后，相机自动调用其analyze方法，提供分析数据
* camerax绑定用例示例(在CameraXManager类中，不需要手动调用)：
```kotlin
cameraProvider.bindToLifecycle(lifeOwner, cameraSelector, preview, imageAnalyzer)
```
* 继承ImageAnalysis.Analyzer，并调用ml-kit示例：
继承ImageAnalysis.Analyzer，在analyze方法中调用ml-kit的检测器，将相机数据交给ml-kit做人脸检测，检测器部分和上面是一样的。
```kotlin
class BaseImageAnalyzer : ImageAnalysis.Analyzer {
  private val executor = ScopedExecutor(TaskExecutors.MAIN_THREAD)
  //mlkit的人脸检测器
  private val detector: FaceDetector
    //...省略初始化detector和其他东西
  
  //重写ImageAnalysis.Analyzer 的analyze方法
  @SuppressLint("UnsafeExperimentalUsageError", "UnsafeOptInUsageError")
  override fun analyze(imageProxy: ImageProxy) {
    val mediaImage = imageProxy.image
    val rotationDegrees = imageProxy.imageInfo.rotationDegrees

    mediaImage?.let {
      //检测
      detector.detectInImage(InputImage.fromMediaImage(it, rotationDegrees))
        .addOnSuccessListener { results ->
          onSuccess(
            imageProxy,
            results,
            graphicOverlay,
          )
        }
        .addOnFailureListener {
          graphicOverlay.clear()
          graphicOverlay.postInvalidate()
          onFailure(it)
        }
        .addOnCompleteListener {
          imageProxy.close()
        }
    }
  }
}
```
### 本库中ml-kit使用：
* 使用google ml-kit 并连接到相机分析流示例：
还是以CameraExampleActivity 为例：

1. 需要在配置相机时，指定模式为CaptureMode.imageAnalysis
````kotlin
    override fun configAll(intent: Intent): ManagerConfig {
        //....省略
    return ManagerConfig().apply {
        this.captureMode = CaptureMode.imageAnalysis//设置为分析图像模式
        this.size = Size(1920, 1080)//拍照，预览的分辨率，期望值，不一定会用这个值
  }
}

````

2. 在cameraHolderInitStart方法中，调用 cameraHolder.changeAnalyzer()设置图像分析器，这样即可完成上面所说将分析器绑定到相机

* 示例1 使用ml-kit处理图像，绘制人脸边框和“特征点”的连线
   FaceContourDetectionProcessor继承自BaseImageAnalyzer，实现了绘制人脸图像框体，点位连线等的内容
```kotlin
    override fun cameraHolderInitStart(cameraHolder: CameraHolder) {
        super.cameraHolderInitStart(cameraHolder)
        val cameraPreview = cameraHolder.cameraPreview
  
        //使用mlkit进行人脸检测，并绘制人脸框体和点位
        val analyzer = FaceContourDetectionProcessor(
            cameraPreview,
            page.graphicOverlayFinder,
        ).also {
            cameraHolder.changeAnalyzer(it)//设置图像分析器
        }
        //监听分析结果
        (analyzer as FaceContourDetectionProcessor).analyzeListener =
            AnalyzeResultListener {
                // when analyze success
            }
    }

```

* 示例2 还是使用ml-kit处理图像，得到包含人脸信息的照片，然后将其交给tensorflow lite模型处理，得到面部特征点。
```kotlin
    override fun cameraHolderInitStart(cameraHolder: CameraHolder) {
        super.cameraHolderInitStart(cameraHolder)
        //加载tensorflow lite模型，这里仅做演示
        val model = FaceDetection.create(
            this.assets,
            TestFileDecActivity.TF_OD_API_MODEL_FILE,
            TestFileDecActivity.TF_OD_API_LABELS_FILE,
            TestFileDecActivity.TF_OD_API_IS_QUANTIZED
        )
        //TensorFlowLink继承自ImageAnalysis.Analyzer，将其作为分析器设置给相机即可拿到分析流
        val tensorFlowLink = TensorFlowLink {image: ImageProxy ->
            //获取图像
            val bitmap= image.toBitmap()
            //这里使用了ml-kit分析是否包含面部数据，如果包含，则将面部图像裁剪下来
            MyFileProcessor.process(bitmap){
                it?.let {
                    //将裁剪后的面部图像转换成特定尺寸bitmap
                    val tmp = FaceDetection.convertBitmap(it)
                    //将处理好的面部图像交给模型处理，获取特征点
                    val masks = model.detectionBitmap(tmp)
                }
            }
           
        }.also {
            cameraHolder.changeAnalyzer(it)//设置图像分析器
        }

    }

```
### 除使用相机的分析流之外，还可以手动获取相机图片进行分析
示例：CameraExampleActivity文件中
```kotlin
  /**
     * 每隔20ms从预览视图中获取bitmap
     * 然后运行图像分析，绘制矩形框
     * 但是这种方式分析图象后，绘制框体会有延迟、卡顿感，不如直接使用图像分析流畅
     */
    suspend fun runFaceDetection(interval: Long = 20L) {
        if (cameraConfig.isUsingImageAnalyzer() || stopAnalyzer) {
            Log.d(TAG, "runFaceDetection: 已使用图像分析或stopAnalyzer==true")
            return
        } else {
            BitmapProcessor.analyzeListener = AnalyzeResultListener {
                // when analyze success
            }
            flow<Boolean> {
                while (true) {
                    delay(interval)
                    emit(stopAnalyzer)
                    if (stopAnalyzer) {
                        break
                    }
                }
            }.collect {
                cameraXF.provideBitmap()?.let { originalBitmap ->
                    //识别图像
                    BitmapProcessor.process(originalBitmap) { faces: List<Face> ->
                        //上面依据识别成功，得到了返回数据，我们在这里调用了一个普通方法来使用识别出来的数据
                        BitmapProcessor.onSuccess(faces, page.graphicOverlayFinder)
                    }
                }

            }
        }
    }
```

## 面部识别，特征点计算

> 来源

> [文章链接](https://medium.com/@estebanuri/real-time-face-recognition-with-android-tensorflow-lite-14e9c6cc53a5)
* TestFileDecActivity
* 若需要连接到相机分析流，请看上面章节
* 加载tensorflow lite模型，运行检测，请看`FaceDetection.kt`文件
```
//使用TensorFlow Lite 模型的处理器
private val model = FaceDetection.create(
        this.assets,
        TF_OD_API_MODEL_FILE,
        TF_OD_API_LABELS_FILE,
        TF_OD_API_IS_QUANTIZED
   )
StoreX.with(this).safHelper.selectFile(fileType = "image/*") { uri ->
            //选择图片后经过mlkit的处理，以及MyFileProcessor中的裁剪，得到只有面部区域的bitmap
            MyFileProcessor.process(contentResolver, uri) {
                //处理bitmap,获取面部特征点
                it?.let { it1 ->
                    //将bitmap转换成特定尺寸bitmap
                    val tmp = FaceDetection.convertBitmap(it1)
                    //获取特征点
                    val masks = model.detectionBitmap(tmp)
                }
            }
        }
```

# CameraButton

此按钮控件用于拍照和录像，支持点击拍照，点击录像和长按录像，长按录制有动画

支持设置录制时长，到达录制时长后回调通知结束录制。

可以设置按钮仅支持拍照，仅支持录制，或都支持

1. 在开启长按录制时，点击录制将不可用
2. 开启点击录制时，拍照不可用

布局文件示例

```
<com.kiylx.camerax_lib.main.buttons.CameraButton
    android:id="@+id/full_capture_btn"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    app:layout_constraintBottom_toBottomOf="parent"
    app:layout_constraintLeft_toLeftOf="parent"
    app:layout_constraintRight_toRightOf="parent"
    app:buttonMode="both" //指定可拍照，可录制
    app:longPassRecord="true" //开启长按录制
    app:layout_constraintTop_toTopOf="parent"
    app:size="80" />
    
    属性：
    buttonMode 可选值：only_capture，only_record，both
    maxDuration 秒
    longPassRecord 是否可长按录制
```

使用示例

```
page.fullCaptureBtn.setCaptureListener(object : DefaultCaptureListener(){
	//拍照
    override fun takePictures() {
        cameraXF.takePhoto()
    }
    //开始录制视频
    override fun recordStart() {
        page.captureVideoBtn.visibility = View.GONE
        LogUtils.dTag("录制activity", "开始")
        cameraXF.startRecord()
        //录制视频时隐藏摄像头切换
        page.switchBtn.visibility=View.GONE
    }

    //录制视频到达预定的时长，可以结束了
    override fun recordShouldEnd(time: Long) {
        page.captureVideoBtn.visibility = View.VISIBLE
        LogUtils.dTag("录制activity", "停止")
        cameraXF.stopRecord(time)
        page.switchBtn.visibility=View.VISIBLE
    }
})
```

# tensorflow lite

## 依赖

可以不使用下面这个教程上的依赖

```
implementation 'org.tensorflow:tensorflow-lite:0.0.0-nightly'
```

可以引入

```
implementation 'org.tensorflow:tensorflow-lite-api:2.9.0'
implementation 'org.tensorflow:tensorflow-lite:2.9.0'
```

参考

https://jarcasting.com/artifacts/org.tensorflow/tensorflow-lite/

https://discuss.tensorflow.org/t/tensorflow-lite-aar-2-9-0-android-integration/11796

* 如果需要更简便的使用方式，可以同时引入`TensorFlow Lite Task Library`

详情参考https://tensorflow.google.cn/lite/inference_with_metadata/overview?hl=zh-cn

## 工具类 DataSize，使用时数字加上".单位"
[来源](https://github.com/forJrking/KotlinSkillsUpgrade/blob/main/kup/src/main/java/com/example/kup/DataSize.kt)
简化单位换算
例如：
```kotlin
//原来的方式
val tenMegabytes = 10 * 1024 * 1024//10mb
//简化后，会自动换算为bytes
val tenMegabytes =10.mb
//其他例子：
    100.mb
    100.kb
    100.gb
    100.bytes
```
