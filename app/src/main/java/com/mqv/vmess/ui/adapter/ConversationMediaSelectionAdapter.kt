package com.mqv.vmess.ui.adapter

import android.content.Context
import android.view.View
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.mqv.vmess.R
import com.mqv.vmess.databinding.ItemConversationMediaSelectionBinding
import com.mqv.vmess.ui.data.Media
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

class ConversationMediaSelectionAdapter(
    private val context: Context,
    private val mRecyclerView: RecyclerView
) : BaseAdapter<Media, ItemConversationMediaSelectionBinding>(object :
    DiffUtil.ItemCallback<Media>() {
    override fun areItemsTheSame(oldItem: Media, newItem: Media) =
        oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: Media, newItem: Media) =
        oldItem == newItem
}) {
    private var mCallback: Consumer<Int>? = null

    override fun getViewRes() = R.layout.item_conversation_media_selection

    override fun getView(view: View) = ItemConversationMediaSelectionBinding.bind(view)

    override fun bindItem(item: Media, binding: ViewBinding) {
        val itemBinding = binding as ItemConversationMediaSelectionBinding

        with(itemBinding) {
            Glide.with(context)
                .load(item.uri)
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(imageThumbnail)

            radioSelect.isChecked = item.isSelected

            if (item.isVideo) {
                textVideoDuration.visibility = View.VISIBLE
                textVideoDuration.text = formatTime(item.duration)
            } else {
                textVideoDuration.visibility = View.GONE
            }
        }
    }

    override fun onBindViewHolder(
        holder: BaseViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        super.onBindViewHolder(holder, position, payloads)

        if (payloads.isNotEmpty() && payloads[0] == "select_payload") {
            val itemBinding = holder.binding as ItemConversationMediaSelectionBinding
            val item = getItem(position)

            with(itemBinding) {
                radioSelect.isChecked = item.isSelected
            }
        }
    }

    override fun afterCreateViewHolder(binding: ViewBinding) {
        super.afterCreateViewHolder(binding)

        val itemBinding = binding as ItemConversationMediaSelectionBinding

        with(itemBinding) {
            imageThumbnail.setOnClickListener { _ ->
                val pos = mRecyclerView.getChildLayoutPosition(binding.root)
                val item = getItem(pos)

                changeItemSelectState(pos, !item.isSelected)

                mRecyclerView.post {
                    mRecyclerView.smoothScrollToPosition(pos)
                }
                mCallback?.accept(
                    currentList.stream()
                        .filter { media -> media.isSelected }
                        .count()
                        .toInt()
                )
            }
        }
    }

    fun setOnMediaClickListener(callback: Consumer<Int>) {
        mCallback = callback
    }

    fun changeItemSelectState(position: Int, isSelected: Boolean) {
        getItem(position).apply {
            this.isSelected = isSelected
        }

        notifyItemChanged(position, "select_payload")
    }


    private fun formatTime(time: Long): String {
        var time = time
        val hours = TimeUnit.MILLISECONDS.toHours(time)
        time -= TimeUnit.HOURS.toMillis(hours)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(time)
        time -= TimeUnit.MINUTES.toMillis(minutes)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(time)
        return if (hours > 0) {
            zeroPad(hours) + ":" + zeroPad(minutes) + ":" + zeroPad(seconds)
        } else {
            zeroPad(minutes) + ":" + zeroPad(seconds)
        }
    }

    private fun zeroPad(value: Long): String {
        return if (value < 10) {
            "0$value"
        } else {
            value.toString()
        }
    }
}