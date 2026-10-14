package com.linked.scanner.logic

import com.linked.api.openAIRequest
import com.linked.bidder.appConfig
import com.linked.utils.extractStringsBetweenBrackets
import com.linked.utils.replaceMultipleNewLines
import com.linked.utils.trimNewLines

enum class CoverLetterType { botNote, botRefer }

fun formatCoverLetterWithAI(description: String, coverLetterType: CoverLetterType): String {

    // set a universal Note here just in case
    var coverLetter = appConfig.botNote
    var promts = extractStringsBetweenBrackets(appConfig.botNote)

    if (coverLetterType == CoverLetterType.botNote) {
        // if the bot can add a Note, it adds the universal one
        coverLetter = appConfig.botNote
        promts = extractStringsBetweenBrackets(appConfig.botNote)
    } else if (coverLetterType == CoverLetterType.botRefer) {
        // if the bot added the person, send a Referral regardless of whether a Note was added
        coverLetter = appConfig.botRefer
        promts = extractStringsBetweenBrackets(appConfig.botRefer)
    }

    for (promt in promts) {
        val result = openAIRequest(description, promt)
        coverLetter = coverLetter.replace(promt, result)
    }

    return trimNewLines(replaceMultipleNewLines(coverLetter.replace("[", "").replace("]", "")))
}
