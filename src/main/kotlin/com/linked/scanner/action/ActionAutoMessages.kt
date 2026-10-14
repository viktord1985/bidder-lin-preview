package com.linked.scanner.action

import com.linked.bidder.linkedAlertPingBot
import com.linked.model.ChromeProfile
import com.linked.utils.isCompleteRequest
import com.linked.utils.sendScriptToJSShellCommandWithResult
import com.linked.utils.sleepWithRandom

fun autoMessages(profile: ChromeProfile): Boolean {

    //delay for page load
    // messages open very slowly, so pause
    sleepWithRandom(12000, 3000)

    val htmlStr0 =
        sendScriptToJSShellCommandWithResult(
            "document.querySelector('.msg-conversations-container__conversations-list').querySelectorAll('.msg-conversation-listitem').length",
            "msg dialogs count"
        )

    if (!isCompleteRequest(htmlStr0)) {
        return false
    }

    val htmlStrNewNotification = sendScriptToJSShellCommandWithResult(
        "Array.from(document.querySelectorAll(\".msg-conversation-card__rows\")).map(e => e.outerText).filter(i => i.includes(\"new notification\"))",
        "get new notifications"
    )

    if (!isCompleteRequest(htmlStrNewNotification)) {
        return false
    }

    if (htmlStrNewNotification != "[]") {

        var list = listOf<String>()

        list = htmlStrNewNotification.replace("[", "")
            .replace("]", "")
            .replace("\n", "")
            .replace("\"", "")
            .trim()
            .split(",")

        linkedAlertPingBot.sendMessage(
            "*New notifications:*\n${
                list.toString().replace("\t", "")
                    .replace("[", "")
                    .replace("]", "")
                    .replace(",", "\n")
                    .replace("\\n", "\n")
                    .lines()
                    .filter { item -> !item.contains("new notification") } // filter "new notification" lines
                    .map { item -> item.trim() } // trim leading whitespace
                    .joinToString("\n")
            }"
        )
    } else {
        if (profile.needCheck) {
            linkedAlertPingBot.sendMessage("*No unread dialogs*")
        } else {
            linkedAlertPingBot.sendMessage("No unread dialogs")
        }
    }

//    //only last dialogs
//    val MAX_DIALOG_COUNT = 5
//
//    var dialogsCount = -1
//
//    try {
//        dialogsCount = htmlStr0.toInt()
//    } catch (_: NumberFormatException) {
//    }
//
//    if (dialogsCount == -1) {
//        Logger.logDebug("!!! Dialogs not found !!!")
//        return true
//    }
//
//    if (dialogsCount > MAX_DIALOG_COUNT) {
//        dialogsCount = MAX_DIALOG_COUNT
//    }
//
//    var htmlStr1 = ""
//    var htmlStr2 = ""
//
//    for (i in 0..(dialogsCount - 1)) {
//
//        sleepWithRandom(2000, 100)
//        htmlStr1 =
//            sendScriptToJSShellCommandWithResult(
//                "document.querySelector('.msg-convo-wrapper').querySelectorAll('.msg-s-event-listitem__message-bubble').length",
//                "getLastDialogMsgCount"
//            )
//
//        if (!isCompleteRequest(htmlStr2)) {
//            return false
//        }
//
//        sleepWithRandom(1000, 1000)
//        htmlStr2 =
//            sendScriptToJSShellCommandWithResult(
//                "document.querySelector('.msg-convo-wrapper').querySelector('div[contenteditable=\"true\"]')",
//                "getInputField"
//            )
//
//        if (!isCompleteRequest(htmlStr2)) {
//            return false
//        }
//
//        // only one message in dialog and input field exist
//
//        val htmlStr3 = sendScriptToJSShellCommandWithResult("document.querySelector('.msg-s-profile-card')", "getProfileCard")
//        val doc = Jsoup.parse(htmlStr3)
//        val link = doc.getElementsByClass("profile-card-one-to-one__profile-link").first()?.attr("href") ?: ""
//        val title = doc.getElementsByClass("profile-card-one-to-one__profile-link").first()?.text()?.replace("'", "") ?: ""
//
//        //    val message = formatCoverLetterWithAI("", CoverLetterType.botRefer);
//        //
//        //    sendScriptToJSShellCommand(
//        //        "function simulateTyping(div, text) { div.focus(); document.execCommand('selectAll', false, null); document.execCommand('insertText', false, text); } simulateTyping(document.querySelector('div[contenteditable=\"true\"]'), \"${
//        //            convertXmlTextToJS(
//        //                message
//        //            )
//        //        }\");",
//        //        "set msg text"
//        //    )
//        //    sleepWithRandom(5000, 2000)
//        //    LoggerAI.logMessage("clicked button - xxx")
//        //    sendScriptToJSShellCommand(
//        //        "document.querySelector('.msg-convo-wrapper').querySelector('.msg-form__send-button').click();",
//        //        "click send button"
//        //    )
//        //
//        //    linkedAlertPingBot.sendMessage("*xxx, sent a message with XXX*")
//
//    }

    return true
}
