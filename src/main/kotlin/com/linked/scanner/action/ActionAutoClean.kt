package com.linked.scanner.action

import com.linked.bidder.linkedAlertPingBot
import com.linked.bidder.utils.Config
import com.linked.bidder.utils.Logger
import com.linked.model.ChromeProfile
import com.linked.model.command.scrollToPageBottom
import com.linked.utils.isCompleteRequest
import com.linked.utils.sendScriptToJSShellCommand
import com.linked.utils.sendScriptToJSShellCommandWithResult
import com.linked.utils.sleepWithRandom

const val testCleanConnectionCardsV1 = "document.querySelector('.invitation-card__container')"
const val cleanConnectionCardsV1 = "document.querySelectorAll('.invitation-card__container')"
const val cleanConnectionCardsV2 = "document.querySelectorAll('[role=\"listitem\"]')"

fun autoCleanConnection(profile: ChromeProfile): Boolean {
    //delay for page load
    sleepWithRandom(6000, 3000)

    val htmlStrTestV1 = sendScriptToJSShellCommandWithResult(testCleanConnectionCardsV1, "testCleanConnectionCardsV1")
    if (!isCompleteRequest(htmlStrTestV1)) {
        return false
    }

    if (htmlStrTestV1 != "null") {
        return autoCleanConnectionV1(profile)
    } else {
        return autoCleanConnectionV2(profile)
    }
}

// variant without lazy loading, 100 items per page
// works only with Page1 and Page2
fun autoCleanConnectionV1(profile: ChromeProfile): Boolean {

    linkedAlertPingBot.sendMessage("autoCleanConnectionV1")

    // if everything fits on one page, exit
    val htmlStr = sendScriptToJSShellCommandWithResult("document.querySelector('[aria-label^=\"Next\"]')", "isNextExist")
    if (!isCompleteRequest(htmlStr)) {
        return false
    }
    if (htmlStr == "null") {
        Logger.logInfo("No {Next} button, too low count of connections")
        return true
    }

    sendScriptToJSShellCommand("document.querySelector('[aria-label^=\"Next\"]').click()", "click btn {Next}")
    sleepWithRandom(6500, 3500)

    linkedAlertPingBot.sendMessage("next page 2")

    scrollToPageBottom()

    val MAX_CLEAN_CONNECTION_PER_RUN = if (profile.isOldProfile()) Config.maxCleanConnectionPerRunOld else Config.maxCleanConnectionPerRun

    val MAX_FOUND_ATTEMPT_COUNT = 9

    var withdrawCount = 0

    var foundAttemptCount = 0

    val htmlCount = sendScriptToJSShellCommandWithResult("$cleanConnectionCardsV1.length", "cleanConnectionCards length")
    if (!isCompleteRequest(htmlCount)) {
        return false
    }

    Logger.logInfo("Page: 2, Count: $htmlCount")

    for (i in (htmlCount.toInt() - 1) downTo 0) {

        val htmlTimeBadge =
            sendScriptToJSShellCommandWithResult("$cleanConnectionCardsV1[$i].querySelector(\".time-badge\").outerText", "timeBadge")
        if (!isCompleteRequest(htmlTimeBadge)) {
            return false
        }

        if (withdrawCount == MAX_CLEAN_CONNECTION_PER_RUN || foundAttemptCount == MAX_FOUND_ATTEMPT_COUNT || !checkTimeBadge(htmlTimeBadge)) {

            // navigated back (there was a jump to page 2)
            sendScriptToJSShellCommand("history.back()", "navigation back")

            sleepWithRandom(6000, 3000)

            Logger.logDebug("exit withdrawCount: $withdrawCount, foundAttemptCount: $foundAttemptCount, checkTimeBadge: ${checkTimeBadge(htmlTimeBadge)}")
            return true

        } else {

            val htmlHref = sendScriptToJSShellCommandWithResult(
                "$cleanConnectionCardsV1[$i].querySelector('[href]').getAttribute('href')", "href"
            )
            if (!isCompleteRequest(htmlHref)) {
                return false
            }

            profile.addWithdrawProfiles(htmlHref)

            sendScriptToJSShellCommand(
                "$cleanConnectionCardsV1[$i].querySelector('[aria-label^=\"Withdraw\"]').click()", "click btn {Withdraw}"
            )

            sleepWithRandom(6500, 1000)

            sendScriptToJSShellCommand(
                "document.querySelector('[data-test-dialog-primary-btn]').click()", "click btn {data-test-dialog-primary-btn}"
            )

            linkedAlertPingBot.sendMessage("*Withdraw* connection | $htmlTimeBadge | num: $i")

            sleepWithRandom(6500, 1000)

            withdrawCount++
        }

        foundAttemptCount++
    }

    // navigated back (there was a jump to page 2)
    sendScriptToJSShellCommand("history.back()", "navigation back")

    sleepWithRandom(6000, 3000)

    return true
}

//todo - for some reason the 3rd Withdraw didn't work, throws an error after confirming in the dialog
fun autoCleanConnectionV2(profile: ChromeProfile): Boolean {

    linkedAlertPingBot.sendMessage("autoCleanConnectionV2")

    val MAX_CLEAN_CONNECTION_PER_RUN = if (profile.isOldProfile()) Config.maxCleanConnectionPerRunOld else Config.maxCleanConnectionPerRun

    val MAX_FOUND_ATTEMPT_COUNT = 9

    val MAX_PAGE_COUNT = 6 // todo ??? 6 pages should be enough

    var withdrawCount = 0

    var foundAttemptCount = 0

    var pageCount = 0

    do {

        val htmlPrevCount = sendScriptToJSShellCommandWithResult("$cleanConnectionCardsV2.length", "cleanConnectionCards length")
        if (!isCompleteRequest(htmlPrevCount)) {
            return false
        }

        sendScriptToJSShellCommand(
            "$cleanConnectionCardsV2[${htmlPrevCount.toInt() - 1}].scrollIntoView()",
            "scrollToPageBottom"
        )

        sleepWithRandom(1000, 1000)

        sendScriptToJSShellCommand(
            "Array.from(document.querySelectorAll(\"button\")).filter(i => i.outerText.includes(\"Load more\"))[0].click();",
            "click {Load more}"
        )

        pageCount++

        linkedAlertPingBot.sendMessage("next page $pageCount")

        sleepWithRandom(6500, 1000)

        val htmlCount = sendScriptToJSShellCommandWithResult("$cleanConnectionCardsV2.length", "cleanConnectionCards length")
        if (!isCompleteRequest(htmlCount)) {
            return false
        }

        val htmlTimeBadge = sendScriptToJSShellCommandWithResult(
            "$cleanConnectionCardsV2[${htmlCount.toInt() - 1}].querySelectorAll('p')[2].outerText",
            "get TimeBadge"
        )
        if (!isCompleteRequest(htmlTimeBadge)) {
            return false
        }

        if (checkTimeBadge(htmlTimeBadge)) {

            for (i in (htmlCount.toInt() - 1) downTo 0) {

                val htmlTimeBadge2 =
                    sendScriptToJSShellCommandWithResult(
                        "$cleanConnectionCardsV2[$i].querySelectorAll('p')[2].outerText",
                        "get TimeBadge"
                    )
                if (!isCompleteRequest(htmlTimeBadge2)) {
                    return false
                }

                if (withdrawCount == MAX_CLEAN_CONNECTION_PER_RUN || foundAttemptCount == MAX_FOUND_ATTEMPT_COUNT || !checkTimeBadge(htmlTimeBadge2)) {

                    Logger.logDebug("exit withdrawCount: $withdrawCount, withdrawCount: $withdrawCount, checkTimeBadge: ${checkTimeBadge(htmlTimeBadge)}")
                    return true

                } else {

                    val htmlHref = sendScriptToJSShellCommandWithResult(
                        "$cleanConnectionCardsV2[$i].querySelector('[href]').getAttribute('href')", "get profile href"
                    )
                    if (!isCompleteRequest(htmlHref)) {
                        return false
                    }

                    profile.addWithdrawProfiles(htmlHref)

                    sendScriptToJSShellCommand(
                        "$cleanConnectionCardsV2[$i].querySelector('[data-view-name=\"sent-invitations-withdraw-single\"]').click()",
                        "click btn {Withdraw}"
                    )

                    sleepWithRandom(6500, 1000)

                    sendScriptToJSShellCommand(
                        "document.querySelector('[aria-label^=\"Withdrawn\"]').click()", "click btn {data-test-dialog-primary-btn}"
                    )

                    linkedAlertPingBot.sendMessage("*Withdraw* connection | $htmlTimeBadge2 | num: $i")

                    sleepWithRandom(6500, 1000)

                    withdrawCount++

                }

                foundAttemptCount++
            }

        }

        Logger.logDebug("htmlPrevCount: $htmlPrevCount, htmlCount: $htmlCount, pageCount: $pageCount, MAX_PAGE_COUNT: $MAX_PAGE_COUNT")

    } while (htmlPrevCount != htmlCount && pageCount != MAX_PAGE_COUNT)

    return true
}

fun checkTimeBadge(htmlTimeBadge: String): Boolean {
    Config.cleanConnectionAge.forEach {
        if (htmlTimeBadge.contains(it)) {
            return true
        }
    }
    return false
}

