package main

import com.aquafx_project.AquaFx
import javafx.application.Application
import javafx.application.Platform
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.layout.AnchorPane
import javafx.stage.Stage
import java.io.File

class Main : Application() {
    override fun start(primaryStage: Stage) {
        AquaFx.style()
        val loader = FXMLLoader(ClassLoader.getSystemResource("main.fxml"))
        primaryStage.scene = Scene(loader.load<AnchorPane>())
        primaryStage.title = "HRTF Simulator"
        loader.getController<Controller>().stage = primaryStage
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