package com.linked.model

import com.linked.bidder.utils.Logger
import com.linked.model.command.navbarTextLog
import com.linked.model.command.navbarTextQuery
import org.simpleframework.xml.Element
import org.simpleframework.xml.Root
import java.time.LocalDateTime

@Root(name = "item", strict = false)
class LinkedPost {
    @field:Element(name = "id", required = false)
    lateinit var id: String

    @field:Element(name = "title", required = false)
    lateinit var title: String

    @field:Element(name = "description", required = false)
    lateinit var description: String

    @field:Element(name = "link", required = false)
    lateinit var link: String

    @field:Element(name = "profileLink", required = false)
    lateinit var profileLink: String

    @field:Element(name = "text", required = false)
    lateinit var text: String

    @field:Element(name = "mach", required = false)
    lateinit var mach: String

    @field:Element(name = "created", required = false)
    lateinit var created: String

    @field:Element(name = "scannerId", required = false)
    lateinit var scannerId: String

    @field:Element(name = "profileInfo", required = false)
    lateinit var profileInfo: String

    @field:Element(name = "location", required = false)
    lateinit var location: String

    // not stored in the DB for now, as it belongs to the profile rather than the post
    var followersConnectionsInfo: String = "NA"

    @field:Element(name = "updated", required = false)
    lateinit var updated: String

    var isCommentExist: Boolean = false

    var isRepostExist: Boolean = false

    //Note: openToWork can't be taken from the feed, it's an image; parsing images is not an option right now
    // example of an optional parameter - untested
    //@field:Element(name = "openToWork", required = false)
    //var openToWork: Boolean? = null

    fun getLinkedPostInfo(): String {
        return "id: $id || title: $title || description: $description || link: $link || profileLink: $profileLink || text: $text || updated: $updated"
    }

    //Note: {text} may be empty for a subscription post {https://www.linkedin.com/posts/john-awodola-172b11234_activity-7326384548538138624-B1T7?utm_source=share&utm_medium=member_desktop&rcm=ACoAABGCRrMBTWtnjB5UZiE4TQl5RbSfrb9EUK0}
    fun selfTest(): Boolean {
        if (id.isEmpty() || title.isEmpty() || description.isEmpty() || link.isEmpty() || profileLink.isEmpty() || text.isEmpty() || updated.isEmpty()) {
            Logger.logDebug("!!! LinkedPost selfTest() !!!")
            Logger.logDebug("!!! ${getLinkedPostInfo()} !!!")
            return false
        } else {
            return true
        }
    }

    fun setProfileLink() {
        var fullLink = link;
        if (fullLink.isNotEmpty() && fullLink.contains("?miniProfileUrn=")) {
            fullLink = fullLink.split("?miniProfileUrn=")[0]
        }
        profileLink = fullLink
    }

    fun setDateTime() {
        created = LocalDateTime.now().format(dateTimeFormatter)
    }

    fun setMach(value: Boolean) {
        if (value) {
            mach = "true"
        } else {
            mach = "false"
        }
    }

    fun isMach(): Boolean {
        return mach == "true"
    }

    fun getAlertText(): String {
        return "*Scanner:* $scannerId" +
                "\n*Query*: " + navbarTextLog +
                "\n*Post*: " + text.replace("hashtag", "") +
                "\n*Updated*: " + updated +
                "\n*Profile Title*: " + title +
                "\n*Profile Description*: " + description +
                "\n*Profile Location*: " + location +
                "\n*Profile Followers*: " + followersConnectionsInfo +
                "\n[Link]($profileLink)"
    }

    fun isOldest(): Boolean {
        return updated == "23h" || updated == "1d" || updated == "2d"
    }
}

fun suggestedItemToLinkedPostItem(element: org.jsoup.nodes.Element, scannerId: String): LinkedPost {
    val linkedPostItem = LinkedPost()
    linkedPostItem.id = "NA"
    linkedPostItem.title = sanitizeXmlString(element.select("span")[1]?.text() ?: "")
    linkedPostItem.description = "NA"
    if (element.select("span").size >= 6) {
        linkedPostItem.description = sanitizeXmlString(element.select("span")[5]?.text() ?: "")
    }
    linkedPostItem.link = element.select("[data-field=\"browsemap_card_click\"]").first()?.attr("href") ?: ""
    linkedPostItem.text = "Found in suggested list"
    linkedPostItem.scannerId = scannerId
    linkedPostItem.profileInfo = "NA"
    linkedPostItem.location = "NA"
    linkedPostItem.followersConnectionsInfo = "NA"
    linkedPostItem.updated = "NA"
    linkedPostItem.setProfileLink()
    linkedPostItem.setDateTime()
    return linkedPostItem
}

fun premiumItemToLinkedPostItem(element: org.jsoup.nodes.Element, scannerId: String): LinkedPost {
    val linkedPostItem = LinkedPost()
    linkedPostItem.id = "NA"
    linkedPostItem.title = sanitizeXmlString(element.select("span")[1]?.text() ?: "")
    linkedPostItem.description = "NA"
    if (element.select("span").size >= 6) {
        linkedPostItem.description = sanitizeXmlString(element.select("span")[5]?.text() ?: "")
    }
    linkedPostItem.link = element.attr("href") ?: ""
    linkedPostItem.text = "Found in premium list"
    linkedPostItem.scannerId = scannerId
    linkedPostItem.profileInfo = "NA"
    linkedPostItem.location = "NA"
    linkedPostItem.followersConnectionsInfo = "NA"
    linkedPostItem.updated = "NA"
    linkedPostItem.setProfileLink()
    linkedPostItem.setDateTime()
    return linkedPostItem
}

fun createPostItemForProfileConnect(title: String, description: String, link: String, scannerId: String): LinkedPost {
    val linkedPostItem = LinkedPost()
    linkedPostItem.id = "NA"
    linkedPostItem.title = title
    linkedPostItem.description = description
    linkedPostItem.link = link
    linkedPostItem.text = "postItem for profile connect"
    linkedPostItem.scannerId = scannerId
    linkedPostItem.profileInfo = "NA"
    linkedPostItem.location = "NA"
    linkedPostItem.followersConnectionsInfo = "NA"
    linkedPostItem.updated = "NA"
    linkedPostItem.setProfileLink()
    linkedPostItem.setDateTime()
    return linkedPostItem
}

fun searchResultItemToLinkedPostItem(element: org.jsoup.nodes.Element, scannerId: String): LinkedPost {
    val linkedPostItem = LinkedPost()
    linkedPostItem.id = element.select("[data-urn]").attr("data-urn").toString().replace("urn:li:activity:", "")
    if (element.getElementsByClass("update-components-actor__meta-link").size == 0) {
        Logger.logDebug("!!! update-components-actor__meta-link size = 0 !!!")
        linkedPostItem.link = ""
        linkedPostItem.title = ""
        linkedPostItem.description = ""
        linkedPostItem.text = ""
    }
    //note: if there is only one link, it's straightforward
    if (element.getElementsByClass("update-components-actor__meta-link").size == 1) {
        linkedPostItem.link = element.getElementsByClass("update-components-actor__meta-link").first()?.attr("href") ?: ""
        linkedPostItem.title =
            element.getElementsByClass("update-components-actor__title").first()?.select(".visually-hidden")?.text() ?: ""
        linkedPostItem.title = sanitizeXmlString(linkedPostItem.title.split("•")[0].trim())
        linkedPostItem.description =
            sanitizeXmlString(element.getElementsByClass("update-components-actor__description").first()?.text() ?: "")
        linkedPostItem.text = sanitizeXmlString(element.getElementsByClass("break-words").first()?.text() ?: "")
    }
    //note: if the company URL comes first and there are more URLs, the company reposted a personal post
    //note: if a person's URL comes first and there are more URLs, the person reposted a company post
    if (element.getElementsByClass("update-components-actor__meta-link").size == 2) {
        val firstLink = element.getElementsByClass("update-components-actor__meta-link").first()?.attr("href") ?: ""
        var firstTitle = element.getElementsByClass("update-components-actor__title").first()?.select(".visually-hidden")?.text() ?: ""
        firstTitle = sanitizeXmlString(firstTitle.split("•")[0].trim())
        val firstDescription = sanitizeXmlString(element.getElementsByClass("update-components-actor__description").first()?.text() ?: "")
        val firstText = sanitizeXmlString(element.getElementsByClass("break-words").first()?.text() ?: "")

        val secondLink = element.getElementsByClass("update-components-actor__meta-link")[1]?.attr("href") ?: ""
        var secondTitle = element.getElementsByClass("update-components-actor__title")[1]?.select(".visually-hidden")?.text() ?: ""
        secondTitle = sanitizeXmlString(secondTitle.split("•")[0].trim())
        var secondDescription = "NA"
        if (element.getElementsByClass("update-components-actor__description").size > 1) {
            secondDescription = sanitizeXmlString(element.getElementsByClass("update-components-actor__description").get(1)?.text() ?: "")
        }
        var secondText = "NA"
        if (element.getElementsByClass("break-words").size > 1) {
            secondText = sanitizeXmlString(element.getElementsByClass("break-words")[1]?.text() ?: "")
        }

        if (firstLink.contains("company") && !secondLink.contains("company")) {
            // company reposts a person's post
            Logger.logDebug("company reposts a person's post")
            linkedPostItem.link = secondLink
            linkedPostItem.title = "company title: $firstTitle; profile title: $secondTitle"
            linkedPostItem.description = "company description: $firstDescription; profile description: $secondDescription"
            linkedPostItem.text = "company text: $firstText; profile text: $secondText"
        } else if (!firstLink.contains("company") && secondLink.contains("company")) {
            // person reposts a company post
            Logger.logDebug("person reposts a company post")
            linkedPostItem.link = firstLink
            linkedPostItem.title = "profile title: $firstTitle; company title: $secondTitle"
            linkedPostItem.description = "profile description: $firstDescription; company description: $secondDescription"
            linkedPostItem.text = "profile text: $firstText; company text: $secondText"
        } else {
            linkedPostItem.link = "NA"
            linkedPostItem.title = "NA"
            linkedPostItem.description = "NA"
            linkedPostItem.text = "NA"
        }
    }
    linkedPostItem.scannerId = scannerId
    linkedPostItem.profileInfo = "NA"
    linkedPostItem.location = "NA"
    linkedPostItem.followersConnectionsInfo = "NA"
    linkedPostItem.updated = element.getElementsByClass("update-components-actor__sub-description").first()?.text() ?: ""
    if (linkedPostItem.updated.contains(" ") && linkedPostItem.updated.length > 3) {
        linkedPostItem.updated = linkedPostItem.updated.split(" ")[0].trim()
    }
    linkedPostItem.setProfileLink()
    linkedPostItem.setDateTime()

    val reactionDetails = element.select("[data-non-reaction-details]").text() ?: ""

    linkedPostItem.isCommentExist = reactionDetails.toLowerCase().contains("comment")

    linkedPostItem.isRepostExist = reactionDetails.toLowerCase().contains("repost")

    // todo - someday
    //  click on the post image
    //  click on the link under the profile (View my services)

    return linkedPostItem
}