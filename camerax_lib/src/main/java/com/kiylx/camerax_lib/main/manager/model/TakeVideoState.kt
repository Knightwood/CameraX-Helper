package com.kiylx.camerax_lib.main.manager.model

/**
 * 指示manager运行时的状态
 */
object TakeVideoState {
    const val none = 0
    const val takePhoto = 1
    const val takeVideo = 2
    const val imageDetection = 3
}

/**
 * camerax此时绑定了什么实例
 */
enum class WhichInstanceBind{
    NONE,//什么都没有绑定
    IMAGE_DETECTION,//绑定了图像识别
    PICTURE,//绑定了拍照
    VIDEO//绑定了录像
}