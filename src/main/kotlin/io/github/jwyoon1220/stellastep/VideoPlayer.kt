package io.github.jwyoon1220.stellastep

import io.github.jwyoon1220.stellastep.util.ErrorHandler
import uk.co.caprica.vlcj.factory.MediaPlayerFactory
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer
import uk.co.caprica.vlcj.player.embedded.videosurface.VideoSurface
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormat
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormatCallback
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.RenderCallback
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.format.RV32BufferFormat
import java.awt.DisplayMode
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import javax.swing.SwingUtilities

object VideoPlayer {

    private var factory: MediaPlayerFactory? = null
    lateinit var mediaPlayer: EmbeddedMediaPlayer

    @Volatile var videoWidth: Int = 0
    @Volatile var videoHeight: Int = 0

    // 캐시: image data array
    @Volatile private var cachedPixelArray: IntArray? = null



    fun init(width: Int, height: Int, frame: StellarStep) {
        NativeDiscovery().discover()

        factory = MediaPlayerFactory(
            "--quiet",
            "--no-osd",
            "--no-stats",
            "--no-video-title-show",
            "--no-sub-autodetect-file",
            "--drop-late-frames",
            "--skip-frames",
            "--avcodec-hw=d3d11va",
            "--video-filter=",
            "--audio-filter="
        )
        mediaPlayer = factory!!.mediaPlayers().newEmbeddedMediaPlayer()

        frame.videoImage = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        cachedPixelArray = (frame.videoImage!!.raster.dataBuffer as DataBufferInt).data

        val formatCallback: BufferFormatCallback = object : BufferFormatCallback {
            override fun getBufferFormat(sourceWidth: Int, sourceHeight: Int): BufferFormat {
                videoWidth = sourceWidth
                videoHeight = sourceHeight

                if (frame.videoImage?.width != sourceWidth || frame.videoImage?.height != sourceHeight) {
                    frame.videoImage = BufferedImage(sourceWidth, sourceHeight, BufferedImage.TYPE_INT_ARGB)
                    cachedPixelArray = (frame.videoImage!!.raster.dataBuffer as DataBufferInt).data
                }
                return RV32BufferFormat(sourceWidth, sourceHeight)
            }

            override fun allocatedBuffers(buffers: Array<ByteBuffer>) { /* no-op */ }
            override fun newFormatSize(bufferWidth: Int, bufferHeight: Int, displayWidth: Int, displayHeight: Int) { /* no-op */ }
        }

        val renderCallback: RenderCallback = object : RenderCallback {
            override fun lock(mediaPlayer: MediaPlayer) { /* no-op */ }
            override fun unlock(mediaPlayer: MediaPlayer) { /* no-op */ }

            override fun display(
                mediaPlayer: MediaPlayer,
                nativeBuffers: Array<ByteBuffer>,
                bufferFormat: BufferFormat,
                displayWidth: Int,
                displayHeight: Int
            ) {
                val imgArray = cachedPixelArray ?: return
                val intBuffer = nativeBuffers[0].asIntBuffer()
                intBuffer.rewind()
                val len = imgArray.size.coerceAtMost(intBuffer.remaining())
                intBuffer.get(imgArray, 0, len)
            }
        }

        val videoSurface: VideoSurface = factory!!.videoSurfaces()
            .newVideoSurface(formatCallback, renderCallback, false)
        mediaPlayer.videoSurface().set(videoSurface)
    }

    fun play(path: String) {
        if (mediaPlayer.status().isPlaying) {
            mediaPlayer.controls().stop()
        }
       try {
           if (!File("assets/video/$path").exists()) {
               throw IOException("비디오 파일을 찾을 수 없습니다:\n$path")
           }
           mediaPlayer.media().play("assets/video/$path")
       } catch (e: IOException) {
              ErrorHandler.handleError(e, true, 1, "비디오 재생 오류")
       }
    }

    fun stop() {
        if (mediaPlayer.status().isPlaying) {
            mediaPlayer.controls().stop()
        }
    }

    fun release() {
        mediaPlayer.release()
        factory?.release()
        factory = null
    }

    fun updateVideo(img: BufferedImage, g: Graphics2D, screenSize: DisplayMode) {
        val srcW = videoWidth.takeIf { it > 0 } ?: img.width
        val srcH = videoHeight.takeIf { it > 0 } ?: img.height

        val destW = screenSize.width
        val destH = screenSize.height

        val scale = minOf(destW.toDouble() / srcW, destH.toDouble() / srcH)
        val drawW = (srcW * scale).toInt()
        val drawH = (srcH * scale).toInt()

        val offsetX = (destW - drawW) / 2
        val offsetY = (destH - drawH) / 2

        g.drawImage(img, offsetX, offsetY, offsetX + drawW, offsetY + drawH, 0, 0, srcW, srcH, null)
    }

    fun getTime(): Long = mediaPlayer.status()?.time() ?: 0L
    fun getRatio(): Float = mediaPlayer.status().position()
    fun isPlaying(): Boolean = mediaPlayer.status().isPlaying
}