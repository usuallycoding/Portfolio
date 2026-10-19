package com.usuallycoding.portfolio.server

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

object TestImages {
    private fun encode(image: BufferedImage, format: String): ByteArray =
        ByteArrayOutputStream().also { check(ImageIO.write(image, format, it)) }.toByteArray()

    fun jpeg(width: Int, height: Int): ByteArray {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        image.createGraphics().apply { color = Color.ORANGE; fillRect(0, 0, width, height) }
        return encode(image, "jpg")
    }

    /** A PNG whose left half is fully transparent. */
    fun png(width: Int, height: Int, transparentLeftHalf: Boolean = false): ByteArray {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val g = image.createGraphics()
        g.color = Color(10, 120, 200, 255)
        g.fillRect(0, 0, width, height)
        if (transparentLeftHalf) {
            for (x in 0 until width / 2) for (y in 0 until height) image.setRGB(x, y, 0)
        }
        g.dispose()
        return encode(image, "png")
    }

    fun read(bytes: ByteArray): BufferedImage = ImageIO.read(ByteArrayInputStream(bytes))
}
