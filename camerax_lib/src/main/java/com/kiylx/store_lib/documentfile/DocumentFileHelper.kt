package com.kiylx.store_lib.documentfile

import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.DocumentsProvider
import androidx.annotation.RequiresApi
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.FragmentActivity
import com.kiylx.store_lib.kit.ext.runSafely
import com.kiylx.store_lib.kit.ext.runSafelyNoNull
import com.kiylx.store_lib.kit.ext.runSafelyNullable
import com.kiylx.store_lib.kit.fileProcessResult
import com.kiylx.store_lib.kit.uriResult


/**
 * @param parentUri 要创建文件的父文件夹uri
 * @param displayName 要创建的文件的名称
 * @param mimeType 文件类型[com.kiylx.store_lib.kit.MimeTypeConsts]
 * @param block(documentFile) 创建文件后给与documentFile，或者创建失败将会抛出异常
 */
fun FragmentActivity.createDocumentFile(
    parentUri: Uri,
    displayName: String,
    mimeType: String,
    block: (file: DocumentFile?) -> Unit,
) {
    val df: DocumentFile? =
        DocumentFile.fromTreeUri(this, parentUri)
    val documentFile = df?.createFile(mimeType, displayName)
    documentFile.runSafelyNullable(block)
}

/**
 * @param parentUri 要创建文件文件夹的父文件夹uri
 * @param displayName 要创建的文件夹的名称
 * @param block(documentFile) 创建文件夹后给与documentFile，或者创建失败给与null
 */
fun FragmentActivity.createDocumentFileDir(
    parentUri: Uri,
    displayName: String,
    block: (file: DocumentFile?) -> Unit,
) {
    val df = DocumentFile.fromTreeUri(this, parentUri)
    val document = df?.createDirectory(displayName)
    document.runSafelyNullable(block)
}


/**
 * @param parentUri 要创建文件的父文件夹uri
 * @param displayName 要创建的文件的名称
 * @param mimeType 文件类型[com.kiylx.store_lib.kit.MimeTypeConsts]
 * @param block(uri) 创建文件后给与uri，或者创建失败将会抛出异常
 */
fun FragmentActivity.createFile(
    parentUri: Uri,
    displayName: String,
    mimeType: String,
    block: (uri: Uri?) -> Unit,
) {
    val uri = DocumentsContract.createDocument(contentResolver, parentUri, mimeType, displayName)
    uri.runSafelyNullable(block)
}

/**
 * 复制文档
 *
 * @param sourceFileUri document with
 *     [android.provider.DocumentsContract.Document.FLAG_SUPPORTS_COPY]
 * @param targetFolderUri document which will become a parent of the source
 *     document's copy.
 * @param block(uri) 被复制文档的uri，如果失败，内部会抛出异常
 */
@RequiresApi(24)
fun FragmentActivity.copyFile(
    sourceFileUri: Uri,
    targetFolderUri: Uri,
    block: uriResult,/* = (uri: android.net.Uri?) -> kotlin.Unit */
) {
    val uri: Uri? =
        DocumentsContract.copyDocument(
            contentResolver,
            sourceFileUri,
            targetFolderUri
        )
    uri.runSafelyNullable(block)
}

/**
 * Moves the given document under a new parent.
 *
 * @param sourceFileUri document with
 *     [android.provider.DocumentsContract.Document.FLAG_SUPPORTS_MOVE]
 * @param sourceFileParentUri parent document of the document to move.
 * @param targetFolderUri document which will become a new parent of the
 *     source document.
 * @param block(uri) uri is the moved document, or {@code null} if failed.
 */
@RequiresApi(24)
fun FragmentActivity.moveFile(
    sourceFileUri: Uri,
    sourceFileParentUri: Uri,
    targetFolderUri: Uri,
    block: uriResult,/* = (uri: android.net.Uri?) -> kotlin.Unit */
) {
    val uri: Uri? =
        DocumentsContract.moveDocument(
            contentResolver,
            sourceFileUri,
            sourceFileParentUri,
            targetFolderUri
        )
    uri.runSafelyNullable(block)
}

/**
 * Change the display name of an existing document.
 *
 * If the underlying provider needs to create a new
 * [android.provider.DocumentsContract.Document.COLUMN_DOCUMENT_ID] to
 * represent the updated display name, that new document is returned and
 * the original document is no longer valid. Otherwise, the original
 * document is returned.
 *
 * @param sourceFileUri document with
 *     [android.provider.DocumentsContract.Document.FLAG_SUPPORTS_RENAME]
 * @param newName updated name for document
 * @param block(uri) the existing or new document after the rename, or
 *     {@code null} if failed.
 */
fun FragmentActivity.rename(
    sourceFileUri: Uri,
    newName: String,
    block: uriResult,/* = (uri: android.net.Uri) -> kotlin.Unit */
) {
    DocumentsContract.renameDocument(
        contentResolver,
        sourceFileUri,
        newName
    ).runSafelyNullable(block)
}

/**
 * Removes the given document from a parent directory.
 *
 * In contrast to [deleteFile] it requires specifying the parent. This
 * method is especially useful if the document can be in multiple parents.
 *
 * @param sourceFileUri document with
 *     [android.provider.DocumentsContract.Document.FLAG_SUPPORTS_REMOVE]
 * @param sourceFileParentUri parent document of the document to remove.
 * @param block(b) true if the document was removed successfully.
 */
@RequiresApi(24)
fun FragmentActivity.removeFile(
    sourceFileUri: Uri,
    sourceFileParentUri: Uri,
    block: fileProcessResult, /* = (result: kotlin.Boolean) -> kotlin.Unit */
) {
    DocumentsContract.removeDocument(
        contentResolver,
        sourceFileUri, sourceFileParentUri
    ).runSafely(block)
}

fun FragmentActivity.deleteFile(uri: Uri, block: fileProcessResult) {
    DocumentsContract.deleteDocument(getContentResolver(), uri)
        .runSafely(block)
}

/**
 * Create a [DocumentFile] representing the document tree rooted
 * at the given [Uri]. This is only useful on devices running
 * [android.os.Build.VERSION_CODES.LOLLIPOP] or later, and will
 * return {@code null} when called on earlier platform versions.
 *
 * @param treeUri the [Intent.getData] from a successful
 *     [Intent.ACTION_OPEN_DOCUMENT_TREE] request.
 */
fun FragmentActivity.getDocumentTreeFile(treeUri: Uri): DocumentFile? {
    return DocumentFile.fromTreeUri(this, treeUri)
}

/**
 * Create a [DocumentFile] representing the single document
 * at the given [Uri]. This is only useful on devices running
 * [android.os.Build.VERSION_CODES.KITKAT] or later, and will
 * return {@code null} when called on earlier platform versions.
 *
 * @param singleUri the [Intent.getData] from a successful
 *     [Intent.ACTION_OPEN_DOCUMENT] or [Intent.ACTION_CREATE_DOCUMENT]
 *     request.
 */
fun FragmentActivity.getSingleDocumentFile(singleUri: Uri): DocumentFile? {
    return DocumentFile.fromSingleUri(this, singleUri)
}