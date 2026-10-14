package com.linked.scanner.logic

import com.linked.bidder.appConfig
import com.linked.bidder.utils.Logger
import com.linked.db.findLinkedJobById
import com.linked.model.LinkedJob
import com.linked.model.LinkedPostDB
import com.linked.utils.isArrayContainsStr

fun isLinkedPreviewJobFits(item: LinkedJob, linkedPostDB: LinkedPostDB): Boolean {
    // general
    if (item.id.isEmpty() || item.url.isEmpty() || !item.url.contains("https://")) {
        Logger.logInfo("TMP: Skip by /broken job item/")
        return false
    }
    if (findLinkedJobById(linkedPostDB, item.id) != null) {
        Logger.logInfo("TMP: Skip by /job is exist/")
        return false
    }
    // special
    if (!isArrayContainsStr(appConfig.includeWords, item.title)) {
        Logger.logInfo("TMP: Skip {title} by /includeWords/")
        return false
    }
    if (isArrayContainsStr(appConfig.excludeWords, item.title)
        || isArrayContainsStr(appConfig.excludeWords, item.company)
        || isArrayContainsStr(appConfig.excludeWords, item.location)
    ) {
        Logger.logInfo("TMP: Skip {title or company or location} by /excludeWords/")
        return false
    }
    return true
}

fun isLinkedJobFits(item: LinkedJob, linkedPostDB: LinkedPostDB): Boolean {
    // general
    if (item.id.isEmpty() || item.url.isEmpty() || !item.url.contains("https://")) {
        Logger.logInfo("TMP: Skip by /broken job item/")
        return false
    }
    if (findLinkedJobById(linkedPostDB, item.id) != null) {
        Logger.logInfo("TMP: Skip by /job is exist/")
        return false
    }
    // special
    if (isArrayContainsStr(appConfig.excludeWords, item.aboutJob)
        || isArrayContainsStr(appConfig.excludeWords, item.companyCategory)
        || isArrayContainsStr(appConfig.excludeWords, item.aboutCompany)
    ) {
        Logger.logInfo("TMP: Skip {aboutJob or companyCategory or aboutCompany} by /excludeWords/")
        return false
    }

    return true
}

