package com.kiylx.camerax_lib.main.manager.model

/**
 * manager中产生了某些事件，通知给外界
 */
interface CameraEventListener{

    fun initCameraFinished()

    fun switchCamera(lensFacing: Int){}
}