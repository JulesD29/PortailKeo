package fr.julesdupont.portail

import com.google.zxing.BinaryBitmap
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import org.junit.Assert.assertEquals
import org.junit.Test

class QrCodeTest {
    @Test
    fun `le QR code genere se relit a l'identique`() {
        val link = ConfigShare.encode(SharedConfig("+33 6 11 22 33 44", 47.218371, -1.553621, 250))
        val m = QrCode.matrix(link, 600)
        val pixels = IntArray(m.width * m.height) { i -> if (m[i % m.width, i / m.width]) 0xFF000000.toInt() else -1 }
        val read = QRCodeReader().decode(BinaryBitmap(HybridBinarizer(RGBLuminanceSource(m.width, m.height, pixels))))
        assertEquals(link, read.text)
        assertEquals(link, ConfigShare.encode(ConfigShare.decode(read.text)!!))
    }
}
