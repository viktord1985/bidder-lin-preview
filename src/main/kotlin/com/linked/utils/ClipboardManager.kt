package com.linked.utils

import java.awt.Toolkit
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection

object ClipboardManager {

    private val clipboard: Clipboard = Toolkit.getDefaultToolkit().systemClipboard

    // Function to set text to the clipboard
    fun setClipboardContent(text: String) {
        val stringSelection = StringSelection(text)
        clipboard.setContents(stringSelection, null)
    }

    // Function to get text from the clipboard
    fun getClipboardContent(): String? {
        val contents = clipboard.getContents(null)
        return try {
            contents.getTransferData(DataFlavor.stringFlavor) as String?
        } catch (e: Exception) {
            null  // Handle the case where clipboard does not contain string data
        }
    }
}
