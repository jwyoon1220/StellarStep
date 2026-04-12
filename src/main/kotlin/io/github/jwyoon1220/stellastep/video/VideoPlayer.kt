package io.github.jwyoon1220.stellastep.video

import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import uk.co.caprica.vlcj.factory.MediaPlayerFactory
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormat
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormatCallback
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.RenderCallback
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.format.RV32BufferFormat
import java.io.File
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

/**
 * VLCJ-based background video player for libGDX.
 *
 * Usage:
 *   val vp = VideoPlayer()
 *   if (vp.available) { vp.open("assets/video/myvideo.mp4"); vp.play() }
 *   // in render():
 *   vp.update()   // call every frame on GL thread
 *   vp.texture?.let { batch.draw(it, 0f, 0f, w, h) }
 *   // on dispose:
 *   vp.dispose()
 *
 * Falls back gracefully when LibVLC is not installed (available = false).
 *
 * NOTE: Video frames come in VLC's RV32 (BGRA) format; we convert to RGBA for libGDX.
 */
class VideoPlayer {
    /** True only if LibVLC native library was found and VLCJ initialised successfully. */
    var available: Boolean = false
        private set

    private var factory: MediaPlayerFactory? = null
    private var player: EmbeddedMediaPlayer? = null

    @Volatile private var videoWidth  = 0
    @Volatile private var videoHeight = 0

    // Ping-pong buffers: VLCJ writes to the back buffer; GL thread reads from the front.
    @Volatile private var backBuffer:  IntArray = IntArray(0)
    @Volatile private var frontBuffer: IntArray = IntArray(0)
    private val newFrameReady = AtomicBoolean(false)

    /** The libGDX texture containing the latest decoded video frame. Null if unavailable. */
    var texture: Texture? = null
        private set
    private var pixmap: Pixmap? = null

    init {
        try {
            NativeDiscovery().discover()
            factory = MediaPlayerFactory("--quiet", "--no-osd", "--no-stats", "--no-video-title-show",
                "--no-sub-autodetect-file", "--drop-late-frames", "--skip-frames")
            val mp = factory!!.mediaPlayers().newEmbeddedMediaPlayer()

            val formatCallback = object : BufferFormatCallback {
                override fun getBufferFormat(srcW: Int, srcH: Int): BufferFormat {
                    videoWidth  = srcW
                    videoHeight = srcH
                    val size = srcW * srcH
                    backBuffer  = IntArray(size)
                    frontBuffer = IntArray(size)
                    return RV32BufferFormat(srcW, srcH)
                }
                override fun allocatedBuffers(buffers: Array<ByteBuffer>) {}
                override fun newFormatSize(bW: Int, bH: Int, dW: Int, dH: Int) {}
            }
            val renderCallback = object : RenderCallback {
                override fun lock(mp: MediaPlayer) {}
                override fun unlock(mp: MediaPlayer) {}
                override fun display(mp: MediaPlayer, nativeBuffers: Array<ByteBuffer>, fmt: BufferFormat, dW: Int, dH: Int) {
                    val back = backBuffer
                    if (back.isEmpty()) return
                    val ib = nativeBuffers[0].asIntBuffer()
                    ib.rewind()
                    val len = back.size.coerceAtMost(ib.remaining())
                    ib.get(back, 0, len)
                    // Swap B and R channels: VLC RV32 is BGRA, libGDX RGBA8888 needs RGBA
                    for (i in 0 until len) {
                        val v = back[i]
                        back[i] = (v and 0xFF00FF00.toInt()) or
                                  ((v and 0x00FF0000) shr 16) or
                                  ((v and 0x000000FF) shl 16)
                    }
                    // Swap buffers
                    val tmp   = frontBuffer
                    frontBuffer = back
                    backBuffer  = tmp
                    newFrameReady.set(true)
                }
            }

            val surface = factory!!.videoSurfaces()
                .newVideoSurface(formatCallback, renderCallback, false)
            mp.videoSurface().set(surface)
            player = mp
            available = true
        } catch (e: Exception) {
            println("VideoPlayer: LibVLC not available – video disabled. (${e.message})")
            available = false
        }
    }

    /** Open a video file. Path is absolute or relative to CWD. */
    fun open(path: String) {
        if (!available) return
        val file = File(path)
        if (!file.exists()) { println("VideoPlayer: file not found: $path"); return }
        player?.media()?.prepare(file.absolutePath)
    }

    fun play()  { if (available) player?.controls()?.play() }
    fun pause() { if (available) player?.controls()?.pause() }
    fun stop()  { if (available) player?.controls()?.stop() }
    fun isPlaying(): Boolean = available && (player?.status()?.isPlaying ?: false)

    /**
     * Must be called once per frame on the GL thread.
     * Uploads any new decoded frame to [texture].
     */
    fun update() {
        if (!available || !newFrameReady.compareAndSet(true, false)) return
        val w = videoWidth;  val h = videoHeight
        if (w == 0 || h == 0) return
        val front = frontBuffer
        if (front.size < w * h) return

        // Lazily create / recreate Pixmap & Texture when dimensions change
        if (pixmap == null || pixmap!!.width != w || pixmap!!.height != h) {
            pixmap?.dispose(); texture?.dispose()
            pixmap  = Pixmap(w, h, Pixmap.Format.RGBA8888)
            texture = Texture(pixmap)
            texture!!.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        }
        // Copy int pixels to Pixmap ByteBuffer
        val pm  = pixmap!!
        val buf = pm.pixels
        buf.rewind()
        val ib  = buf.asIntBuffer()
        ib.put(front, 0, w * h)
        buf.rewind()
        // Re-upload to GPU
        texture!!.draw(pm, 0, 0)
    }

    fun dispose() {
        try { player?.release() } catch (_: Exception) {}
        try { factory?.release() } catch (_: Exception) {}
        pixmap?.dispose()
        texture?.dispose()
        pixmap  = null
        texture = null
    }
}
