package com.linked.scanner.action

import com.linked.api.postImpressionsEvent
import com.linked.api.profileViewersEvent
import com.linked.bidder.appConfig
import com.linked.bidder.linkedAlertPingBot
import com.linked.bidder.utils.Logger
import com.linked.chromedriver.profileAnalyticName
import com.linked.model.ChromeProfile
import com.linked.utils.isCompleteRequest
import com.linked.utils.sendScriptToJSShellCommand
import com.linked.utils.sendScriptToJSShellCommandWithResult
import com.linked.utils.sleepWithRandom

fun autoAnalytics(profile: ChromeProfile): Boolean {

    //delay for page load
    sleepWithRandom(9000, 3000)

    val htmlStrTest = sendScriptToJSShellCommandWithResult(
        "document.querySelector('.feed-left-nav-growth-widgets__entity-list-item').outerText", "test is Analytics Exist"
    )

    if (!isCompleteRequest(htmlStrTest)) {
        return false
    }

    var profileViewers = 0
    var postImpressions = 0

    // for non-premium profiles we need to open the profile and get the info there
    if (htmlStrTest.contains("Profile viewers")) {

        val htmlStrProfileViewers = sendScriptToJSShellCommandWithResult(
            "document.querySelectorAll(\".feed-identity-widget-item__stat\")[0].outerText", "get profileViewers"
        )

        if (!isCompleteRequest(htmlStrProfileViewers)) {
            return false
        }

        val htmlStrPostImpressions = sendScriptToJSShellCommandWithResult(
            "document.querySelectorAll(\".feed-identity-widget-item__stat\")[1].outerText", "get postImpressions"
        )

        if (!isCompleteRequest(htmlStrPostImpressions)) {
            return false
        }

        try {
            profileViewers = htmlStrProfileViewers
                .replace(",", "")
                .trim().toInt()
        } catch (e: Exception) {
            Logger.logDebug(e.toString())
        }

        try {
            postImpressions = htmlStrPostImpressions
                .replace(",", "")
                .trim().toInt()
        } catch (e: Exception) {
            Logger.logDebug(e.toString())
        }

    } else {

        sendScriptToJSShellCommand("document.querySelector('.profile-card-name').click()", "got to profile")
        linkedAlertPingBot.sendMessage("opened the profile")
        sleepWithRandom(9000, 3000)

        val htmlStrProfileViewers = sendScriptToJSShellCommandWithResult(
            "document.querySelector('[data-field=\"insights_wvmp\"]').parentNode.parentNode.outerText", "get profileViewers"
        )

        if (!isCompleteRequest(htmlStrProfileViewers)) {
            return false
        }

        val htmlStrPostImpressions = sendScriptToJSShellCommandWithResult(
            "document.querySelector('[data-field=\"insights_content_impressions\"]').parentNode.parentNode.outerText", "get postImpressions"
        )

        if (!isCompleteRequest(htmlStrPostImpressions)) {
            return false
        }

        try {
            profileViewers = htmlStrProfileViewers.split(" ")[0]
                .replace(",", "")
                .trim().toInt()
        } catch (e: Exception) {
            Logger.logDebug("!!! htmlStrPostImpressions: $htmlStrProfileViewers !!!")
            Logger.logDebug(e.toString())
        }

        try {
            postImpressions = htmlStrPostImpressions.split(" ")[0]
                .replace(",", "")
                .trim().toInt()
        } catch (e: Exception) {
            Logger.logDebug("!!! htmlStrPostImpressions: $htmlStrPostImpressions !!!")
            Logger.logDebug(e.toString())
        }

        linkedAlertPingBot.sendMessage("navigated back")
        sendScriptToJSShellCommand("history.back()", "navigation back")
        sleepWithRandom(9000, 3000)
    }

    Logger.logInfo("profile:${profileAnalyticName} profileViewers: $profileViewers - ${profile.profileViewersCount}, postImpressions: $postImpressions - ${profile.postImpressionsCount}")

    if (profileViewers > profile.profileViewersCount.toInt()) {
        profileViewersEvent(appConfig.botID, profileAnalyticName, profileViewers - profile.profileViewersCount.toInt())
    }

    profile.profileViewersCount = profileViewers.toString()

    if (postImpressions > profile.postImpressionsCount.toInt()) {
        postImpressionsEvent(appConfig.botID, profileAnalyticName, postImpressions - profile.postImpressionsCount.toInt())
    }

    profile.postImpressionsCount = postImpressions.toString()

    return true
}
