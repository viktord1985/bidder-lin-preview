package com.linked.scanner

import com.linked.api.bulkEvent
import com.linked.api.getAnalyticNameByChromeProfile
import com.linked.api.linkedPostEvent
import com.linked.bidder.appConfig
import com.linked.bidder.linkedAlertPingBot
import com.linked.bidder.utils.Config
import com.linked.bidder.utils.Logger
import com.linked.chromedriver.MyChromeDriver
import com.linked.chromedriver.profileAnalyticName
import com.linked.db.*
import com.linked.model.*
import com.linked.model.command.*
import com.linked.scanner.logic.CoverLetterType
import com.linked.scanner.logic.isLinkedPostFits
import com.linked.scanner.logic.isLinkedProfileFits
import com.linked.utils.*
import org.jsoup.Jsoup

class LinkedPostsScanner() {
    private var thread: Thread? = null

    fun start(
        onMach: (LinkedPost?) -> Unit, onBeforeStart: () -> Unit,
        // selectedLinkedPosts, limitReach, failCount, blocked, scannerUsageStatus, scannerLimitStatus, failedProfilesInfo, isMaxPostCount, processedCount, bidCount
        onSuccess: (List<LinkedPost>?, Boolean, Int, Boolean, String, String, Boolean, Boolean, Int, Int) -> Unit
    ) {
        thread = Thread(Runnable {

            try {

                onBeforeStart()

                runLinkedPostsScanner(onMach, onSuccess)

            } catch (e: Exception) {
                e.printStackTrace()
                Logger.logError("#Error on LinkedPostsScanner: $e")
                onSuccess(null, false, -1, false, "Error", "Error", false, false, -1, -1);
            }
        })
        thread!!.start()
    }

    private val linkedPostDB = loadLinkedPostDB()
    private val allFilteredLinkedPostList = mutableListOf<LinkedPost>()
    private var isFeedEnd = false
    private var isMaxPostCount = false
    private var blockCount = 0
    private var maxRepeatCount = 0
    private var processedCount = 0
    private var bidCount = 0

    private val failedChromeProfiles = mutableListOf<ChromeProfile>()
    private val excludedChromeProfiles = mutableListOf<ChromeProfile>()

    //analytics
    private var totalCount = 0
    private var processCount = 0
    private var openCount = 0
    private var matchCount = 0

    private var bootRunCount = 0;

    private fun runLinkedPostsScanner(
        // selectedLinkedPosts, limitReach, failCount, blocked, scannerUsageStatus, scannerLimitStatus, failedProfilesInfo, isMaxPostCount, processedCount, bidCount
        onMach: (LinkedPost?) -> Unit,
        onSuccess: (List<LinkedPost>?, Boolean, Int, Boolean, String, String, Boolean, Boolean, Int, Int) -> Unit
    ) {

        bootRunCount++

        val firebaseProfiles = getFirebaseProfiles(appConfig.botID)

        val scannerProfile = findProfileForNextRun(linkedPostDB, firebaseProfiles, failedChromeProfiles, excludedChromeProfiles)

        var scannerProfileName = ""
        if (scannerProfile != null) {
            scannerProfileName = " | " + appConfig.getProfileNameByChromeProfileName(scannerProfile.id)
        }

        if (scannerProfile != null) {

            // so it doesn't run again in the same session
            if (scannerProfile.needCheck) {
                excludedChromeProfiles.add(scannerProfile)
            }

            val needCheckInfo = if (scannerProfile.needCheck) " | *needCheck is active*" else ""

            linkedAlertPingBot.sendMessage("*Scanner profile:* ${scannerProfile.id}$scannerProfileName$needCheckInfo ..")

            linkedAlertPingBot.sendMessage(scannerProfile.getProfileLimitsInfo())

            val scannerProfileId = scannerProfile.id + scannerProfileName

            var repeatCount = REPEAT_COUNT

            Logger.logInfo("#Start LinkedPostsScanner, profile id: ${scannerProfile.id}")

            isFeedEnd = false
            isMaxPostCount = false

            MyChromeDriver(scannerProfile, linkedPostDB, appConfig.startUrl).start(onFeedCommands = {

                while (!(run { val res = isPostListReady() ?: return@start null; res }) && repeatCount > 0) {
                    repeatCount--;
                    Logger.logInfo("!!! posts parse - repeat check after - $REPEAT_TIMEOUT");
                    sleep(REPEAT_TIMEOUT)
                }

                if (repeatCount == 0) {
                    Logger.logInfo("!!! posts parse - skip on max repeat count");
                    maxRepeatCount++
                    val isProfileBlockedTest = isProfileBlocked()
                    if (isProfileBlockedTest == null) {
                        Logger.logInfo("!!! isProfileBlocked fail !!!")
                        return@start null;
                    }
                    if (isProfileBlockedTest) {
                        Logger.logInfo("!!! profile parse - blocked !!!")
                        blockCount++
                        scannerProfile.setBlocked()
                    }
                    return@start null;
                }

                val processedLinkedPostList = mutableListOf<LinkedPost>()
                val filteredLinkedPostList = mutableListOf<LinkedPost>()

                var isShowMoreButtonExistTest: Boolean? = null

                do {

                    val isFiltersBarActiveTest = isFiltersBarActive()
                    if (isFiltersBarActiveTest == null) {
                        return@start null
                    }
                    if (!isFiltersBarActiveTest) {
                        Logger.logInfo("!!! no SearchReusablesList !!! profile: $scannerProfileId")
                        linkedAlertPingBot.sendMessage("!!! no SearchReusablesList !!! profile: $scannerProfileId")
                    }

                    val linkedPostList = getLinkedPostList(scannerProfileId)
                    if (linkedPostList == null) {
                        return@start null
                    }

                    Logger.logDebug("linkedPostList count: ${linkedPostList.size} on this step for profile id: ${scannerProfile.id}")

                    val notProcessedLinkedPostList = mutableListOf<LinkedPost>()

                    if (linkedPostList.size > Config.maxPostsPerRun * bootRunCount) {
                        isMaxPostCount = true
                        Logger.logDebug("set {isMaxPostCount = true}")
                    }

                    if (!isMaxPostCount) {

                        linkedPostList.forEach { item ->
                            if (item.selfTest() && !isArrayContainsLinkedPost(processedLinkedPostList, item)) {
                                notProcessedLinkedPostList.add(item)
                                processedLinkedPostList.add(item)
                                processedCount++
                                processCount++
                            }
                        }

                        totalCount = linkedPostList.size

                        notProcessedLinkedPostList.forEach { item ->

                            Logger.logDebug("TMP: notProcessedItem profileLink: ${item.profileLink}")

                            if (!isArrayContainsLinkedPost(
                                    linkedPostDB.items, item
                                ) && isLinkedPostFits(item, linkedPostDB) && scannerProfile.isCanFeedProcess()
                            ) {

                                testOpenProfileTimeout(scannerProfile, notProcessedLinkedPostList)

                                scannerProfile.incOpenedProfileCount()
                                openCount++

                                linkedAlertPingBot.sendMessage("Open profile ..")

                                Logger.logInfo("open item id: ${item.id}, link: ${item.link}")

                                sleepWithRandom(12000, 4000)
                                openLinkOnNewTab(item.link)

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

                                //testMemoryUsage()

                                repeatCount = REPEAT_COUNT

                                while (!(run { val res = isProfileReady(item.title) ?: return@start null; res }) && repeatCount > 0) {
                                    repeatCount--;
                                    Logger.logInfo("!!! profile parse - repeat check after - $REPEAT_TIMEOUT");
                                    sleep(REPEAT_TIMEOUT)
                                }

                                if (repeatCount == 0) {
                                    Logger.logInfo("!!! profile parse - skip on max repeat count");
                                    maxRepeatCount++
                                    val isProfileBlockedTest = isProfileBlocked()
                                    if (isProfileBlockedTest == null) {
                                        Logger.logInfo("!!! isProfileBlocked fail !!!")
                                        return@start null;
                                    }
                                    if (isProfileBlockedTest) {
                                        Logger.logInfo("!!! profile parse - blocked !!!")
                                        blockCount++
                                        scannerProfile.setBlocked()
                                    }
                                    return@start null;
                                }

                                val linkedProfile = getLinkedProfile(item.link, item.title)

                                if (linkedProfile == null) {
                                    return@start null
                                }

                                linkedProfile.selfTest()

                                item.setMach(isLinkedProfileFits(linkedProfile))

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

                                linkedPostDB.items.add(item)

                                if (item.isMach()) {
                                    filteredLinkedPostList.add(item)
                                    matchCount++
                                    onMach(item)
                                }

                                if (item.isMach()) {

                                    if (scannerProfile.isCanBidBot()) {

                                        Logger.logInfo("Start bot bidder, link: ${linkedProfile.url} ")

                                        val checkBidProfileTest = checkBidProfile(linkedProfile, onContinue = {

                                            val tag = "scanner"

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

                                            scannerProfile.addBidTag(tag, linkedProfileUrl)

                                            bidCount++

                                            return@checkBidProfile true
                                        })

                                        if (checkBidProfileTest == null) {
                                            return@start null
                                        }

                                        if (!checkBidProfileTest) {
                                            linkedAlertPingBot.sendMessage("*Pending profile or 1st or Week limits*")
                                        }


                                    } else {

                                        linkedAlertPingBot.sendMessage("*Out of bids*")
                                    }
                                }

                                //close window
                                sleepWithRandom(4000, 1000)
                                sendCloseChromeTabCommand("LinkedPostsScanner")
                            }
                        }

                        linkedAlertPingBot.sendMessage("${scannerProfile.id} total: $totalCount, process: $processCount, open: $openCount, match: $matchCount, last post updated: ${if (processedLinkedPostList.isNotEmpty()) processedLinkedPostList.last().updated else "NA"} ..")

                        scrollToPageBottom()

                        sleepWithRandom(6000, 9000)

                        for (i in 1..REPEAT_COUNT) {
                            val isLoadingMoreResultsExistTest = isLoadingMoreResultsExist()
                            if (isLoadingMoreResultsExistTest == null) {
                                return@start null
                            }
                            if (isLoadingMoreResultsExistTest && i == REPEAT_COUNT) {
                                Logger.logDebug("!!! skip on max wait time for load next post !!!")
                                takeScreenshot(profileAnalyticName)
                                return@start null
                            }
                            if (isLoadingMoreResultsExistTest) {
                                linkedAlertPingBot.sendMessage("Loading ..")
                                //note: it starts entering the loop (sometimes needs more scrolling, but not always)
                                if (i == (REPEAT_COUNT / 2)) {
                                    scrollToPageBottom()
                                }
                                sleep(3000)
                                Logger.logInfo("isLoadingMoreResultsExist == true, waiting ..")
                            } else {
                                break
                            }
                        }
                    }

                    sleepWithRandom(3000, 1000)

                    isShowMoreButtonExistTest = isShowMoreButtonExist()
                    if (isShowMoreButtonExistTest == null) {
                        return@start null
                    }

                    if (!scannerProfile.isCanFeedProcess() || isMaxPostCount || !isShowMoreButtonExistTest) {
                        linkedAlertPingBot.sendMessage("canFeedProcess: ${scannerProfile.isCanFeedProcess()} | maxCount: $isMaxPostCount | showMore: $isShowMoreButtonExistTest")
                    }

                } while (scannerProfile.isCanFeedProcess() && !isMaxPostCount && isShowMoreButtonExistTest!!)

                if (!scannerProfile.isCanFeedProcess()) {
                    Logger.logInfo("!!! isCanFeedProcess = false, profile: ${scannerProfile.id} ")
                }

                isFeedEnd = !(isShowMoreButtonExistTest!!) && scannerProfile.isCanFeedProcess()

                if (!isShowMoreButtonExistTest || isMaxPostCount) {
                    linkedPostDB.goNextSearchQueryIndex(navbarTextArraySize)
                    linkedAlertPingBot.sendMessage("*Go Next Search Query* last index: ${linkedPostDB.isSearchQueryIndexLast(navbarTextArraySize)}")
                }

                return@start filteredLinkedPostList

            }, onFeedCommandsEnd = {

                linkedPostEvent("total", appConfig.botID, profileAnalyticName, totalCount)
                linkedPostEvent("process", appConfig.botID, profileAnalyticName, processCount)
                linkedPostEvent("open", appConfig.botID, profileAnalyticName, openCount)
                linkedPostEvent("match", appConfig.botID, profileAnalyticName, matchCount)

                bulkEvent(
                    "post", appConfig.botID, profileAnalyticName, mapOf(
                        "total" to totalCount,
                        "process" to processCount,
                        "open" to openCount,
                        "match" to matchCount,
                    )
                )

            }, onSuccess = { filteredProjectList ->

                if (filteredProjectList == null) {
                    failedChromeProfiles.add(scannerProfile)
                    linkedAlertPingBot.sendMessage("*Added to failedProfiles* repeats: $repeatCount")
                    Logger.logInfo("!!! Added to failedChromeProfiles ${scannerProfile.id} !!! repeatCount: $repeatCount")
                }

                if (filteredProjectList != null) {
                    allFilteredLinkedPostList.addAll(filteredProjectList)
                }

                // reach end of the feed
                if (isFeedEnd || isMaxPostCount) {

                    if (isFeedEnd) {
                        Logger.logInfo("!!! Success feed end !!! ")
                    }

                    if (isMaxPostCount) {
                        Logger.logInfo("!!! Max post count !!! ")
                    }

                    val isLimitReach = isLimitReach(linkedPostDB)
                    val scannerUsageStatus = getScannersUsageInfo(linkedPostDB)
                    val scannerLimitStatus = getScannersLimitInfo(linkedPostDB)

                    Logger.logInfo("resetOpenProfilesLimit")
                    resetScannerOpenProfilesLimit(linkedPostDB)

                    // update DB
                    saveLinkedPostDB(linkedPostDB)
                    sleepWithRandom(1500, 1500)

                    onSuccess(
                        allFilteredLinkedPostList,
                        isLimitReach,
                        maxRepeatCount,
                        blockCount != 0,
                        scannerUsageStatus,
                        scannerLimitStatus,
                        failedChromeProfiles.isNotEmpty(),
                        isMaxPostCount,
                        processedCount,
                        bidCount
                    )
                    return@start
                }

                // update DB
                saveLinkedPostDB(linkedPostDB)
                sleepWithRandom(1500, 1500)

                runLinkedPostsScanner(onMach, onSuccess)
            })

        } else {

            Logger.logInfo("!!! No profiles for next run !!!")

            linkedAlertPingBot.sendMessage("*!!! No profiles for next run !!!*")

            val isLimitReach = isLimitReach(linkedPostDB)
            val scannerUsageStatus = getScannersUsageInfo(linkedPostDB)
            val scannerLimitStatus = getScannersLimitInfo(linkedPostDB)

            Logger.logInfo("resetOpenProfilesLimit")
            resetScannerOpenProfilesLimit(linkedPostDB)

            // update DB
            saveLinkedPostDB(linkedPostDB)
            sleepWithRandom(1500, 1500)

            onSuccess(
                allFilteredLinkedPostList,
                isLimitReach,
                maxRepeatCount,
                blockCount != 0,
                scannerUsageStatus,
                scannerLimitStatus,
                failedChromeProfiles.isNotEmpty(),
                false,
                processedCount,
                bidCount
            )
            return
        }
    }

    fun stop() {
        if (thread != null) {
            thread!!.stop()
        }
    }
}

var REPEAT_COUNT = 9 // count
var REPEAT_TIMEOUT = 3500 // 3.5sec

const val getShowMoreButtonExist =
    "Array.prototype.slice.call(document.querySelectorAll('span.artdeco-button__text')).filter(function (el) { return el.innerText.includes('Show more results') })"

//<div class="a11y-text" role="alert" data-test-loader-a11y="">Loading more results</div>
const val getLoadingMoreResults = "document.querySelector('[data-test-loader-a11y]')"

fun testOpenProfileTimeout(scannerProfile: ChromeProfile, notProcessedLinkedPostList: MutableList<LinkedPost>) {

    if (scannerProfile.isNeedPauseForOpenedProfile()) {

        val item: LinkedPost?

        if (shouldPerformAction(0.5)) {

            item = findLastByCondition(notProcessedLinkedPostList) { it.isCommentExist }

            if (item != null) {

                scrollToUrnLi(item.id)

                sleepWithRandom(6000, 3000)

                clickHowCommentOnUrnLi(item.id)

                sleepWithRandom(6000, 3000)
            }

        } else {

            item = findLastByCondition(notProcessedLinkedPostList) { it.isRepostExist }

            if (item != null) {

                scrollToUrnLi(item.id)

                sleepWithRandom(6000, 3000)

                clickHowRepostOnUrnLi(item.id)

                sleepWithRandom(12000, 3000)

                // click how repost dialog
                clickCloseDialog()

                sleepWithRandom(6000, 3000)
            }
        }

        if (item == null) {

            linkedAlertPingBot.sendMessage("No posts found for actions")

            linkedAlertPingBot.sendMessage("**Adding delay before opening profiles**")

            sleepWithRandom(18000, 3000)
        }
    }
}

fun testMemoryUsage() {
    sleep(3000)
    val htmlStr = sendScriptToJSShellCommandWithResult("(performance.memory.usedJSHeapSize / 1024 / 1024).toFixed()", "testMemoryUsage")
    Logger.logDebug("testMemoryUsage: $htmlStr")
    linkedAlertPingBot.sendMessage("Memory usage: ${htmlStr}MB")
    sleep(3000)
}

fun isJSConsoleReadyOnProfilePage(): Boolean {
    ClipboardManager.setClipboardContent("null")
    val htmlStr = sendScriptToJSShellCommandWithResult("document.querySelector('#root')", "isJSConsoleReady")
    return isCompleteRequest(htmlStr)
}

fun isPostListReady(): Boolean? {
    ClipboardManager.setClipboardContent("null")
    val htmlStr = sendScriptToJSShellCommandWithResult("document.querySelector('.artdeco-card.mb2')", "isLinkedPostListReady")
    if (!isCompleteRequest(htmlStr)) {
        return null
    }
    sleepWithRandom(6000, 4000)
    return htmlStr != "null"
}

fun isLoadingMoreResultsExist(): Boolean? {
    val htmlStr = sendScriptToJSShellCommandWithResult(getLoadingMoreResults, "isLoadingMoreResults")
    if (!isCompleteRequest(htmlStr)) {
        return null
    }
    return htmlStr != "null"
}

fun isShowMoreButtonExist(): Boolean? {
    val htmlStr2 = sendScriptToJSShellCommandWithResult(getShowMoreButtonExist, "isShowMoreButtonExist")
    if (!isCompleteRequest(htmlStr2)) {
        return null
    }
    val result = htmlStr2 != "[]"
    if (result) {
        // sometimes it gets stuck and doesn't load more automatically
        sendScriptToJSShellCommand(
            "$getShowMoreButtonExist[0].click();", "isShowMoreButtonExist | click more"
        )
        sleepWithRandom(9000, 3000)
    }
    return result;
}

const val getLayoutProfileCard = "document.querySelector('[componentkey^=\"com.linkedin.sdui.profile.card\"]')"
const val getLayoutProfileAbout = "document.querySelector('[data-view-name=\"profile-card-about\"]')"

fun isProfileReady(title: String): Boolean? {
    ClipboardManager.setClipboardContent("null")
    val htmlStr = sendScriptToJSShellCommandWithResult(getLayoutProfileCard, "isLinkedProfileReady | profile card")
    if (!isCompleteRequest(htmlStr)) {
        return null
    }
    sleepWithRandom(6000, 4000)

    Logger.logDebug("# isLinkedProfileReady title: $title")
    return true

    // some profiles have no About; other profile sections may need to be added here since they load slowly
    // but they're not used yet, so they're not added
//    if (htmlStr != "null") {
//        val htmlStr2 = sendScriptToJSShellCommandWithResult(getLayoutProfileAbout, "isLinkedProfileReady | profile about")
//        if (!isCompleteRequest(htmlStr2)) {
//            return null
//        }
//        sleepWithRandom(3000, 3000)
//        if (htmlStr2 != "null") {
//            Logger.logDebug("# isLinkedProfileReady title: $title")
//            return true
//        } else {
//            return false
//        }
//    } else {
//        return false;
//    }
}

fun isProfileBlocked(): Boolean? {
    ClipboardManager.setClipboardContent("null")
    var htmlStr =
        sendScriptToJSShellCommandWithResult("document.querySelector('#captcha-internal')", "isProfileBlocked | captcha container")
    if (!isCompleteRequest(htmlStr)) {
        return null
    }
    Logger.logDebug("# isProfileBlocked htmlString: $htmlStr")
    if (htmlStr == "null") {
        sleepWithRandom(2000, 100, true)
        htmlStr = sendScriptToJSShellCommandWithResult(
            "document.querySelector('.login__form_action_container')", "isProfileBlocked | login container"
        )
        if (!isCompleteRequest(htmlStr)) {
            return null
        }
        sleepWithRandom(2000, 100, true)
        if (htmlStr == "null") {
            sleepWithRandom(2000, 100, true)
            htmlStr = sendScriptToJSShellCommandWithResult(
                "document.querySelector('[data-test-id=\"home-hero-sign-in-cta\"]')", "isProfileBlocked | main page login container"
            )
            if (!isCompleteRequest(htmlStr)) {
                return null
            }
            return htmlStr != "null"
        } else {
            return true
        }
    } else {
        return true
    }
}

fun isFiltersBarActive(): Boolean? {
    val htmlStr = sendScriptToJSShellCommandWithResult(getFiltersBarContainer, "isFiltersBarActive")
    if (!isCompleteRequest(htmlStr)) {
        return null
    }
    return htmlStr != "null"
}

val filterListItemText = "Are these results helpful? Your feedback helps us improve search results"
val filterListItemText2 = "Subscribe"

fun getLinkedPostList(scannerId: String): List<LinkedPost>? {
    val htmlStr = sendScriptToJSShellCommandWithResult("document.querySelector('.scaffold-finite-scroll__content')", "getLinkedPostList")
    if (!isCompleteRequest(htmlStr)) {
        return null
    }
    val doc = Jsoup.parse(htmlStr)
    if (doc.select(".search-reusable-search-no-results").size > 0) {
        Logger.logInfo("No search results, scannerId: $scannerId")
        takeScreenshot(profileAnalyticName)
        return mutableListOf<LinkedPost>()
    }
    val articles = doc.select(".artdeco-card.mb2")
    val items = mutableListOf<LinkedPost>()
    articles.forEach { item ->
        if (item.select(".occludable-update-hint").size == 0
            && !item.text().contains(filterListItemText)
            && !item.text().contains(filterListItemText2)
        ) {
            val post = searchResultItemToLinkedPostItem(item, scannerId)
            items.add(post)
            if (!post.selfTest()) {
                Logger.logDebug("!!! ${item.text()} !!!")
            }
        }
    }
    return items
}

fun openLinkOnNewTab(url: String) {
    sendScriptToJSShellCommand(
        "window.open('$url', '_blank', 'noopener');", "openLinkOnNewTab"
    )
}

fun getLinkedProfile(link: String, title: String): LinkedProfile? {
    val docProfileCardText = sendScriptToJSShellCommandWithResult("$getLayoutProfileCard.outerText", "getLinkedProfile | getLinkedProfile.outerText")
    if (!isCompleteRequest(docProfileCardText)) {
        return null
    }

    val docProfileAboutStr = sendScriptToJSShellCommandWithResult(getLayoutProfileAbout, "getLinkedProfile | getLayoutProfileAbout")
    if (!isCompleteRequest(docProfileAboutStr)) {
        return null
    }
    val docProfileAbout = Jsoup.parse(docProfileAboutStr)

    return docToLinkedProfile(docProfileCardText, docProfileAbout, link, title)
}

fun getScannersUsageInfo(linkedPostDB: LinkedPostDB): String {
    var result = "active->\n"
    appConfig.linkedScannerProfiles.forEach { profileId ->
        val profile = findChromeProfileById(linkedPostDB, profileId)
        if (profile != null && !profile.isBlocked()) {
            result += "${profile.getProfileUsageInfo()}\n"
        }
    }
    result = "${result}\nblocked->\n"
    var isNotBlocked = true
    appConfig.linkedScannerProfiles.forEach { profileId ->
        val profile = findChromeProfileById(linkedPostDB, profileId)
        if (profile != null && profile.isBlocked()) {
            isNotBlocked = false
            result += "${profile.getProfileUsageInfo()}\n"
        }
    }
    if (isNotBlocked) {
        result += "empty\n"
    }
    return result
}

fun getScannersLimitInfo(linkedPostDB: LinkedPostDB): String {
    var result = "active->\n"
    appConfig.linkedScannerProfiles.forEach { profileId ->
        val profile = findChromeProfileById(linkedPostDB, profileId)
        if (profile != null && !profile.isBlocked()) {
            result += "${profile.getProfileLimitsInfo()}\n"
        }
    }
    result = "${result}\nblocked->\n"
    var isNotBlocked = true
    appConfig.linkedScannerProfiles.forEach { profileId ->
        val profile = findChromeProfileById(linkedPostDB, profileId)
        if (profile != null && profile.isBlocked()) {
            isNotBlocked = false
            result += "${profile.getProfileLimitsInfo()}\n"
        }
    }
    if (isNotBlocked) {
        result += "empty\n"
    }
    return result
}

fun isLimitReach(linkedPostDB: LinkedPostDB): Boolean {
    var limits = true
    appConfig.linkedScannerProfiles.forEach { profileId ->
        val profile = findChromeProfileById(linkedPostDB, profileId)
        if (profile?.isCanProcess() == true) {
            limits = false
        }
    }
    return limits
}

fun resetScannerOpenProfilesLimit(linkedPostDB: LinkedPostDB) {

    appConfig.linkedScannerProfiles.forEach { profileId ->
        val profile = findChromeProfileById(linkedPostDB, profileId)
        profile?.resetOpenProfileCount()
    }
}

fun findProfileForNextRun(
    linkedPostDB: LinkedPostDB,
    firebaseProfiles: MutableList<FirebaseProfile>,
    failed: List<ChromeProfile>,
    excluded: List<ChromeProfile>
): ChromeProfile? {

    val list = mutableListOf<ChromeProfile>()

    appConfig.linkedScannerProfiles.forEach { profileId ->
        val profile = findChromeProfileById(linkedPostDB, profileId)
        // variant #1 - can parse feeds -> { isCanProcess() + isCanBidBot() }
        if (profile?.isCanFeedProcess() == true) {
            // force opening "My Network", otherwise it happens with probability 0.7
            val firebaseProfile = findFirebaseProfileItemByAnalyticName(firebaseProfiles, getAnalyticNameByChromeProfile(profile))
            if (firebaseProfile != null && firebaseProfile.needCheck) {
                profile.needCheck = true
            }
            list.add(profile)
            // variant #2 - can process and the profile is marked for checking -> { isCanProcess() + needCheck == true }
        } else if (profile?.isCanProcess() == true) {
            val firebaseProfile = findFirebaseProfileItemByAnalyticName(firebaseProfiles, getAnalyticNameByChromeProfile(profile))
            if (firebaseProfile != null && firebaseProfile.needCheck) {
                profile.needCheck = true
                list.add(profile)
            }
        }
    }

    val list1 = excludeChromeProfiles(list, failed)

    val list2 = excludeChromeProfiles(list1, excluded)

    return list2.minWithOrNull(compareBy({ it.openedSessionCount.toInt() }, { it.openedProfileCount.toInt() }))
}