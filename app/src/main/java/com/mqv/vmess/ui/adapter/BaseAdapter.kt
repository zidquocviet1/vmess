package com.mqv.vmess.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

abstract class BaseAdapter<T : Any, VB : ViewBinding>(diffCallback: DiffUtil.ItemCallback<T>) :
    ListAdapter<T, BaseAdapter.BaseViewHolder>(diffCallback) {

    private var eventHandler: ItemEventHandler? = null

    interface ItemEventHandler {
        fun onItemClick(position: Int) {}
        fun onItemLongClick(position: Int) {}
        fun onListItemSizeChanged(size: Int) {}
    }

    abstract fun getViewRes(): Int
    abstract fun getView(view: View): VB
    abstract fun bindItem(item: T, binding: ViewBinding)

    open fun bindItem(item: T, binding: ViewBinding, payloads: MutableList<Any>) {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        return BaseViewHolder(
            getView(
                LayoutInflater.from(parent.context).inflate(getViewRes(), parent, false)
            ),
            eventHandler
        )
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        bindItem(getItem(position), holder.binding)
    }

    override fun onBindViewHolder(
        holder: BaseViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        super.onBindViewHolder(holder, position, payloads)

        bindItem(getItem(position), holder.binding, payloads)
    }

    override fun onCurrentListChanged(previousList: MutableList<T>, currentList: MutableList<T>) {
//        eventHandler?.onListItemSizeChanged(currentList.size)
    }

    fun registerEventHandler(event: ItemEventHandler) {
        eventHandler = event
    }

    class BaseViewHolder(
        val binding: ViewBinding,
        val event: ItemEventHandler?
    ) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                event?.onItemClick(layoutPosition)
            }
            binding.root.setOnLongClickListener {
                return@setOnLongClickListener event?.onItemLongClick(layoutPosition) != null
            }
        }
    }
}