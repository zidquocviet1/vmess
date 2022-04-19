package com.mqv.vmess.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.mqv.vmess.R
import com.mqv.vmess.network.service.StorageService
import com.mqv.vmess.util.Const
import com.mqv.vmess.util.MediaUtil
import com.mqv.vmess.util.ServiceUtil
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.awaitResponse
import java.io.InputStream

/*
* Primary worker to download all the media message like: Videos, Photos, Files...
* If the media file so large then show the foreground service as the result of downloading.
* */
class DownloadMediaWorkWrapper(
    val context: Context,
    private val url: String?,
    private val isVideo: Boolean
) : BaseWorker(context) {
    override fun retrieveConstraint() =
        Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresStorageNotLow(true)
            .build()

    override fun isUniqueWork() = false

    override fun createRequest() =
        OneTimeWorkRequest.Builder(DownloadMediaWorker::class.java)
            .setInputData(workDataOf(ARG_FILE_URL to url, ARG_IS_VIDEO to isVideo))
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setConstraints(retrieveConstraint())
            .build()

    @HiltWorker
    class DownloadMediaWorker @AssistedInject constructor(
        @Assisted appContext: Context,
        @Assisted workerParams: WorkerParameters,
        private val storageService: StorageService
    ) : CoroutineWorker(appContext, workerParams) {

        override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
            val url = inputData.getString(ARG_FILE_URL)
            val isVideo = inputData.getBoolean(ARG_IS_VIDEO, false)

            return@withContext url?.run { download(this, isVideo) } ?: Result.failure()
        }

        override suspend fun getForegroundInfo(): ForegroundInfo {
            val channel = NotificationChannel(
                CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW
            ).also { channel ->
                ServiceUtil.getNotificationManager(applicationContext)
                    .createNotificationChannel(channel)
            }
            return ForegroundInfo(
                NOTIFICATION_ID, NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                    .setContentTitle(applicationContext.getString(R.string.app_name))
                    .setContentText(applicationContext.getString(R.string.title_notification_download_file))
                    .setAutoCancel(false)
                    .setOngoing(true)
                    .setChannelId(channel.id)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .build()
            )
        }

        private suspend fun download(url: String, isVideo: Boolean): Result? {
            val user = FirebaseAuth.getInstance().currentUser!!
            val token = Tasks.await(user.getIdToken(true)).token

            return token?.run {
                val response =
                    storageService.download(Const.PREFIX_TOKEN + this, url, isVideo)
                        .awaitResponse()
                val body = response.body()

                if (response.isSuccessful && body != null) {
                    body.byteStream().use { inputStream ->
                        val headers = response.headers()
                        val contentType = headers["Content-Type"] ?: ""
                        val extension = contentType.extractExtension()
                        val fileName = extension.createReceivedFileName()

                        if (isVideo) {
                            writeVideo(inputStream, fileName, contentType, body.contentLength())
                        } else {
                            writeImage(inputStream, fileName, contentType, body.contentLength())
                        }

                        Handler(Looper.getMainLooper()).postDelayed({
                            Toast.makeText(applicationContext, "Saved File", Toast.LENGTH_SHORT)
                                .show()
                        }, 1000)
                    }
                    return@run Result.success()
                } else {
                    return@run Result.failure()
                }
            }
        }

        private fun isApiOver29() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

        private fun String.extractExtension() = this.split("/")[1]

        private fun String.createReceivedFileName() = "received_${System.currentTimeMillis()}.$this"

        private fun writeStreamToFile(inputStream: InputStream, uri: Uri?) {
            uri?.let {
                val outputStream = applicationContext.contentResolver.openOutputStream(it)
                val buf = ByteArray(8192)
                var len: Int

                outputStream?.use { stream ->
                    while (inputStream.read(buf).also { byte -> len = byte } > 0) {
                        stream.write(buf, 0, len)
                    }
                }
            }
        }

        private fun writeImage(
            inputStream: InputStream,
            fileName: String,
            contentType: String,
            contentLength: Long
        ) {
            val values = createContentValue(fileName, contentType, contentLength, false)

            val resolver = applicationContext.contentResolver
            val collection =
                if (isApiOver29()) MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
                else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val uriSavedVideo = resolver.insert(collection, values)

            writeStreamToFile(inputStream, uriSavedVideo)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uriSavedVideo!!, values, null, null)
            }
        }

        private fun writeVideo(
            inputStream: InputStream,
            fileName: String,
            contentType: String,
            contentLength: Long
        ) {
            val values = createContentValue(fileName, contentType, contentLength, true)
            val resolver = applicationContext.contentResolver
            val collection =
                if (isApiOver29()) MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
                else MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            val uriSavedVideo = resolver.insert(collection, values)

            writeStreamToFile(inputStream, uriSavedVideo)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Video.Media.IS_PENDING, 0)
                resolver.update(uriSavedVideo!!, values, null, null)
            }
        }

        private fun createContentValue(
            fileName: String,
            contentType: String,
            contentLength: Long,
            isVideo: Boolean
        ): ContentValues {
            val folderName = "VMess"
            val currentTime = System.currentTimeMillis()

            return if (isVideo) ContentValues().apply {
                if (isApiOver29()) {
                    put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/$folderName")
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }
                put(MediaStore.Video.Media.TITLE, fileName)
                put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Video.Media.MIME_TYPE, MediaUtil.checkMimeType(contentType))
                put(MediaStore.Video.Media.SIZE, contentLength)
                put(MediaStore.Video.Media.DATE_ADDED, currentTime / 1000)
                put(MediaStore.Video.Media.DATE_TAKEN, currentTime)
                put(MediaStore.Video.Media.DATE_MODIFIED, currentTime / 1000)
            } else ContentValues().apply {
                if (isApiOver29()) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/$folderName")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                put(MediaStore.Images.Media.TITLE, fileName)
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, MediaUtil.checkMimeType(contentType))
                put(MediaStore.Images.Media.SIZE, contentLength)
                put(MediaStore.Images.Media.DATE_ADDED, currentTime / 1000)
                put(MediaStore.Images.Media.DATE_TAKEN, currentTime)
            }
        }
    }

    companion object {
        const val NOTIFICATION_ID = 101
        const val CHANNEL_ID = "123"
        const val CHANNEL_NAME = "Downloader"

        const val ARG_FILE_URL = "file_url"
        const val ARG_IS_VIDEO = "is_video"
    }
}