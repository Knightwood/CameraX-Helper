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

    //1. 录制视频到达预定的时长结束
    //2. 或者手动按下按钮录制结束
    override fun recordShouldEnd(time: Long) {
        page.captureVideoBtn.visibility = View.VISIBLE
        LogUtils.dTag("录制activity", "停止")
        cameraXF.stopRecord(time)
        page.switchBtn.visibility=View.VISIBLE
    }
})
```