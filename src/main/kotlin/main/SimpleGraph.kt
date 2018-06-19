package main

import javafx.beans.binding.Bindings
import javafx.geometry.Bounds
import javafx.geometry.Insets
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.control.Label
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import javafx.scene.text.Font
import javafx.scene.text.Text
import javafx.scene.transform.Affine
import javax.xml.crypto.Data

class SimpleGraph : Pane() {
    data class DataPoint(val x: Double, val y: Double)

    var xNegative = false
    var yNegative = false
    var yTranslateToPositive = false

    private val canvas = Canvas()
    private val g: GraphicsContext

    var background: Paint = Color.WHITE


    var data: List<DataPoint> = ArrayList()
        set(value) {
            field = if (yTranslateToPositive) {
                val min = value.minBy { it.y }?.y ?: 0.0
                value.map { DataPoint(it.x, it.y - min) }
            } else {
                value
            }
            draw()
        }

    var title = "Title"
    var xAxisName = "x"
    var yAxisName = "y"

    init {
        g = canvas.graphicsContext2D

        val sampleData = ArrayList<DataPoint>()
        for (i in 0 until 1000) {
            sampleData.add(DataPoint(i.toDouble() - 500, Math.sin(i / 100.0)))
        }
        data = sampleData

        widthProperty().addListener { _, _, n ->
            canvas.width = n.toDouble()
            padding = Insets(
                    20.0, 20.0,
                    if (yNegative) canvas.height / 2 else 20.0,
                    if (xNegative) canvas.width / 2 else 20.0)
            draw()
        }
        heightProperty().addListener { _, _, n ->
            canvas.height = n.toDouble()
            padding = Insets(
                    20.0, 20.0,
                    if (yNegative) canvas.height / 2 else 20.0,
                    if (xNegative) canvas.width / 2 else 20.0)
            draw()
        }
        children.add(canvas)
        g.font = Font(40.0)
    }

    private fun draw() {
        g.fill = background
        g.stroke = Color.BLACK

        g.fillRect(0.0, 0.0, canvas.width, canvas.height)
        g.strokeLine(padding.left, padding.top, padding.left, canvas.height - padding.bottom)
        g.strokeLine(padding.left, canvas.height - padding.bottom, canvas.width - padding.right, canvas.height - padding.bottom)

        g.fill = Color.BLACK
        g.font = Font(20.0)
        g.fillText(title, canvas.width / 2 - estimateTextSize(title).width / 2, estimateTextSize(title).height)
        g.fillText(yAxisName, padding.left - estimateTextSize(yAxisName).width / 2, estimateTextSize(yAxisName).height / 2)
        g.fillText(xAxisName, canvas.width - padding.right, canvas.height - padding.bottom + estimateTextSize(xAxisName).height / 2)

        val xMax = data.map { Math.abs(it.x) }.max() ?: 1.0
        val yMax = data.map { Math.abs(it.y) }.max() ?: 1.0

        data.forEach {
            val y = padding.top + (1.0 - (it.y / yMax)) * (canvas.height - padding.top - padding.bottom)
            val x = padding.left + (it.x / xMax) * (canvas.width - padding.right - padding.left)

            g.strokeLine(x, y, x, canvas.height - padding.bottom)
        }
    }

    private fun estimateTextSize(text: String): Bounds {
        val t = Text(text)
        t.font = g.font
        return t.boundsInLocal
    }

}