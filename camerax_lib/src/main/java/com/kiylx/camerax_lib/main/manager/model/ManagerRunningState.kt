package com.kiylx.camerax_lib.main.manager.model

/** 指示manager运行时的状态 */
object ManagerRunningState {
    const val IDLE = 0 //空闲
    const val TAKING_PHOTO = 1 //正在拍照
    const val RECORDING = 2//正在录像
    const val IMAGE_ANALYZING = 3//正在分析图像
    const val CUSTOM_USE_CASE_GROUP_RUNNING = 4//自定义用例组合正在运行

}

/** camerax此时绑定了什么实例 */
enum class WhichInstanceBind {
    CUSTOM_USE_CASE_GROUP,//自定义用例组合
    NONE,//什么都没有绑定
    IMAGE_ANALYZER,//绑定了图像识别
    PICTURE,//绑定了拍照
    VIDEO//绑定了录像
}