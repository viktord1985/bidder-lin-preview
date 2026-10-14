package com.linked.model.command

import com.linked.bidder.infoMessage
import com.linked.bidder.linkedAlertPingBot
import com.linked.bidder.utils.Config
import com.linked.bidder.utils.Logger
import com.linked.chromedriver.profileAnalyticName
import com.linked.model.ChromeProfile
import com.linked.model.LinkedPostDB
import com.linked.scanner.action.*
import com.linked.scanner.logic.clickFirstTemplatePosition
import com.linked.scanner.logic.clickTemplatePosition
import com.linked.scanner.logic.isNotValidPoint
import com.linked.utils.*
import org.jsoup.Jsoup

class CommandClick(
    val name: String,
    val type: String,
    var id: String,
    val delay: String,
    var probability: String,
) {
    fun run(profile: ChromeProfile, linkedPostDB: LinkedPostDB, autoActions: Boolean): Boolean {
        if (shouldPerformAction(probability.toDouble())) {

            infoMessage(".. (${name}/${type}/${id})")

            linkedAlertPingBot.sendMessage(".. (${name}/${type}/${id})")

            if (type == "navbar") {
                if (!clickNavbarButtonByTextLabel(id)) {
                    Logger.logDebug("!!! clickNavbarButtonByTextLabel fail !!!")
                    return false
                }

                // pause is placed inside the if so that actions can run after it
                sleepWithRandom(delay.toInt(), delay.toInt(), true)

                if (autoActions) {
                    if (!testAndFixIframe()) {
                        return false
                    }
                    if (id == "My Network") {

                        if (Config.isAutoAcceptConnection) {
                            // accept the first connection if there is one
                            if (!autoAcceptFirstContact(profile)) {
                                Logger.logDebug("!!! autoAcceptFirstContact fail !!!")
                                return false
                            }
                        }
                        if (Config.isAutoSendReferAndAutoAddSuggested) {

                            //Old: does not work
                            //sendScriptToJSShellCommand(
                            //    "document.querySelector('a[href*=\"/mynetwork/invite-connect/connections\"]').click();",
                            //    "click btn {Connections} v2"
                            //)
                            if (isNotValidPoint(clickTemplatePosition("ic_connections"))) {
                                return false;
                            }

                            // need to return focus to the JS console
                            refocusJSConsole()

                            sleepWithRandom(12000, 3000)

                            if (!testAndFixIframe()) {
                                return false
                            }

                            // start analysis if fresh responses have appeared
                            if (!autoReferLatterOnMyNetwork(profile, linkedPostDB)) {
                                Logger.logDebug("!!! autoReferLatterOnMyNetwork fail !!!")
                                return false
                            }

                            // navigated back
                            sendScriptToJSShellCommand("history.back()", "navigation back")

                            sleepWithRandom(12000, 3000)
                        }
                        //todo !!! profile hardcoded !!!
                        if (profileAnalyticName == "s1x" && Config.isAutoCleanConnection && shouldPerformAction(Config.isAutoCleanConnectionProbability)
                            && profile.getWeeksFromActivatedDate() >= Config.activeWeeksForRunCleanConnection && !profile.needCheck
                        ) {

                            // opened the page with my connections ({all invitations} button)
                            //Old: sendScriptToJSShellCommand(
                            //    "document.querySelector('a[href*=\"/mynetwork/invitation-manager\"]').click();",
                            //    "click btn {all invitations}"
                            //)

                            //sleepWithRandom(12000, 3000)

                            //Old:  there should be a test and a page reload, but this page crashes on its own after reload
                            //if (!testAndFixIframe()) {
                            //    return false
                            //}

                            // opened the page with my connections ({Sent} button)
                            //Old: val htmlStrTestSenBtnV1 = sendScriptToJSShellCommandWithResult(
                            //    "document.querySelector('[aria-label^=\"Manage\"] button')",
                            //    "test Sent Btn V1"
                            //)
                            //Old: if (!isCompleteRequest(htmlStrTestSenBtnV1)) {
                            //    return false
                            //}
                            //Old: if (htmlStrTestSenBtnV1 != "null") {
                            //    sendScriptToJSShellCommand(
                            //        "document.querySelectorAll('[aria-label^=\"Manage\"] button')[1].click();",
                            //        "click btn {Sent} v1"
                            //    )
                            //} else {
                            //    sendScriptToJSShellCommand(
                            //        "document.querySelector('a[href*=\"mynetwork/invitation-manager/sent\"]').click();",
                            //        "click btn {Sent} v2"
                            //    )
                            //}

                            if (isNotValidPoint(clickFirstTemplatePosition(listOf("ic_contacts_manage", "ic_contacts_show_all")))) {
                                return false;
                            }

                            sleepWithRandom(12000, 3000)

                            if (isNotValidPoint(clickTemplatePosition("ic_contacts_sent"))) {
                                return false;
                            }

                            // need to return focus to the JS console
                            refocusJSConsole()

                            sleepWithRandom(12000, 3000)

                            if (!testAndFixIframe()) {
                                return false
                            }

                            // start analysis if fresh responses have appeared
                            if (!autoCleanConnection(profile)) {
                                Logger.logDebug("!!! autoCleanConnection fail !!!")
                                return false
                            }

                            // navigated back (after the Sent button)
                            sendScriptToJSShellCommand("history.back()", "navigation back")

                            sleepWithRandom(12000, 3000)

                            // navigated back (after the {Manage, ShowAll} buttons)
                            sendScriptToJSShellCommand("history.back()", "navigation back")

                            sleepWithRandom(12000, 3000)
                        } else {
                            linkedAlertPingBot.sendMessage(
                                "Skip run clean " +
                                        "${if (profile.getWeeksFromActivatedDate() < Config.activeWeeksForRunCleanConnection) "| too early" else ""} " +
                                        "| needCheck: ${profile.needCheck} "
                            )
                        }
                    } else if (id == "Messaging") {

                        // could also send the message text here, since it can't be obtained via email
                        if (!autoMessages(profile)) {
                            return false
                        }
                    } else if (id == "Home") {

                        // delay enabled here to let the page load
                        if (Config.isAutoAnalyticsEnable) {
                            autoAnalytics(profile)
                        }

                        // various promo dialogs were seen on the home page; close one just in case, without checking it exists
                        clickCloseDialog()
                        // Warning: if there is no dialog, a hidden button exists that scrolls down on click, so we need to scroll back
                        scrollToPageTop()
                        sleep(3000)

                        if (Config.isAutoPostContent) {
                            if (!autoContentPost(profile)) {
                                Logger.logDebug("!!! autoContentPost fail !!!")
                                return false
                            }
                        }

                        if (Config.isAutoConnectProfiles) {
                            if (!autoConnectProfiles(profile, linkedPostDB)) {
                                Logger.logDebug("!!! autoConnectProfiles fail !!!")
                                return false
                            }
                        }

                    } else if (id == "Jobs") {

                        // ..
                    }
                }
            } else if (type == "searchBar") {

                var skipCountValue = 0
                if (id.contains("/")) {
                    skipCountValue = id.split("/")[1].toInt()
                    id = id.split("/")[0]
                }

                if (!clickSearchBarButtonByTextLabel(id, skipCountValue)) {
                    Logger.logDebug("!!! clickSearchBarButtonByTextLabel fail !!!")
                    return false
                }

                // pause is placed inside the if so that actions can run after it
                sleepWithRandom(delay.toInt(), delay.toInt(), true)

            } else if (type == "searchBarLabel") {

                if (!clickSearchBarLabelByTextLabel(id)) {
                    Logger.logDebug("!!! clickSearchBarLabelByTextLabel fail !!!")
                    return false
                }

                // pause is placed inside the if so that actions can run after it
                sleepWithRandom(delay.toInt(), delay.toInt(), true)

            } else {
                Logger.logError("!!! Unknown click type !!!")
            }
        } else {
            Logger.logInfo("[Skip click id: $id]")
        }
        return true
    }
}

fun getCommandClick(text: String): CommandClick {

    return CommandClick(
        text.split("|")[0],
        text.split("|")[1],
        text.split("|")[2],
        text.split("|")[3],
        text.split("|")[4],
    )
}

const val getIframeFlag = "document.querySelector('[data-testid=\"interop-shadowdom\"]')"

const val getNavContentV1 = "document.querySelector('.global-nav__content')"
const val getNavContentV2 = "document.querySelector('header')"
const val getFiltersBarContainer = "document.querySelector('#search-reusables__filters-bar')"

fun testAndFixIframe(): Boolean {

    val htmlStrIframe = sendScriptToJSShellCommandWithResult(getIframeFlag, "Test Iframe")

    if (!isCompleteRequest(htmlStrIframe)) {
        return false
    }

    if (htmlStrIframe != "null") {

        Logger.logInfo("!!! Found Iframe !!!")

        sendReloadPageCommand()

        //Note: increased 3x, this is a full page reload and the internet + browser performance are very poor
        //delay for loading js console
        sleepWithRandom(7500 * 3, 2000, true)
    }

    return true
}

fun clickNavbarButtonByTextLabel(label: String): Boolean {

    Logger.logInfo("clickNavbarButtonByTextLabel label: $label")

    if (!testAndFixIframe()) {
        return false
    }

    val htmlStr = sendScriptToJSShellCommandWithResult(getNavContentV1, "clickNavbar | test nav container V1")

    if (!isCompleteRequest(htmlStr)) {
        return false
    }

    val isContentV1 = htmlStr != "null"

    Logger.logInfo("clickNavbarButtonByTextLabel select: ${if (isContentV1) "V1" else "V2"}")

    val htmlStr2 = sendScriptToJSShellCommandWithResult(
        if (isContentV1) getNavContentV1 else getNavContentV2,
        "clickNavbar | nav container $label"
    )

    if (!isCompleteRequest(htmlStr2)) {
        return false
    }

    val doc = Jsoup.parse(htmlStr2)
    var index = 0
    val items = if (isContentV1) doc.select("[data-test-app-aware-link]") else doc.select("[data-view-name]")

    items.forEach { item ->
        if (item.text().trim().contains(label)) {
            if (isContentV1) {
                sendScriptToJSShellCommand(
                    "$getNavContentV1.querySelectorAll('[data-test-app-aware-link]')[$index].click();",
                    "clickNavbar | click V1 $label"
                )
            } else {
                sendScriptToJSShellCommand(
                    "$getNavContentV2.querySelectorAll('[data-view-name]')[$index].click();",
                    "clickNavbar | click V2 $label"
                )
            }
            return true
        }
        index++
    }

    Logger.logError("!!! clickNavbarButtonByTextLabel !!! no items found !!!")
    return false
}

fun clickSearchBarButtonByTextLabel(label: String, skipCount: Int = 0): Boolean {

    Logger.logInfo("clickSearchBarButtonByTextLabel label: $label, skipCount: $skipCount")

    if (!testAndFixIframe()) {
        return false
    }

    val htmlStr =
        sendScriptToJSShellCommandWithResult(getFiltersBarContainer, "clickSearchBarButtonByTextLabel | reusables container $label")

    if (!isCompleteRequest(htmlStr)) {
        return false
    }

    val doc = Jsoup.parse(htmlStr)
    var index = 0
    val items = doc.select("button")
    var skipCountValue = 0

    items.forEach { item ->
        var contain = false
        if (label.contains("(*)")) {
            val startString = label.split("(*)")[0]
            var endString = label.split("(*)")[1]
            if (endString.endsWith("-")) {
                // endString = result-
                // itemLastWord = result | results
                endString = endString.replace("-", "") // endString = result
                val itemLastWord = item.text().trim().split(" ").last()
                contain = item.text().trim().startsWith(startString) && itemLastWord.startsWith(endString)
                //println("endString: $endString itemLastWord: $itemLastWord")
            } else {
                contain = item.text().trim().startsWith(startString) && item.text().trim().endsWith(endString)
            }
        } else {
            contain = item.text().trim().contains(label)
        }
        if (contain) {

            if (skipCountValue == skipCount) {
                sendScriptToJSShellCommand(
                    "$getFiltersBarContainer.querySelectorAll('button')[$index].click();",
                    "clickSearchBarButtonByTextLabel | click $label"
                )
                return true
            } else {
                skipCountValue++
            }

        }
        index++
    }

    Logger.logError("!!! clickSearchBarButtonByTextLabel !!! no items found !!!")
    return false
}

fun clickSearchBarLabelByTextLabel(label: String): Boolean {

    Logger.logInfo("clickSearchBarLabelByTextLabel label: $label")

    val htmlStr =
        sendScriptToJSShellCommandWithResult(getFiltersBarContainer, "clickSearchBarLabelByTextLabel | reusables container $label")

    if (!isCompleteRequest(htmlStr)) {
        return false
    }

    val doc = Jsoup.parse(htmlStr)
    var index = 0
    val items = doc.select("label")

    items.forEach { item ->
        // note: hide + visible text so I use {contains}
        if (item.text().trim().contains(label)) {
            sendScriptToJSShellCommand(
                "$getFiltersBarContainer.querySelectorAll('label')[$index].click();",
                "clickSearchBarLabelByTextLabel | click $label"
            )
            return true
        }
        index++
    }

    Logger.logError("!!! clickSearchBarLabelByTextLabel !!! no items found !!!")
    return false
}

fun clickHowCommentOnUrnLi(urnLi: String) {

    linkedAlertPingBot.sendMessage("Opened post comments")

    sendScriptToJSShellCommand(
        "document.querySelector('[data-urn=\"urn:li:activity:$urnLi\"]').querySelector('[aria-label*=\"comment\"]').click();",
        "scrollToItem '$urnLi'"
    )
}

fun clickHowRepostOnUrnLi(urnLi: String) {

    linkedAlertPingBot.sendMessage("Opened dialog - who reposted the post")

    sendScriptToJSShellCommand(
        "document.querySelector('[data-urn=\"urn:li:activity:$urnLi\"]').querySelector('[aria-label*=\"repost\"]').click();",
        "scrollToItem '$urnLi'"
    )
}

fun clickCloseDialog() {

    linkedAlertPingBot.sendMessage("Closing dialog")

    sendScriptToJSShellCommand("document.querySelector('[aria-label^=\"Dismiss\"]').click();", "click btn {Dismiss}")

    sleep(6000)
}
