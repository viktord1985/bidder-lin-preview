package com.linked.model

import com.linked.bidder.utils.Logger
import org.simpleframework.xml.Element
import org.simpleframework.xml.Root
import java.time.LocalDateTime

@Root(name = "company", strict = false)
class LinkedCompany {
    @field:Element(name = "title", required = false)
    lateinit var title: String

    @field:Element(name = "description", required = false)
    lateinit var description: String

    @field:Element(name = "link", required = false)
    lateinit var link: String

    @field:Element(name = "created", required = false)
    lateinit var created: String

    @field:Element(name = "scannerId", required = false)
    lateinit var scannerId: String

    @field:Element(name = "location", required = false)
    lateinit var location: String

    fun getLinkedCompanyInfo(): String {
        return "title: $title || description: $description || link: $link || location: $location"
    }

    fun selfTest(): Boolean {
        if (title.isEmpty() || description.isEmpty() || link.isEmpty() || location.isEmpty()) {
            Logger.logDebug("!!! LinkedCompany selfTest() !!!")
            Logger.logDebug("!!! ${getLinkedCompanyInfo()} !!!")
            return false
        } else {
            return true
        }
    }


    fun setDateTime() {
        created = LocalDateTime.now().format(dateTimeFormatter)
    }

    fun getAlertText(): String {
        return "*Scanner:* $scannerId" +
                "\n*Company Title*: " + title +
                "\n*Company Description*: " + description +
                "\n*Company Location*: " + location +
                "\n[Link]($link)"
    }

}

fun searchResultItemToLinkedCompanyItem(element: org.jsoup.nodes.Element, scannerId: String): LinkedCompany {
    val linkedPostItem = LinkedCompany()
    linkedPostItem.title = element.select(".mb1").first()!!.select("div")[1]!!.text()
    linkedPostItem.title = sanitizeXmlString(linkedPostItem.title)
    linkedPostItem.description = element.select(".mb1").first()!!.select("div")[3].text().split("•")[0].trim()
    linkedPostItem.description = sanitizeXmlString(linkedPostItem.description)
    linkedPostItem.link = element.select("[data-test-app-aware-link]").first()?.attr("href") ?: ""
    linkedPostItem.scannerId = scannerId
    linkedPostItem.location = element.select(".mb1").first()!!.select("div")[3].text().split("•")[1].trim()
    linkedPostItem.location = sanitizeXmlString(linkedPostItem.location)
    linkedPostItem.setDateTime()
    return linkedPostItem
}