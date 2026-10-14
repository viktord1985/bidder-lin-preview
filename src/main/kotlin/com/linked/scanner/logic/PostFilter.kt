package com.linked.scanner.logic

import com.linked.bidder.appConfig
import com.linked.bidder.utils.Logger
import com.linked.model.LinkedPost
import com.linked.model.LinkedPostDB
import com.linked.utils.isArrayContainsStr

fun isLinkedPostFits(item: LinkedPost, linkedPostDB: LinkedPostDB): Boolean {
    if (item.link.isEmpty() || !item.link.contains("https://") || item.profileLink.isEmpty() || !item.profileLink.contains("https://")) {
        Logger.logInfo("TMP: Skip by /broken post item/")
        return false
    }
    if (isProfileLinkExistInLinkedPostDB(item.profileLink, linkedPostDB)) {
        Logger.logInfo("TMP: Skip by /profile url exist/")
        return false
    }
    if (item.link.contains("company") || item.link.contains("showcase") || item.link.contains("school")) {
        Logger.logInfo("TMP: Skip by /company, showcase, school/")
        return false
    }
    if (isArrayContainsStr(appConfig.excludeWords, item.title)
        || isArrayContainsStr(appConfig.excludeWords, item.description)
        || isArrayContainsStr(appConfig.excludeWords, item.text)
    ) {
        Logger.logInfo("TMP: Skip by /excludeWords/")
        return false
    }
    //Note: openToWork can't be taken from the feed, it's an image; parsing images is not an option right now
    //if (item.openToWork != null && item.openToWork!!) {
    //    Logger.logInfo("TMP: Skip by /openToWork/")
    //    return false
    //}
    return true
}

fun isProfileLinkExistInLinkedPostDB(profileLink: String, linkedPostDB: LinkedPostDB): Boolean {
    linkedPostDB.items.forEach { item ->
        if (item.profileLink == profileLink) {
            Logger.logDebug("TMP: isProfileLinkExistInLinkedPostDB profileLink: $profileLink")
            return true
        }
    }
    return false
}