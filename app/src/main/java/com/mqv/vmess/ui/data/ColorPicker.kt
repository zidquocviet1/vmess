package com.mqv.vmess.ui.data

data class ColorPicker(
    val colorCode: String,
    val isDefault: Boolean = false,
    val colorName: String = "",
    var isSelected: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ColorPicker

        if (colorCode != other.colorCode) return false
        if (isDefault != other.isDefault) return false
        if (colorName != other.colorName) return false
        if (isSelected != other.isSelected) return false

        return true
    }

    override fun hashCode(): Int {
        var result = colorCode.hashCode()
        result = 31 * result + isDefault.hashCode()
        result = 31 * result + colorName.hashCode()
        result = 31 * result + isSelected.hashCode()
        return result
    }
}
