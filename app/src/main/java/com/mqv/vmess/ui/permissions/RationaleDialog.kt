package com.mqv.vmess.ui.permissions

import android.app.AlertDialog
import android.content.Context
import android.content.res.Resources.getSystem
import android.graphics.Color
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.mqv.vmess.R

class RationaleDialog {
    companion object {
        fun createFor(context: Context, message: String, drawables: IntArray): AlertDialog.Builder {
            val view =
                LayoutInflater.from(context).inflate(R.layout.dialog_rationale_permissions, null)
            val header = view.findViewById<ViewGroup>(R.id.header_container)
            val text = view.findViewById<TextView>(R.id.message)

            for (i in drawables.indices) {
                val drawable = ContextCompat.getDrawable(context, drawables[i])
                DrawableCompat.setTint(drawable!!, ContextCompat.getColor(context, R.color.white))
                val imageView = ImageView(context)
                imageView.setImageDrawable(drawable)
                imageView.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                header.addView(imageView)
                if (i != drawables.size - 1) {
                    val plus = TextView(context)
                    plus.text = "+"
                    plus.setTextSize(TypedValue.COMPLEX_UNIT_SP, 40f)
                    plus.setTextColor(Color.WHITE)
                    val layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )

                    layoutParams.setMargins(20.px, 0, 20.px, 0)
                    plus.layoutParams = layoutParams
                    header.addView(plus)
                }
            }

            text.text = message

            return AlertDialog.Builder(
                context,
                R.style.Theme_VMess_AlertDialog_Light_Cornered
            )
                .setView(view)
        }

        fun createForPermanentlyDenied(
            context: Context,
            messageTitle: String,
            message: String,
            constructionMessage: String
        ): AlertDialog.Builder {
            val view =
                LayoutInflater.from(context).inflate(R.layout.dialog_rationale_settings, null)
            val titleText = view.findViewById<TextView>(R.id.message_title)
            val text = view.findViewById<TextView>(R.id.message)
            val textConstruction = view.findViewById<TextView>(R.id.sub_message)

            titleText.text = messageTitle
            text.text = message
            textConstruction.text = constructionMessage

            return AlertDialog.Builder(
                context,
                R.style.Theme_VMess_AlertDialog_Light_Cornered
            )
                .setView(view)
        }
    }
}

val Int.px: Int get() = (this * getSystem().displayMetrics.density).toInt()
