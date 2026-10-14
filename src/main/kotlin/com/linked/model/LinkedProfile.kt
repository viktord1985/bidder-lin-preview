package com.linked.model

import com.linked.bidder.utils.Logger
import org.jsoup.nodes.Document

class LinkedProfile(
    val id: String,
    val url: String,
    val title: String,
    val description: String,
    val location: String,
    val about: String,
    val openToWork: Boolean,
    val followersConnectionsInfo: String,
) {
    fun getProfileInfo(): String {
        return "id: $id || title: $title || description: $description || location: $location || about: $about || openToWork: $openToWork || followers: $followersConnectionsInfo"
    }

    fun selfTest() {
        //note: {about, openToWork, followersConnectionsInfo} are not part of selfTest
        if (id.isEmpty() || url.isEmpty() || title.isEmpty() || description.isEmpty() || location.isEmpty()) {
            Logger.logDebug("!!! LinkedProfile selfTest() !!!")
            Logger.logDebug("!!! ${getProfileInfo()} !!!")
        }
    }
}

fun extractDescription(profile: String): String? {
    val startIndex = profile.indexOf("\n\n")
        .let { first ->
            if (first == -1) return null
            profile.indexOf("\n\n", first + 2)
        }

    if (startIndex == -1) return null

    val afterSecondBreak = profile.substring(startIndex + 2)

    // Looking for: \n\nANY TEXT\n\n·\n\nContact info
    val endRegex = Regex("\\n\\n.*?\\n\\n·\\n\\nContact info", RegexOption.DOT_MATCHES_ALL)
    val match = endRegex.find(afterSecondBreak)

    val description = if (match != null) {
        afterSecondBreak.substring(0, match.range.first)
    } else {
        afterSecondBreak
    }

    return description.trim().ifEmpty { null }
}

fun extractLocation(profile: String): String? {
    val endMarker = "\n\n·\n\nContact info"
    val endIndex = profile.indexOf(endMarker)
    if (endIndex == -1) return null

    val beforeEnd = profile.substring(0, endIndex)

    val lastDoubleBreak = beforeEnd.lastIndexOf("\n\n")
    if (lastDoubleBreak == -1) return null

    val location = beforeEnd.substring(lastDoubleBreak + 2)

    return location.trim().ifEmpty { null }
}

fun docToLinkedProfile(docProfileCardText: String, docProfileAbout: Document, link: String, title: String): LinkedProfile {

    //Ex1: getLinkedProfile
    //'Brian Mwithi\n\n· 2nd\n\nRemote Job Consultant  | Helping Professionals Transition To Fully Remote Roles FAST
    // | Resume Writing | ATS Compliant CV Writing |  LinkedIn Optimization | Interview Preparation | Job Application
    // Services\n\nThika, Kiambu, Kenya\n\n·\n\nContact info\n\nThe Graced Hands of Compassion Foundation\n\nThe
    // Technical University of Kenya\n\n56,184 followers\n\nSuzana is a mutual connection\n\nFollow\nMessage\nVisit my website'

    //Ex2: getLinkedProfile
    //"Gina Sapién\n\n· 2nd\n\n💥 Remote Career & Job Search Coach | Helping marketers in tech use personal branding + networking
    // to land remote roles in 12 weeks | 40+ clients across 10+ countries | Fractional Brand and Content Marketer\n\nMexico\n\n·\n\n
    // Contact info\n\nThe Remote Marketer\n\nLet's get you hired remotely! \n\n66,507 followers\n\n·\n\n500+\n\nconnections\n\nFollowed
    // by Suzana, Karthik Reddy and 2 others you know\n\nFollow\nMessage"

    //Ex3: getLinkedProfile
    //'Yuriy Polyakov\n\n· 3rd\n\nTechnical PM | IT Delivery Lead | APIs & High-Load Systems | Fintech & Enterprise\n\nCyprus\n\n·\n\n
    // Contact info\n\nACME-TECH\n\n42\n\nconnections\n\nMessage\nFollow\n\nOpen to work\n\nBusiness Analyst, Technical Project Manager
    // and Product Owner roles\n\nShow details'

    //Ex: getLayoutProfileAbout
    //"About\n\n💥The Career Rebrand Founding Cohort is open for enrollment ...

    val id = link.split("?miniProfileUrn")[0].replace("https://www.linkedin.com/", "")
    val url = link.split("?miniProfileUrn")[0]
    val description = extractDescription(docProfileCardText) ?: ""

    val location = extractLocation(docProfileCardText) ?: ""

    val docProfileAboutHTML = docProfileAbout.select("[data-testid=\"expandable-text-box\"]").html()

    var about = ""
    if (docProfileAboutHTML.contains("<button")) {
        // this is the case when there is a ..more button
        about = docProfileAboutHTML.split("<button")[0]
    } else {
        about = docProfileAboutHTML
    }

    val openToWork = docProfileCardText.contains("Open to work")

    var followers = "NA"
    var connections = "NA"

    if (docProfileCardText.contains("followers")) {
        followers = docProfileCardText.split("followers")[0].trim().split("\n\n").last()
    }

    if (docProfileCardText.contains("\n\nconnections")) {
        connections = docProfileCardText.split("\n\nconnections")[0].trim().split("\n\n").last()
    }

    return LinkedProfile(
        id,
        url,
        sanitizeXmlString(title),
        sanitizeXmlString(description),
        sanitizeXmlString(location),
        sanitizeXmlString(about),
        openToWork,
        "F:$followers C:$connections",
    )
}

fun sanitizeXmlString(input: String): String {
    val regex = "[^\\u0009\\u000A\\u000D\\u0020-\\uD7FF\\uE000-\\uFFFD\\u10000-\\u10FFFF]".toRegex()
    return input.replace(regex, "")
}