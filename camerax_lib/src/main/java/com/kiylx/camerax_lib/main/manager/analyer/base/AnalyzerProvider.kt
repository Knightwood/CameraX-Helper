package com.kiylx.camerax_lib.main.manager.analyer.base

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy


object AnalyzerProvider {
    val emptyAnalyzer=object : ImageAnalysis.Analyzer {
        override fun analyze(image: ImageProxy) {}
    }
}