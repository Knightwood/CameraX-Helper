package com.kiylx.camerax_lib.main.manager.model

import com.kiylx.camerax_lib.main.manager.CameraXManager

/**
 * manager中产生了某些事件，通知给外层
 */
interface CameraManagerEventListener{
    /**
     * manager在绑定生命周期后，触发生命周期，开始执行初始化
     */
    fun initCameraStart(cameraXManager: CameraXManager){}

    /**
     * manager初始化完成
     */
    fun initCameraFinished(cameraXManager: CameraXManager){}

    fun switchCamera(lensFacing: Int){}

    /**
     * 相机的预览数据开始
     */
    fun previewStreamStart(){}

    /**
     * 拍照后
     */
    fun photoTaken() {}
}