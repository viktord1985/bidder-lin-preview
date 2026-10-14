package com.linked.scanner

import com.linked.bidder.appConfig
import com.linked.bidder.linkedAlertBot
import com.linked.bidder.linkedAlertPingBot
import com.linked.bidder.utils.Config
import com.linked.bidder.utils.Logger
import com.linked.db.saveLinkedPostDB
import com.linked.model.*
import com.linked.scanner.logic.isLinkedJobFits
import com.linked.scanner.logic.isLinkedPreviewJobFits
import com.linked.utils.isCompleteRequest
import com.linked.utils.sendScriptToJSShellCommand
import com.linked.utils.sendScriptToJSShellCommandWithResult
import com.linked.utils.sleepWithRandom
import org.jsoup.Jsoup

const val jobList = "document.querySelector('.scaffold-layout__list')"

const val jobListItems = "document.querySelectorAll('.job-card-list')"

const val jobDetails = "document.querySelector('.jobs-details__main-content')"

const val jobPages = "document.querySelector('.jobs-search-pagination__pages')"

fun linkedJobsScanner(scannerProfile: ChromeProfile, linkedPostDB: LinkedPostDB): Boolean {

    Logger.logInfo("Start autoJobs")

    sleepWithRandom(12000, 3000)

    var page = 1

    linkedAlertPingBot.sendMessage("Open job page $page")

    val scannerProfileId = scannerProfile.id + " | " + appConfig.getProfileNameByChromeProfileName(scannerProfile.id)

    do {

        sleepWithRandom(9000, 6000)

        val htmlStr = sendScriptToJSShellCommandWithResult(jobList, "jobList")
        if (!isCompleteRequest(htmlStr)) {
            return false
        }

        val jobsPreview = getLinkedJobPreview(htmlStr, scannerProfileId)

        var index = 0

        jobsPreview.forEach {

            if (isLinkedPreviewJobFits(it, linkedPostDB)) {

                sleepWithRandom(9000, 6000)

                sendScriptToJSShellCommand("$jobListItems[$index].click()", "jobListItemClick")

                linkedAlertPingBot.sendMessage("job item ${index + 1} click")
                linkedAlertPingBot.sendMessage(".. ${it.title} / ${it.company} / ${it.location}")

                sleepWithRandom(12000, 6000)

                val htmlStr2 = sendScriptToJSShellCommandWithResult(jobDetails, "jobDetails")
                if (!isCompleteRequest(htmlStr2)) {
                    return false
                }

                //todo - use [show more] for the company if more company info is needed

                val item = previewItemToLinkedJobItem(it, Jsoup.parse(htmlStr2))

                if (isLinkedJobFits(item, linkedPostDB)) {
                    Logger.logInfo("Mach found: ${item.url}")
                    linkedAlertBot.sendMessage(item.getAlertText())
                }

                linkedPostDB.jobs.add(item)

                saveLinkedPostDB(linkedPostDB)

            }

            index++
        }

        page++

        if (page > Config.jobsMaxPage) {
            break
        }

        val htmlStr3 = sendScriptToJSShellCommandWithResult("$jobPages.outerText", "jobPagesText")
        if (!isCompleteRequest(htmlStr3)) {
            return false
        }

        if (!htmlStr3.contains(page.toString())) {
            break
        }

        sendScriptToJSShellCommand("$jobPages.querySelectorAll('button')[${page - 1}].click()", "jobPages click")

        linkedAlertPingBot.sendMessage("Open job page $page")

    } while (true)

    return true
}

fun getLinkedJobPreview(htmlStr: String, scannerId: String): List<LinkedJob> {

    val doc = Jsoup.parse(htmlStr)
    val jobs = doc.select(".job-card-list")
    val items = mutableListOf<LinkedJob>()
    jobs.forEach { item ->
        items.add(searchResultItemToLinkedJobItemPreview(item, scannerId))
    }
    return items
}