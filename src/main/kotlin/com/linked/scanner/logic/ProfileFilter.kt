package com.linked.scanner.logic

import com.linked.bidder.appConfig
import com.linked.bidder.utils.Logger
import com.linked.model.LinkedProfile
import com.linked.utils.isArrayContainsStr

fun isLinkedProfileFits(item: LinkedProfile): Boolean {
    if (item.location.isEmpty()) {
        Logger.logInfo("TMP: Skip by /broken profile item/")
        return false
    }
    if (isArrayContainsStr(appConfig.excludeWords, item.title)
        || isArrayContainsStr(appConfig.excludeWords, item.description)
        || isArrayContainsStr(appConfig.excludeWords, item.location)
        || isArrayContainsStr(appConfig.excludeWords, item.about)
    ) {
        Logger.logInfo("TMP: Skip by /excludeWords/")
        return false
    }
    // added to distinguish between an employer and a client
    if (item.openToWork) {
        Logger.logInfo("TMP: Skip by /openToWork/")
        return false
    }
    return true
}