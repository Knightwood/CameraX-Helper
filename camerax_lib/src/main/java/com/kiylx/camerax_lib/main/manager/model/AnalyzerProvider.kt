package com.kiylx.camerax_lib.main.manager.model

import androidx.camera.core.ImageAnalysis

/**
 * 根据分析器类型，提供不同的分析器
 */
interface AnalyzerProvider {
    fun provider(verType: VisionType): ImageAnalysis.Analyzer
}