package org.myefoss.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Shader
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView

/**
 * An ImageView that renders an image with a smooth corner-to-content gradient fade-out
 * using PorterDuff DST_IN mask so the image naturally melts into whatever background/card
 * is underneath (both Light and Dark themes, zero hardcoded color).
 */
class CornerFadingImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    private val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
    }

    init {
        // Required for PorterDuff blending on view layer
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) {
            super.onDraw(canvas)
            return
        }

        val saveCount = canvas.saveLayer(0f, 0f, w, h, null)
        super.onDraw(canvas)

        // Diagonal gradient from Top-Right (visible) to Bottom-Left (transparent 0)
        // Angle ~ 45 degrees: image is clearest at top-right corner, fades smoothly into the card
        val shader = LinearGradient(
            w * 0.15f, h * 0.85f, // Bottom-left start of fade (fully transparent)
            w, 0f,                // Top-right corner (visible)
            intArrayOf(0x00000000, 0x55000000, 0xEE000000.toInt()),
            floatArrayOf(0.0f, 0.55f, 1.0f),
            Shader.TileMode.CLAMP
        )
        maskPaint.shader = shader
        canvas.drawRect(0f, 0f, w, h, maskPaint)

        canvas.restoreToCount(saveCount)
    }
}
