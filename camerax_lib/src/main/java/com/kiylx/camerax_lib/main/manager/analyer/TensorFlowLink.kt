package com.kiylx.camerax_lib.main.manager.analyer

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy

class TensorFlowLink(val func: (image: ImageProxy) -> Unit) : ImageAnalysis.Analyzer {

    override fun analyze(image: ImageProxy) {
        func.invoke(image)
    }
}