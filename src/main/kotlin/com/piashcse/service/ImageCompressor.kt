package com.piashcse.service

import org.slf4j.LoggerFactory
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import javax.imageio.stream.FileImageOutputStream

object ImageCompressor {
    private val log = LoggerFactory.getLogger(ImageCompressor::class.java)

    fun compress(filePath: String) {
        val file = File(filePath)
        if (!file.exists()) return

        val ext = file.extension.lowercase()
        if (ext !in listOf("jpg", "jpeg", "png")) return

        val originalSize = file.length()

        val image = ImageIO.read(file) ?: return
        val scaled = scaleDown(image)
        if (scaled !== image) image.flush()

        val writerFormat = if (ext == "png") "png" else "jpeg"
        val writers = ImageIO.getImageWritersByFormatName(writerFormat)
        if (!writers.hasNext()) return

        val writer = writers.next()
        try {
            FileImageOutputStream(file).use { output ->
                writer.output = output
                val param = writer.defaultWriteParam
                if (writerFormat == "jpeg") {
                    param.compressionMode = ImageWriteParam.MODE_EXPLICIT
                    param.compressionQuality = 0.8f
                }
                writer.write(null, javax.imageio.IIOImage(scaled, null, null), param)
            }
        } finally {
            writer.dispose()
            scaled.flush()
        }

        val saved = (originalSize - file.length()) * 100 / if (originalSize > 0) originalSize else 1
        log.info("Compressed {}: {}KB -> {}KB ({}% saved)", filePath, originalSize / 1024, file.length() / 1024, saved)
    }

    private fun scaleDown(image: BufferedImage): BufferedImage {
        val (w, h) = image.width to image.height
        if (w <= 2048 && h <= 2048) return image

        val ratio = minOf(2048.0 / w, 2048.0 / h)
        val scaled = BufferedImage((w * ratio).toInt(), (h * ratio).toInt(), BufferedImage.TYPE_INT_RGB)
        val g = scaled.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        g.drawImage(image, 0, 0, scaled.width, scaled.height, null)
        g.dispose()
        return scaled
    }
}
