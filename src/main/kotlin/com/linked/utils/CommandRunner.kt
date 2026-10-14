package com.linked.utils

import com.linked.bidder.appConfig
import com.linked.bidder.dontCloseChromeForDebug
import com.linked.bidder.infoMessage
import com.linked.bidder.utils.Logger
import com.linked.chromedriver.profileAnalyticName
import com.linked.scanner.logic.isTextExistOnScreen
import java.io.BufferedReader
import java.io.InputStreamReader

fun runShellCommand(command: String): String {
    if (command.contains("keystroke \"v\"")) {
        Logger.logDebug("Shell: CMD+V")
    } else if (command.contains("keystroke \"c\"")) {
        Logger.logDebug("Shell: CMD+C")
    } else {
        Logger.logInfo("Shell: $command")
    }
    try {
        val processBuilder = ProcessBuilder()
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            processBuilder.command("cmd.exe", "/c", command)
        } else {
            processBuilder.command("sh", "-c", command)
        }

        val process = processBuilder.start()
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        val output = StringBuilder()

        var line: String?
        while (reader.readLine().also { line = it } != null) {
            output.append(line).append("\n")
        }

        val exitVal = process.waitFor()
        if (exitVal == 0) {
            return output.toString()
        } else {

            val process2 = processBuilder.start()
            val reader2 = BufferedReader(InputStreamReader(process2.errorStream))
            val output2 = StringBuilder()

            var line2: String?
            while (reader2.readLine().also { line2 = it } != null) {
                output2.append(line2).append("\n")
            }

            Logger.logError("Output Msg: $output")

            Logger.logError("Error Msg: $output2")

            Logger.logError("The command execution failed with exit code $exitVal")
            return "The command execution failed with exit code $exitVal"
        }
    } catch (e: Exception) {
        throw RuntimeException("Failed to run the command: $command", e)
    }
}


fun switchWifiNetworkInternal(SSID: String, password: String): Boolean {
    //Note: on new macOS

    // > ipconfig getsummary en0 | awk -F ' SSID : '  '/ SSID : / {print $2}'
    //Note: get eth info
    // > networksetup -listallhardwareports

    // Set Wifi
    runShellCommand("networksetup -setairportnetwork ${appConfig.wifiEth} $SSID $password")
    // Get Wifi
    val currentWifi = runShellCommand("networksetup -getairportnetwork ${appConfig.wifiEth}")
    if (appConfig.isWifiCheckSkip()) {
        return true
    }
    if (!currentWifi.contains("Current Wi-Fi Network:")) {
        Logger.logError("!!! switchWifiNetwork error: $currentWifi")
        return false
    }
    return currentWifi.split(":")[1].trim() == (SSID.replace("'", ""))
}

fun switchWifiNetwork(SSID: String, password: String): Boolean {
    infoMessage("switchWifiNetwork SSID:$SSID")
    if (switchWifiNetworkInternal(SSID, password)) {
        return true
    } else {
        sleep(6000)
        Logger.logError("!!! switchWifiNetwork next attempt !!!")
        return switchWifiNetworkInternal(SSID, password)
    }
}

fun delayUntilAnyOpen() {
    Logger.logInfo("Start delayUntilAnyOpen")
    var windowsCount = "0"
    do {
        windowsCount = runShellCommand("osascript -e 'tell application \"Google Chrome\" to get count of windows'").trim()
        Logger.logInfo("Active windows count: $windowsCount")
        runShellCommand("sleep 3")
    } while (windowsCount != "0")
}

fun openChromeCommand(profileDir: String, x: Int, y: Int, x2: Int, y2: Int, url: String): String {
    infoMessage("openChromeCommand profileDir:$profileDir")
    // Set ENG lang
    runShellCommand(System.getProperty("user.dir") + "/keyboardSwitcher select ABC")
    sleep(1000)

    //Note: for logs
    // --enable-logging --v=1
    // will be in this path -> ~/Library/Application Support/Google/Chrome/chrome_debug.log

    //Note: mem config
    // --js-flags="--max-old-space-size=4096" --max_old_space_size=8192

    // switch to profile
    runShellCommand("open -na 'Google Chrome' --args --profile-directory=$profileDir")
    sleep(6000)
    val command = "osascript " + "-e 'tell application \"Google Chrome\"'" + //
            //" -e 'make new window'" +
            " -e 'activate'" + //
            " -e 'set the bounds of the first window to {$x, $y, $x2, $y2}'" + //
            " -e 'delay 3'" + //
            " -e 'set URL of the active tab of the first window to \"$url\"'" + //
            " -e 'end tell'"
    return runShellCommand(command)
}

fun sendEventCommand(event: String): String {
    infoMessage("sendEventCommand $event")
    return runShellCommand("cliclick $event")
}

fun closeConsoleSendEscOpenConsole() {
    infoMessage("closeConsole sendEsc openConsole")
    sleep(1000)
    sendOpenJSConsoleCommand()
    sleep(1000)
    runShellCommand(
        "osascript " + //
                "-e 'tell application \"Google Chrome\" to activate' " + //
                "-e 'tell application \"System Events\" to key code 53' "
    )
    sleep(1000)
    sendOpenJSConsoleCommand()
    sleep(6000)
}

fun refocusJSConsole() {
    infoMessage("refocusJSConsole")
    sleep(1000)
    sendOpenJSConsoleCommand()
    sleep(1000)
    sendOpenJSConsoleCommand()
    sleep(6000)
}

fun sendOpenJSConsoleCommand(): String {
    infoMessage("sendOpenJSConsoleCommand")
    // cmd + alt + j
    return runShellCommand(
        "osascript " + //
                "-e 'tell application \"Google Chrome\" to activate' " + //
                "-e 'tell application \"System Events\" to keystroke \"j\" using {command down, option down}'"
    );
}

fun sendClickCommand(x: Int, y: Int): String {
    return sendEventCommand("c:$x,$y w:10n");
}

fun sendMoveCommand(x: Int, y: Int): String {
    return sendEventCommand("m:$x,$y w:10n");
}

fun sendDoubleClickCommand(x: Int, y: Int): String {
    return sendEventCommand("dc:$x,$y w:10n");
}

fun sendCloseChromeTabCommand(tag: String): String {
    infoMessage("sendCloseChromeTabCommand $tag")
    Logger.logInfo("sendCloseChromeTabCommand $tag")
    // cmd + w
    return runShellCommand(
        "osascript " + //
                "-e 'tell application \"Google Chrome\" to activate' " + //
                "-e 'tell application \"System Events\" to keystroke \"w\" using command down'"
    )
}

fun sendReloadPageCommand(): String {
    infoMessage("sendReloadPageCommand")
    // cmd + r
    return runShellCommand(
        "osascript " + //
                "-e 'tell application \"Google Chrome\" to activate' " + //
                "-e 'tell application \"System Events\" to keystroke \"r\" using command down'"
    )
}

fun sendCloseAllChromeTabCommand() {
    infoMessage("sendCloseAllChromeTabCommand")
    if (!dontCloseChromeForDebug) {
        runShellCommand("osascript -e 'tell application \"Google Chrome\" to close (tabs of windows)'")
    } else {
        Logger.logDebug("!!! Skip sendCloseAllChromeTabCommand for dontCloseChromeForDebug = true !!!")
    }
}

const val JS_RUN_MAX_ATTEMPTS = 9

fun isCompleteRequest(htmlString: String): Boolean {
    return htmlString != "NA"
}

fun sendScriptToJSShellCommandWithResult(script: String, description: String): String {
    val fullScript = "clear(); copy($script);"
    Logger.logInfo("js description: $description | js run: $fullScript")
    infoMessage(
        description.replace("clickButtonByTextLabel", "clkBtnByText")
            .replace("isButtonVisibleByTextLabelInModal", "isBtnVisibleInModal")
            .replace("clickSearchBarLabel", "clkSrchBarText")
    )
    ClipboardManager.setClipboardContent(fullScript)
    // cmd + v + enter
    //note: don't delete ' / n' from end of this command
    runShellCommand(
        "osascript " + //
                "-e 'tell application \"Google Chrome\" to activate' " + //
                "-e 'delay 0.5' " + //
                "-e 'tell application \"System Events\" to keystroke \"v\" using command down' " + //
                "-e 'delay 0.2' " + //
                "-e 'tell application \"System Events\" to key code 36' "
    )
    sleep(600)
    if (isTextExistOnScreen("copy(", false)) {
        infoMessage("!!! not found clear input field !!!")
        runShellCommand(
            "osascript " + //
                    "-e 'tell application \"Google Chrome\" to activate' " + //
                    "-e 'delay 0.5' " + //
                    "-e 'tell application \"System Events\" to key code 36' "
        )
    }
    var attempt = JS_RUN_MAX_ATTEMPTS
    while (attempt > 0) {
        val result = ClipboardManager.getClipboardContent()
        if (result != null && result != fullScript) {
            return result
        } else {
            infoMessage("!!! js run: next attempt: [ $attempt / $JS_RUN_MAX_ATTEMPTS ] !!!")
            Logger.logInfo("!!! js run: next attempt: [ $attempt / $JS_RUN_MAX_ATTEMPTS ] !!!")
            if (result == null) {
                Logger.logInfo("!!! result: null !!!")
            }
            sleep(6000)
            attempt--
        }
    }

    //Note: analyze screenshots for JS errors ( ??? e.g. a required class is missing while parsing ??? )
    takeScreenshot(profileAnalyticName)

    infoMessage("!!! js run fail !!!")
    Logger.logDebug("!!! js run fail !!!")
    return "NA"
}

fun sendScriptToJSShellCommand(script: String, description: String) {
    val fullScript = "clear(); $script"
    Logger.logInfo("js description: $description | js run: $fullScript")
    infoMessage(description)
    ClipboardManager.setClipboardContent(fullScript)
    // cmd + v + enter
    //note: don't delete ' / n' from end of this command
    runShellCommand(
        "osascript " + //
                "-e 'tell application \"Google Chrome\" to activate' " + //
                "-e 'delay 0.5' " + //
                "-e 'tell application \"System Events\" to keystroke \"v\" using command down' " + //
                "-e 'delay 0.2' " + //
                "-e 'tell application \"System Events\" to key code 36'\n"
    )
    sleep(600)
}

fun selectFileInOpenedChrome(filePath: String) {
    infoMessage("selectFileInOpenedChrome")
    ClipboardManager.setClipboardContent(filePath)
    runShellCommand(
        "osascript -e 'tell application \"Google Chrome\" to activate' " + //
                "-e 'delay 0.5' " + //
                "-e 'tell application \"System Events\" to keystroke \"g\" using {command down, shift down}' " + //
                "-e 'delay 0.5' " + //
                "-e 'tell application \"System Events\" to keystroke \"v\" using {command down}' " + //
                "-e 'delay 0.5' " + //
                "-e 'tell application \"System Events\" to key code 36' " + //
                "-e 'delay 0.5' " + //
                "-e 'tell application \"System Events\" to key code 36'"
    )
}

fun sendTextToActiveInput(text: String, description: String) {
    Logger.logInfo("description: $description | text: $text")
    infoMessage(description)
    ClipboardManager.setClipboardContent(text)
    // cmd + v + enter
    //note: don't delete ' / n' from end of this command
    runShellCommand(
        "osascript " + //
                "-e 'tell application \"Google Chrome\" to activate' " + //
                "-e 'delay 0.5' " + //
                "-e 'tell application \"System Events\" to keystroke \"v\" using command down' "
    )
    sleep(600)
}


