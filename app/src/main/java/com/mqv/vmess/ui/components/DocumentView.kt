package com.mqv.vmess.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.annotation.AttrRes
import com.mqv.vmess.R

class DocumentView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val container: View
    private val iconContainer: ViewGroup
    private val fileName: TextView
    private val fileSize: TextView
    private val document: TextView

//    fun setDownloadClickListener(listener: SlideClickListener?) {
//        downloadListener = listener
//    }
//
//    fun setDocumentClickListener(listener: SlideClickListener?) {
//        viewListener = listener
//    }

//    fun setDocument(
//        documentSlide: Slide,
//        showControls: Boolean
//    ) {
//        if (showControls && documentSlide.isPendingDownload()) {
//            controlToggle.displayQuick(downloadButton)
//            downloadButton.setOnClickListener(
//                org.thoughtcrime.securesms.components.DocumentView.DownloadClickedListener(
//                    documentSlide
//                )
//            )
//            if (downloadProgress.isSpinning()) downloadProgress.stopSpinning()
//        } else if (showControls && documentSlide.getTransferState() === AttachmentDatabase.TRANSFER_PROGRESS_STARTED) {
//            controlToggle.displayQuick(downloadProgress)
//            downloadProgress.spin()
//        } else {
//            controlToggle.displayQuick(iconContainer)
//            if (downloadProgress.isSpinning()) downloadProgress.stopSpinning()
//        }
//        this.documentSlide = documentSlide
//        fileName.setText(
//            documentSlide.getFileName()
//                .or(documentSlide.getCaption())
//                .or(context.getString(R.string.DocumentView_unnamed_file))
//        )
//        fileSize.setText(Util.getPrettyFileSize(documentSlide.getFileSize()))
//        document.setText(documentSlide.getFileType(context).or("").toLowerCase())
//    }

//    private inner class DownloadClickedListener private constructor(private val slide: Slide) :
//        OnClickListener {
//        override fun onClick(v: View) {
//            if (downloadListener != null) downloadListener.onClick(v, slide)
//        }
//    }
//
//    private inner class OpenClickedListener private constructor(slide: Slide) : OnClickListener {
//        private val slide: Slide
//        override fun onClick(v: View) {
//            if (!slide.isPendingDownload() && !slide.isInProgress() && viewListener != null) {
//                viewListener.onClick(v, slide)
//            }
//        }
//
//        init {
//            this.slide = slide
//        }
//    }

    companion object {
    }

    init {
        inflate(context, R.layout.custom_document_view, this)

        container = findViewById(R.id.document_container)
        iconContainer = findViewById(R.id.icon_container)
        fileName = findViewById(R.id.file_name)
        fileSize = findViewById(R.id.file_size)
        document = findViewById(R.id.document)
    }
}