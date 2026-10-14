package com.linked.chromedriver

import com.linked.api.DetectAPI
import com.linked.bidder.appConfig
import com.linked.bidder.utils.Config
import com.linked.bidder.utils.Logger
import com.linked.utils.delayUntilAnyOpen
import com.linked.utils.openChromeCommand
import com.linked.utils.switchWifiNetwork
import javafx.stage.Screen

class MyChromeRunner(private val profile: String, private val url: String) {
    private var thread: Thread? = null

    fun start(onSuccess: (Boolean) -> Unit) {

        thread = Thread(Runnable {

            Logger.logInfo("#MyChromeRunner Started")

            Logger.logInfo("Start switch to work WIFI")

            val SSID_name = appConfig.getProfileNameByChromeProfileName(profile, "SSID_name")
            val SSID_password = appConfig.getProfileNameByChromeProfileName(profile, "SSID_password")

            val SSID_name_def = appConfig.getDefaultNetworkSSID()
            val SSID_password_def = appConfig.getDefaultNetworkPassword()

            if (switchWifiNetwork(SSID_name, SSID_password)) {
                Logger.logInfo("Switched to WIFI: $SSID_name")
            } else {
                if (switchWifiNetwork(SSID_name_def, SSID_password_def)) {
                    Logger.logInfo("Switched back to WIFI: $SSID_name_def")
                }
                onSuccess(false)
                return@Runnable
            }

            DetectAPI().start(SSID_name)

            //open Chrome app
            val height = Screen.getPrimary().bounds.height
            val width = Screen.getPrimary().bounds.width
            openChromeCommand(
                profile,
                (width * Config.leftSideSize).toInt() + 4,
                0,
                width.toInt() - appConfig.getProfileNameByChromeProfileName(profile, "width_minus").toInt(),
                height.toInt() - appConfig.getProfileNameByChromeProfileName(profile, "height_minus").toInt(),
                url
            )

            delayUntilAnyOpen()

            if (switchWifiNetwork(SSID_name_def, SSID_password_def)) {
                Logger.logInfo("Switched back to WIFI: $SSID_name_def")
            } else {
                onSuccess(false)
                return@Runnable
            }

            DetectAPI().start(SSID_name_def)

            onSuccess(true)

            Logger.logInfo("#MyChromeRunner Ended")

        })
        thread!!.start()
    }

    fun stop() {
        if (thread != null) {
            thread!!.stop()
        }
    }
}