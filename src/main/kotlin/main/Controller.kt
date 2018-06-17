package main

import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.*
import javafx.scene.control.Button
import javafx.scene.control.Slider
import javafx.scene.layout.Pane
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.scene.paint.PhongMaterial
import javafx.scene.shape.Box
import javafx.scene.shape.Circle
import javafx.scene.shape.Sphere
import javafx.scene.transform.Rotate
import javafx.stage.DirectoryChooser
import javafx.stage.FileChooser
import javafx.stage.Stage
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.FrameGrabber
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

    var stage: Stage? = null
        set(value) {
            field = value
            //stage?.scene?.camera = PerspectiveCamera(true)
        }

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

    lateinit var manager: HrtfManager

    lateinit var srcGrabber: FFmpegFrameGrabber

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        subScene.camera = PerspectiveCamera(true)
        subScene.camera.translateZ = -400.0 * 45.0 / 360.0 * (Math.PI * 2)
        subScene.camera.translateY = -400.0 * 45.0 / 360.0 * (Math.PI * 2)
        subScene.camera.rotationAxis = Rotate.X_AXIS
        subScene.camera.rotate = -45.0
        subScene.camera.farClip = 1000.0

        val sphere = Sphere(10.0).apply {

            material = PhongMaterial().apply {
                diffuseColor = Color.BLUE
                specularColor = Color.SKYBLUE
            }
        }
        (subScene.root as Group).children.add(sphere)


        val circle = Circle(100.0).apply{
            fill = Color.TRANSPARENT
            stroke = Color.YELLOW
            rotationAxis = Rotate.X_AXIS
            rotate = 90.0
        }
        (subScene.root as Group).children.add(circle)


        val box = Box(10.0, 10.0, 10.0).apply {

            material = PhongMaterial().apply {
                diffuseColor = Color.GREEN
                specularColor = Color.YELLOWGREEN
            }
        }

        (subScene.root as Group).children.add(box)
        (subScene.root as Group).children.add(PointLight().apply {
            translateX = 50.0
            translateY = -40.0
        })
        (subScene.root as Group).children.add(AmbientLight(Color.rgb(80, 80, 80, 0.5)))
        subScene.isManaged = false

        subScene.widthProperty().bind(subSceneContainer.widthProperty())
        subScene.heightProperty().bind(subSceneContainer.heightProperty())

        slider.valueProperty().addListener { _, _, n ->
            box.translateX = Math.cos((n.toDouble() + 90.0) / 360.0 * Math.PI * 2) * 100
            box.translateZ = Math.sin((n.toDouble() + 90.0) / 360.0 * Math.PI * 2) * 100

        }
    }

    fun onImpulseSelect() {
        val dir = DirectoryChooser().showDialog(root.scene.window) ?: return
        manager = HrtfManager(dir)
        println(manager.L)
    }

    fun play(actionEvent: ActionEvent) {

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
            while (true) {
                val sample = readSamples(manager.size / 2) ?: break

                val src = prevSample + sample
                val dstL = manager.applyHRTF(src, "L", slider.value.toInt() / 5 * 5, slider2.value.toInt() / 5 * 5)
                val dstR = manager.applyHRTF(src, "R", slider.value.toInt() / 5 * 5, slider2.value.toInt() / 5 * 5)

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

    fun onSrcSelect(actionEvent: ActionEvent) {
        val file = FileChooser().showOpenDialog(root.scene.window) ?: return
        srcGrabber = FFmpegFrameGrabber(file)
        srcGrabber.audioChannels = 1
        srcGrabber.sampleRate = 48000
        srcGrabber.sampleMode = FrameGrabber.SampleMode.FLOAT
        srcGrabber.start()
    }
}