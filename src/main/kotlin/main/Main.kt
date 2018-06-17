package main

import javafx.application.Application
import javafx.application.Platform
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.layout.AnchorPane
import javafx.stage.Stage

class Main : Application() {
    override fun start(primaryStage: Stage) {
        primaryStage.scene = Scene(FXMLLoader.load<AnchorPane>(ClassLoader.getSystemResource("main.fxml")))
        primaryStage.setOnCloseRequest {
            Platform.exit()
        }
        primaryStage.show()
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            launch(Main::class.java, *args)
        }
    }
}