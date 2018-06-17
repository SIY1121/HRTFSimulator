package main

import java.io.BufferedReader
import java.io.File
import java.io.FileReader

class HrtfManager(val dir: File) {

    val L = HashMap<Int, FloatArray>()
    val R = HashMap<Int, FloatArray>()

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
            L[deg] = list.toFloatArray()
        }
    }
}