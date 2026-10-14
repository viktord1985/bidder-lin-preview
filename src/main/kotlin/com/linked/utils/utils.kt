package com.linked.utils

import com.linked.bidder.dontCloseChromeForDebug
import com.linked.bidder.utils.Logger
import com.linked.model.ChromeProfile
import com.linked.model.LinkedPost
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.LocalTime
import java.util.*

fun isArrayContainsLinkedPost(array: List<LinkedPost>, linkedPost: LinkedPost): Boolean {
    array.forEach { item ->
        if (linkedPost.id == item.id || linkedPost.profileLink == item.profileLink) {
            return true
        }
    }
    return false
}

fun excludeChromeProfiles(list1: List<ChromeProfile>, list2: List<ChromeProfile>): List<ChromeProfile> {
    val idsToExclude = list2.map { it.id }.toSet()
    return list1.filterNot { it.id in idsToExclude }
}

//////////////////////////////////// Sleep //////////////////////////////////////////////////////

fun sleepWithRandom(delay: Int, delta: Int, withMouthEvent: Boolean = false) {
    var delayValue = delay
    if (delayValue == 0) {
        delayValue = 1
    }
    val delayWithRandom = delayValue.toLong() + Random().nextInt(if (delta > 0) delta else 1)
    if (withMouthEvent) {
        drawFigure8(delayWithRandom)
    } else {
        Thread.sleep(delayWithRandom)
    }
}

fun sleep(delay: Int, withMouthEvent: Boolean = false) {
    var delayValue = delay
    if (delayValue == 0) {
        delayValue = 1
    }
    if (withMouthEvent) {
        drawFigure8(delayValue.toLong())
    } else {
        Thread.sleep(delayValue.toLong())
    }
}

//////////////////////////////////// Utils //////////////////////////////////////////////////////

fun isArrayContainsStr(array: List<String>, str: String): Boolean {
    array.forEach { item ->
        if (str.toLowerCase().trim().contains(item.toLowerCase().trim())) {
            return true
        }
    }
    return false
}

fun replaceMultipleNewLines(text: String): String {
    return text.replace(Regex("\n{3,}"), "\n\n")
}

fun trimNewLines(input: String): String {
    return input.replace(Regex("^\\s*\n|\n\\s*$"), "")
}

fun extractStringsBetweenBrackets(text: String): List<String> {
    val pattern = "\\[(.*?)\\]".toRegex()
    return pattern.findAll(text).map { it.groupValues[1] }.toList()
}

fun getCurrentTimeValue(): Int {
    return LocalTime.now().hour * 60 + LocalTime.now().minute
}

fun shouldPerformAction(probability: Double): Boolean {
    return Random().nextDouble() < probability
}

fun removeRandomChar(input: String): String {
    if (input.isEmpty()) {
        return input
    }

    // Get a random index in the string
    val randomIndex = Random().nextInt(input.length)

    // Remove the character at the random index
    return input.removeRange(randomIndex, randomIndex + 1)
}

fun <T> getRandomItem(array: Array<T>): T? {
    return if (array.isNotEmpty()) array.random() else null
}

fun <T> findLastByCondition(array: MutableList<T>, predicate: (T) -> Boolean): T? {
    return array.lastOrNull(predicate)
}

fun printArray(list: List<String>, tag: String) {
    Logger.logDebug(
        "$tag\n${
            list.toString().replace("\t", "")
                .replace("[", "")
                .replace("]", "")
                .replace(",", "\n")
        }"
    )
}

fun skipUntilSecondDoubleNewline(input: String): String {
    val parts = input.split("\n\n", limit = 3)
    return if (parts.size >= 3) parts[2] else input
}

//////////////////////////////////// PING //////////////////////////////////////////////////////

fun ping(host: String = "8.8.8.8", count: Int = 4): List<Double> {
    val command = "ping -c $count $host"
    val process = ProcessBuilder(*command.split(" ").toTypedArray()).start()

    val reader = BufferedReader(InputStreamReader(process.inputStream))
    val responseTimes = mutableListOf<Double>()

    reader.useLines { lines ->
        lines.forEach { line ->
            val regex = "time=([0-9]+\\.?[0-9]*) ms".toRegex()
            val match = regex.find(line)
            match?.groups?.get(1)?.value?.toDoubleOrNull()?.let {
                responseTimes.add(it)
            }
        }
    }

    process.waitFor()
    return responseTimes
}

fun pingFormatted(times: List<Double>): String {

    if (times.isNotEmpty()) {
        val avgPing = times.average()
        //println("Ping results : $times")
        return "PING => Average time: ${"%.2f".format(avgPing)} ms\nRAW: $times"
    } else {
        return "!!! PING - Failed !!!"
    }
}

fun pingAverage(times: List<Double>): Double {

    if (times.isNotEmpty()) {
        return times.average()
    } else {
        return -1.0
    }
}

fun pingInfoWithDynamicDelay(isTestMode: Boolean = false) {

    val times = ping()
    Logger.logDebug(pingFormatted(times))
    val pingValue = pingAverage(times)
    if (pingValue > 60) {
        Logger.logDebug("!!! PING > 60ms, sleep 18sec !!!")
        if (!isTestMode) {
            sleep(18000)
        }
    }
}

//////////////////////////////////// Shell scripts //////////////////////////////////////////////////////

fun drawFigure8(millis: Long) {
    Logger.logDebug("drawFigure8")
    runShellCommand(System.getProperty("user.dir") + "/draw_figure8.sh $millis")
}

fun takeScreenshot(profileAnalyticName: String) {
    Logger.logDebug("takeScreenshot: $profileAnalyticName")
    runShellCommand(System.getProperty("user.dir") + "/take_screenshot.sh $profileAnalyticName")
}

fun startScreenRecording(profileAnalyticName: String) {
    Logger.logDebug("startScreenRecording: $profileAnalyticName")
    runShellCommand(System.getProperty("user.dir") + "/start_screen_recording.sh $profileAnalyticName")
}

fun stopScreenRecording() {
    Logger.logDebug("stopScreenRecording")
    runShellCommand(System.getProperty("user.dir") + "/stop_screen_recording.sh")
}

fun forceCloseChromeApp() {
    if (!dontCloseChromeForDebug) {
        runShellCommand(System.getProperty("user.dir") + "/close_chrome.sh")
    } else {
        Logger.logDebug("!!! Skip forceCloseChromeApp for dontCloseChromeForDebug = true !!!")
    }
}

//////////////////////////////////// OpenCV //////////////////////////////////////////////////////

fun sendClickConsoleCommand(x: Int, y: Int): String {
    return sendEventCommand("c:$x,$y w:10n");
}

fun takeScreenshotForDetect(): String {
    Logger.logDebug("takeScreenshotForDetect")
    return runShellCommand(System.getProperty("user.dir") + "/take_screenshot_detect.sh").replace("\n", "")
}

fun takeScreenshotForOCR(): String {
    Logger.logDebug("takeScreenshotForOCR")
    return runShellCommand(System.getProperty("user.dir") + "/take_screenshot_ocr.sh").replace("\n", "")
}

fun addSpaceAfterEachCharWithTrailing(input: String): String {
    return buildString {
        input.forEach {
            append(it)
            append(" ")
        }
    }
}