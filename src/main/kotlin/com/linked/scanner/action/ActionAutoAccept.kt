package com.linked.scanner.action

import com.linked.api.getConnectEvent
import com.linked.bidder.appConfig
import com.linked.bidder.linkedAlertPingBot
import com.linked.bidder.utils.Logger
import com.linked.chromedriver.profileAnalyticName
import com.linked.model.ChromeProfile
import com.linked.scanner.logic.isTextExistOnScreen
import com.linked.utils.isCompleteRequest
import com.linked.utils.sendScriptToJSShellCommand
import com.linked.utils.sendScriptToJSShellCommandWithResult
import com.linked.utils.sleepWithRandom
import org.jsoup.Jsoup

fun autoAcceptFirstContact(profile: ChromeProfile): Boolean {
    //delay for page load
    sleepWithRandom(9000, 3000)
    val htmlStrIsInvaitesExists =
        sendScriptToJSShellCommandWithResult("document.querySelector('[data-view-name=\"invitation-action\"]')", "isAcceptContactExist")

    if (!isCompleteRequest(htmlStrIsInvaitesExists)) {
        return false
    }

    sleepWithRandom(3000, 3000)

    if (htmlStrIsInvaitesExists != "null") {
        Logger.logInfo("!!! autoAcceptFirstContact - got new contact !!!")

        // Note: the button stopped being clickable, maybe a global update, maybe not - testing
        val htmlStrTestNewLayout =
            sendScriptToJSShellCommandWithResult(
                "document.querySelectorAll('[data-view-name=\"invitation-action\"]')[1].querySelector('button')",
                "test btn exist in layout"
            )

        if (!isCompleteRequest(htmlStrTestNewLayout)) {
            return false
        }

        if (htmlStrTestNewLayout != "null") {
            //New version: accept fist if exist
            sendScriptToJSShellCommand(
                "document.querySelectorAll('[data-view-name=\"invitation-action\"]')[1].querySelector('button').click();",
                "autoAcceptFirstContact [new]"
            )
        } else {
            //Old version: accept fist if exist
            sendScriptToJSShellCommand(
                "document.querySelectorAll('[data-view-name=\"invitation-action\"]')[1].click();",
                "autoAcceptFirstContact [old]"
            )
        }
        sleepWithRandom(3000, 2000)

        //Note: sometimes clicking accept shows an extra warning dialog, which can break the logic
        if (isTextExistOnScreen("Take care when connecting")) {
            //todo - doesn't fit: either a new layout or an odd case, but it doesn't match
            // use {ic_accept_invite} and add one for {conf_1920x1080}
            _clickButtonByTextLabelInModal("Accept invite")
            sleepWithRandom(3000, 2000)
        }

        val htmlStrInvitesLength =
            sendScriptToJSShellCommandWithResult(
                "document.querySelectorAll('[data-view-name=\"invitation-action\"]').length",
                "invitationListLength"
            )

        if (!isCompleteRequest(htmlStrInvitesLength)) {
            return false
        }

        linkedAlertPingBot.sendMessage("*Someone found me, invites left: $htmlStrInvitesLength, accepted the first connection")

        getConnectEvent("organic", appConfig.botID, profileAnalyticName)

        profile.incInputConnectCount()

        //todo - backlog #1
        // may need to analyze who sent the connection request here, or maybe not
        // they may be salespeople with connections / or may not
        // or add a country check to filter by region, although those contacts may also have clients from target countries

        //todo - backlog #2
        // there may be page invitations here
        // should we filter them ??? or just accept all ???
    }

    return true
}

//todo - may not work, such modals were removed from the profile, not sure
const val _getLayoutModal = "document.querySelector('#artdeco-modal-outlet')"

fun _clickButtonByTextLabelInModal(label: String): Boolean {
    val htmlStr = sendScriptToJSShellCommandWithResult(_getLayoutModal, "clickButtonByTextLabelInModal | modal | $label")
    if (!isCompleteRequest(htmlStr)) {
        return false
    }
    var itemId = ""
    val doc2 = Jsoup.parse(htmlStr)
    val buttons2 = doc2.select("button")
    buttons2.forEach { item ->
        if (item.text().trim().contains(label)) {
            itemId = item.id()
        }
    }

    if (itemId.isNotEmpty()) {
        sendScriptToJSShellCommand("document.querySelector('#$itemId').click();", "clickButtonByTextLabelInModal | click | $label")
        return true
    } else {
        return false
    }
}