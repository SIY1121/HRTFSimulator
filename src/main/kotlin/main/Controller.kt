package main

import javafx.fxml.FXML
import javafx.scene.layout.Pane
import javafx.stage.DirectoryChooser
import javafx.stage.FileChooser

class Controller {
    @FXML
    lateinit var root: Pane

    lateinit var manager: HrtfManager

    fun onSrcSelect() {
        val dir = DirectoryChooser().showDialog(root.scene.window) ?: return
        manager = HrtfManager(dir)
    }
}