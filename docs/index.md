# 基本使用


!!! note "前言"
        请仔细阅读本文档

## 基本配置

### 屏幕方向及销毁重建

对于屏幕方向，本库内部已经实现了根据当前手机屏幕方向决定拍摄出来图像的方向。  
如果不固定屏幕方向，对于相机的布局页面，你可能需要在竖屏和横屏下使用不同的布局。  
除了屏幕方向，还有在屏幕方向发生变化时，相机也会销毁重建，为了避免不必要的性能开销，需要设置一下`configChanges`，使其不会发生销毁重建。  
[屏幕方向相关文档](https://developer.android.google.cn/media/camera/camerax/orientation-rotation?hl=en)


相机本身的坐标系是与屏幕不同的，图像分析得到的坐标值需要经过转换才可以绘制到屏幕上，否则会有偏差。  
[坐标转换相关文档](https://developer.android.google.cn/media/camera/camerax/transform-output?hl=en)

1. 屏幕方向配置，例如固定竖屏、横屏、不设置等。
```xml

<activity 
        android:screenOrientation="portrait"/>
```
2. 屏幕旋转会涉及到资源的销毁重建，如果上一步选择不固定屏幕方向，最好设置一下`configChanges`.
```xml
<activity 
        android:configChanges="orientation|screenSize"/>
```
### 示例相机
最基础的相机实现类是`CameraXFragment`，其实现了最基本的相机预览，并持有CameraHolder，实现所有的相机功能。  
`BaseCameraXActivity`则持有`CameraXFragment`，并实现了一个基本的相机界面，例如闪光灯、点击对焦，拍照、录像等功能。  
`BaseCameraXFragment`继承自`CameraXFragment`，其与`BaseCameraXActivity`一样，实现了基本的相机界面和功能。  
因此，你可以自行选择使用`BaseCameraXActivity`、`BaseCameraXFragment`实现相机功能， 也可以直接使用`CameraXFragment`实现进一步的定制。
如果`CameraXFragment`也无法满足需求，可以参照`CameraXFragment`，使用`CameraHolder`类实现更深层次的定制。  

`app` module下的`CameraExampleActivity`和`CameraExampleActivity2`则是两个示例，你可以参考它们。
