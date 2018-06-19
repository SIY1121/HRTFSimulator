package main

import org.apache.commons.math3.complex.Complex
import org.apache.commons.math3.transform.DftNormalization
import org.apache.commons.math3.transform.FastFourierTransformer
import org.apache.commons.math3.transform.TransformType
import java.io.BufferedReader
import java.io.File
import java.io.FileReader

class HrtfManager(val dir: File) {

    val L = HashMap<String, Array<Complex>>()
    val R = HashMap<String, Array<Complex>>()
    val LRaw = HashMap<String,FloatArray>()
    val RRaw = HashMap<String,FloatArray>()

    val fft = FastFourierTransformer(DftNormalization.STANDARD)

    val size: Int
        get() = L["0_0"]?.size ?: throw Exception("サイズを返せません")

    init {
        val regex = Regex("(L|R)(.*?)d(.*?)e(.*?)a\\.dat")
        dir.listFiles().forEach {
            if (it.isDirectory)
                it.listFiles().forEach {
                    val res = regex.find(it.name) ?: throw Exception("ファイル名を解析できません")
                    val ch = res.groupValues[1]
                    val deg = res.groupValues[4].toInt()
                    val elev = res.groupValues[3].toInt()

                    val reader = BufferedReader(FileReader(it))
                    val list = ArrayList<Float>()
                    while (true) {
                        val v = reader.readLine() ?: break
                        list.add(v.toFloat())
                    }

                    val src = FloatArray(list.size) + list

                    val irFFT = fft.transform(src.map { it.toDouble() }.toDoubleArray(), TransformType.FORWARD)

                    if (ch == "L"){
                        L["${elev}_$deg"] = irFFT
                        LRaw["${elev}_$deg"] = list.toFloatArray()
                    }
                    else if (ch == "R"){
                        R["${elev}_$deg"] = irFFT
                        RRaw["${elev}_$deg"] = list.toFloatArray()
                    }
                }

        }
    }

    fun applyHRTF(src: FloatArray, ch: String, deg: Int, elev: Int): Array<Complex> {
        if (ch == "L") {
            //FFT変換
            var fftL = fft.transform(src.map { it.toDouble() }.toDoubleArray(), TransformType.FORWARD)

            //周波数領域で畳み込む
            fftL = fftL.mapIndexed { index, complex ->
                complex.multiply(L["${elev}_$deg"]?.get(index)) ?: Complex(0.0)
            }.toTypedArray()

            //逆変換
            return fft.transform(fftL, TransformType.INVERSE)
        } else if (ch == "R") {
            //FFT変換
            var fftR = fft.transform(src.map { it.toDouble() }.toDoubleArray(), TransformType.FORWARD)

            //周波数領域で畳み込む
            fftR = fftR.mapIndexed { index, complex ->
                complex.multiply(R["${elev}_$deg"]?.get(index)) ?: Complex(0.0)
            }.toTypedArray()

            //逆変換
            return fft.transform(fftR, TransformType.INVERSE)
        }
        return Array<Complex>(0) { _ -> Complex(0.0) }
    }
}