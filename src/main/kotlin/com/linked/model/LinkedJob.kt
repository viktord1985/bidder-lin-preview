package com.linked.model

import com.linked.model.command.navbarTextLog
import com.linked.model.command.navbarTextQuery
import org.simpleframework.xml.Element
import org.simpleframework.xml.Root

@Root(name = "job", strict = false)
class LinkedJob {
    @field:Element(name = "id", required = false)
    lateinit var id: String

    @field:Element(name = "url", required = false)
    lateinit var url: String

    @field:Element(name = "title", required = false)
    lateinit var title: String

    @field:Element(name = "company", required = false)
    lateinit var company: String

    @field:Element(name = "location", required = false)
    lateinit var location: String

    @field:Element(name = "aboutJob", required = false)
    lateinit var aboutJob: String

    @field:Element(name = "companyCategory", required = false)
    lateinit var companyCategory: String

    @field:Element(name = "aboutCompany", required = false)
    lateinit var aboutCompany: String

    @field:Element(name = "scannerId", required = false)
    lateinit var scannerId: String

    fun getAlertText(): String {
        return "*Scanner:* $scannerId" +
                "\n*Query*: " + navbarTextLog +
                "\n*Title:* $title" +
                "\n*Company Name*: " + company +
                "\n*Company Location*: " + location +
                "\n*Company category*: " + companyCategory +
                "\n[Link]($url)"
    }
}

fun searchResultItemToLinkedJobItemPreview(element: org.jsoup.nodes.Element, scannerId: String): LinkedJob {
    val item = LinkedJob()
    item.url = element.select("a").first()?.attr("href") ?: ""
    item.url = item.url.split("/?")[0]
    item.id = item.url.replace("/jobs/view/", "")
    if (!item.url.contains("https")) {
        item.url = "https://www.linkedin.com${item.url}"
    }
    item.title = element.select("a").first()?.text() ?: ""
    item.title = item.title.replace("with verification", "")
    item.title = sanitizeXmlString(item.title)
    item.company = element.select(".artdeco-entity-lockup__subtitle").first()?.text() ?: ""
    item.company = sanitizeXmlString(item.company)
    item.location = element.select(".artdeco-entity-lockup__caption").first()?.text() ?: ""
    item.location = sanitizeXmlString(item.location)
    item.aboutJob = ""
    item.companyCategory = ""
    item.aboutCompany = ""
    item.scannerId = scannerId
    return item
}

fun previewItemToLinkedJobItem(item: LinkedJob, element: org.jsoup.nodes.Element): LinkedJob {
    //todo - figure out how to keep formatting, line breaks are currently lost
    item.aboutJob = element.select(".jobs-description__content").first()?.text() ?: ""
    item.aboutJob = sanitizeXmlString(item.aboutJob)
    val jobsCompanyBoxDivs = element.select(".jobs-company__box").select("div")
    item.companyCategory = ""
    jobsCompanyBoxDivs.forEach {
        if (it.text().contains("on LinkedIn")) {
            // Information Services 11-50 employees 94 on LinkedIn
            item.companyCategory = it.text().split("employees").first().trim().substringBeforeLast(" ")
        }
    }
    item.aboutCompany = element.select(".jobs-company__company-description").next().text() ?: ""
    item.aboutCompany = sanitizeXmlString(item.aboutCompany)
    // there is also {Commitments} in {.jos-details-company__commitments-types-container}, but not for every company
    return item
}