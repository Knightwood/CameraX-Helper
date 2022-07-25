# CameraLlib
集成了拍照，录制视频，人脸识别等的camerax库。

内置了人脸识别，并预留出来了改变分析器使用其他图像分析的方法。

`NewCameraXFragment`实现` CameraCommon`接口，对外提供各种相机方法，实际上各类实现是由`cameraHolder`实现。

* `NewCameraXFragment`内部创建`CameraHolder`

```
        cameraHolder = CameraHolder(
            page.cameraPreview,
            page.graphicOverlayFinder,
            cameraConfig,
            page.root,
            this.captureResultListener
        ).apply {
            bindLifecycle(requireActivity())//非常重要，绝对不能漏了绑定生命周期
//使用方式 示例代码：
//            analyzerProvider=object :AnalyzerProvider{
//                override fun provider(verType: VisionType): ImageAnalysis.Analyzer {
//                    TODO("在这里可以提供其他类型的图像识别器")
//                }
//            }
        }
        //使用changeAnalyzer方法改变camerax使用的图像识别器
        // cameraHolder.changeAnalyzer(VisionType.Barcode)
        eventListener?.cameraHolderInited(cameraHolder)//通知外界holder初始化完成了，可以对holder做其他操作了
```

* `BaseCameraXActivity`

  持有`NewCameraXFragment`实现相机功能，并提供了额外的功能。

  初始化`NewCameraXFragment`要遵循代码中的三条初始化顺序。

```
  private fun setCameraFragment() {
          cameraXFragment = NewCameraXFragment.newInstance(cameraConfig)
              .apply {
                  eventListener = object : CameraXFragmentEventListener {
                      override fun cameraHolderInited(cameraHolder: CameraHolder) {
                      //1. holder初始化完成
                          setCameraEventListener(object : CameraEventListener {
                              //2. holder初始化完成后，相机也初始化完成了
                              override fun initCameraFinished() {
                                  this@BaseCameraXActivity.initCameraFinished()
                                  //3. 初始化其他内容
                              }
                          })
                          setAnalyzerResultListener(object : AnalyzeResultListener {
                              //图像分析成功时，例如调用拍照
                              override fun isSuccess() {
                                  captureFace()
                              }
                          })
                          //拍照录视频操作结果通知回调
                          setCaptureResultListener(object : CaptureResultListener {
                              override fun onVideoRecorded(filePath: String) {
                                  Log.d("CameraXFragment", "onVideoRecorded：$filePath")
                                  // 视频拍摄后
  
                              }
  
                              override fun onPhotoTaken(filePath: String) {
                                  Log.d("CameraXFragment", "onPhotoTaken： $filePath")
                                  //图片拍摄后
  
                              }
                          })
                      }
  
                  }
              }
          supportFragmentManager.beginTransaction()
              .replace(R.id.fragment_container, cameraXFragment).commit()
      }
```

  

示例代码在app目录下。

