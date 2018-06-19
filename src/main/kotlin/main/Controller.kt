package main

import javafx.application.Platform
import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.SubScene
import javafx.scene.control.Label
import javafx.scene.control.ProgressBar
import javafx.scene.control.Slider
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.Pane
import javafx.scene.layout.StackPane
import javafx.stage.DirectoryChooser
import javafx.stage.FileChooser
import javafx.stage.Stage
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.FrameGrabber
import ui.WindowFactory
import java.net.URL
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.*
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.SourceDataLine
import kotlin.collections.ArrayList

class Controller : Initializable {

    var stage: Stage? = null

    @FXML
    lateinit var root: Pane
    @FXML
    lateinit var slider: Slider
    @FXML
    lateinit var slider2: Slider
    @FXML
    lateinit var subScene: SubScene
    @FXML
    lateinit var subSceneContainer: StackPane
    @FXML
    lateinit var buttonImage: ImageView
    @FXML
    lateinit var irSampleCanvasL: SimpleGraph
    @FXML
    lateinit var irSampleCanvasR: SimpleGraph
    @FXML
    lateinit var dstSampleCanvasL: SimpleGraph
    @FXML
    lateinit var dstSampleCanvasR: SimpleGraph

    @FXML
    lateinit var currentPositionLabel: Label
    @FXML
    lateinit var seekBar: ProgressBar
    @FXML
    lateinit var subSceneManager: SubSceneManager

    lateinit var manager: HrtfManager

    lateinit var srcGrabber: FFmpegFrameGrabber

    var playing = false

    override fun initialize(location: URL?, resources: ResourceBundle?) {

        subSceneManager = SubSceneManager(subScene)

        subScene.widthProperty().bind(subSceneContainer.widthProperty())
        subScene.heightProperty().bind(subSceneContainer.heightProperty())

        slider.valueProperty().addListener { _, _, n ->
            subSceneManager.setStatus(slider2.value.toInt(), slider.value.toInt())
            irSampleCanvasL.data = manager.LRaw["${slider2.value.toInt() / 5 * 5}_${slider.value.toInt() / 5 * 5}"]?.mapIndexed { index, value -> SimpleGraph.DataPoint(index.toDouble(), value.toDouble()) } ?: ArrayList<SimpleGraph.DataPoint>()
            irSampleCanvasR.data = manager.RRaw["${slider2.value.toInt() / 5 * 5}_${slider.value.toInt() / 5 * 5}"]?.mapIndexed { index, value -> SimpleGraph.DataPoint(index.toDouble(), value.toDouble()) } ?: ArrayList<SimpleGraph.DataPoint>()

        }

        slider2.valueProperty().addListener { _, _, n ->
            subSceneManager.setStatus(slider2.value.toInt(), slider.value.toInt())
            irSampleCanvasL.data = manager.LRaw["${slider2.value.toInt() / 5 * 5}_${slider.value.toInt() / 5 * 5}"]?.mapIndexed { index, value -> SimpleGraph.DataPoint(index.toDouble(), value.toDouble()) } ?: ArrayList<SimpleGraph.DataPoint>()
            irSampleCanvasR.data = manager.RRaw["${slider2.value.toInt() / 5 * 5}_${slider.value.toInt() / 5 * 5}"]?.mapIndexed { index, value -> SimpleGraph.DataPoint(index.toDouble(), value.toDouble()) } ?: ArrayList<SimpleGraph.DataPoint>()

        }
//        seekBar.valueProperty().addListener { _, _, n ->
//            srcGrabber.timestamp = n.toLong()
//        }
    }

    /**
     * インパルス応答のデータベースが選択されたときに呼び出される
     */
    fun onImpulseSelect() {
        val dir = DirectoryChooser().showDialog(root.scene.window) ?: return
        val dialog = WindowFactory.buildOnProgressDialog("Processing", "Loading Database...")
        dialog.show()
        Thread {
            manager = HrtfManager(dir)
            println(manager.L)
            Platform.runLater { dialog.close() }
        }.start()
    }

    /**
     * 畳み込み先ファイルが選択されたときに呼び出される
     */
    fun onSrcSelect(actionEvent: ActionEvent) {
        val file = FileChooser().showOpenDialog(root.scene.window) ?: return
        val dialog = WindowFactory.buildOnProgressDialog("Processing", "Loading Music...")
        dialog.show()
        Thread{
            srcGrabber = FFmpegFrameGrabber(file)
            srcGrabber.audioChannels = 1
            srcGrabber.sampleRate = 48000
            srcGrabber.sampleMode = FrameGrabber.SampleMode.FLOAT
            srcGrabber.start()
            Platform.runLater { dialog.close() }
        }.start()

    }

    fun play(actionEvent: ActionEvent) {

        root.scene.window.setOnCloseRequest {
            playing = false
        }

        if (playing) {
            playing = false
            buttonImage.image = Image(ClassLoader.getSystemResource("baseline_play_arrow_black_48dp.png").toString())
            return
        }
        playing = true
        buttonImage.image = Image(ClassLoader.getSystemResource("baseline_pause_black_48dp.png").toString())
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
            var max = 1f
            var prevSample = FloatArray(manager.size / 2)
            while (playing) {
                val sample = readSamples(manager.size / 2) ?: break

                val src = prevSample + sample
                val dstL = manager.applyHRTF(src, "L", slider.value.toInt() / 5 * 5, slider2.value.toInt() / 5 * 5)
                val dstR = manager.applyHRTF(src, "R", slider.value.toInt() / 5 * 5, slider2.value.toInt() / 5 * 5)

                Platform.runLater {
                    //グラフ描画
                    dstSampleCanvasL.data = dstL.slice(0 until dstL.size / 2).mapIndexed { index, value -> SimpleGraph.DataPoint(index.toDouble(), value.real) }
                    dstSampleCanvasR.data = dstR.slice(0 until dstL.size / 2).mapIndexed { index, value -> SimpleGraph.DataPoint(index.toDouble(), value.real) }
                    seekBar.progress = srcGrabber.timestamp / srcGrabber.lengthInTime.toDouble()
                    currentPositionLabel.text = srcGrabber.timestamp.long2TimeText()
                }


                val dst = FloatArray(dstL.size + dstR.size)
                for (i in 0 until dstL.size) {
                    dst[i * 2] = dstL[i].real.toFloat()
                    dst[i * 2 + 1] = dstR[i].real.toFloat()
                }

                //正規化
                max = Math.max(max, dst.max() ?: 0f)
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
                audioLine.write(arr, 0, arr.size)
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


    fun prev(actionEvent: ActionEvent) {
        srcGrabber.timestamp = 0
    }

    fun Long.long2TimeText(): String {
        val a = this / 1000_000
        return "${a / 60}:${String.format("%02d", a % 60)}"
    }

    fun showProgressDialog() {
        val stage = Stage()
    }
}