package com.linked.bidder

import com.linked.api.ConnectProfileDB
import com.linked.api.ContentPostDB
import com.linked.api.LinkedAlertBot
import com.linked.api.analyticsInit
import com.linked.bidder.utils.Config
import com.linked.bidder.utils.Logger
import com.linked.db.firebaseAppInit
import com.linked.db.loadAppConfig
import com.linked.db.loadAutorunConfig
import com.linked.model.AppConfig
import com.linked.scanner.logic.initOpenCV
import com.linked.utils.stopScreenRecording
import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.stage.Screen
import javafx.stage.Stage
import java.util.*
import kotlin.concurrent.schedule

lateinit var appConfig: AppConfig

lateinit var linkedAlertPingBot: LinkedAlertBot
lateinit var linkedAlertBot: LinkedAlertBot

var fastBootForDebug = false
var dontCloseChromeForDebug = false

class MyApplication : Application() {

    private fun appInit() {

        if (parameters.raw.isEmpty()) {
            println("Error: No AppConfig path!")
            return
        }

        appConfig = loadAppConfig(parameters.raw[0])
        loadAutorunConfig(parameters)

        linkedAlertPingBot = LinkedAlertBot(appConfig.telegramPingChatId)
        linkedAlertBot = LinkedAlertBot(appConfig.telegramChatId)

        Logger.init()
        Logger.logInfo("---------------------------------------------------------------------------------")
        Logger.logInfo("---------------------------------- App started ----------------------------------")
        Logger.logInfo("--------------------------------- ${Config.appVersion} ---------------------------------")
        Logger.logInfo("---------------------------------------------------------------------------------")

        if (fastBootForDebug) {
            Logger.logDebug("!!! fast boot for debug mode !!!")
        }

        if (!fastBootForDebug) {
            analyticsInit()
        } else {
            Logger.logDebug("!!! Skip analyticsInit for fast boot !!!")
        }

        firebaseAppInit()

        // uncomment if needed for algorithm tests
        //myTest()

        //val firebaseProfiles = getFirebaseProfiles(appConfig.botID)
        //Logger.logInfo("FirebaseProfiles botId: ${appConfig.botID} size: ${firebaseProfiles.size}")

        // download files at normal speed; with the current connection the bots handle 20MB files fine
        val posts = ContentPostDB().getList()
        Logger.logInfo("ContentPostDB posts size: ${posts.size}")
        val profiles = ConnectProfileDB().getList()
        Logger.logInfo("ConnectProfileDB profiles size: ${profiles.size}")

        stopScreenRecording() // just in case, since it may be left hanging in the background

        Timer().schedule(if (fastBootForDebug) 0 else 6000) {
            initOpenCV()
        }
    }

    override fun start(stage: Stage) {
        appInit()
        // init UI
        val height = Screen.getPrimary().bounds.height
        val width = Screen.getPrimary().bounds.width
        val fxmlLoader = FXMLLoader(MyApplication::class.java.getResource("menu-view.fxml"))
        val scene = Scene(fxmlLoader.load(), width * Config.leftSideSize, height)
        stage.x = 0.0
        stage.y = 0.0
        stage.title = "Scanner Linked ${Config.appVersion}"
        stage.scene = scene
        stage.show()
    }
}

fun main() {
    Application.launch(MyApplication::class.java)
}