package com.linked.db

import com.linked.bidder.utils.Logger
import com.linked.model.AppConfig
import javafx.application.Application
import org.simpleframework.xml.core.Persister
import java.io.File

fun loadAppConfig(path: String): AppConfig {
    return Persister().read(AppConfig::class.java, File(path))
}

var appConfigAutoRun: Boolean = false

fun loadAutorunConfig(parameters: Application.Parameters) {

    if (parameters.raw.size < 2) {
        appConfigAutoRun = false
        return
    }

    appConfigAutoRun = parameters.raw[1].contains("true");

    Logger.logInfo("loadAutorunConfig appConfigAutoRun:$appConfigAutoRun")
}

