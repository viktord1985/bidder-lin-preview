package com.linked.scanner.action

import com.linked.api.*
import com.linked.bidder.appConfig
import com.linked.bidder.linkedAlertPingBot
import com.linked.bidder.utils.Config
import com.linked.bidder.utils.Logger
import com.linked.chromedriver.profileAnalyticName
import com.linked.db.findFirebaseProfileItemByAnalyticName
import com.linked.db.getFirebaseProfiles
import com.linked.db.updateFirebaseProfile
import com.linked.model.*
import com.linked.scanner.*
import com.linked.scanner.logic.CoverLetterType
import com.linked.scanner.logic.formatCoverLetterWithAI
import com.linked.scanner.logic.getDecisionMakers
import com.linked.scanner.logic.isLinkedProfileFits
import com.linked.utils.*
import org.jsoup.Jsoup

const val connectionCardsV1 = "document.querySelectorAll('.mn-connection-card')"
const val connectionCardsV1Hrefs = "document.querySelectorAll('.mn-connection-card a[href]')"
const val connectionCardsV2 = "document.querySelectorAll('[data-view-name=\"connections-list\"]')"
const val connectionCardsV21Hrefs = "document.querySelectorAll('[data-view-name=\"connections-list\"] a[href]')"

fun autoReferLatterOnMyNetwork(profile: ChromeProfile, linkedPostDB: LinkedPostDB): Boolean {
    //delay for page load
    sleepWithRandom(6000, 3000)

    //Note: this lazy loading didn't work - either too few connections on the profile, or I didn't figure it out
    // trigger loading more of the list
    //scrollToPageBottom()
    //sleepWithRandom(9000, 3000)
    //scrollToPageTop()
    //sleepWithRandom(6000, 3000)

    // get the list length
    var isConnectionCardsV1 = true;
    var htmlStr0 = sendScriptToJSShellCommandWithResult("$connectionCardsV1.length", "connectionCardsV1 length")
    if (!isCompleteRequest(htmlStr0)) {
        return false
    }
    if (htmlStr0 == "0") {
        htmlStr0 = sendScriptToJSShellCommandWithResult("$connectionCardsV2.length", "connectionCardsV2 length")
        if (!isCompleteRequest(htmlStr0)) {
            return false
        }
        if (htmlStr0 == "0") {
            Logger.logDebug("!!! no connection for autoReferLatterOnMyNetwork !!!")
            return true
        } else {
            isConnectionCardsV1 = false
        }
    }

    var profileLinksFromPage = getProfileLinksFromPage(isConnectionCardsV1)

    if (profileLinksFromPage == null) {
        Logger.logDebug("!!! getProfileLinksFromPage fail !!!")
        return false
    }

    // remove duplicates
    profileLinksFromPage = profileLinksFromPage.distinct()

    //Note: without scrolling the page has only a few profiles (11-16, varies). Tried scrolling down, it didn't work, didn't dig further
    //only last dialogs
    val MAX_LAST_CONNECTIONS_COUNT = 18

    Logger.logDebug("---------- $profileAnalyticName ----------")

    printArray(profileLinksFromPage, "!!! profileLinksFromPage !!!")

    if (profileLinksFromPage.size > MAX_LAST_CONNECTIONS_COUNT) {

        profileLinksFromPage = profileLinksFromPage.take(MAX_LAST_CONNECTIONS_COUNT)

        printArray(profileLinksFromPage, "!!! profileLinksFromPage limited to $MAX_LAST_CONNECTIONS_COUNT !!!")
    }

    val notConnectedProfileLinks = getNotConnectedProfileLinks(profileLinksFromPage, profile)

    printArray(notConnectedProfileLinks, "!!! notConnectedProfileLinks !!!")

    Logger.logDebug("profileLinksFromPage size: ${profileLinksFromPage.size}, notConnectedProfileLinks size: ${notConnectedProfileLinks.size}")

    if (checkNotConnectedProfileAndCleanNeedCheckIfNeed(notConnectedProfileLinks, profile)) {
        return true
    }

    val MAX_CONNECTIONS_PER_RUN = if (profile.isOldProfile()) Config.maxConnectsPerRunOld else Config.maxConnectsPerRun

    linkedAlertPingBot.sendMessage("Unprocessed profiles count: ${notConnectedProfileLinks.size}, max: $MAX_CONNECTIONS_PER_RUN")

    var connectionCount = -1

    var connectionPerRunCount = 0

    try {
        connectionCount = htmlStr0.toInt()
        Logger.logInfo("connectionCount from page: $connectionCount")
    } catch (_: NumberFormatException) {
    }

    if (connectionCount == -1) {
        Logger.logDebug("!!! Connections not found !!!")
        return true
    }

    if (connectionCount > MAX_LAST_CONNECTIONS_COUNT) {
        connectionCount = MAX_LAST_CONNECTIONS_COUNT;
    }

    for (i in 0..(connectionCount - 1)) {

        if (connectionPerRunCount >= MAX_CONNECTIONS_PER_RUN) {
            break
        }

        sleepWithRandom(1500, 100)

        // get the profile link
        var htmlLink = sendScriptToJSShellCommandWithResult(
            "${if (isConnectionCardsV1) connectionCardsV1 else connectionCardsV2}[$i].querySelectorAll('[href]')[0].getAttribute('href')",
            "get profile href"
        )
        if (!isCompleteRequest(htmlLink)) {
            return false
        }

        if (!htmlLink.contains("https://www.linkedin.com")) {
            htmlLink = "https://www.linkedin.com$htmlLink";
        }

        if (profile.isConnectedProfile(htmlLink)) {
            Logger.logInfo("Skip connected url: $htmlLink")
        } else {

            connectionPerRunCount++

            val profiles = ConnectProfileDB().getList()
            Logger.logInfo("Loaded profiles count: ${profiles.size}")

            var profileTag = profile.getBidTag(htmlLink)

            if (profileTag.isEmpty()) {
                profileTag = "na"
            }

            profiles.forEach { item ->
                if (htmlLink.startsWith(item.url)) {
                    Logger.logInfo("Found outreach item: ${item.url}")

                    val scannerProfileId = profile.id + " | " + appConfig.getProfileNameByChromeProfileName(profile.id)

                    ConnectProfileDB().updateStatus(item.id, "Connected", scannerProfileId)
                }
            }

            val htmlTitle = sendScriptToJSShellCommandWithResult(
                "${if (isConnectionCardsV1) connectionCardsV1 else connectionCardsV2}[$i].querySelectorAll('[href]')[1].outerText",
                "get profile title"
            )
            if (!isCompleteRequest(htmlTitle)) {
                return false
            }

            linkedAlertPingBot.sendMessage("*Found a new connection* num: ${i + 1}, tag: $profileTag\ntitle: $htmlTitle")

            getConnectEvent(profileTag, appConfig.botID, profileAnalyticName)

            profile.incInputConnectCount()

            //todo - until {sending a referral} and {suggested search} are separated, the check is disabled, otherwise it loops forever
            // because the needCheck status is set
//            if (!profile.isCanBidBot()) {
//
//                linkedAlertPingBot.sendMessage("*Out of bids* cannot continue")
//
//                Logger.logInfo("profile.isCanBidBot() == false")
//                break
//            }

            linkedAlertPingBot.sendMessage("*Inspecting connected profile* num: ${i + 1}")

            val openAcceptedProfileTest = openAcceptedProfile(htmlLink, htmlTitle, profile, linkedPostDB)

            if (openAcceptedProfileTest == null) {
                Logger.logDebug("!!! openAcceptedProfile fail !!!")
                return false
            }

            Logger.logInfo("openAcceptedProfileTest: $openAcceptedProfileTest")

            Logger.logInfo("Added to connected url: $htmlLink")
            profile.addConnectedProfile(htmlLink)
        }
    }

    val notConnectedProfileLinksAfter = getNotConnectedProfileLinks(profileLinksFromPage, profile)

    Logger.logDebug("notConnectedProfileLinksAfter size: ${notConnectedProfileLinksAfter.size}")

    if (checkNotConnectedProfileAndCleanNeedCheckIfNeed(notConnectedProfileLinksAfter, profile)) {
        return true
    } else {
        linkedAlertPingBot.sendMessage("*Unprocessed profiles remain* count: ${notConnectedProfileLinksAfter.size}")
    }

    return true
}

fun checkNotConnectedProfileAndCleanNeedCheckIfNeed(notConnectedProfileLinks: List<String>, profile: ChromeProfile): Boolean {
    if (notConnectedProfileLinks.isEmpty()) {
        Logger.logInfo("notConnectedProfileLinks is empty")

        if (profile.needCheck) {
            linkedAlertPingBot.sendMessage("*All profiles processed*")
        } else {
            linkedAlertPingBot.sendMessage("All profiles processed")
        }

        profile.needCheck = false

        val firebaseProfiles = getFirebaseProfiles(appConfig.botID)

        val firebaseProfile = findFirebaseProfileItemByAnalyticName(firebaseProfiles, getAnalyticNameByChromeProfile(profile))

        if (firebaseProfile != null) {

            if (firebaseProfile.needCheck) {

                firebaseProfile.needCheck = false

                val res = updateFirebaseProfile(firebaseProfile)
                if (!res) {
                    Logger.logDebug("!!! updateFirebaseProfile fail, next attempt #2 !!!")
                    val res2 = updateFirebaseProfile(firebaseProfile)
                    if (!res2) {
                        Logger.logDebug("!!! updateFirebaseProfile fail, next attempt #3!!!")
                        updateFirebaseProfile(firebaseProfile)
                    }
                }

                linkedAlertPingBot.sendMessage("*needCheck is disabled* ${firebaseProfile.analyticName}")
            }
        }

        return true
    }

    return false
}

fun getProfileLinksFromPage(isConnectionCardsV1: Boolean): List<String>? {
    var list = listOf<String>()

    val htmlLink = sendScriptToJSShellCommandWithResult(
        "Array.from(${if (isConnectionCardsV1) connectionCardsV1Hrefs else connectionCardsV21Hrefs}).map(e => e.href).filter(h => h.startsWith('https://www.linkedin.com/in/'))",
        "get profile hrefs from page"
    )

    if (!isCompleteRequest(htmlLink)) {
        return null
    }

    list = htmlLink.replace("[", "").replace("]", "").replace("\n", "").replace("\"", "").replace("\t", "").trim().split(",")
        .map { item -> item.trim() }

    return list.distinct()
}

fun getNotConnectedProfileLinks(profileLinksFromPage: List<String>, chromeProfile: ChromeProfile): List<String> {
    val notConnected = mutableListOf<String>()

    profileLinksFromPage.forEach { item ->
        val url = item.replace("\n", "").trim()
        //Logger.logDebug(">>> item: $item")
        //Logger.logDebug(">>> url: $url")
        if (!chromeProfile.isConnectedProfile(url)) {
            notConnected.add(url)
        }
    }

    return notConnected
}

const val messageList = "document.querySelectorAll('.msg-s-event-listitem__message-bubble')"
const val allMessageTexts =
    "Array.from(document.querySelectorAll('.msg-s-event-listitem__message-bubble')).map(e => 'message: '+ e.outerText)"

const val seeMoreRecommendationsBtn = "document.querySelector('#navigation-overlay-section-see-more-browsemap-recommendations')"

fun closeOpenedChatDialogs(): Boolean {
    val maxAttemptCount = 9
    var attemptCount = 0

    do {

        val htmlStr = sendScriptToJSShellCommandWithResult(
            "document.querySelectorAll('button svg use[href=\"#close-small\"]').length", "count of open dialogs"
        )
        if (!isCompleteRequest(htmlStr)) {
            return false
        }

        Logger.logDebug("close buttons count: $htmlStr");

        if (htmlStr != "null") {

            if (htmlStr == "0") {
                //nope
            } else if (htmlStr == "1") {
                sleep(2000)
                sendScriptToJSShellCommand(
                    "document.querySelector('button svg use[href=\"#close-small\"]')?.closest('button').click();", "click Close"
                )
                sleep(2000)
            } else {
                sleep(2000)
                sendScriptToJSShellCommand(
                    "document.querySelectorAll('button svg use[href=\"#close-small\"]')[1]?.closest('button').click();", "click Close"
                )
                sleep(2000)
            }
        }

        attemptCount++

        if (attemptCount > maxAttemptCount) {
            linkedAlertPingBot.sendMessage("!!! close chat dialog max attempts !!!")
            break
        }

    } while (htmlStr != "0" && htmlStr != "null")

    return true
}

fun openAcceptedProfile(link: String, title: String, scannerProfile: ChromeProfile, linkedPostDB: LinkedPostDB): Boolean? {

    scannerProfile.incOpenedProfileCount()

    linkedAlertPingBot.sendMessage("Open profile ..")

    Logger.logInfo("open item link: $link")

    sleepWithRandom(12000, 4000)
    openLinkOnNewTab(link)

    //open console
    sleepWithRandom(4000, 1000)
    sendOpenJSConsoleCommand();

    pingInfoWithDynamicDelay()

    Logger.logInfo("#############x#############x#############")
    Logger.logInfo("!!! JS console open testing !!!")
    if (!isJSConsoleReadyOnProfilePage()) {
        Logger.logInfo("!!! additional delay for JS console open !!!")
        Logger.logInfo("#############x#############x#############")
        sleepWithRandom(4000, 1000)
    }

    var repeatCount = REPEAT_COUNT

    while (!(run { val res = isProfileReady(title) ?: return null; res }) && repeatCount > 0) {
        repeatCount--;
        Logger.logInfo("!!! profile parse - repeat check after - $REPEAT_TIMEOUT");
        sleep(REPEAT_TIMEOUT)
    }

    if (repeatCount == 0) {
        Logger.logInfo("!!! profile parse - skip on max repeat count");
        //maxRepeatCount++
        val isProfileBlockedTest = isProfileBlocked()
        if (isProfileBlockedTest == null) {
            Logger.logInfo("!!! isProfileBlocked fail !!!")
            return null
        }
        if (isProfileBlockedTest) {
            Logger.logInfo("!!! profile parse - blocked !!!")
            //blockCount++
            scannerProfile.setBlocked()
        }
        return null
    }

    val linkedProfile = getLinkedProfile(link, title)

    if (linkedProfile == null) {
        return null
    }

    linkedProfile.selfTest()

    //item.setMach(isLinkedProfileFits(linkedProfile))

    val profileInfo = linkedProfile.getProfileInfo()

    Logger.logDebug(
        "# ProfileInfo: ${
            if (profileInfo.length < 1024) profileInfo else profileInfo.substring(
                0, 1023
            )
        }"
    )

    //item.location = linkedProfile.location
    //item.followers = linkedProfile.followers

    //Logger.logDebug("# Location: ${item.location}")

    //linkedPostDB.items.add(item)

    //if (item.isMach()) {
    //    filteredLinkedPostList.add(item)
    //    onMach(item)
    //}

    // --- messages logic ---

    if (!checkRedBadge()) {
        Logger.logDebug("!!! checkRedBadge fail !!!")
        return null
    }

    //todo - broken by LinkedIn
    // close all dialogs/chats
    if (!closeOpenedChatDialogs()) {
        Logger.logDebug("!!! closeOpenedChatDialogs fail !!!")
        return null
    }

    // click and open the dialog
    // todo - PrimaryMessage here; it may turn out to be a non-primary or secondary button
    clickPrimaryMessageButtonOnProfileBeforeMoreMenu()

    // probably need to give it time to load here
    sleepWithRandom(9000, 1000)

    // get the number of messages
    val htmlStr3 = sendScriptToJSShellCommandWithResult("$messageList.length", "messageList length")
    if (!isCompleteRequest(htmlStr3)) {
        return null
    }
    var messageCount = -1
    try {
        messageCount = htmlStr3.toInt()
    } catch (_: NumberFormatException) {
    }
    if (messageCount == -1) {
        Logger.logDebug("!!! fail get messageList length !!!")
    }
    var skipMessage = false

    if (messageCount == 0 || messageCount == 1) {
        skipMessage = false
    }

    if (messageCount == 2 || messageCount == 3) {
        // get the dialog
        val htmlAllMessageTexts = sendScriptToJSShellCommandWithResult(allMessageTexts, "allMessageTexts")
        if (!isCompleteRequest(htmlAllMessageTexts)) {
            return null
        }

        if (htmlAllMessageTexts == "[]") {
            skipMessage = true
            Logger.logDebug("htmlAllMessageTexts == []")
        } else {
            val allMessageTextsStr =
                htmlAllMessageTexts.replace("\"", "").replace("\t", "").replace("[", "").replace("]", "").replace(",", "\n")
                    .replace("\\n", "\n").lines().map { item -> item.trim() } // trim leading whitespace
                    .joinToString("\n")

            Logger.logInfo("allMessageTextsStr:\n$allMessageTextsStr")

            linkedAlertPingBot.sendMessage("*All messages:*\n$allMessageTextsStr")

            var result = openAIRequest(allMessageTextsStr, appConfig.isRequestConnectDetectPrompt)
            result = result.toLowerCase().trim()

            Logger.logDebug("openAIRequest result: $result")

            if (result == "yes" || result == "yes.") {
                Logger.logInfo("Confirm isRequestConnectDetectPrompt from AI")
                skipMessage = false
            } else {
                linkedAlertPingBot.sendMessage("*!!! AI blocked sending the message !!!*")
                skipMessage = true
            }
        }
    }

    if (messageCount > 3) {
        skipMessage = true
    }

    if (skipMessage) {

        Logger.logInfo("messageList length: $messageCount, skip dialog")

        linkedAlertPingBot.sendMessage("*!!! The chat already has messages !!!*")

    } else {

        val messageText = formatCoverLetterWithAI(
            linkedProfile.title + "\n" + linkedProfile.description + "\n" + linkedProfile.about, CoverLetterType.botRefer
        );

        linkedAlertPingBot.sendMessage(messageText)

        // add a delay so we don't send many messages at once
        sleepWithRandom(6000, 3000)

        // set text into the first active input field
        sendScriptToJSShellCommand(
            "function simulateTyping(div, text) { div.focus(); document.execCommand('selectAll', false, null); document.execCommand('insertText', false, text); } simulateTyping(document.querySelector('div[contenteditable=\"true\"]'), \"${
                convertXmlTextToJS(
                    messageText
                )
            }\");", "set msg text"
        )
        sleepWithRandom(5000, 2000)
        //Note: no longer remember why {click Send} is done twice
        sendScriptToJSShellCommand(
            "document.querySelector('.msg-convo-wrapper').querySelector('.msg-form__send-button').click();", "click send button"
        )

        // send
        sendScriptToJSShellCommand("document.querySelector('[type=\"submit\"]').click();", "click Send")

        sleepWithRandom(9000, 2000)

        sendReferEvent(appConfig.botID, profileAnalyticName)

        linkedAlertPingBot.sendMessage("*Sent message* with referral")
    }

    // close the dialog
    sendScriptToJSShellCommand(
        "document.querySelector('button svg use[href=\"#close-small\"]')?.closest('button').click();", "click Close"
    )

    // --- random logic ---

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

    // --- suggested logic ---

    //todo if (scannerProfile.isCanBidBot()) {
    //todo disabled the Suggested scan and bids based on it
    if (false) {

        linkedAlertPingBot.sendMessage("*Looking for profiles to connect with*")

        var suggestedLinkedPostList = listOf<LinkedPost>()

        sleepWithRandom(3000, 2000)
        val htmlStr = sendScriptToJSShellCommandWithResult(seeMoreRecommendationsBtn, "seeMoreRecommendationsBtn isExist")

        if (!isCompleteRequest(htmlStr)) {
            return null
        }

        if (htmlStr != "null") {

            sleepWithRandom(12000, 2000)
            sendScriptToJSShellCommand("$seeMoreRecommendationsBtn.click();", "seeMoreRecommendationsBtn click")

            sleepWithRandom(5000, 2000)
            val suggestedLinkedPostListTest = getSuggestedLinkedPostList(scannerProfile.id)
            if (suggestedLinkedPostListTest == null) {
                Logger.logDebug("!!! getSuggestedLinkedPostList fail !!!")
                return null
            }
            suggestedLinkedPostList = suggestedLinkedPostListTest

        } else {
            linkedAlertPingBot.sendMessage("*Missing - More profiles for you*")

            sleepWithRandom(3000, 2000)
            val htmlStr2 = sendScriptToJSShellCommandWithResult(
                "document.querySelector('#premium_browsemap_recommendation')", "premiumRecommendation isExist"
            )

            if (!isCompleteRequest(htmlStr2)) {
                return null
            }

            if (htmlStr2 != "null") {

                sleepWithRandom(3000, 2000)
                val suggestedLinkedPostListTest = getPremiumLinkedPostList(scannerProfile.id)
                if (suggestedLinkedPostListTest == null) {
                    Logger.logDebug("!!! getPremiumLinkedPostList fail !!!")
                    return null
                }
                suggestedLinkedPostList = suggestedLinkedPostListTest

            } else {
                linkedAlertPingBot.sendMessage("*Missing - Explore Premium profiles*")
            }
        }

        if (suggestedLinkedPostList.isNotEmpty()) {

            val excludeProfileLinks = mutableListOf<String>()

            var attemptsRepeatCount =
                if (scannerProfile.isOldProfile()) Config.maxAttemptsFindSuggestedProfileOld else Config.maxAttemptsFindSuggestedProfile

            while (attemptsRepeatCount > 0) {

                val item = getSuggestedLinkedPost(suggestedLinkedPostList, excludeProfileLinks)
                if (item != null) {
                    excludeProfileLinks.add(item.profileLink)

                    linkedAlertPingBot.sendMessage("*Inspecting suggested profile* var: ${Config.maxAttemptsFindSuggestedProfile - attemptsRepeatCount + 1}\ntitle: ${item.title}, desc: ${item.description}")

                    val openSuggestedProfileTest = openAndBidProfile(item, scannerProfile, linkedPostDB, "suggested")
                    if (openSuggestedProfileTest == null) {
                        Logger.logDebug("!!! openSuggestedProfile fail !!!")
                        return null
                    }
                    if (openSuggestedProfileTest) {

                        Logger.logInfo("openSuggestedProfileTest: $openSuggestedProfileTest")

                        //close window
                        sleepWithRandom(4000, 1000)
                        sendCloseChromeTabCommand("openAcceptedProfile")

                        return true
                    } else {
                        attemptsRepeatCount--
                        sleepWithRandom(12000 * (Config.maxAttemptsFindSuggestedProfile - attemptsRepeatCount), 5000)
                    }
                } else {
                    linkedAlertPingBot.sendMessage("*No decision maker found*")
                    break
                }
            }

            if (attemptsRepeatCount == 0) {
                linkedAlertPingBot.sendMessage("*Max search attempts used*")
            }

        } else {
            linkedAlertPingBot.sendMessage("*Suggested List is Empty*")
        }

    } else {
        linkedAlertPingBot.sendMessage("*Max number of bids used*")
    }

    //close window
    sleepWithRandom(4000, 1000)
    sendCloseChromeTabCommand("openAcceptedProfile")

    return true
}

fun getSuggestedLinkedPost(list: List<LinkedPost>, excludeListIds: List<String>): LinkedPost? {
    Logger.logDebug("!!! Start check profile desc !!!")
    val excludedList = mutableListOf<LinkedPost>()
    list.forEach { item ->
        if (!excludeListIds.contains(item.profileLink)) {
            excludedList.add(item)
        }
    }
    Logger.logDebug("!!! ------- !!!")
    excludedList.forEach { item ->
        Logger.logDebug("!!! List#1 desc: ${item.description}")
    }
    val decisionMakers = getDecisionMakers(excludedList)
    if (decisionMakers.isNotEmpty()) {
        Logger.logDebug("!!! ------- !!!")
        decisionMakers.forEach { item ->
            Logger.logDebug("!!! List#2 desc: ${item.description}")
        }
        return decisionMakers[0]
    }
    Logger.logDebug("no decision maker found")
//Note: disabled the variant where someone was always returned, to save connections
//    if (excludedList.isEmpty()) {
//        return null
//    } else {
//        return excludedList[0]
//    }
    return null
}

fun getSuggestedLinkedPostList(scannerId: String): List<LinkedPost>? {
    sleep(333)
    val htmlStr = sendScriptToJSShellCommandWithResult("document.querySelector('.artdeco-modal__content')", "getModalList")
    if (!isCompleteRequest(htmlStr)) {
        return null
    }
    val doc = Jsoup.parse(htmlStr)
    val articles = doc.select(".artdeco-list__item")
    val items = mutableListOf<LinkedPost>()
    articles.forEach { item ->
        items.add(suggestedItemToLinkedPostItem(item, scannerId))
    }
    return items
}

fun getPremiumLinkedPostList(scannerId: String): List<LinkedPost>? {
    sleep(333)
    //note: not done now because this feature is disabled
    val htmlStr = "" //todo a -> sendScriptToJSShellCommandWithResult(getLayoutMain, "getLayoutMain for parse premium list")
    if (!isCompleteRequest(htmlStr)) {
        return null
    }
    val doc = Jsoup.parse(htmlStr)
    val articles = doc.select("[data-field='premium_spotlight_card_click']")
    val items = mutableListOf<LinkedPost>()
    var skipItem = true;
    articles.forEach { item ->
        if (!skipItem) {
            // odd elements only
            items.add(premiumItemToLinkedPostItem(item, scannerId))
        }
        skipItem = !skipItem
    }
    return items
}

//return == null -> error
//return == true -> connected
//return == false -> no connection: isMatch() failed or already connected
fun openAndBidProfile(
    item: LinkedPost, scannerProfile: ChromeProfile, linkedPostDB: LinkedPostDB, tag: String, runMatch: Boolean = true
): Boolean? {

    scannerProfile.incOpenedProfileCount()

    linkedAlertPingBot.sendMessage("Open profile ..")

    Logger.logInfo("open item id: ${item.id}, link: ${item.link}")

    sleepWithRandom(12000, 4000)
    openLinkOnNewTab(item.link)

    //open console
    sleepWithRandom(4000, 1000)
    sendOpenJSConsoleCommand()

    pingInfoWithDynamicDelay()

    Logger.logInfo("#############x#############x#############")
    Logger.logInfo("!!! JS console open testing !!!")
    if (!isJSConsoleReadyOnProfilePage()) {
        Logger.logInfo("!!! additional delay for JS console open !!!")
        Logger.logInfo("#############x#############x#############")
        sleepWithRandom(4000, 1000)
    }

    var repeatCount = REPEAT_COUNT

    while (!(run { val res = isProfileReady(item.title) ?: return null; res }) && repeatCount > 0) {
        repeatCount--;
        Logger.logInfo("!!! profile parse - repeat check after - $REPEAT_TIMEOUT");
        sleep(REPEAT_TIMEOUT)
    }

    if (repeatCount == 0) {
        Logger.logInfo("!!! profile parse - skip on max repeat count");
        //maxRepeatCount++
        val isProfileBlockedTest = isProfileBlocked()
        if (isProfileBlockedTest == null) {
            Logger.logInfo("!!! isProfileBlocked fail !!!")
            return null
        }
        if (isProfileBlockedTest) {
            Logger.logInfo("!!! profile parse - blocked !!!")
            //blockCount++
            scannerProfile.setBlocked()
        }
        return null
    }

    val linkedProfile = getLinkedProfile(item.link, item.title)

    if (linkedProfile == null) {
        return null
    }

    linkedProfile.selfTest()

    if (runMatch) {
        item.setMach(isLinkedProfileFits(linkedProfile))
    } else {
        item.setMach(true)
    }

    item.profileInfo = linkedProfile.getProfileInfo()

    Logger.logDebug(
        "# ProfileInfo: ${
            if (item.profileInfo.length < 1024) item.profileInfo else item.profileInfo.substring(
                0, 1023
            )
        }"
    )

    item.location = linkedProfile.location
    item.followersConnectionsInfo = linkedProfile.followersConnectionsInfo

    Logger.logDebug("# Location: ${item.location}")

    //save to DB
    linkedPostDB.items.add(item)

    //if (item.isMach()) {
    //    filteredLinkedPostList.add(item)
    //    onMach(item)
    //}

    if (item.isMach()) {

        if (scannerProfile.isCanBidBot()) {

            Logger.logInfo("Start bot bidder, link: ${linkedProfile.url} ")

            val checkBidProfileTest = checkBidProfile(linkedProfile, onContinue = {

                val bidProfileTest = bidProfile(scannerProfile, linkedProfile, CoverLetterType.botNote, tag)

                if (bidProfileTest == null) {
                    // something went wrong
                    takeScreenshot(profileAnalyticName)
                    return@checkBidProfile null
                }

                if (!bidProfileTest) {
                    return@checkBidProfile false
                }

                scannerProfile.incBidProfileBotCount()

                var linkedProfileUrl = linkedProfile.url
                if (linkedProfile.url.last() != '/') {
                    linkedProfileUrl = "$linkedProfileUrl/"
                }

                linkedAlertPingBot.sendMessage("Link: $linkedProfileUrl")

                scannerProfile.addBidTag(tag, linkedProfileUrl)

                return@checkBidProfile true
            })

            // this can be null, success or false; if the check failed (already connected) we exit

            Logger.logInfo("checkBidProfileTest: $checkBidProfileTest")

            //close window
            sleepWithRandom(4000, 1000)
            sendCloseChromeTabCommand("openAndBidProfile")

            return checkBidProfileTest

        } else {
            linkedAlertPingBot.sendMessage("*Out of bids*")
        }

    } else {
        linkedAlertPingBot.sendMessage("isMach() == false")
    }

    //close window
    sleepWithRandom(4000, 1000)
    sendCloseChromeTabCommand("openAndBidProfile")

    return item.isMach()
}

