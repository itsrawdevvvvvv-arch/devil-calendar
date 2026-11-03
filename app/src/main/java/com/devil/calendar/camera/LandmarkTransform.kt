package com.devil.calendar.camera

import androidx.compose.ui.geometry.Offset

/**
 * Maps a landmark from normalised upright-image space onto the preview view,
 * using the SAME scale type the PreviewView uses.
 *
 * ML Kit returns points in the upright (rotated) image space. The preview fills
 * the view by centre-cropping, so the mapping has to crop identically or the
 * skeleton drifts away from the body. The front camera preview is mirrored, so
 * X is flipped too.
 */
data class ViewMapping(
    val imgW: Float,
    val imgH: Float,
    val scale: Float,
    val dx: Float,
    val dy: Float,
    val mirror: Boolean,
) {
    fun map(nx: Float, ny: Float, viewW: Float): Offset {
        val px = nx * imgW * scale + dx
        val py = ny * imgH * scale + dy
        return Offset(if (mirror) viewW - px else px, py)
    }

    companion object {
        fun compute(
            viewW: Float,
            viewH: Float,
            imgW: Float,
            imgH: Float,
            fillCenter: Boolean = true,
            mirror: Boolean,
        ): ViewMapping {
            if (viewW <= 0f || viewH <= 0f || imgW <= 0f || imgH <= 0f) {
                return ViewMapping(imgW, imgH, 1f, 0f, 0f, mirror)
            }
            val sx = viewW / imgW
            val sy = viewH / imgH
            val scale = if (fillCenter) maxOf(sx, sy) else minOf(sx, sy)
            return ViewMapping(
                imgW = imgW,
                imgH = imgH,
                scale = scale,
                dx = (viewW - imgW * scale) / 2f,
                dy = (viewH - imgH * scale) / 2f,
                mirror = mirror,
            )
        }
    }
}
