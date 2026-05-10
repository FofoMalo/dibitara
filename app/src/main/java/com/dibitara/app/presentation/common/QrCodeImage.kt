package com.dibitara.app.presentation.common

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

/**
 * Affiche un QR code généré à partir d'un contenu textuel (ex: URI otpauth://).
 * Le rendu est mémorisé — ZXing ne recalcule que si [content] change.
 */
@Composable
fun QrCodeImage(content: String, taillePx: Int = 512, modifier: Modifier = Modifier) {
    val bitmap = remember(content) {
        val writer    = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, taillePx, taillePx)
        val bmp       = Bitmap.createBitmap(taillePx, taillePx, Bitmap.Config.RGB_565)
        for (x in 0 until taillePx) {
            for (y in 0 until taillePx) {
                bmp.setPixel(x, y,
                    if (bitMatrix[x, y]) android.graphics.Color.BLACK
                    else                 android.graphics.Color.WHITE
                )
            }
        }
        bmp.asImageBitmap()
    }
    Image(bitmap = bitmap, contentDescription = "QR Code 2FA", modifier = modifier)
}
