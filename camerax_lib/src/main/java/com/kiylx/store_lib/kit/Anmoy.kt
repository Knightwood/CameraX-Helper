package com.kiylx.store_lib.kit

import android.net.Uri
import java.io.InputStream
import java.io.OutputStream

/**
 * 通知处理结果
 * 用true或false表示结果
 */
typealias fileProcessResult = (result: Boolean) -> Unit
/**
 * 通知文件的路径uri
 */
typealias uriResult = (uri: Uri?) -> Unit
/**
 * 通知文件的路径uri
 */
typealias noNullUriResult = (uri: Uri) -> Unit

typealias input = (inputStream: InputStream) -> Unit

typealias output = (outputStream: OutputStream) -> Unit