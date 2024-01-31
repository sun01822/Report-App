package com.example.report_app.helper

import android.graphics.Bitmap

class Helper {
    fun compressBitmap(bitmap: Bitmap): Bitmap {
        // Calculate new dimensions based on your desired resolution
        val maxWidth = 800 // Adjust as needed
        val maxHeight = 600 // Adjust as needed

        val scale = (maxWidth.toFloat() / bitmap.width).coerceAtMost(maxHeight.toFloat() / bitmap.height)
        val newWidth = (bitmap.width * scale).toInt()
        val newHeight = (bitmap.height * scale).toInt()

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
}