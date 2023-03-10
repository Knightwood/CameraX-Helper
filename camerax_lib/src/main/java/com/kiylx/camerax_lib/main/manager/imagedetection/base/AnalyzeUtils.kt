package com.kiylx.camerax_lib.main.manager.imagedetection.base

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy

class AnalyzeUtils {
    companion object{
        fun emptyAnalyzer()=object : ImageAnalysis.Analyzer {
            override fun analyze(image: ImageProxy) {}
        }
    }
}