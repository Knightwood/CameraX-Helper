# 基本使用

目前可以直接直接引入，后续会构建出aar包方便使用。
此存储库有如下模块

````kotlin
include(":app") // 示例
include(":camerax_lib")// 相机库，实现拍照，录像，图像分析接口等
include(":compose_camerax") // compose版相机库
include(":camerax_analyzer") // 集成mlkit，绘制人脸坐标，分析亮度等
include(":camerax_analyzer_tensorflow") // 集成TensorFlowList，使用tflite模型运行分析
````

其中，`camerax_analyzer`和`camerax_analyzer_tensorflow`都依赖`camerax_lib` module。  
`compose_camerax` module还未进行开发。

使用：

1. 克隆代码到本地
```shell
  git clone git@github.com:Knightwood/CameraX-mlkit-FaceDetection.git
```
2. 引入依赖
   在AndroidStudio中，File->New->Import Module...  
   将`camerax_lib`、`camerax_analyzer`、`camerax_analyzer_tensorflow`三个module导入到项目中。  
   如果不需要mlkit或tensorflow，可以不导入`camerax_analyzer`、`camerax_analyzer_tensorflow` 这两个module。  
3. app module的build.gradle文件添加依赖
```kotlin
dependencies {
    implementation(project(":camerax_lib"))
    implementation(project(":camerax_analyzer")) //可选
    implementation(project(":camerax_analyzer_tensorflow")) //可选
}
```
## 屏幕方向及销毁重建

对于屏幕方向，本库内部已经实现了根据当前手机屏幕方向决定拍摄出来图像的方向。  
如果不固定屏幕方向，对于相机的布局页面，你可能需要在竖屏和横屏下使用不同的布局。  
除了屏幕方向，还有在屏幕方向发生变化时，相机也会销毁重建，为了避免不必要的性能开销，需要设置一下`configChanges`
，使其不会发生销毁重建。  
[屏幕方向相关文档](https://developer.android.google.cn/media/camera/camerax/orientation-rotation?hl=en)

相机本身的坐标系是与屏幕不同的，图像分析得到的坐标值需要经过转换才可以绘制到屏幕上，否则会有偏差。  
[坐标转换相关文档](https://developer.android.google.cn/media/camera/camerax/transform-output?hl=en)

1. 屏幕方向配置，例如固定竖屏、横屏、不设置等。
```xml

<activity android:screenOrientation="portrait" />
```
2. 屏幕旋转会涉及到资源的销毁重建，如果上一步选择不固定屏幕方向，最好设置一下`configChanges`.
```xml

<activity android:configChanges="orientation|screenSize" />
```

## 示例相机

`app` module下的`CameraExampleActivity`和`CameraExampleActivity2`  
则是两个示例，分别使用了`BaseCameraXActivity`和`BaseCameraXFragment`实现相机，你可以将他们复制到你的项目中使用
如何进行相机配置，在这两个实例中也有足够的解释。

如果没有更进一步的需求，只知道这四个方法足够了。即使不进行存储方面的配置，也会正常运行，会自动存入相册文件夹。

| 函数                     | 描述             |
|------------------------|----------------|
| cameraHolderInitStart  | 这个会在相机初始化之前被调用 |
| cameraHolderInitFinish | 这个会在相机初始化之后被调用 |             
| onPhotoTaken           | 照片拍摄完成         |
| onVideoRecorded        | 视频录制完成         |

同时呢，如果给相机配置图像分析，则需要在`cameraHolderInitStart`中配置。
```kotlin

    /** 拍完照片 */
    override fun onPhotoTaken(saveFileData: SaveFileData?) {
        super.onPhotoTaken(saveFileData)
        Log.d("CameraXFragment", "onPhotoTaken： $saveFileData")
        cameraXF.indicateTakePhoto()//拍照闪光
    }
    
    /** 录完视频 */
    override fun onVideoRecorded(saveFileData: SaveFileData?) {
        super.onVideoRecorded(saveFileData)
        saveFileData?.let {
            Log.d(TAG, "onVideoRecorded: $it")
        }
    }

     //FaceContourDetectionProcessor是ImageAnalysis.Analyzer的实现
     var analyzer: FaceContourDetectionProcessor? = null

    /**
     * Camera holder init start
     * 此函数会在相机初始化之前回调，所以适合在这里初始化图像分析
     *
     * @param cameraHolder
     */
    override fun cameraHolderInitStart(cameraHolder: CameraHolder) {
        super.cameraHolderInitStart(cameraHolder)
        val cameraPreview = cameraHolder.cameraPreview

        //示例1
        //生成图像分析器
        analyzer = FaceContourDetectionProcessor(
            cameraPreview,
            findViewById<GraphicOverlayView>(R.id.graphicOverlay_finder),
        )
        setUpAnalyzer(analyzer)//设置分析器
        //监听分析结果
        (analyzer as FaceContourDetectionProcessor).analyzeListener =
            AnalyzeResultListener {
                // when analyze success
            }

        //示例2
        //加载tensorflow lite模型，这里仅做演示
//        val model = FaceDetection.create(
//            this.assets,
//            TestFileDecActivity.TF_OD_API_MODEL_FILE,
//            TestFileDecActivity.TF_OD_API_LABELS_FILE,
//            TestFileDecActivity.TF_OD_API_IS_QUANTIZED
//        )
//        //TensorFlowLink继承自ImageAnalysis.Analyzer，将其作为分析器设置给相机即可拿到分析流
//        val tensorFlowLink = TensorFlowLink {image: ImageProxy ->
//            //获取图像
//            val bitmap= image.toBitmap()
//            //这里使用了mlkit分析是否包含面部数据，如果包含，则将面部图像裁剪下来
//            MyFileProcessor.process(bitmap){
//                it?.let {
//                    //将裁剪后的面部图像转换成特定尺寸bitmap
//                    val tmp = FaceDetection.convertBitmap(it)
//                    //将处理好的面部图像交给模型处理，获取特征点
//                    val masks = model.detectionBitmap(tmp)
//                }
//            }
//
//        }
//        setUpAnalyzer(tensorFlowLink)//设置分析器

    }
```
图片

| ![](.\assert\1.jpg) | ![](.\assert\2.jpg) |
|---------------------|---------------------|

## 自定义相机

最基础的相机实现类是`CameraXFragment`
，其实现了最基本的相机预览，并持有CameraHolder，实现所有的相机功能。  
`BaseCameraXActivity`则持有`CameraXFragment`
，并实现了一个基本的相机界面，例如闪光灯、点击对焦，拍照、录像等功能。  
`BaseCameraXFragment`继承自`CameraXFragment`，其与`BaseCameraXActivity`
一样，实现了基本的相机界面和功能。  
因此，你可以自行选择使用`BaseCameraXActivity`、`BaseCameraXFragment`实现相机功能，
也可以直接使用`CameraXFragment`实现进一步的定制。
如果`CameraXFragment`也无法满足需求，可以参照`CameraXFragment`，使用`CameraHolder`类实现更深层次的定制。

## 相机配置

!!! note
    详细内容请点击查看左侧目录菜单

配置分为了相机的初始化配置，存储配置两部分。  
存储方面已适配Android10以上，且配置简单。

* 相机的初始化配置中包括了如下内容：
  是否开启闪光灯  
  启动后使用何种用例，例如启动后使用拍照、录像或是图像分析等。  
  拍照配置->例如是否水平翻转，垂直翻转、位置信息、图像质量、是否使用零快门延迟等  
  录制配置->文件尺寸限制，拍摄质量、视频编码比特率、镜像设置等

* 存储配置中包括了如下内容：  
  使用CameraXStoreConfig类配置拍摄和录像的存储位置和存储方式  
  例如存储在公共目录、存储在私有目录中、其他目录等。    
  使用file api存储，使用saf存储，使用MediaStore存储  
