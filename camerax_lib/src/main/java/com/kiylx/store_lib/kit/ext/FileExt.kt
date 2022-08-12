package com.kiylx.store_lib.kit.ext

import android.database.Cursor
import android.net.Uri
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import androidx.fragment.app.FragmentActivity
import com.kiylx.store_lib.kit.input
import com.kiylx.store_lib.kit.output
import java.text.SimpleDateFormat
import java.util.*

class FileDescriptorMode {
    companion object {
        const val onlyRead = "r"
        const val onlyWrite = "w"

        // "rw" :读和写
        const val readAndWrite = "rw"

        // "rwt" :截断或覆盖现有文件内容。
        const val overwriting = "rwt"

        /**
         *“a”表示文件应该以追加模式打开，相当于OsConstants.O_APPEND
         * 在每次写入之前，文件偏移量位于文件末尾。
         */
        const val append = "wa"

        /**
         *“t”表示文件应该以截断模式打开，相当于OsConstants.O_TRUNC。
         *如果文件已经存在并且是常规文件并且被打开以进行写入，它将被截断为长度 0。
         */
        const val truncate = "wt"

    }
}

/**
 * 生成一个以日期命名的文件名
 */
fun generateFileName(): String {
    val fileName = SimpleDateFormat("yyyyMMddhhmmss", Locale.getDefault()).format(Date())
    return fileName
}

/**
 * 打开uri指定的fd。
 * 可以自动关闭流，不要再自己调用close之类的方法
 *
 * @param mode [FileDescriptorMode]
 * @param cancellationSignal 取消正在进行的操作的信号，如果没有，则为 null。的操作被取消，然后 OperationCanceledException 将被抛出。
 */
fun FragmentActivity.openFD(
    uri: Uri,
    mode: String,
    cancellationSignal: CancellationSignal? = null,
    block: (pfd: ParcelFileDescriptor?) -> Unit,
) {
    val f: ParcelFileDescriptor? = contentResolver.openFileDescriptor(
        uri,
        mode,
        cancellationSignal
    )
    f.use(block)
}


fun FragmentActivity.readFileFromUri(uri: Uri, input: input) {
    val ins = contentResolver.openInputStream(uri)
    ins.runSafelyNoNull(input)
}

fun FragmentActivity.writeFileFromUri(uri: Uri, output: output) {
    val ons = contentResolver.openOutputStream(uri)
    ons.runSafelyNoNull(output)
}

/**
 * 遍历cursor:
 * 如果judge返回true，将会终止遍历,并对cursor调用close()
 * 如果judge永远不返回true,将会在最后一个cursor被遍历后，结束遍历，并调用close()
 *
 * @param judge (cursor: Cursor) -> Boolean ，遍历时提供这次cursor，如果返回false，将会终止遍历操作
 */
fun Cursor.filter(judge: (cursor: Cursor) -> Boolean) {
    while (this.moveToNext()) {
        val b = judge(this)
        if (b) {
            break
        }
    }
    if (!this.isClosed)
        this.close()
}
