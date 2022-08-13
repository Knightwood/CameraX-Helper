package com.kiylx.camerax_lib.main.manager.model;

import android.net.Uri
import com.kiylx.camerax_lib.main.store.FileMetaData

/**
 * 拍照，视频后的回调，fragment实现它，把结果传给外界
 */
interface CaptureResultListener {

    //Called when the video record is finished and saved
    fun onVideoRecorded(fileMetaData: FileMetaData?);

    //called when the photo is taken and saved
    fun  onPhotoTaken(filePath:Uri? );

}
