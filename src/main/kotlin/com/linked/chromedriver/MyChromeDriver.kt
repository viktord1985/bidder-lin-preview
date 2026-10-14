package com.linked.chromedriver

import com.linked.api.DetectAPI
import com.linked.api.initProfileAnalyticName
import com.linked.bidder.appConfig
import com.linked.bidder.fastBootForDebug
import com.linked.bidder.linkedAlertPingBot
import com.linked.bidder.utils.Config
import com.linked.bidder.utils.Logger
import com.linked.model.ChromeProfile
import com.linked.model.LinkedPost
import com.linked.model.LinkedPostDB
import com.linked.model.command.CommandClick
import com.linked.model.command.CommandInput
import com.linked.model.command.CommandScroll
import com.linked.model.command.navbarTextLog
import com.linked.scanner.isProfileBlocked
import com.linked.scanner.linkedJobsScanner
import com.linked.utils.*
import javafx.stage.Screen
import java.util.*

var profileAnalyticName = "na"

class MyChromeDriver(
    private val profile: ChromeProfile,
    private val linkedPostDB: LinkedPostDB,
    private var url: String,
    private val autoActions: Boolean = true,
    private val isCanJobsProcess: Boolean = true,
    private val isCanFeedProcess: Boolean = true,
    private val isUrlStartPath: Boolean = true,
    private val isUrlEndPath: Boolean = true,
) {
    private var thread: Thread? = null

    fun start(onFeedCommands: () -> List<LinkedPost>?, onFeedCommandsEnd: () -> Unit, onSuccess: (List<LinkedPost>?) -> Unit) {

        thread = Thread(Runnable {

            initProfileAnalyticName(profile)

            stopScreenRecording() // just in case, since it may be left hanging in the background
            startScreenRecording(profileAnalyticName)

            Logger.logInfo("#MyChromeDriver Started")

            Logger.logInfo("Start switch to work WIFI")

            val SSID_name = appConfig.getProfileNameByChromeProfileName(profile.id, "SSID_name")
            val SSID_password = appConfig.getProfileNameByChromeProfileName(profile.id, "SSID_password")

            val SSID_name_def = appConfig.getDefaultNetworkSSID()
            val SSID_password_def = appConfig.getDefaultNetworkPassword()

            if (switchWifiNetwork(SSID_name, SSID_password)) {
                Logger.logInfo("Switched to WIFI: $SSID_name")
            } else {
                Logger.logDebug("!!! switchWifiNetwork fail !!!")
                stopScreenRecording()
                onSuccess(null)
                return@Runnable
            }

            DetectAPI().start(SSID_name)

            if (url.contains("|")) {
                url = url.replace("\n", "").split("|").random()
            }

            //open Chrome app
            val height = Screen.getPrimary().bounds.height
            val width = Screen.getPrimary().bounds.width
            openChromeCommand(
                profile.id,
                (width * Config.leftSideSize).toInt() + 4,
                0,
                width.toInt() - appConfig.getProfileNameByChromeProfileName(profile.id, "width_minus").toInt(),
                height.toInt() - appConfig.getProfileNameByChromeProfileName(profile.id, "height_minus").toInt(),
                url
            )

            //set focus or disable lock screen
            Logger.logInfo("set focus or disable lock screen")
            sleepWithRandom(4000, 1000)
            sendClickCommand((width * Config.leftSideSize).toInt() + firstPageOffset, 50)

            //set focus, repeat previous event
            Logger.logInfo("set focus, repeat previous event")
            sleepWithRandom(4000, 1000)
            sendClickCommand((width * Config.leftSideSize).toInt() + firstPageOffset, 50)

            if (!fastBootForDebug) {
                //emulate mouse event on webpage
                Logger.logInfo("emulate mouse event on webpage")
                for (i in 0..(0 + Random().nextInt(4))) {
                    sleepWithRandom(3000, 3000)
                    sendClickCommand((width * Config.leftSideSize).toInt() + Random().nextInt(44) + 10, 175)
                }
            } else {
                Logger.logDebug("!!! skip 'emulate mouse event on webpage' for fast boot !!!")
            }

            //set focus, repeat previous event
            Logger.logInfo("set focus, repeat previous event")
            sleepWithRandom(4000, 1000)
            sendClickCommand((width * Config.leftSideSize).toInt() + firstPageOffset, 50)

            //open console
            Logger.logInfo("open console")
            sleepWithRandom(4000, 1000)
            sendOpenJSConsoleCommand()

            //delay for loading js console
            sleepWithRandom(7500, 2000)

            // start -> check Profile block before run

            profile.incOpenedSessionCount()

            val profileBlocked = isProfileBlocked()
            if (profileBlocked == null) {
                linkedAlertPingBot.sendMessage("*Start fail*")
                Logger.logDebug("!!! Start fail !!!")

                forceCloseAllTabsAndSwitchWifiBack(SSID_name_def, SSID_password_def)

                stopScreenRecording()

                onSuccess(null)
                return@Runnable
            }

            if (profileBlocked) {
                linkedAlertPingBot.sendMessage("*Profile Blocked*")
                Logger.logDebug("!!! Profile Blocked !!!")

                profile.setBlocked()

                forceCloseAllTabsAndSwitchWifiBack(SSID_name_def, SSID_password_def)

                stopScreenRecording()

                onSuccess(null)
                return@Runnable
            }

            // end -> check Profile block before run

            linkedAlertPingBot.sendMessage("Start path run")

            val forceIdsClick = mutableListOf<String>()
            if (profile.needCheck) {
                forceIdsClick.add("My Network")
                forceIdsClick.add("Messaging")
            }

            if (isUrlStartPath) {
                if (!processPathActions(
                        linkedPostDB,
                        appConfig.urlStartPathActions,
                        SSID_name_def,
                        SSID_password_def,
                        "Start",
                        forceIdsClick
                    )
                ) {
                    onSuccess(null)
                    return@Runnable
                }
            } else {
                Logger.logDebug("!!! Skip urlStartPathActions on isUrlStartPath = false !!!")
            }

            //Note: the Jobs parser lives in actions, kept here as a template
            //val jobsList: List<LinkedJob>?

            if (isCanJobsProcess && profile.isCanJobsProcess()) {

                linkedAlertPingBot.sendMessage("Jobs path run")

                if (!processPathActions(linkedPostDB, appConfig.urlJobsActions, SSID_name_def, SSID_password_def, "Jobs")) {
                    onSuccess(null)
                    return@Runnable
                }

                //Note: the Jobs parser lives in actions, kept here as a template
                //jobsList = onCommands()
                //
                //onCommandsEnd()

                if (!linkedJobsScanner(profile, linkedPostDB)) {
                    Logger.logDebug("!!!onCommands fail !!!")
                    pingInfoWithDynamicDelay(true)
                    linkedAlertPingBot.sendMessage("*Commands run fail*")

                    forceCloseAllTabsAndSwitchWifiBack(SSID_name_def, SSID_password_def)

                    stopScreenRecording()

                    onSuccess(null)
                    return@Runnable
                }
            } else {
                //Note: the Jobs parser lives in actions, kept here as a template
                //jobsList = emptyList()

                linkedAlertPingBot.sendMessage("Jobs path run *Executed actions only*")
            }

            val postList: List<LinkedPost>?

            if (profile.isCanFeedProcess()) {

                linkedAlertPingBot.sendMessage("Feed path run")

                if (isCanFeedProcess) {
                    if (!processPathActions(linkedPostDB, appConfig.urlFeedActions, SSID_name_def, SSID_password_def, "Feed")) {
                        onSuccess(null)
                        return@Runnable
                    }
                } else {
                    Logger.logDebug("!!! Skip urlFeedActions on isCanFeedProcess = false !!!")
                }

                postList = onFeedCommands()

                onFeedCommandsEnd()

                if (postList == null) {
                    Logger.logDebug("!!!onCommands fail !!!")
                    pingInfoWithDynamicDelay(true)
                    linkedAlertPingBot.sendMessage("*Commands run fail*")

                    forceCloseAllTabsAndSwitchWifiBack(SSID_name_def, SSID_password_def)

                    stopScreenRecording()

                    onSuccess(null)
                    return@Runnable
                }
            } else {
                postList = emptyList()

                linkedAlertPingBot.sendMessage("Feed path run *Executed actions only*")
            }

            linkedAlertPingBot.sendMessage("End path run")

            if (isUrlEndPath) {
                if (!processPathActions(linkedPostDB, appConfig.urlEndPathActions, SSID_name_def, SSID_password_def, "End")) {
                    onSuccess(null)
                    return@Runnable
                }
            } else {
                Logger.logDebug("!!! Skip urlEndPathActions on isUrlEndPath = false !!!")
            }

            //set focus on first page
            Logger.logInfo("set focus on first pag")
            sleepWithRandom(4000, 1000)
            sendClickCommand((width * Config.leftSideSize).toInt() + firstPageOffset, 50)

            //close window
            Logger.logInfo("close window")
            sleepWithRandom(4000, 1000)
            sendCloseChromeTabCommand("MyChromeDriver")

            //force close all tabs
            Logger.logInfo("force close all tabs")
            sleepWithRandom(2000, 1000)
            sendCloseAllChromeTabCommand()
            forceCloseChromeApp()

            if (switchWifiNetwork(SSID_name_def, SSID_password_def)) {
                Logger.logInfo("Switched back to WIFI: $SSID_name_def");
            } else {
                Logger.logDebug("!!! switchWifiNetwork fail !!!")
                stopScreenRecording()
                onSuccess(null)
                return@Runnable
            }

            DetectAPI().start(SSID_name_def)

            Logger.logInfo("#MyChromeDriver Ended");

            stopScreenRecording()
            onSuccess(postList)
        })
        thread!!.start()
    }

    private fun processPathActions(
        linkedPostDB: LinkedPostDB,
        pathAction: MutableList<String>,
        SSID_name_def: String,
        SSID_password_def: String,
        tag: String,
        forceIdsClick: List<String> = emptyList()
    ): Boolean {

        navbarTextLog = ""

        appConfig.getPathActions(pathAction).forEach { item ->

            if (item is CommandClick) {

                if (forceIdsClick.contains(item.id)) {
                    item.probability = "1.0"
                }

                if (!item.run(profile, linkedPostDB, autoActions)) {
                    linkedAlertPingBot.sendMessage("*$tag path run fail*")
                    Logger.logDebug("!!! item.run fail !!!")
                    pingInfoWithDynamicDelay(true)

                    forceCloseAllTabsAndSwitchWifiBack(SSID_name_def, SSID_password_def)

                    stopScreenRecording()

                    return false
                }

            } else if (item is CommandScroll) {

                item.run()

            } else if (item is CommandInput) {

                if (!item.run(linkedPostDB)) {
                    linkedAlertPingBot.sendMessage("*$tag path run fail*")
                    Logger.logDebug("!!! item.run fail !!!")
                    pingInfoWithDynamicDelay(true)

                    forceCloseAllTabsAndSwitchWifiBack(SSID_name_def, SSID_password_def)

                    stopScreenRecording()

                    return false
                }

            } else {
                Logger.logError("!!! Unknown command type !!!")
            }
        }

        return true
    }

    private fun forceCloseAllTabsAndSwitchWifiBack(SSID_name_def: String, SSID_password_def: String) {

        //force close all tabs
        Logger.logInfo("force close all tabs")
        sleepWithRandom(2000, 1000)
        sendCloseAllChromeTabCommand()
        forceCloseChromeApp()

        if (switchWifiNetwork(SSID_name_def, SSID_password_def)) {
            Logger.logInfo("Switched back to WIFI: $SSID_name_def");
        } else {
            Logger.logDebug("!!! switchWifiNetwork fail !!!")
        }
    }

    fun stop() {
        if (thread != null) {
            thread!!.stop()
        }
    }
}

const val firstPageOffset = 200
const val secondPageOffset = 220