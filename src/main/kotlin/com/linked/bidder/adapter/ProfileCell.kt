package com.linked.bidder.adapter

import com.linked.bidder.appConfig
import com.linked.db.findChromeProfileById
import com.linked.db.saveLinkedPostDB
import com.linked.model.LinkedPostDB
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.control.ListCell
import javafx.scene.control.TextInputDialog
import javafx.scene.layout.HBox
import javafx.scene.text.Text

class ProfileCell(val linkedPostDB: LinkedPostDB) : ListCell<String?>() {

    private val content: HBox
    private val name: Text
    private val addProfile: Button

    init {
        name = Text()
        name.style = "-fx-font-size: 12.0" // "-fx-font-weight: bold; -fx-font-size: 12.0"
        addProfile = Button()
        addProfile.text = "Add"
        content = HBox(addProfile, name)
        content.spacing = 4.0
        content.alignment = Pos.CENTER_LEFT
    }

    override fun updateItem(item: String?, empty: Boolean) {
        super.updateItem(item, empty)
        if (item == null || empty) {
            graphic = null
        } else {
            var status = "NA"
            val profile = findChromeProfileById(linkedPostDB, item)
            if (profile != null) {
                status = profile.status
                //println("profile lastActivityLocalDate: ${profile.getLastActivityLocalDate()}")
                //println("profile isOld: ${profile.isOldProfile()}")
            }
            name.text = item + "|" + appConfig.getProfileNameByChromeProfileName(item) + "|" + status
            addProfile.setOnAction {
                val htmlLink = showInputDialog("Add Connected Profile", "Profile", "Url:")
                if (htmlLink != null) {
                    profile?.addConnectedProfile(htmlLink)
                    saveLinkedPostDB(linkedPostDB)
                }
            }
            graphic = content
        }
    }

}

fun showInputDialog(title: String, header: String, content: String): String? {
    val dialog = TextInputDialog().apply {
        this.title = title
        this.headerText = header
        this.contentText = content
    }

    val result = dialog.showAndWait()
    return if (result.isPresent) result.get() else null
}