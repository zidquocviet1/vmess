package com.mqv.vmess.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

typealias PositionRetriever = (view: View) -> Int
typealias ChildViewClickListener = (pos: Int) -> Unit

abstract class BaseAdapter<T : Any, VB : ViewBinding>(diffCallback: DiffUtil.ItemCallback<T>) :
    ListAdapter<T, BaseAdapter.BaseViewHolder>(diffCallback) {

    protected var mRetriever: PositionRetriever? = null
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

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        mRetriever = { view ->
            recyclerView.getChildAdapterPosition(view)
        }
        super.onAttachedToRecyclerView(recyclerView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        return BaseViewHolder(
            getView(
                LayoutInflater.from(parent.context).inflate(getViewRes(), parent, false)
            ),
            eventHandler
        ) { binding -> afterCreateViewHolder(binding) }
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
        eventHandler?.onListItemSizeChanged(currentList.size)
    }

    fun registerEventHandler(event: ItemEventHandler) {
        eventHandler = event
    }

    open fun afterCreateViewHolder(binding: ViewBinding) {
        // Default function for the derived class if it wants to register listener some different widgets
    }

    class BaseViewHolder(
        val binding: ViewBinding,
        val event: ItemEventHandler?,
        val afterCreateSuccess: (ViewBinding) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                event?.onItemClick(layoutPosition)
            }
            binding.root.setOnLongClickListener {
                return@setOnLongClickListener event?.onItemLongClick(layoutPosition) != null
            }
            afterCreateSuccess(binding)
        }
    }
}