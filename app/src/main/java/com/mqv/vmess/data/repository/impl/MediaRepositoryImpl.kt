package com.mqv.vmess.data.repository.impl

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.WorkerThread
import com.mqv.vmess.data.repository.MediaRepository
import com.mqv.vmess.ui.data.Media
import com.mqv.vmess.util.Logging
import com.mqv.vmess.util.StorageUtil
import java.util.*
import java.util.concurrent.Executors
import java.util.function.Consumer

class MediaRepositoryImpl : MediaRepository {
    override fun getMediaInBucket(
        context: Context,
        bucketId: String,
        callback: Consumer<List<Media>>
    ) {
        if (!StorageUtil.canReadDataFromMediaStore(context)) {
            Logging.debug(TAG, "No storage permission")
            callback.accept(mutableListOf())
            return
        }

        Executors.newSingleThreadExecutor().execute {
            callback.accept(getMediaInBucket(context, bucketId))
        }
    }

    @WorkerThread
    private fun getMediaInBucket(context: Context, bucketId: String): List<Media> {
        val photos =
            getMediaInBucket(context, bucketId, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true)
        val videos =
            getMediaInBucket(context, bucketId, MediaStore.Video.Media.EXTERNAL_CONTENT_URI, false)
        val media = mutableListOf<Media>()

        media.addAll(photos)
        media.addAll(videos)
        media.sortWith { o1, o2 -> o2.date.compareTo(o1.date) }

        return media
    }

    @WorkerThread
    private fun getMediaInBucket(
        context: Context,
        bucketId: String,
        contentUri: Uri,
        isPhoto: Boolean
    ): List<Media> {
        val media: MutableList<Media> = LinkedList()
        var selection =
            MediaStore.Images.Media.BUCKET_ID + " = ? AND " + isNotPending() + " AND " + MediaStore.Images.Media.MIME_TYPE + " NOT LIKE ?"
        var selectionArgs = arrayOf(bucketId, "%image/svg%")
        val sortBy = MediaStore.Images.Media.DATE_MODIFIED + " DESC"

        val projection: Array<String> = if (isPhoto) {
            arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.MIME_TYPE,
                MediaStore.Images.Media.DATE_MODIFIED,
                MediaStore.Images.Media.ORIENTATION,
                MediaStore.Images.Media.SIZE
            )
        } else {
            arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.MIME_TYPE,
                MediaStore.Images.Media.DATE_MODIFIED,
                MediaStore.Images.Media.SIZE,
                MediaStore.Video.Media.DURATION
            )
        }

        if (Media.ALL_BUCKET_ID == bucketId) {
            selection = isNotPending() + " AND " + MediaStore.Images.Media.MIME_TYPE + " NOT LIKE ?"
            selectionArgs = arrayOf("%image/svg%")
        }

        context.contentResolver.query(contentUri, projection, selection, selectionArgs, sortBy)
            .use { cursor ->
                while (cursor != null && cursor.moveToNext()) {
                    val rowId = cursor.getLong(cursor.getColumnIndexOrThrow(projection[0]))
                    val uri = ContentUris.withAppendedId(contentUri, rowId)
                    val path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA))
                    val mimetype =
                        cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE))
                    val date =
                        cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED))
                    val orientation =
                        if (isPhoto) cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.ORIENTATION)) else 0
                    val size =
                        cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE))
                    val duration =
                        if (!isPhoto) cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION))
                            .toLong() else 0.toLong()
                    media.add(
                        Media(
                            rowId,
                            uri,
                            path,
                            mimetype,
                            bucketId,
                            duration,
                            size,
                            date,
                            isVideo = duration > 0,
                            isSelected = false
                        )
                    )
                }
            }

        return media
    }

    private fun isNotPending(): String {
        return if (Build.VERSION.SDK_INT <= 28) MediaStore.Images.Media.DATA + " NOT NULL" else MediaStore.MediaColumns.IS_PENDING + " != 1"
    }

    companion object {
        val TAG: String = MediaRepositoryImpl::class.java.simpleName
    }
}