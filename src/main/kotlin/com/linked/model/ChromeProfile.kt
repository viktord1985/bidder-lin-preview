package com.linked.model

import com.linked.bidder.appConfig
import com.linked.bidder.utils.Config
import com.linked.bidder.utils.Logger
import com.linked.scanner.logic.getMaxProfilesBotBidPerDayDynamic
import org.simpleframework.xml.Element
import org.simpleframework.xml.Root
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Root(name = "profile", strict = false)
class ChromeProfile {
    @field:Element(name = "id", required = false)
    lateinit var id: String

    // number of sessions
    @field:Element(name = "openedSessionCount", required = false)
    lateinit var openedSessionCount: String

    @field:Element(name = "contentPostCount", required = false)
    lateinit var contentPostCount: String

    @field:Element(name = "connectProfileCount", required = false)
    lateinit var connectProfileCount: String

    @field:Element(name = "connectCompanyProfileCount", required = false)
    lateinit var connectCompanyProfileCount: String

    //note: the DB will probably always store 0, because the reset happens on scanner exit and is then persisted
    @field:Element(name = "openedProfileCount", required = false)
    lateinit var openedProfileCount: String

    @field:Element(name = "bidProfileBotCount", required = false)
    lateinit var bidProfileBotCount: String

    @field:Element(name = "allBidProfileBotCount", required = false)
    lateinit var allBidProfileBotCount: String

    @field:Element(name = "inputConnectCount", required = false)
    lateinit var inputConnectCount: String

    @field:Element(name = "allOpenedProfileCount", required = false)
    lateinit var allOpenedProfileCount: String

    @field:Element(name = "lastActivityDate", required = false)
    lateinit var lastActivityDate: String

    @field:Element(name = "type", required = false)
    lateinit var type: String

    @field:Element(name = "status", required = false)
    lateinit var status: String

    // --- so we don't reopen a profile from the connected list again

    @field:Element(name = "connectedProfiles", required = false)
    lateinit var connectedProfiles: String

    fun isConnectedProfile(url: String): Boolean {
        return connectedProfiles.contains(url)
    }

    fun addConnectedProfile(url: String) {
        if (connectedProfiles == "Empty") {
            connectedProfiles = url
        } else {
            connectedProfiles = "$connectedProfiles|$url"
        }
    }

    // --- store used posts locally

    @field:Element(name = "usedPost", required = false)
    lateinit var usedPost: String

    fun isUsedPost(postName: String): Boolean {
        return usedPost.contains(postName)
    }

    fun addUsedPost(postName: String) {
        if (usedPost == "Empty") {
            usedPost = postName
        } else {
            usedPost = "$usedPost|$postName"
        }
    }

    // --- duplicate added outreach profiles locally

    @field:Element(name = "usedProfiles", required = false)
    lateinit var usedProfiles: String

    fun isUsedProfile(url: String): Boolean {
        return usedProfiles.contains(url)
    }

    fun addUsedProfile(url: String) {
        if (usedProfiles == "Empty") {
            usedProfiles = url
        } else {
            usedProfiles = "$usedProfiles|$url"
        }
    }

    // withdraw profiles

    @field:Element(name = "withdrawProfiles", required = false)
    lateinit var withdrawProfiles: String

    fun isWithdrawProfiles(url: String): Boolean {
        return withdrawProfiles.contains(url)
    }

    fun addWithdrawProfiles(url: String) {
        if (withdrawProfiles == "Empty") {
            withdrawProfiles = url
        } else {
            withdrawProfiles = "$withdrawProfiles|$url"
        }
    }

    // --- add a tag when bidding a profile / read the tag when checking for a new connection

    @field:Element(name = "bidTags", required = false)
    lateinit var bidTags: String

    fun getBidTag(url: String): String {
        if (bidTags == "Empty") {
            return ""
        }
        if (!bidTags.contains("|")) {
            val itemTag = bidTags.split("@")[0]
            val itemUrl = bidTags.split("@")[1]
            if (url.startsWith(itemUrl)) {
                return itemTag
            } else {
                return ""
            }
        }
        val tags = bidTags.split("|")
        tags.forEach { item ->
            val itemTag = item.split("@")[0]
            val itemUrl = item.split("@")[1]
            if (url.startsWith(itemUrl)) {
                return itemTag
            }
        }
        return ""
    }

    fun addBidTag(tag: String, url: String) {
        if (bidTags == "Empty") {
            bidTags = "$tag@$url"
        } else {
            bidTags = "$bidTags|$tag@$url"
        }
    }

    // --- defines when we started working with the profile; young profiles get reduced limits

    @field:Element(name = "activatedDate", required = false)
    lateinit var activatedDate: String

    fun setActivatedDate(localDate: LocalDate) {
        activatedDate = dateFormatter.format(localDate)
    }

    fun getActivatedDate(): LocalDate {
        return LocalDate.parse(activatedDate, dateFormatter)
    }

    fun isOldProfile(): Boolean {
        val currentDateVar = LocalDate.now()
        val activatedDateVar = getActivatedDate()

        val daysDifference = ChronoUnit.DAYS.between(activatedDateVar, currentDateVar).toInt()

        return daysDifference > Config.minOldProfileInterval
    }

    fun getWeeksFromActivatedDate(): Int {
        val currentDateVar = LocalDate.now()
        val activatedDateVar = getActivatedDate()

        return ChronoUnit.WEEKS.between(activatedDateVar, currentDateVar).toInt()
    }

    // --- defines the interval between posts

    @field:Element(name = "lastPostDateTime", required = false)
    lateinit var lastPostDateTime: String

    fun setLastPostDateTime(localDateTime: LocalDateTime) {
        lastPostDateTime = dateTimeFormatter.format(localDateTime)
    }

    fun getLastPostDateTime(): LocalDateTime {
        return LocalDateTime.parse(lastPostDateTime, dateTimeFormatter)
    }

    fun isCanMakePost(): Boolean {
        val currentDateVar = LocalDateTime.now()
        val activatedDateVar = getLastPostDateTime()

        val hoursDifference = ChronoUnit.HOURS.between(activatedDateVar, currentDateVar).toInt()

        return hoursDifference > Config.minPostInterval
    }

    // -- content publishing modes {No, Yes }

    @field:Element(name = "personalContent", required = false)
    lateinit var personalContent: String

    fun isNotPersonalContent(): Boolean {
        return personalContent == "No"
    }

    // -- for analytics

    @field:Element(name = "profileViewersCount", required = false)
    lateinit var profileViewersCount: String

    fun profileViewersCountInit(): Boolean {
        return this::profileViewersCount.isInitialized
    }

    @field:Element(name = "postImpressionsCount", required = false)
    lateinit var postImpressionsCount: String

    fun postImpressionsCountInit(): Boolean {
        return this::postImpressionsCount.isInitialized
    }

    // ---

    var needCheck: Boolean = false

    // ---

    fun isBlocked(): Boolean {
        return status == "blocked"
    }

    fun setBlocked() {
        status = "blocked"
    }

    fun incOpenedSessionCount() {
        val openedSessionCountInt = openedSessionCount.toInt()
        openedSessionCount = (openedSessionCountInt + 1).toString()
    }

    fun incContentPostCount() {
        val contentPostCountInt = contentPostCount.toInt()
        contentPostCount = (contentPostCountInt + 1).toString()
    }

    fun incConnectProfileCount() {
        val connectProfileCountInt = connectProfileCount.toInt()
        connectProfileCount = (connectProfileCountInt + 1).toString()
    }

    fun incConnectCompanyProfileCount() {
        val connectCompanyProfileCountInt = connectCompanyProfileCount.toInt()
        connectCompanyProfileCount = (connectCompanyProfileCountInt + 1).toString()
    }

    var openedProfileCountTimestamp = 0L

    fun incOpenedProfileCount() {
        val openedProfileCountInt = openedProfileCount.toInt()
        openedProfileCount = (openedProfileCountInt + 1).toString()
        val allOpenedProfileCountInt = allOpenedProfileCount.toInt()
        allOpenedProfileCount = (allOpenedProfileCountInt + 1).toString()

        openedProfileCountTimestamp = System.currentTimeMillis()
    }

    fun isNeedPauseForOpenedProfile(): Boolean {
        val pauseInSec = 63
        return (System.currentTimeMillis() - openedProfileCountTimestamp) < pauseInSec * 1000
    }

    fun incBidProfileBotCount() {
        val bidProfileBotCountInt = bidProfileBotCount.toInt()
        bidProfileBotCount = (bidProfileBotCountInt + 1).toString()
        val allBidProfileBotCountInt = allBidProfileBotCount.toInt()
        allBidProfileBotCount = (allBidProfileBotCountInt + 1).toString()
    }

    fun incInputConnectCount() {
        val inputConnectCountInt = inputConnectCount.toInt()
        inputConnectCount = (inputConnectCountInt + 1).toString()
    }

    fun setLastActivityLocalDate(localDate: LocalDate) {
        lastActivityDate = dateFormatter.format(localDate)
    }

    fun getLastActivityDate(): LocalDate {
        return LocalDate.parse(lastActivityDate, dateFormatter)
    }

    private fun resetLastActivityDateIfNeed() {
        val lastActivityDateToday = dateFormatter.format(LocalDate.now())
        if (lastActivityDate != lastActivityDateToday) {

            Logger.logInfo("resetLastActivityDateIfNeed {lastActivityDate != lastActivityDateToday}")

            val currentDateVar = LocalDate.now()
            val lastActivityDateVar = getLastActivityDate()
            val daysDifference = ChronoUnit.DAYS.between(lastActivityDateVar, currentDateVar).toInt()
            if (daysDifference > 1) {
                daysFromLastActivity = daysDifference.toString()
                Logger.logDebug(
                    "!!! daysDifference: $daysDifference, profile: ${
                        appConfig.getProfileNameByChromeProfileName(id).split(" ")[1]
                    } !!! "
                )
            } else {
                val maxProfilesBotBidPerDayVar = getMaxProfilesBotBidPerDayDynamic(
                    daysFromLastActivityInt(),
                    getWeeksFromActivatedDate(),
                    Config.maxProfilesBotBidPerDay
                )
                Logger.logDebug("allBidProfileBotCount: $allBidProfileBotCount, maxProfilesBotBidPerDay: $maxProfilesBotBidPerDayVar")
                Logger.logDebug("days: ${daysFromLastActivityInt()}, weeks: ${getWeeksFromActivatedDate()}")
                //todo - what if there were starts but no bids, e.g. bad internet connection
                if (allBidProfileBotCount.toInt() < maxProfilesBotBidPerDayVar) {
                    Logger.logDebug("!!! skip decDaysFromLastActivity !!!")
                } else {
                    Logger.logDebug("decDaysFromLastActivity")
                    decDaysFromLastActivity()
                }
            }

            lastActivityDate = lastActivityDateToday
            // per-run limit
            openedSessionCount = "0"
            // per-run and per-day limit
            openedProfileCount = "0"
            allOpenedProfileCount = "0"
            // per-run and per-day limit
            bidProfileBotCount = "0"
            allBidProfileBotCount = "0"
            // per-day limit
            contentPostCount = "0"
            // per-day limit
            connectProfileCount = "0"
            connectCompanyProfileCount = "0"
            // this is analytics
            inputConnectCount = "0"
            profileViewersCount = "0"
            postImpressionsCount = "0"
        }
    }

    // period we were paused for

    @field:Element(name = "daysFromLastActivity", required = false)
    lateinit var daysFromLastActivity: String

    fun daysFromLastActivityInt(): Int {
        return daysFromLastActivity.toInt()
    }

    fun decDaysFromLastActivity() {
        var daysFromLastActivityInt = daysFromLastActivityInt()
        if (daysFromLastActivityInt > 0) {
            daysFromLastActivityInt--
        }
        daysFromLastActivity = daysFromLastActivityInt.toString()
    }

    // ---

    fun isCanProcess(): Boolean {

        resetLastActivityDateIfNeed()

        if (isBlocked()) {
            return false
        }

        val maxOpenedProfilesPerRunVar = if (isOldProfile()) Config.maxOpenedProfilesPerRunOld else Config.maxOpenedProfilesPerRun

        val maxOpenedSessionsPerDayVar = if (isOldProfile()) Config.maxOpenedSessionsPerDayOld else Config.maxOpenedSessionsPerDay

        if (openedSessionCount.toInt() >= maxOpenedSessionsPerDayVar || openedProfileCount.toInt() >= maxOpenedProfilesPerRunVar) {
            return false
        }

        return true
    }

    fun isCanJobsProcess(): Boolean {

        // check the number of profiles opened per run and the number of sessions per day
        if (!isCanProcess()) {
            return false
        }

        return true
    }

    fun isCanFeedProcess(): Boolean {

        // check the number of profiles opened per run and the number of sessions per day
        if (!isCanProcess()) {
            return false
        }

        //todo - adjusted for the new {listener} logic instead of the {sequential bidder}
        // we don't just scan the feed, we need to connect, so continue if we can
        //if (!isCanBidBot()) {
        //    return false
        //}

        return true
    }

    // permission to publish content
    fun isCanPostContent(): Boolean {

        resetLastActivityDateIfNeed()

        if (isBlocked()) {
            return false
        }

        val maxContentPostPerDayVar = if (isOldProfile()) Config.maxContentPostPerDayOld else Config.maxContentPostPerDay

        if (contentPostCount.toInt() >= maxContentPostPerDayVar) {
            return false
        }

        return true
    }

    // permission for outreach
    fun isCanConnectProfile(): Boolean {

        resetLastActivityDateIfNeed()

        if (isBlocked()) {
            return false
        }

        val maxConnectProfilesPerDayVar = if (isOldProfile()) Config.maxConnectProfilesPerDayOld else Config.maxConnectProfilesPerDay

        if (connectProfileCount.toInt() >= maxConnectProfilesPerDayVar) {
            return false
        }

        // take the total number of bids into account
        if (!isCanBidBot()) {
            return false
        }

        return true
    }

    // total number of bids for the whole profile
    fun isCanBidBot(): Boolean {

        resetLastActivityDateIfNeed()

        if (appConfig.isSkipBidderBot()) {
            return false
        }

        if (isBlocked()) {
            return false
        }

        val maxProfilesBotBidPerDayVar = getMaxProfilesBotBidPerDayDynamic(
            daysFromLastActivityInt(),
            getWeeksFromActivatedDate(),
            Config.maxProfilesBotBidPerDay
        )

        val maxProfilesBotBidPerRunVar = getMaxProfilesBotBidPerDayDynamic(
            daysFromLastActivityInt(),
            getWeeksFromActivatedDate(),
            Config.maxProfilesBotBidPerRun
        )

        if (allBidProfileBotCount.toInt() >= maxProfilesBotBidPerDayVar || bidProfileBotCount.toInt() >= maxProfilesBotBidPerRunVar) {
            return false
        }

        return true
    }

    //note: the DB will probably always store 0, because the reset happens on scanner exit and is then persisted
    fun resetOpenProfileCount() {
        openedProfileCount = "0"
        bidProfileBotCount = "0"
    }

    fun getProfileUsageInfo(): String {
        isCanProcess()// call for update values
        return "*${
            appConfig.getProfileNameByChromeProfileName(id).split(" ")[0] + " " + appConfig.getProfileNameByChromeProfileName(id)
                .split(" ")[1]
        }* | old:${if (isOldProfile()) "+" else "-"} | need:${if (needCheck) "+" else "-"} | sess:${
            openedSessionCount.padStart(
                2,
                'x'
            )
        } | prof:${
            allOpenedProfileCount.padStart(
                2,
                'x'
            )
        } | canP:${if (isCanProcess()) "+" else "-"} " +
                "| canFP:${if (isCanFeedProcess()) "+" else "-"} " +
                "| canB:${if (isCanBidBot()) "+" else "-"} " +
                "| block:${if (isBlocked()) "+" else "-"} " +
                "| post:$contentPostCount " +
                "| bid:${
                    allBidProfileBotCount.padStart(
                        2,
                        ' '
                    )
                } | in:$inputConnectCount"
    }

    fun getProfileLimitsInfo(): String {
        isCanProcess()// call for update values
        return "*${
            appConfig.getProfileNameByChromeProfileName(id).split(" ")[0] + " " + appConfig.getProfileNameByChromeProfileName(id)
                .split(" ")[1]
        }* | pers:${if (isNotPersonalContent()) "-" else "+"} " +
                " | d: ${daysFromLastActivityInt().toString().padStart(3, 'x')}" +
                " | w: ${getWeeksFromActivatedDate().toString().padStart(2, 'x')}" +
                " | maxB: ${Config.maxProfilesBotBidPerDay.toString().padStart(2, 'x')}" +
                " / curB: ${
                    getMaxProfilesBotBidPerDayDynamic(
                        daysFromLastActivityInt(),
                        getWeeksFromActivatedDate(),
                        Config.maxProfilesBotBidPerDay
                    ).toString().padStart(2, 'x')
                } | maxBR: ${Config.maxProfilesBotBidPerRun.toString().padStart(2, 'x')}" +
                " / curBR: ${
                    getMaxProfilesBotBidPerDayDynamic(
                        daysFromLastActivityInt(),
                        getWeeksFromActivatedDate(),
                        Config.maxProfilesBotBidPerRun
                    ).toString().padStart(2, 'x')
                } | V: ${profileViewersCount.padStart(3, 'x')} / I: ${postImpressionsCount.padStart(4, 'x')}"
    }
}