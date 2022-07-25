package com.kiylx.camerax_lib.main.manager.model;

/**
 * 拍照，视频后的回调，fragment实现它，把结果传给外界
 */
interface CaptureResultListener {

    //Called when the video record is finished and saved
    fun onVideoRecorded(filePath:String);

    //called when the photo is taken and saved
    fun  onPhotoTaken(filePath:String );

}
