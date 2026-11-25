package be.ap.laatsteronde.ui.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import kotlin.math.ceil

private const val SELECTED_FILL = 0xFF1F5E3B.toInt() // forest green, as the chosen pin in the Figma
private const val NORMAL_TEXT = 0xFF1B1F1A.toInt()

/**
 * The round label with a price that stands on the map for a shop (Figma "5 - Kaart": "€4", "€5,50").
 * White with a dark border; the chosen shop is green with white text. It is drawn as a picture, because
 * osmdroid shows a drawable on the map.
 */
fun priceMarkerDrawable(context: Context, text: String, selected: Boolean): BitmapDrawable {
    val density = context.resources.displayMetrics.density
    val border = 2 * density
    val horizontalPadding = 12 * density
    val height = 34 * density

    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (selected) Color.WHITE else NORMAL_TEXT
        textSize = 14 * density
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val width = textPaint.measureText(text) + 2 * horizontalPadding + 2 * border

    val bitmap = Bitmap.createBitmap(ceil(width).toInt(), ceil(height).toInt(), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val box = RectF(border / 2, border / 2, width - border / 2, height - border / 2)
    val radius = box.height() / 2

    canvas.drawRoundRect(
        box, radius, radius,
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = if (selected) SELECTED_FILL else Color.WHITE
        }
    )
    canvas.drawRoundRect(
        box, radius, radius,
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = border
            color = if (selected) Color.WHITE else NORMAL_TEXT
        }
    )
    // The text in the middle: the baseline is lower than the middle by half of the text height.
    val baseline = height / 2 - (textPaint.ascent() + textPaint.descent()) / 2
    canvas.drawText(text, (width - textPaint.measureText(text)) / 2, baseline, textPaint)

    return BitmapDrawable(context.resources, bitmap)
}
