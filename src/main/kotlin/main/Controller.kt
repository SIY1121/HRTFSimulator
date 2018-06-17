package main

import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.control.Slider
import javafx.scene.layout.Pane
import javafx.stage.DirectoryChooser
import javafx.stage.FileChooser
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.FFmpegFrameRecorder
import org.bytedeco.javacv.FrameGrabber
import java.io.File
import java.net.URL
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.*
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.SourceDataLine

class Controller : Initializable {
    override fun initialize(location: URL?, resources: ResourceBundle?) {
        println("initialize")
    }

    @FXML
    lateinit var root: Pane
    @FXML
    lateinit var slider: Slider

    lateinit var manager: HrtfManager

    lateinit var srcGrabber: FFmpegFrameGrabber

    fun onImpulseSelect() {
        val dir = DirectoryChooser().showDialog(root.scene.window) ?: return
        manager = HrtfManager(dir)
        println(manager.L)
    }

    fun play(actionEvent: ActionEvent) {
        srcGrabber = FFmpegFrameGrabber("D:\\OneDrive - 筑波大学\\Music\\塩谷哲 - ACCORDING TO LA METEO.mp3")
        srcGrabber.audioChannels = 1
        srcGrabber.sampleRate = 48000
        srcGrabber.sampleMode = FrameGrabber.SampleMode.FLOAT
        srcGrabber.start()
        Thread {
            val audioFormat = AudioFormat((srcGrabber.sampleRate.toFloat() ?: 0f), 16, 2, true, false)

            val info = DataLine.Info(SourceDataLine::class.java, audioFormat)
            val audioLine = AudioSystem.getLine(info) as SourceDataLine
            audioLine.open(audioFormat)
            audioLine.start()

            //val rec = FFmpegFrameRecorder(File("out.mp3"), 2)
            //rec.sampleRate = 44100
            //rec.audioBitrate = 192_000

            //rec.start()

            var prevSample = FloatArray(manager.size / 2)
            while (true) {
                val sample = readSamples(manager.size / 2) ?: break

                val src = prevSample + sample
                val dstL = manager.applyHRTF(src, "L", slider.value.toInt() / 5 * 5)
                val dstR = manager.applyHRTF(src, "R", slider.value.toInt() / 5 * 5)

                val dst = FloatArray(dstL.size + dstR.size)
                for (i in 0 until dstL.size) {
                    dst[i * 2] = dstL[i].real.toFloat()
                    dst[i * 2 + 1] = dstR[i].real.toFloat()
                }

                //正規化
                val max = 300f
                println(max)
                for (i in 0 until dst.size)
                    dst[i] /= max

                //shortに変換してバイト配列に変換する
                //円状畳み込み結果である前半は切り捨て
                val buf = ByteBuffer.allocate(dst.size).order(ByteOrder.LITTLE_ENDIAN)
                for (i in 0 until dst.size / 2) {
                    buf.putShort((dst[i] * Short.MAX_VALUE).toShort())
                }
                buf.position(0)

                val arr = buf.array()
                audioLine.write(arr, 0, arr.size )
                //rec.recordSamples(44100, 2, buf)

                prevSample = sample
            }
            //rec.stop()
        }.start()
    }

    var tmpBuffer: FloatBuffer? = null
    /**
     * 指定された数だけ、畳み込み先のサンプルを返す
     */
    private fun readSamples(size: Int): FloatArray? {
        val result = FloatArray(size)
        var read = 0
        while (read < size) {
            if (tmpBuffer == null || tmpBuffer?.remaining() == 0)
                tmpBuffer = srcGrabber?.grabSamples()?.samples?.get(0) as? FloatBuffer ?: break

            val toRead = Math.min(tmpBuffer?.remaining() ?: 0, size - read)
            tmpBuffer?.get(result, read, toRead)
            read += toRead
        }
        return if (read > 0) result else null
    }

    /**
     *指定された配列のデータを置き換える
     */
    fun FloatArray.replaceRange(start: Int, end: Int, replacement: FloatArray) {
        if (end - start != replacement.size) throw Exception("置き換えの配列と範囲の大きさが一致しません")
        for (i in start until end)
            this[i] = replacement[i - start]
    }

    /**
     * 渡された配列を長さが２の累乗になるようにパディングして返す
     */
    fun FloatArray.toPower2(): FloatArray {
        var i = 1.0
        while (this.size > Math.pow(2.0, i)) {
            i++
        }

        return this + FloatArray(Math.pow(2.0, i).toInt() - this.size)
    }
}