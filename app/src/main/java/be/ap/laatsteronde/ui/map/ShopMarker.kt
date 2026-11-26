package be.ap.laatsteronde.ui.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.text.TextPaint
import android.text.TextUtils
import kotlin.math.ceil

private const val SELECTED_FILL = 0xFF1F5E3B.toInt() // forest green, as the chosen pin in the Figma
private const val NORMAL_TEXT = 0xFF1B1F1A.toInt()

// Sizes in dp. 48 dp high: large enough to read and to tap (the minimum tap size of Android).
private const val MARKER_HEIGHT_DP = 48f
private const val TEXT_SIZE_SP = 16f
private const val BORDER_DP = 2.5f
private const val HORIZONTAL_PADDING_DP = 16f

// A shop name can be 60 characters long; longer than this it is cut off with "…", so it does not cover the map.
private const val MAX_TEXT_WIDTH_DP = 170f

/**
 * The round label with the name of a shop that stands on the map (feedback of the lector: the name instead
 * of the price, and bigger). White with a dark border; the chosen shop is green with white text. It is drawn
 * as a picture, because osmdroid shows a drawable on the map.
 */
fun shopMarkerDrawable(context: Context, name: String, selected: Boolean): BitmapDrawable {
    val density = context.resources.displayMetrics.density
    val fontScale = context.resources.configuration.fontScale
    val border = BORDER_DP * density
    val horizontalPadding = HORIZONTAL_PADDING_DP * density
    val height = MARKER_HEIGHT_DP * density

    val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (selected) Color.WHITE else NORMAL_TEXT
        // Follows the font size of the phone, like the other texts of the app.
        textSize = TEXT_SIZE_SP * density * fontScale
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val text = TextUtils.ellipsize(name, textPaint, MAX_TEXT_WIDTH_DP * density, TextUtils.TruncateAt.END).toString()
    val textWidth = textPaint.measureText(text)
    val width = textWidth + 2 * horizontalPadding + 2 * border

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
    canvas.drawText(text, (width - textWidth) / 2, baseline, textPaint)

    return BitmapDrawable(context.resources, bitmap)
}
