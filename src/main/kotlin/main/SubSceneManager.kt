package main

import javafx.scene.*
import javafx.scene.paint.Color
import javafx.scene.paint.PhongMaterial
import javafx.scene.shape.Box
import javafx.scene.shape.Circle
import javafx.scene.shape.Sphere
import javafx.scene.transform.Rotate

class SubSceneManager(subScene : SubScene) {
    val root = subScene.root as Group

    val guideLine = Circle(100.0).apply {
        fill = Color.TRANSPARENT
        stroke = Color.RED
        rotationAxis = Rotate.X_AXIS
        rotate = 180.0
    }

    val sphere = Sphere(10.0).apply {

        material = PhongMaterial().apply {
            diffuseColor = Color.BLUE
            specularColor = Color.SKYBLUE
        }
    }

    val circle = Circle(100.0).apply {
        fill = Color.TRANSPARENT
        stroke = Color.YELLOW
        rotationAxis = Rotate.X_AXIS
        rotate = 90.0
    }

    val box = Box(10.0, 10.0, 10.0).apply {

        material = PhongMaterial().apply {
            diffuseColor = Color.GREEN
            specularColor = Color.YELLOWGREEN
        }
    }
    init{
        subScene.camera = PerspectiveCamera(true)
        subScene.camera.translateZ = -400.0 * 30.0 / 360.0 * (Math.PI * 2) - 150
        subScene.camera.translateY = -400.0 * 30.0 / 360.0 * (Math.PI * 2)
        subScene.camera.rotationAxis = Rotate.X_AXIS
        subScene.camera.rotate = -30.0
        subScene.camera.farClip = 1000.0

        (subScene.root as Group).children.add(guideLine)
        (subScene.root as Group).children.add(sphere)
        (subScene.root as Group).children.add(circle)
        (subScene.root as Group).children.add(box)

        (subScene.root as Group).children.add(PointLight().apply {
            translateX = 100.0
            translateY = -200.0
        })

        (subScene.root as Group).children.add(AmbientLight(Color.rgb(80, 80, 80, 0.5)))
        subScene.isManaged = false
    }

    /**
     * 3Dビューを更新
     */
    fun setStatus(elev : Int , deg : Int){
        box.translateX = Math.cos((deg + 90.0) / 360.0 * Math.PI * 2) * 100 * Math.cos(elev / 360.0 * Math.PI * 2)
        box.translateZ = Math.sin((deg + 90.0) / 360.0 * Math.PI * 2) * 100 * Math.cos(elev / 360.0 * Math.PI * 2)
        box.translateY = Math.sin(elev / 360.0 * Math.PI * 2) * -100

        guideLine.rotationAxis = Rotate.Y_AXIS
        guideLine.rotate = -deg + 90.0
    }
}