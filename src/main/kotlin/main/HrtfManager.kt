package main

import org.apache.commons.math3.complex.Complex
import org.apache.commons.math3.transform.DftNormalization
import org.apache.commons.math3.transform.FastFourierTransformer
import org.apache.commons.math3.transform.TransformType
import java.io.BufferedReader
import java.io.File
import java.io.FileReader

class HrtfManager(val dir: File) {

    val L = HashMap<Int, Array<Complex>>()
    val R = HashMap<Int, Array<Complex>>()
    val fft = FastFourierTransformer(DftNormalization.STANDARD)

    val size: Int
        get() = L[0]?.size ?: 0

    init {
        val regex = Regex("(L|R)(.*?)e(.*?)a\\.dat")
        dir.listFiles().forEach {
            val res = regex.find(it.name) ?: throw Exception("ファイル名を解析できません")
            val ch = res.groupValues[1]
            val deg = res.groupValues[3].toInt()

            val reader = BufferedReader(FileReader(it))
            val list = ArrayList<Float>()
            while (true) {
                val v = reader.readLine() ?: break
                list.add(v.toFloat())
            }

            val src = FloatArray(list.size) + list

            val irFFT = fft.transform(src.map { it.toDouble() }.toDoubleArray(), TransformType.FORWARD)

            if (ch == "L")
                L[deg] = irFFT
            else if (ch == "R")
                R[deg] = irFFT
        }
    }

    fun applyHRTF(src: FloatArray, ch: String, deg: Int): Array<Complex> {
        if (ch == "L") {
            //FFT変換
            var fftL = fft.transform(src.map { it.toDouble() }.toDoubleArray(), TransformType.FORWARD)

            //周波数領域で畳み込む
            fftL = fftL.mapIndexed { index, complex ->
                complex.multiply(L[deg]?.get(index)) ?: Complex(0.0)
            }.toTypedArray()

            //逆変換
            return fft.transform(fftL, TransformType.INVERSE)
        } else if (ch == "R") {
            //FFT変換
            var fftR = fft.transform(src.map { it.toDouble() }.toDoubleArray(), TransformType.FORWARD)

            //周波数領域で畳み込む
            fftR = fftR.mapIndexed { index, complex ->
                complex.multiply(L[deg]?.get(index)) ?: Complex(0.0)
            }.toTypedArray()

            //逆変換
            return fft.transform(fftR, TransformType.INVERSE)
        }
        return Array<Complex>(0) { _ -> Complex(0.0) }
    }
}