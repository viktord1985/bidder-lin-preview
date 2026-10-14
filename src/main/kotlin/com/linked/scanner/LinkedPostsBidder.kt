package com.linked.scanner

import com.linked.api.sendConnectEvent
import com.linked.bidder.appConfig
import com.linked.bidder.linkedAlertPingBot
import com.linked.bidder.utils.Logger
import com.linked.chromedriver.profileAnalyticName
import com.linked.model.ChromeProfile
import com.linked.model.LinkedProfile
import com.linked.model.command.dispatchEventInput
import com.linked.scanner.action.getFirstThreeWords
import com.linked.scanner.logic.*
import com.linked.utils.*
import org.jsoup.Jsoup

fun additionalActionsOnProfile(probability: Double, label: String): Boolean? {

    //todo ??? disabled for now, otherwise it may look like I'm scraping data

//    if (shouldPerformAction(probability)) {
//
//        val testBtn = isButtonVisibleByTextLabelOnLayoutMain(label)
//        if (testBtn == null) {
//            return null
//        }
//
//        if (testBtn) {
//
//            scrollToAriaLabel("Select type of recent activity")
//
//            if (!clickButtonByTextLabel(label)) {
//                Logger.logDebug("!!! clickButtonByTextLabelInModal fail !!!")
//                return null
//            }
//
//            linkedAlertPingBot.sendMessage("Scroll to 'recent activity' section and click '$label'")
//
//            sleepWithRandom(6000, 3000)
//
//            scrollToPageTop()
//        }
//
//    }

    return true
}

fun checkRedBadge(): Boolean {
    val htmlLayoutMemberText = sendScriptToJSShellCommandWithResult("$getLayoutProfileCard.outerText", "getLayoutProfileCard outerText")
    if (!isCompleteRequest(htmlLayoutMemberText)) {
        return false
    }
    if (htmlLayoutMemberText.contains("Verify now")) {
        Logger.logDebug("!!! Red Badge !!! $profileAnalyticName")
        linkedAlertPingBot.sendMessage("*!!! Red Badge !!!* $profileAnalyticName")

        // it used to be possible to close it like this; not sure now, everything here was changed
        // document.querySelector('[data-test-verification-entry-point-card]').querySelectorAll('button')[0].click();
        // but I don't do that for now, watching it disappear by itself
    }
    return true
}

fun checkBidProfile(linkedProfile: LinkedProfile, onContinue: () -> Boolean?): Boolean? {
    if (!checkRedBadge()) {
        return null
    }
    val testBtnPending = isButtonVisibleOnProfileBeforeMoreMenu("Pending")
    if (testBtnPending == null) {
        return null
    }
    if (testBtnPending) {
        Logger.logInfo("!!! Pending profile, link: ${linkedProfile.url} ")
        linkedAlertPingBot.sendMessage("*Pending profile* link: ${linkedProfile.url}")
    } else if (getDistanceBadgeLabel() == "1st") {
        Logger.logInfo("!!! Connected profile, link: ${linkedProfile.url} ")
        linkedAlertPingBot.sendMessage("*1st profile* link: ${linkedProfile.url}")
    } else {
        return onContinue()
    }
    return true
}

fun bidProfile(profile: ChromeProfile, linkedProfile: LinkedProfile, coverLetterType: CoverLetterType, tag: String): Boolean? {

    sleepWithRandom(6000, 3000)

    if (additionalActionsOnProfile(0.4, "Comments") == null) {
        return null
    }

    if (additionalActionsOnProfile(0.4, "Images") == null) {
        return null
    }

    if (additionalActionsOnProfile(0.4, "Articles") == null) {
        return null
    }

    if (additionalActionsOnProfile(0.4, "Videos") == null) {
        return null
    }

    // screenshot 1 - before the bid starts
    takeScreenshot(profileAnalyticName)

    Logger.logDebug(">>>>>>>>>>>>>>> checking whether the {Connect} button is before the More menu")

    val testBtnConnectExist = isButtonVisibleOnProfileBeforeMoreMenu("Connect")
    if (testBtnConnectExist == null) {
        return null
    }

    if (testBtnConnectExist) {

        Logger.logDebug(">>>>>>>>>>>>>>> {Connect} button is before the More menu, clicking")

        clickConnectButtonOnProfileBeforeMoreMenu()

        Logger.logDebug(">>>>>>>>>>>>>>> checking for the weekly limit ({Got it} button) in the modal dialog")

        // check whether the weekly limit appeared

        if (isTextExistOnScreen("Got it")) {
            // screenshot 1a - limits reached
            takeScreenshot(profileAnalyticName)
            Logger.logDebug("!!! $profileAnalyticName - You're approaching the weekly invitation limit !!!")
            profile.daysFromLastActivity = STOP_BID_DAYS.toString()
            Logger.logDebug("!!! $profileAnalyticName - daysFromLastActivity: ${profile.daysFromLastActivity} !!!")
            linkedAlertPingBot.sendMessage("*!!! You're approaching the weekly invitation limit !!!*")

            Logger.logDebug(">>>>>>>>>>>>>>> weekly limit exceeded, exiting")

            return false
        }

        Logger.logDebug(">>>>>>>>>>>>>>> checking for the premium offer ({Not now} button) in the modal dialog")

        // closes the premium offer; there is also a verification request here, which in theory it should close too

        if (isTextExistOnScreen("Not now")) {

            Logger.logDebug(">>>>>>>>>>>>>>> clicking the {Not now} button in the modal dialog")

            closeConsoleSendEscOpenConsole()

            linkedAlertPingBot.sendMessage("*Not now* closed")

            Logger.logDebug(">>>>>>>>>>>>>>> {Connect} button is before the More menu, clicking")

            //note: after "Not now" the dialog disappeared and we had to start over
            clickConnectButtonOnProfileBeforeMoreMenu()
        }

    } else {

        Logger.logDebug(">>>>>>>>>>>>>>> no {Connect} button before the More menu, clicking {More}")

        clickMoreButtonOnProfileBeforeMoreMenu()

        sleepWithRandom(6000, 3000)

        Logger.logDebug(">>>>>>>>>>>>>>> clicking {Connect} in the dropdown menu")

        clickButtonOnProfileInDropdownMoreMenu("Connect")

        Logger.logDebug(">>>>>>>>>>>>>>> checking for the weekly limit ({Got it} button) in the modal dialog")

        // check whether the weekly limit appeared

        if (isTextExistOnScreen("Got it")) {
            // screenshot 1a - limits reached
            takeScreenshot(profileAnalyticName)
            Logger.logDebug("!!! $profileAnalyticName - You're approaching the weekly invitation limit !!!")
            profile.daysFromLastActivity = STOP_BID_DAYS.toString()
            Logger.logDebug("!!! $profileAnalyticName - daysFromLastActivity: ${profile.daysFromLastActivity} !!!")
            linkedAlertPingBot.sendMessage("*!!! You're approaching the weekly invitation limit !!!*")

            Logger.logDebug(">>>>>>>>>>>>>>> weekly limit exceeded, exiting")

            return false
        }

        Logger.logDebug(">>>>>>>>>>>>>>> checking for the premium offer ({Not now} button) in the modal dialog")

        // closes the premium offer; there is also a verification request here, which in theory it should close too

        if (isTextExistOnScreen("Not now")) {

            Logger.logDebug(">>>>>>>>>>>>>>> closing the modal")

            closeConsoleSendEscOpenConsole()

            linkedAlertPingBot.sendMessage("*Not now* closed")

            Logger.logDebug(">>>>>>>>>>>>>>> no {Connect} button before the More menu, clicking {More}")

            //note: after "Not now" the dialog disappeared and we had to start over
            clickMoreButtonOnProfileBeforeMoreMenu()

            sleepWithRandom(6000, 3000)

            Logger.logDebug(">>>>>>>>>>>>>>> clicking {Connect} in the dropdown menu")

            clickButtonOnProfileInDropdownMoreMenu("Connect")
        }
    }

    sleepWithRandom(12000, 3000)

    // screenshot 2 - after clicking "Connect"
    takeScreenshot(profileAnalyticName)

    // example https://www.linkedin.com/in/sundarpichai/ for S1
    //"To verify this member knows you, please enter their email to connect"
    if (isTextExistOnScreen("To verify this member knows you")){

        Logger.logDebug(">>>>>>>>>>>>>>> cannot connect without an email")

        return false
    }

    Logger.logDebug(">>>>>>>>>>>>>>> clicking {ic_add_note} via OCR")

    if (isNotValidPoint(clickFirstTemplatePosition(listOf("ic_add_note")))) {
        Logger.logDebug("!!! clickFirstTemplatePosition fail !!!")
        return null
    }

    sleepWithRandom(6000, 3000)

    // there was a click on the screen; an input field may open and grab focus, so return focus to the console
    Logger.logDebug(">>>>>>>>>>>>>>> console may have lost focus, returning focus to the JS console")
    refocusJSConsole()

    sleepWithRandom(6000, 3000)

    // screenshot 2 - check whether "Try Premium" appears
    takeScreenshot(profileAnalyticName)

    // when notes can still be added
    // 'With Premium, you can add longer personalized notes to all your invites.\nTry Premium for UAH0'

    // when notes can no longer be added
    // 'Try Premium for UAH0'

    //todo a "Reactivate Premium" does not trigger on macLin1
    // replaced with "You're out of free custom notes."
    // why {todo a}: we can't rely on bold labels inside buttons

    if (isTextExistOnScreen("Try Premium") || isTextExistOnScreen("You're out of free custom notes.")) {

        sleepWithRandom(3000, 3000)

        Logger.logDebug(">>>>>>>>>>>>>>> closing the modal")

        closeConsoleSendEscOpenConsole()

        linkedAlertPingBot.sendMessage("*Try Premium* closed")

        sleepWithRandom(6000, 3000)

        Logger.logDebug(">>>>>>>>>>>>>>> checking whether the {Connect} button is before the More menu")

        val testBtnConnect = isButtonVisibleOnProfileBeforeMoreMenu("Connect")
        if (testBtnConnect == null) {
            return null
        }

        if (testBtnConnect) {

            Logger.logDebug(">>>>>>>>>>>>>>> {Connect} button is before the More menu, clicking")

            clickConnectButtonOnProfileBeforeMoreMenu()

        } else {

            Logger.logDebug(">>>>>>>>>>>>>>> no {Connect} button before the More menu, clicking {More}")

            clickMoreButtonOnProfileBeforeMoreMenu()

            sleepWithRandom(6000, 3000)

            Logger.logDebug(">>>>>>>>>>>>>>> clicking {Connect} in the dropdown menu")

            clickButtonOnProfileInDropdownMoreMenu("Connect")
        }

        sleepWithRandom(6000, 3000)

        // screenshot 2 - while bidding

        takeScreenshot(profileAnalyticName)

        Logger.logDebug(">>>>>>>>>>>>>>> clicking {ic_send_without_note} via OCR")

        if (isNotValidPoint(clickFirstTemplatePosition(listOf("ic_send_without_note")))) {
            Logger.logDebug("!!! clickFirstTemplatePosition fail !!!")
            return null
        }

        sleepWithRandom(12000, 4000)

        // screenshot 3 - after the bid

        takeScreenshot(profileAnalyticName)

        linkedAlertPingBot.sendMessage("*Send connect* without a note ($tag: ${linkedProfile.title}/${getFirstThreeWords(linkedProfile.description)}/${linkedProfile.location})")

    } else {

        // follow the case where notes can be added
        Logger.logDebug(">>>>>>>>>>>>>>> following the case where notes can be added")

        val noteText = formatCoverLetterWithAI(
            linkedProfile.title + " " + linkedProfile.description + " Location: " + linkedProfile.location + ", " + linkedProfile.about,
            coverLetterType
        )

        // increased the interval between actions to look more human
        sleepWithRandom(18000, 9000)

        Logger.logDebug(">>>>>>>>>>>>>>> closing the JS console")

        // close the console
        sendOpenJSConsoleCommand()
        sleep(3000)

        Logger.logDebug(">>>>>>>>>>>>>>> typing text into the active field")

        //old: setCustomNote(noteText)
        sendTextToActiveInput(noteText.replace("—", "-"), "note input")
        sleep(3000)

        Logger.logDebug(">>>>>>>>>>>>>>> opening the JS console")

        // open the console
        sendOpenJSConsoleCommand()
        sleep(3000)

        linkedAlertPingBot.sendMessage(noteText)

        sleepWithRandom(6000, 3000)

        // screenshot 2 - while bidding
        takeScreenshot(profileAnalyticName)

        Logger.logDebug(">>>>>>>>>>>>>>> clicking {ic_send} via OCR")

        //!!! Note !!!: after this click it's unclear where focus will be, so if more HTML work is needed,
        // make sure focus is in the JS console
        if (isNotValidPoint(clickFirstTemplatePosition(listOf("ic_send")))) {
            Logger.logDebug("!!! clickFirstTemplatePosition fail !!!")
            return null
        }

        sleepWithRandom(12000, 4000)

        // screenshot 3 - after the bid
        takeScreenshot(profileAnalyticName)

        linkedAlertPingBot.sendMessage("*Send connect* ($tag: ${linkedProfile.title}/${getFirstThreeWords(linkedProfile.description)}/${linkedProfile.location})")
    }

    sendConnectEvent(tag, appConfig.botID, profileAnalyticName)

    return true
}

const val getCustomMessageField = "document.querySelector('#custom-message')"

fun isButtonVisibleOnProfileBeforeMoreMenu(label: String): Boolean? {
    var layoutProfileCard = sendScriptToJSShellCommandWithResult("$getLayoutProfileCard.outerText", "isButtonVisibleOnProfileBeforeMoreMenu | $label")
    if (!isCompleteRequest(layoutProfileCard)) {
        return null
    }
    if (layoutProfileCard.contains("\n\nOpen to work")) {
        layoutProfileCard = layoutProfileCard.split("\n\nOpen to work").first()
    }
    val btn1 = layoutProfileCard.split("\n")[layoutProfileCard.split("\n").size - 1]
    val btn2 = layoutProfileCard.split("\n")[layoutProfileCard.split("\n").size - 2]
    return btn1 == label || btn2 == label
}

// Note: this is only used on the profile for {additionalActionsOnProfile}; if brought back,
// take the section id on the profile and search by text inside it
//fun isButtonVisibleByTextLabelOnLayoutMain(label: String): Boolean? {
//    val htmlStr = sendScriptToJSShellCommandWithResult(getLayoutProfileCard, "isButtonVisibleByTextLabel on LayoutMain | $label")
//    if (!isCompleteRequest(htmlStr)) {
//        return null
//    }
//    val doc = Jsoup.parse(htmlStr)
//    var isVisible = false
//    val buttons = doc.select("button")
//    buttons.forEach { item ->
//        if (item.text().trim().contains(label)) {
//            isVisible = true
//        }
//    }
//    return isVisible
//}

fun getDistanceBadgeLabel(): String? {
    val docProfileCardText =
        sendScriptToJSShellCommandWithResult("$getLayoutProfileCard.outerText", "getDistanceBadgeLabel | getLinkedProfile.outerText")
    if (!isCompleteRequest(docProfileCardText)) {
        return null
    }
    //Ex -> "Brian Mwithi\n\n· 2nd\n\nRemote Job Consultant  | Helping Professionals Transition To Fully Remote Roles FAST ..."
    return docProfileCardText.split("\n\n")[1].replace("·", "").trim().split("\n\n").first()
}

fun clickConnectButtonOnProfileBeforeMoreMenu(): Boolean {
    sendScriptToJSShellCommand(
        "$getLayoutProfileCard.querySelector('[aria-label^=\"Invite\"]').click()",
        "clickConnectButtonOnProfileBeforeMoreMenu"
    )
    return true
}

fun clickMoreButtonOnProfileBeforeMoreMenu(): Boolean {
    sendScriptToJSShellCommand(
        "$getLayoutProfileCard.querySelector('[data-view-name=\"profile-overflow-button\"] span').click()",
        "clickMoreButtonOnProfileBeforeMoreMenu"
    )
    return true
}

fun clickPrimaryMessageButtonOnProfileBeforeMoreMenu(): Boolean {
    sendScriptToJSShellCommand(
        "$getLayoutProfileCard.querySelector('[data-view-name=\"profile-primary-message\"] span').click()",
        "clickMoreButtonOnProfileBeforeMoreMenu"
    )
    return true
}

//fun clickButtonOnProfileBeforeMoreMenu(label: String): Boolean {
//    var id = ""
//    if (label == "Connect") {
//        .. this variant does not work if the Following button comes first
//        id = "relationship-building-button"
//    } else if (label == "More") {
//        // there is no "More" text on the page, it's my code word for this button
//        id = "profile-overflow-button"
//    } else if (label == "Message") {
//        // written for the future, not used now
//        //todo there can be two variants here; try both in turn, one of them will work
//        id = "profile-secondary-message"
//        id = "profile-primary-message"
//    } else if (label == "Accept") {
//        // written for the future, not used now
//        //todo same as {Ignore} here, need to look deeper to extract the name
//        id = "invitation-action"
//    } else if (label == "Ignore") {
//        // written for the future, not used now
//        //todo same as {Accept} here, need to look deeper to extract the name
//        id = "invitation-action"
//    } else if (label == "Follow") {
//        id = "edge-creation-follow-action"
//    }
//
//    if (id.isEmpty()) {
//        return false
//    }
//    sendScriptToJSShellCommand(
//        "$getLayoutProfileCard.querySelector('[data-view-name=\"$id\"] span').click()",
//        "clickButtonOnProfileBeforeMoreMenu | $label"
//    )
//    return true
//}

fun clickButtonOnProfileInDropdownMoreMenu(label: String): Boolean {
    var id = ""
    if (label == "Connect") {
        id = "edge-creation-connect-action"
    } else if (label == "Report") {
        id = "profile-report"
    }

    if (id.isEmpty()) {
        return false
    }
    sendScriptToJSShellCommand(
        "document.querySelector('[role=\"menu\"]').querySelector('[data-view-name=\"$id\"] p').click()",
        "clickOnProfileInDropdownMoreMenu | $label"
    )
    return true
}

//note: not used now, issues started with the shadow popup
fun setCustomNote(message: String) {
    // set input value
    sendScriptToJSShellCommand(
        "$getCustomMessageField.value = \"${convertXmlTextToJS(message)}\"", "setCustomMessage | set input value"
    )

    sleepWithRandom(3000, 3000)

    // sent input event to input field
    sendScriptToJSShellCommand(
        "$getCustomMessageField.$dispatchEventInput", "setCustomMessage | sent input event"
    )
}

fun convertXmlTextToJS(text: String): String {
    return text.replace("\n", "\\n")
}



