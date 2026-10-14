package com.linked.model.command

import com.linked.bidder.infoMessage
import com.linked.bidder.linkedAlertPingBot
import com.linked.bidder.utils.Logger
import com.linked.utils.sendScriptToJSShellCommand
import com.linked.utils.shouldPerformAction
import com.linked.utils.sleepWithRandom

class CommandScroll(
    val name: String,
    val type: String,
    val delay: String,
    val probability: String,
) {
    fun run() {
        if (shouldPerformAction(probability.toDouble())) {

            infoMessage(".. (${name}/${type})")

            linkedAlertPingBot.sendMessage(".. (${name}/${type})")

            if (type == "top") {
                scrollToPageTop()
            } else if (type == "bottom") {
                scrollToPageBottom()
            } else {
                Logger.logError("!!! Unknown scroll type !!!")
            }
            sleepWithRandom(delay.toInt(), delay.toInt())
        } else {
            Logger.logInfo("[Skip scroll type: $type]")
        }
    }
}

fun scrollToPageTop() {

    Logger.logInfo("scrollToPageTop")

    sendScriptToJSShellCommand(
        "window.scrollTo({ left: 0, top: 0, behavior: \"smooth\" });",
        "scrollToPageTop"
    )
}

fun scrollToPageBottom() {

    Logger.logInfo("scrollToPageBottom")

    sendScriptToJSShellCommand(
        "window.scrollTo({ left: 0, top: document.body.scrollHeight, behavior: \"smooth\" });",
        "scrollToPageBottom"
    )
}

fun getCommandScroll(text: String): CommandScroll {

    return CommandScroll(
        text.split("|")[0],
        text.split("|")[1],
        text.split("|")[2],
        text.split("|")[3],
    )
}

fun scrollToAriaLabel(label: String) {

    sendScriptToJSShellCommand(
        "document.querySelector('[aria-label^=\"$label\"]').scrollIntoView({ left: 0, top: 0, behavior: \"smooth\" });",
        "scrollAriaLabel '$label'"
    )
}

fun scrollToUrnLi(urnLi: String) {

    linkedAlertPingBot.sendMessage("Scroll to post")

    sendScriptToJSShellCommand(
        "document.querySelector('[data-urn=\"urn:li:activity:$urnLi\"]').scrollIntoView({ left: 0, top: 0, behavior: \"smooth\" });",
        "scrollToItem '$urnLi'"
    )
}
