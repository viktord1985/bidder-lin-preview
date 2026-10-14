package com.linked.model

import com.linked.model.command.getCommandClick
import com.linked.model.command.getCommandInput
import com.linked.model.command.getCommandScroll
import org.simpleframework.xml.Element
import org.simpleframework.xml.ElementList
import org.simpleframework.xml.Root
import java.time.format.DateTimeFormatter
import java.util.*

val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.ENGLISH)

val dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm MMM dd, yyyy", Locale.ENGLISH)

@Root(name = "AppConfig", strict = false)
class AppConfig {

    // exportFolderPath
    @field:Element(name = "exportFolderPath", required = false)
    lateinit var exportFolderPath: String

    // local DB config
    @field:Element(name = "dbPath", required = false)
    lateinit var dbPath: String

    // application log path
    @field:Element(name = "applicationLogPath", required = false)
    lateinit var applicationLogPath: String

    // Wifi config

    @field:Element(name = "wifiEth", required = false)
    lateinit var wifiEth: String

    @field:Element(name = "wifiCheckSkip", required = false)
    lateinit var wifiCheckSkip: String

    fun isWifiCheckSkip(): Boolean {
        return wifiCheckSkip.toLowerCase() == "true"
    }

    // Display config

    @field:Element(name = "displayConfigEnable", required = false)
    lateinit var displayConfigEnable: String

    fun isDisplayConfigEnable(): Boolean {
        return displayConfigEnable.toLowerCase() == "true"
    }

    @field:Element(name = "displayConfigApp", required = false)
    lateinit var displayConfigApp: String

    // Linked config

    @field:Element(name = "defaultNetwork", required = false)
    lateinit var defaultNetwork: String

    fun getDefaultNetworkSSID(): String {
        return defaultNetwork.split("|")[0]
    }

    fun getDefaultNetworkPassword(): String {
        return defaultNetwork.split("|")[1]
    }

    @field:Element(name = "startUrl", required = false)
    lateinit var startUrl: String

    @field:ElementList(entry = "urlStartPathActions", required = false)
    lateinit var urlStartPathActions: MutableList<String>

    @field:ElementList(entry = "urlJobsActions", required = false)
    lateinit var urlJobsActions: MutableList<String>

    @field:ElementList(entry = "urlFeedActions", required = false)
    lateinit var urlFeedActions: MutableList<String>

    @field:ElementList(entry = "urlEndPathActions", required = false)
    lateinit var urlEndPathActions: MutableList<String>

    fun getPathActions(pathActions: MutableList<String>): MutableList<Any> {
        val list = mutableListOf<Any>()
        pathActions.forEach { item ->
            val name = item.split("|")[0]
            if (name == "click") {
                list.add(getCommandClick(item))
            } else if (name == "scroll") {
                list.add(getCommandScroll(item))
            } else if (name == "input") {
                list.add(getCommandInput(item))
            }
        }
        return list
    }

    @field:ElementList(entry = "linkedScannerProfiles", required = false)
    lateinit var linkedScannerProfiles: MutableList<String>

    @field:ElementList(entry = "linkedProfileNames", required = false)
    lateinit var linkedProfileNames: MutableList<String>

    fun getProfileNameByChromeProfileName(chromeProfileNameToFind: String): String {
        linkedProfileNames.forEach { item ->
            val chromeProfileName = item.split("->")[0]
            val profileName = item.split("->")[1]
            if (chromeProfileName == chromeProfileNameToFind) {
                return profileName;
            }
        }
        return "NA"
    }

    @field:ElementList(entry = "linkedProfileDeviceId", required = false)
    lateinit var linkedProfileDeviceId: MutableList<String>

    fun getProfileNameByChromeProfileName(chromeProfileNameToFind: String, configId: String): String {
        var configIdIndex = 0;
        if (configId == "width_minus") {
            configIdIndex = 0
        } else if (configId == "height_minus") {
            configIdIndex = 1
        } else if (configId == "SSID_name") {
            configIdIndex = 2
        } else if (configId == "SSID_password") {
            configIdIndex = 3
        }
        linkedProfileDeviceId.forEach { item ->
            val chromeProfileName = item.split("->")[0]
            if (chromeProfileName == chromeProfileNameToFind) {
                return item.split("->")[1].toString().split("|")[configIdIndex];
            }
        }
        return "NA"
    }

    @field:Element(name = "scanTimeFrom", required = false)
    lateinit var scanTimeFrom: String

    fun getScanFromValue(): Int {
        return scanTimeFrom.split(":")[0].toInt() * 60 + scanTimeFrom.split(":")[1].toInt()
    }

    @field:Element(name = "scanTimeTo", required = false)
    lateinit var scanTimeTo: String

    fun getScanToValue(): Int {
        return scanTimeTo.split(":")[0].toInt() * 60 + scanTimeTo.split(":")[1].toInt()
    }

    @field:Element(name = "scanInterval", required = false)
    lateinit var scanInterval: String

    fun getScanIntervalValue(): Int {
        return scanInterval.toInt() * 60 * 1000
    }

    @field:Element(name = "scanIntervalDelta", required = false)
    lateinit var scanIntervalDelta: String

    fun getScanIntervalDeltaValue(): Int {
        return scanIntervalDelta.toInt() * 60 * 1000
    }

    // Telegram Alerts

    @field:Element(name = "enableTelegramAlerts", required = false)
    lateinit var enableTelegramAlerts: String

    @field:Element(name = "telegramChatId", required = false)
    lateinit var telegramChatId: String

    fun isEnableTelegramAlerts(): Boolean {
        return enableTelegramAlerts.toLowerCase() == "true"
    }

    @field:Element(name = "enablePingTelegramAlerts", required = false)
    lateinit var enablePingTelegramAlerts: String

    fun isEnablePingTelegramAlerts(): Boolean {
        return enablePingTelegramAlerts.toLowerCase() == "true"
    }

    @field:Element(name = "telegramPingChatId", required = false)
    lateinit var telegramPingChatId: String

    @field:Element(name = "botID", required = false)
    lateinit var botID: String

    // Filter Config - Start

    @field:ElementList(entry = "includeWords", required = false)
    lateinit var includeWords: MutableList<String>

    @field:ElementList(entry = "excludeWords", required = false)
    lateinit var excludeWords: MutableList<String>

    // Filter Config - End

    // Bidder Bot config - Start

    @field:Element(name = "skipBidderBot", required = false)
    lateinit var skipBidderBot: String

    fun isSkipBidderBot(): Boolean {
        return skipBidderBot.toLowerCase() == "true"
    }

    @field:Element(name = "botNote", required = false)
    lateinit var botNote: String

    @field:Element(name = "botRefer", required = false)
    lateinit var botRefer: String

    // Bidder Bot config - End

    @field:Element(name = "isRequestConnectDetectPrompt", required = false)
    lateinit var isRequestConnectDetectPrompt: String

    // OpenCV config

    @field:Element(name = "openCVdylib", required = false)
    lateinit var openCVdylib: String

    @field:Element(name = "openCVConfFolder", required = false)
    lateinit var openCVConfFolder: String
}


