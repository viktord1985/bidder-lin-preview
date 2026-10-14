package com.linked.scanner.action

import com.linked.api.ConnectProfileDB
import com.linked.api.connectProfileSelfTest
import com.linked.bidder.appConfig
import com.linked.bidder.linkedAlertPingBot
import com.linked.bidder.utils.Logger
import com.linked.model.ChromeProfile
import com.linked.model.LinkedPostDB
import com.linked.model.command.setNavbarFocus
import com.linked.model.command.setNavbarSubmit
import com.linked.model.command.setNavbarText
import com.linked.model.createPostItemForProfileConnect
import com.linked.utils.isCompleteRequest
import com.linked.utils.sendScriptToJSShellCommandWithResult
import com.linked.utils.sleepWithRandom
import java.util.*

//tag: outreach
fun autoConnectProfiles(scannerProfile: ChromeProfile, linkedPostDB: LinkedPostDB): Boolean {

    Logger.logInfo("autoConnectProfiles")

    if (!scannerProfile.isOldProfile()) {

        linkedAlertPingBot.sendMessage("Profile is too young (outreach)")

        Logger.logInfo("isOldProfile() == false")

        return true
    }

    Logger.logInfo("Start load profiles")

    val profiles = ConnectProfileDB().getList()
    Logger.logInfo("Loaded profiles count: ${profiles.size}")

    // look for new profiles, check they weren't connected yet
    val notConnected = profiles.filter { it -> connectProfileSelfTest(it) && it.status == "New" && !scannerProfile.isUsedProfile(it.url) }
    Logger.logInfo("notConnected profiles count: ${notConnected.size}")

    if (notConnected.isNotEmpty()) {

        // pick a profile from the list
        val connectProfile = notConnected[Random().nextInt(notConnected.size)]

        Logger.logInfo("Select for connect: ${connectProfile.name}, ${connectProfile.description}, ${connectProfile.url}")

        if (scannerProfile.isCanConnectProfile()) {

            linkedAlertPingBot.sendMessage("Start (outreach) ..")

            // close all dialogs/chats
            if (!closeOpenedChatDialogs()) {
                Logger.logDebug("!!! closeOpenedChatDialogs fail !!!")
                return false
            }

            val scannerProfileId = scannerProfile.id + " | " + appConfig.getProfileNameByChromeProfileName(scannerProfile.id)

            val text = connectProfile.name + " " + getFirstThreeWords(connectProfile.description)

            setNavbarFocus("home", text)

            sleepWithRandom(3000, 3000)

            setNavbarText("home", text)

            sleepWithRandom(3000, 3000)

            setNavbarSubmit("home", text)

            sleepWithRandom(3000, 3000)

            val htmlStr =
                sendScriptToJSShellCommandWithResult(
                    "document.querySelector(\"[data-chameleon-result-urn]\")",
                    "get first search item - exist"
                )

            if (!isCompleteRequest(htmlStr)) {
                return false
            }

            var fullText = ""

            if (htmlStr != "null") {
                fullText = sendScriptToJSShellCommandWithResult(
                    "document.querySelector(\"[data-chameleon-result-urn]\").outerText",
                    "get first search item - text"
                )

                Logger.logInfo("fullText: $fullText")
            }

            if (htmlStr != "null" && fullText.contains(connectProfile.name) && fullText.contains(connectProfile.description)) {

                val profileUrl = sendScriptToJSShellCommandWithResult(
                    "document.querySelector(\"[data-chameleon-result-urn]\").querySelector('[href]').getAttribute('href')",
                    "get first search item - url"
                )

                val linkedPost =
                    createPostItemForProfileConnect(connectProfile.name, connectProfile.description, profileUrl, scannerProfile.id)

                val result = openAndBidProfile(linkedPost, scannerProfile, linkedPostDB, "outreach", false)

                if (result == null) {
                    return false
                }

                if (result) {
                    ConnectProfileDB().updateStatus(connectProfile.id, "Sent", scannerProfileId)

                    scannerProfile.incConnectProfileCount()

                    scannerProfile.addUsedProfile(connectProfile.url)

                    linkedAlertPingBot.sendMessage("*New profile added*")

                    Logger.logInfo("autoConnectProfiles - success")
                } else {
                    linkedAlertPingBot.sendMessage("*Something went wrong*")

                    Logger.logInfo("autoConnectProfiles - some error")
                }

            } else {
                linkedAlertPingBot.sendMessage("*Not found in the first search position* (${connectProfile.name})")

                Logger.logInfo("autoConnectProfiles - no profile on first page")
            }
        } else {
            linkedAlertPingBot.sendMessage("Limits exhausted (outreach)")

            Logger.logInfo("autoConnectProfiles - no limits")
        }
    }

    return true
}

fun getFirstThreeWords(text: String): String {
    return text.split("\\s+".toRegex()) // Split by whitespace
        .take(3) // Take the first 3 words
        .joinToString(" ") // Join back into a string
}