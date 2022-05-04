package com.mqv.vmess.ui.adapter

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.view.View
import androidx.recyclerview.widget.DiffUtil
import androidx.viewbinding.ViewBinding
import com.mqv.vmess.R
import com.mqv.vmess.databinding.ItemColorPickerBinding
import com.mqv.vmess.ui.data.ColorPicker

class ColorPickerAdapter :
    BaseAdapter<ColorPicker, ItemColorPickerBinding>(object : DiffUtil.ItemCallback<ColorPicker>() {
        override fun areItemsTheSame(oldItem: ColorPicker, newItem: ColorPicker) =
            oldItem.colorCode == newItem.colorCode

        override fun areContentsTheSame(oldItem: ColorPicker, newItem: ColorPicker) =
            oldItem == newItem

    }) {
    override fun getViewRes(): Int = R.layout.item_color_picker

    override fun getView(view: View) = ItemColorPickerBinding.bind(view)

    override fun bindItem(item: ColorPicker, binding: ViewBinding) {
        with(binding as ItemColorPickerBinding) {
            textDefault.visibility = if (item.isDefault) View.VISIBLE else View.GONE
            textColorName.text = item.colorName
            textColorName.visibility = if (item.colorName.isNotEmpty()) View.VISIBLE else View.GONE

            val layerDrawable = imageColor.background as LayerDrawable
            val color = layerDrawable.findDrawableByLayerId(R.id.color) as GradientDrawable
            val outer = layerDrawable.findDrawableByLayerId(R.id.outer) as GradientDrawable

            color.setColor(Color.parseColor(item.colorCode))
            outer.alpha = if (item.isSelected) 255 else 0
        }
    }

    override fun onBindViewHolder(
        holder: BaseViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        super.onBindViewHolder(holder, position, payloads)

        if (payloads.isNotEmpty() && payloads[0] == PAYLOAD_SELECT) {
            with (holder.binding as ItemColorPickerBinding) {
                val layerDrawable = imageColor.background as LayerDrawable
                val outer = layerDrawable.findDrawableByLayerId(R.id.outer) as GradientDrawable

                outer.alpha = if (getItem(position).isSelected) 255 else 0
            }
        }
    }

    companion object {
        const val PAYLOAD_SELECT = "select"
    }
}