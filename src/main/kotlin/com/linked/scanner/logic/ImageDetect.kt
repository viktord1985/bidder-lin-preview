package com.linked.scanner.logic

import com.linked.bidder.appConfig
import com.linked.bidder.infoMessage
import com.linked.bidder.utils.Logger
import com.linked.utils.*
import javafx.stage.Screen
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc

fun initOpenCV() {

    //Warning: this only works for icons; need to check whether the font is system or hardcoded

    //Note: pulls in a lot of native libs and won't work without installing them system-wide
    //libopencv_java4110 - build for M1 (arm64)
    //libopencv_java4120 - build for x86_64

    System.load(System.getProperty("user.dir") + "/opencv/${appConfig.openCVdylib}");

    //self test
    if (isNotValidPoint(findTemplatePosition("ic_test"))) {
        Logger.logDebug("!!! initOpenCV self test Fail !!!")
    }
}

fun findTemplateCoordinates(imagePath: String, templatePath: String): Point {

    Logger.logInfo("imagePath: $imagePath")
    Logger.logInfo("templatePath: $templatePath")

    // Load the image and the template
    val image = Imgcodecs.imread(imagePath)
    val template = Imgcodecs.imread(templatePath)

    // Convert images to unsigned 8-bit (CV_8U)
    val grayImage = Mat()
    Imgproc.cvtColor(image, grayImage, Imgproc.COLOR_BGR2GRAY)

    val grayTemplate = Mat()
    Imgproc.cvtColor(template, grayTemplate, Imgproc.COLOR_BGR2GRAY)

    // Convert the image and the template to CV_8U
    grayImage.convertTo(grayImage, CvType.CV_8U)
    grayTemplate.convertTo(grayTemplate, CvType.CV_8U)

    // Apply template matching
    val result = Mat()
    Imgproc.matchTemplate(grayImage, grayTemplate, result, Imgproc.TM_CCOEFF_NORMED)

    if (result.empty()) {
        return NOT_VALID_POINT
    }

    // Find the location of the maximum value
    val minMaxLoc = Core.minMaxLoc(result)

    // Check the match score
    val maxValue = minMaxLoc.maxVal
    val confidenceThreshold = 0.9  // Tune depending on template quality

    if (maxValue < confidenceThreshold) {
        return NOT_VALID_POINT  // Weak match, ignore
    }

    // Top-left coordinates of the matched template
    val topLeft = minMaxLoc.maxLoc

    //println("Checkbox coordinates: x = ${topLeft.x}, y = ${topLeft.y}")

    return topLeft
}

enum class PositionMode { Center, Right }

// find center point (special mode = right)
fun findTemplatePosition(templateName: String, positionMode: PositionMode = PositionMode.Center): Point {
    infoMessage("findTemplatePos | $templateName")
    var imagePath = takeScreenshotForDetect()
    if (imagePath == "Failed") {
        return NOT_VALID_POINT
    }
    val templatePath = getTemplateImagePath(templateName)
    imagePath = System.getProperty("user.dir") + "/" + imagePath
    val topLeft = findTemplateCoordinates(imagePath, templatePath)
    if (!isValidPoint(topLeft)) {
        return NOT_VALID_POINT
    }
    if (positionMode == PositionMode.Right) {
        val template = Imgcodecs.imread(templatePath)
        val centerX = topLeft.x + template.width() - template.height() / 2
        val centerY = topLeft.y + template.height() / 2
        return Point(centerX, centerY)
    } else {
        val template = Imgcodecs.imread(templatePath)
        val centerX = topLeft.x + template.width() / 2
        val centerY = topLeft.y + template.height() / 2
        return Point(centerX, centerY)
    }
}

fun getTemplateImagePath(templateName: String): String {
    return System.getProperty("user.dir") + "/opencv/${appConfig.openCVConfFolder}/" + templateName + ".png"
}

fun clickTemplatePosition(templateName: String): Point {
    infoMessage("clickTemplatePos | $templateName")
    val templateCenter = findTemplatePosition(templateName)
    if (isValidPoint(templateCenter)) {
        val screenDPI = getScreenDPI()
        Logger.logInfo("screenDPI: $screenDPI")
        val clickPoint = Point(templateCenter.x.toInt() / screenDPI, templateCenter.y / screenDPI)
        sendClickConsoleCommand(clickPoint.x.toInt(), clickPoint.y.toInt())
        return clickPoint
    } else {
        return NOT_VALID_POINT
    }
}

fun getScreenDPI(): Double {
    val primaryScreen = Screen.getPrimary()
    //val bounds = primaryScreen.bounds // Logical resolution (in points)
    //println("Logical resolution: ${bounds.width}x${bounds.height}")
    return primaryScreen.outputScaleY
}

fun isValidPoint(point: Point): Boolean {
    return point.y != -1.0 && point.y != 1.0
}

fun isNotValidPoint(point: Point): Boolean {
    return !isValidPoint(point)
}

val NOT_VALID_POINT = Point(-1.0, -1.0)

fun clickFirstTemplatePosition(templateNames: List<String>): Point {
    templateNames.forEach {
        val result = clickTemplatePosition(it)
        if (isValidPoint(result)) {
            return result
        }
    }
    return NOT_VALID_POINT
}

fun findFirstTemplatePosition(templateNames: List<String>): Point {
    templateNames.forEach {
        val result = findTemplatePosition(it)
        if (isValidPoint(result)) {
            return result
        }
    }
    return NOT_VALID_POINT
}

fun isTextExistOnScreen(query: String, isLogger: Boolean = true): Boolean {
    Logger.logDebug("isTextExistOnImage: $query")
    if (isLogger) {
        infoMessage("isTextExistOnImage | ${addSpaceAfterEachCharWithTrailing(query)}")
    }
    val imagePath = takeScreenshotForOCR()
    if (imagePath == "Failed") {
        return false
    }
    val text = runShellCommand("tesseract $imagePath - -l eng --psm 11")
    return text.toLowerCase().contains(query.toLowerCase())
}

fun waitUntilTextExistOnScreen(query: String, isLogger: Boolean = true) {
    val maxAttempts = 7
    var attempts = 1

    while (isTextExistOnScreen(query, isLogger)) {
        sleep(3000)
        attempts++
        if (attempts > maxAttempts) {
            infoMessage("!!! get {$query} maxAttempts !!!")
            Logger.logDebug("!!! get {$query} maxAttempts !!!")
            break
        }
    }
}