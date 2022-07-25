package com.kiylx.camerax_lib.main

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ImageAnalysis
import androidx.lifecycle.LifecycleOwner

interface CameraListener {
    fun initCameraFinished()

    fun getAnalyzer(): ImageAnalysis.Analyzer

    fun switchCamera(lensFacing: Int){}
}