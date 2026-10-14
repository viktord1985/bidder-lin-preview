package com.linked.db

import com.linked.api.getAnalyticNameByChromeProfile
import com.linked.bidder.appConfig
import com.linked.bidder.utils.Logger
import com.linked.model.*
import org.simpleframework.xml.core.Persister
import java.io.File
import java.time.LocalDate

fun loadLinkedPostDB(): LinkedPostDB {
    Logger.logInfo("Read local DB")
    val file = File(appConfig.exportFolderPath + appConfig.dbPath)
    val linkedPostDB: LinkedPostDB
    if (!file.exists()) {
        val newLinkedPostDB = LinkedPostDB()
        newLinkedPostDB.items = mutableListOf()
        newLinkedPostDB.companies = mutableListOf()
        newLinkedPostDB.profiles = mutableListOf()
        newLinkedPostDB.searchQueryIndex = "0"
        newLinkedPostDB.searchQueryIndexDate = dateFormatter.format(LocalDate.now())
        linkedPostDB = newLinkedPostDB
    } else {
        linkedPostDB = Persister().read(LinkedPostDB::class.java, file)
    }

    val firebaseProfiles = getFirebaseProfiles(appConfig.botID)

    appConfig.linkedScannerProfiles.forEach { profileId ->
        // add to the local DB
        if (findChromeProfileById(linkedPostDB, profileId) == null) {
            val chromeProfile = ChromeProfile()
            chromeProfile.id = profileId
            chromeProfile.openedSessionCount = "0"
            chromeProfile.contentPostCount = "0"
            chromeProfile.connectProfileCount = "0"
            chromeProfile.connectCompanyProfileCount = "0"
            chromeProfile.openedProfileCount = "0"
            chromeProfile.bidProfileBotCount = "0"
            chromeProfile.allBidProfileBotCount = "0"
            chromeProfile.inputConnectCount = "0"
            chromeProfile.allOpenedProfileCount = "0"
            chromeProfile.setLastActivityLocalDate(LocalDate.now())
            chromeProfile.type = "scanner"
            chromeProfile.status = "active"
            chromeProfile.connectedProfiles = "Empty"
            chromeProfile.usedPost = "Empty"
            chromeProfile.usedProfiles = "Empty"
            chromeProfile.bidTags = "Empty"
            chromeProfile.withdrawProfiles = "Empty"
            chromeProfile.setActivatedDate(LocalDate.now())
            chromeProfile.lastPostDateTime = "00:00 Jan 01, 2025"
            chromeProfile.daysFromLastActivity = "0"
            chromeProfile.profileViewersCount = "0"
            chromeProfile.postImpressionsCount = "0"
            chromeProfile.personalContent = "No"
            linkedPostDB.profiles.add(chromeProfile)
        }
        // add to Firebase
        addChromeProfileToFirebase(linkedPostDB, profileId, firebaseProfiles)
    }

    // migrations
    val allProfiles = mutableListOf<String>()
    appConfig.linkedScannerProfiles.forEach {
        allProfiles.add(it)
    }
    allProfiles.forEach { profileId ->
        val profile = findChromeProfileById(linkedPostDB, profileId)
        if (profile != null) {
            if (!profile.profileViewersCountInit()) {
                profile.profileViewersCount = "0"
            }
            if (!profile.postImpressionsCountInit()) {
                profile.postImpressionsCount = "0"
            }
            //todo - Firebase migration if needed
        }
    }

    return linkedPostDB
}

fun findChromeProfileById(linkedPostDB: LinkedPostDB, id: String): ChromeProfile? {
    linkedPostDB.profiles.forEach { item ->
        if (item.id == id) {
            return item
        }
    }
    return null
}

fun findLinkedJobById(linkedPostDB: LinkedPostDB, id: String): LinkedJob? {
    linkedPostDB.jobs.forEach { item ->
        if (item.id == id) {
            return item
        }
    }
    return null
}

fun addChromeProfileToFirebase(linkedPostDB: LinkedPostDB, profileId: String, firebaseProfiles: List<FirebaseProfile>) {
    val profileItem = findChromeProfileById(linkedPostDB, profileId)
    if (profileItem != null) {
        val profileItemAnalyticName = getAnalyticNameByChromeProfile(profileItem)
        if (findFirebaseProfileItemByAnalyticName(firebaseProfiles, profileItemAnalyticName) == null) {
            val firebaseProfileItem = FirebaseProfile(
                "",
                profileItemAnalyticName,
                appConfig.botID,
                false
            )
            createFirebaseProfile(firebaseProfileItem)
        }
    }
}

fun saveLinkedPostDB(linkedPostDB: LinkedPostDB) {
    Logger.logInfo("Save local DB")
    val file = File(appConfig.exportFolderPath + appConfig.dbPath)
    Persister().write(linkedPostDB, file)
}