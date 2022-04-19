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
import android.webkit.MimeTypeMap
import androidx.annotation.RequiresApi
import androidx.core.database.getIntOrNull
import com.mqv.vmess.ui.data.ImageThumbnail
import com.mqv.vmess.ui.data.Media
import id.zelory.compressor.Compressor
import id.zelory.compressor.constraint.format
import id.zelory.compressor.constraint.quality
import id.zelory.compressor.constraint.resolution
import id.zelory.compressor.constraint.size
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.future.future
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.*
import java.util.concurrent.CompletableFuture

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
    fun getImagesShownInApiLower29(
        resolver: ContentResolver,
        specificUri: Uri?
    ): List<ImageThumbnail?> {
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
    fun getAllPhotoFromExternal(
        resolver: ContentResolver,
        specificUri: Uri?
    ): List<ImageThumbnail?> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getImagesShownInApi29(resolver, specificUri)
        } else {
            getImagesShownInApiLower29(resolver, specificUri)
        }
    }

    @JvmStatic
    fun getMimeTypeFromUri(context: Context, uri: Uri): String? = uri.getMimeType(context)

    fun Uri.getMimeType(context: Context): String? {
        return when (scheme) {
            ContentResolver.SCHEME_CONTENT -> context.contentResolver.getType(this)
            ContentResolver.SCHEME_FILE -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                MimeTypeMap.getFileExtensionFromUrl(toString()).lowercase(Locale.getDefault())
            )
            else -> null
        }
    }

    // This content uri must has already id in the URI like "content://external/media/image:63"
    @JvmStatic
    fun getMediaFromUriSpecificId(context: Context, contentUri: Uri): Media? {
        var media: Media? = null
        val projection: Array<String> = arrayOf(
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.ORIENTATION,
            MediaStore.Images.Media.SIZE,
            MediaStore.Video.Media.DURATION
        )

        context.contentResolver.query(contentUri, projection, null, null, null)
            .use { cursor ->
                if (cursor != null && cursor.moveToFirst()) {
                    val path =
                        cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA))
                    val mimetype =
                        cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE))
                    val date =
                        cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED))
                    val size =
                        cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE))
                    val duration =
                        cursor.getIntOrNull(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION))
                            ?.toLong() ?: 0
                    media = Media(
                        -1,
                        contentUri,
                        path ?: "",
                        mimetype,
                        null,
                        duration,
                        size,
                        date,
                        isVideo = duration > 0,
                        isSelected = false
                    )
                }
            }
        return media
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
    fun isLocalFile(path: String?): Boolean {
        return if (path == null) {
            false
        } else {
            File(path).exists()
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    @JvmStatic
    fun compressFileFutureForMessage(context: Context, file: File): CompletableFuture<File> =
        GlobalScope.future(Dispatchers.Main) {
            Compressor.compress(context, file) {
                resolution(1280, 720)
                quality(80)
                format(Bitmap.CompressFormat.JPEG)
                size(104_857) // 100KB
            }
        }

    @OptIn(DelicateCoroutinesApi::class)
    @JvmStatic
    fun compressFileFuture(context: Context, file: File): CompletableFuture<File> =
        GlobalScope.future(Dispatchers.Main) {
            Compressor.compress(context, file) {
                resolution(1280, 720)
                quality(80)
                format(Bitmap.CompressFormat.JPEG)
                size(524_288) // 500KB
            }
        }

//    @JvmStatic
//    fun createTempFile(context: Context, uri: Uri): File {
//        val inputStream = context.contentResolver.openInputStream(uri)!!
//        val bytes = byteArrayOf()
//
//        inputStream.read(bytes)
//
//        val tempFile = Files.createTempFile(context.filesDir.toPath(), "temp_file_${System.currentTimeMillis()}", "")
//    }

    @JvmStatic
    fun getPath(context: Context, uri: Uri): String {
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

                if (id.startsWith("raw:")) {
                    return id.removePrefix("raw:")
                }
                val contentUri = ContentUris.withAppendedId(
                    Uri.parse("content://downloads/public_downloads"),
                    id.toLong()
                )
                return getDataColumn(context, contentUri, null, null) ?: ""
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
                    "document" -> contentUri = MediaStore.Files.getContentUri("external")
                }
                val selection = "_id=?"
                val selectionArgs = arrayOf(split[1])
                return getDataColumn(context, contentUri, selection, selectionArgs) ?: ""
            }
            // MediaStore (and general)
            else if ("content".equals(uri.scheme, true)) {

                // Return the remote address
                if (isGooglePhotosUri(uri))
                    return uri.lastPathSegment ?: ""

                return getDataColumn(context, uri, null, null) ?: ""
            }
            // File
            else if ("file".equals(uri.scheme, true)) {
                return uri.getPath() ?: ""
            }
        }
        return ""
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