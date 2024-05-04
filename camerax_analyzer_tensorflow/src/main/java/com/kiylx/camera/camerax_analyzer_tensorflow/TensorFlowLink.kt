package com.kiylx.camera.camerax_analyzer_tensorflow

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy

class TensorFlowLink(val func: (image: ImageProxy) -> Unit) : ImageAnalysis.Analyzer {

    override fun analyze(image: ImageProxy) {
        func.invoke(image)
    }
}