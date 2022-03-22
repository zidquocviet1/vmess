package com.mqv.vmess.util

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Size
import androidx.annotation.RequiresApi
import com.mqv.vmess.ui.data.ImageThumbnail
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

object FileProviderUtil {
    /*
    * Create temp file before take picture and save it
    * Using ActivityResultContracts.TakePicture to handle it
    * */
    @JvmStatic
    fun createTempFilePicture(resolver: ContentResolver): Uri? {
        val uri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        val formatter = DateTimeFormatter.ofPattern(Const.IMAGE_FILE_NAME_PATTERN)
        val suffix = LocalDateTime.now().format(formatter)
        val fileName = "VMESS_IMG_$suffix"
        val cv = ContentValues()

        cv.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
        cv.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) cv.put(
            MediaStore.MediaColumns.DATE_TAKEN,
            System.currentTimeMillis().toString()
        )

        return resolver.insert(uri, cv)
    }

    @JvmStatic
    @RequiresApi(29)
    fun getImagesShownInApi29(resolver: ContentResolver, specificUri: Uri?): List<ImageThumbnail?> {
        var images: MutableList<ImageThumbnail?>? = null
        val uri = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_TAKEN,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.RELATIVE_PATH,
            MediaStore.MediaColumns.DATA
        )
        val imageDateSort = MediaStore.MediaColumns.DATE_TAKEN + " DESC"
        val cursor = resolver.query(
            specificUri ?: uri,
            projection,
            null,
            null,
            imageDateSort
        )

        if (cursor != null && cursor.count > 0) {
            images = ArrayList()

            val idIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            val dateIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_TAKEN)
            val typeIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
            val pathIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.RELATIVE_PATH)
            val dataIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIndex)
                val name = cursor.getString(nameIndex)
                val size = cursor.getString(sizeIndex)
                val date = cursor.getString(dateIndex)
                val type = cursor.getString(typeIndex)
                val relativePath = cursor.getString(pathIndex)
                val realPath = cursor.getString(dataIndex)
                val contentUri = ContentUris.withAppendedId(uri, id)
                var thumbnail: Bitmap? = null
                try {
                    thumbnail = resolver.loadThumbnail(contentUri, Size(480, 480), null)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                val timestamp: LocalDateTime? = try {
                    LocalDateTime.parse(date)
                } catch (e: DateTimeParseException) {
                    LocalDateTime.MIN
                }
                images.add(
                    ImageThumbnail(
                        id,
                        name,
                        size?.toLong() ?: 0L,
                        timestamp,
                        contentUri,
                        thumbnail,
                        type,
                        relativePath,
                        realPath
                    )
                )
            }
            cursor.close()
        }
        return images ?: ArrayList()
    }

    @JvmStatic
    fun getImagesShownInApiLower29(resolver: ContentResolver, specificUri: Uri?): List<ImageThumbnail?> {
        var images: MutableList<ImageThumbnail?>? = null
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.DATA
        )
        val cursor = resolver.query(
            specificUri ?: uri,
            projection,
            null,
            null,
            null
        )
        if (cursor != null && cursor.count > 0) {
            images = java.util.ArrayList()
            val idIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            val typeIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
            val dataIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIndex)
                val name = cursor.getString(nameIndex)
                val size = cursor.getString(sizeIndex)
                val type = cursor.getString(typeIndex)
                val realPath = cursor.getString(dataIndex)
                val contentUri = ContentUris.withAppendedId(uri, id)
                var thumbnail: Bitmap? = null
                try {
                    thumbnail = MediaStore.Images.Media.getBitmap(resolver, contentUri)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                images.add(
                    ImageThumbnail(
                        id,
                        name,
                        size?.toLong() ?: 0,
                        null,
                        contentUri,
                        thumbnail,
                        type,
                        "",
                        realPath
                    )
                )
            }
            cursor.close()
        }
        return images ?: java.util.ArrayList()
    }

    @JvmStatic
    fun getAllPhotoFromExternal(resolver: ContentResolver, specificUri: Uri?): List<ImageThumbnail?> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getImagesShownInApi29(resolver, specificUri)
        } else {
            getImagesShownInApiLower29(resolver, specificUri)
        }
    }

    @JvmStatic
    fun getImageThumbnailFromUri(resolver: ContentResolver, uri: Uri?): ImageThumbnail? {
        return uri?.run {
            val cursor = resolver.query(uri, null, null, null, null)

            return cursor?.run {
                val idIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                val typeIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
                val dataIndex = getColumnIndex(MediaStore.Images.ImageColumns.DATA)
                var imageThumbnail: ImageThumbnail? = null

                while (moveToNext()) {
                    val id = cursor.getLong(idIndex)
                    val name = cursor.getString(nameIndex)
                    val size = cursor.getString(sizeIndex)
                    val type = cursor.getString(typeIndex)
                    val realPath = cursor.getString(dataIndex)
//                    val contentUri = ContentUris.withAppendedId(uri, id)
                    var thumbnail: Bitmap? = null
                    try {
                        thumbnail = MediaStore.Images.Media.getBitmap(resolver, uri)
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                    imageThumbnail = ImageThumbnail(
                        id,
                        name,
                        size?.toLong() ?: 0,
                        null,
                        uri,
                        thumbnail,
                        type,
                        "",
                        realPath
                    )
                }

                close()
                imageThumbnail
            }
        }
    }

    @JvmStatic
    fun getImagePathFromUri(resolver: ContentResolver, uri: Uri?): String? {
        return uri?.run {
            val cursor = resolver.query(uri, null, null, null, null)

            return cursor?.run {
                val dataIndex = getColumnIndex(MediaStore.Images.ImageColumns.DATA)
                var path: String? = null

                while (moveToNext()) {
                    path = getString(dataIndex)
                }

                close()
                path
            }
        }
    }

    @JvmStatic
    fun getPath(context: Context, uri: Uri): String? {
        // DocumentProvider
        if (DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                val docId: String = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":").toTypedArray()
                val type = split[0]
                if ("primary".equals(type, ignoreCase = true)) {
                    return Environment.getExternalStorageDirectory().toString() + "/" + split[1]
                }
            } else if (isDownloadsDocument(uri)) {
                val id: String = DocumentsContract.getDocumentId(uri)
                val contentUri = ContentUris.withAppendedId(
                    Uri.parse("content://downloads/public_downloads"),
                    java.lang.Long.valueOf(id)
                )
                return getDataColumn(context, contentUri, null, null)
            } else if (isMediaDocument(uri)) {
                val docId: String = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":").toTypedArray()
                val type = split[0]
                var contentUri: Uri? = null
                when (type) {
                    "image" -> {
                        contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    }
                    "video" -> {
                        contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    }
                    "audio" -> {
                        contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                    }
                }
                val selection = "_id=?"
                val selectionArgs = arrayOf(split[1])
                return getDataColumn(context, contentUri, selection, selectionArgs)
            }
        }
        return null
    }

    private fun getDataColumn(
        context: Context,
        uri: Uri?,
        selection: String?,
        selectionArgs: Array<String>?
    ): String? {
        var cursor: Cursor? = null
        val column = "_data"
        val projection = arrayOf(column)

        return uri?.run {
            var result: String? = null
            try {
                cursor =
                    context.contentResolver.query(this, projection, selection, selectionArgs, null)
                cursor?.let {
                    if (it.moveToFirst()) {
                        val index = it.getColumnIndexOrThrow(column)
                        result = it.getString(index)
                    }
                }
            } finally {
                cursor?.close()
            }
            result
        }
    }

    private fun isExternalStorageDocument(uri: Uri): Boolean {
        return "com.android.externalstorage.documents" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    private fun isDownloadsDocument(uri: Uri): Boolean {
        return "com.android.providers.downloads.documents" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    private fun isMediaDocument(uri: Uri): Boolean {
        return "com.android.providers.media.documents" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    fun isGooglePhotosUri(uri: Uri): Boolean {
        return "com.google.android.apps.photos.content" == uri.authority
    }
}