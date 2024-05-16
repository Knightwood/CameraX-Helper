package com.kiylx.camerax_lib.main.manager.model

import com.kiylx.camerax_lib.main.manager.CameraXManager

/**
 * camera manager 初始化相机各项内容，将各项事件通知外层
 */
interface CameraManagerEventListener {
    /**
     * manager在绑定生命周期后，在camerax初始化并绑定用例之前
     */
    fun initCameraStart(cameraXManager: CameraXManager) {}

    /**
     * manager在绑定生命周期后，
     * 调用cameraProviderFuture.addListener方法后触发
     * cameraProviderFuture.addListener方法发生在线程池
     */
    fun initCameraStarting(cameraXManager: CameraXManager) {}

    /**
     * manager 触发初始化之后，并且相机初始化完成
     */
    fun initCameraFinished(cameraXManager: CameraXManager) {}

    /**
     *更新当前使用的相机镜头是前置还是后置
     */
    fun switchCamera(lensFacing: Int) {}

    /**
     * 设备旋转，对坐标做转换
     */
    fun cameraRotationChanged(rotation: Int, angle: Int) {}

    /**
     * 相机的预览数据开始
     */
    fun previewStreamStart() {}

}