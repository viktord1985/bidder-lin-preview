package com.linked.bidder

import com.linked.api.openAIRequest
import com.linked.bidder.adapter.ProfileCell
import com.linked.bidder.utils.Logger
import com.linked.chromedriver.MyChromeDriver
import com.linked.chromedriver.MyChromeRunner
import com.linked.db.appConfigAutoRun
import com.linked.db.findChromeProfileById
import com.linked.db.loadLinkedPostDB
import com.linked.db.saveLinkedPostDB
import com.linked.scanner.*
import com.linked.scanner.action.openAcceptedProfile
import com.linked.scanner.logic.CoverLetterType
import com.linked.scanner.logic.formatCoverLetterWithAI
import com.linked.scanner.logic.isLinkedPostFits
import com.linked.scanner.logic.isLinkedProfileFits
import com.linked.utils.*
import javafx.application.Platform
import javafx.fxml.FXML
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.ListView
import javafx.scene.input.MouseEvent
import javafx.scene.layout.Pane
import javafx.scene.layout.VBox
import javafx.util.Callback
import java.time.LocalTime
import java.util.*

class MenuController {

    @FXML
    lateinit var logText: Label

    @FXML
    lateinit var tartPostJobsScannerButton: Button

    @FXML
    lateinit var testModeGroup: VBox

    @FXML
    lateinit var btnStop: Button

    // ------ ListView {profiles, logs} start -----

    @FXML
    lateinit var profilesListView: ListView<String>

    @FXML
    private fun handleProfilesListViewClick(event: MouseEvent) {
        if (isRun) {
            return
        }
        val selectedItem = profilesListView.selectionModel.selectedItem
        if (selectedItem != null) {
            println("Selected item: $selectedItem")

            val profileId = selectedItem.split("|")[0]

            showStartUI()

            MyChromeRunner(profileId, "https://gmail.com/").start({

                showStopUI()

            })
        }
    }

    @FXML
    lateinit var logsListView: ListView<String>

    private fun initProfilesList() {
        val linkedPostDB = loadLinkedPostDB()

        profilesListView.selectionModel.selectionMode = javafx.scene.control.SelectionMode.MULTIPLE;
        profilesListView.setOnMouseClicked(this::handleProfilesListViewClick);
        profilesListView.items.clear()
        profilesListView.cellFactory = Callback { ProfileCell(linkedPostDB) }

        appConfig.linkedScannerProfiles.forEach {
            profilesListView.items.add(it)
        }
    }

    @FXML
    private fun handleLogsListViewClick(event: MouseEvent) {
        val selectedItem = logsListView.selectionModel.selectedItem
        if (selectedItem != null) {
            println("Selected item: $selectedItem")
            ClipboardManager.setClipboardContent(selectedItem)
        }
    }

    // ------ ListView {profiles, logs} end -----

    private var isRun = false

    fun initialize() {
        btnStop.isVisible = false
        logsListViewGlobal = logsListView
        setPaneVisible(testModeGroup, false)
        initProfilesList()
        if (fastBootForDebug || dontCloseChromeForDebug) {
            tartPostJobsScannerButton.text = "⚠️Debug Mode⚠️" + tartPostJobsScannerButton.text
        }
        autoRunConfig()
    }

    private fun autoRunConfig() {
        var startDelay = 60
        if (appConfigAutoRun) {
            val localTime = Timer()
            localTime.schedule(object : TimerTask() {
                override fun run() {
                    startDelay--
                    if (startDelay != 0) {
                        Platform.runLater {
                            Logger.logInfo("Start in $startDelay sec..")
                            logText.text = "Start in $startDelay sec.."
                        }
                    } else {
                        localTime.cancel()
                        Platform.runLater {
                            onStartPostJobsScannerClick()
                        }
                    }
                }
            }, 1000, 1000)
        }
    }

    private fun showStartUI() {
        Platform.runLater {
            logText.text = "Scanner started"
            btnStop.isVisible = true
        }
        isRun = true
    }

    private var timer: Timer? = null

    private var runCount = 0

    private fun showStopUI() {
        Platform.runLater {
            logText.text = "Scanner stopped"
            btnStop.isVisible = false
            if (timer != null) {
                timer!!.cancel()
                timer = null
            }
            runCount = 0
        }
        isRun = false
    }

    @FXML
    private fun onStop() {
        showStopUI()
    }

    // ------------------------------- Main Worker -------------------------------

    @FXML
    private fun onStartPostJobsScannerClick() {
        if (isRun) {
            return
        }

        logsListView.selectionModel.selectionMode = javafx.scene.control.SelectionMode.MULTIPLE;
        logsListView.setOnMouseClicked(this::handleLogsListViewClick);
        logsListView.items.clear()

        showStartUI()

        //timeout to close teamviewer
        infoMessage("-- teamviewer delay --")
        Timer().schedule(object : TimerTask() {
            override fun run() {
                postsAndJobsScanner()
            }
        }, 5000)
    }

    private fun postsAndJobsScanner() {

        Logger.logInfo("run postsAndJobsScanner()")

        if (!isRun) {
            return
        }

        if (appConfig.getScanFromValue() <= getCurrentTimeValue() && getCurrentTimeValue() <= appConfig.getScanToValue()) {

            // inc run count
            runCount++

        } else {
            Logger.logInfo("Skip scanner on ${LocalTime.now().hour}:${LocalTime.now().minute}")

            //reset run count
            runCount = 0

            //send ping message
            if (appConfig.isEnablePingTelegramAlerts()) {
                Logger.logInfo("Scanner ping: not time")
                linkedAlertPingBot.sendMessage("Scanner starting.. | not time |")
            }

            timer = Timer()
            timer!!.schedule(object : TimerTask() {
                override fun run() {
                    postsAndJobsScanner()
                }
            }, appConfig.getScanIntervalValue().toLong() + appConfig.getScanIntervalDeltaValue().toLong() * runCount)

            return
        }

        LinkedPostsScanner().start(onBeforeStart = {

            //set display config
            if (appConfig.isDisplayConfigEnable()) {
                Logger.logInfo("#Run displayConfigApp")
                runShellCommand(appConfig.displayConfigApp)
            }

            sleepWithRandom(5000, 1)

            // close teamViewer alert
            Logger.logInfo("#Close TeamViewer alert window")
            runShellCommand(
                "osascript -e 'tell application \"System Events\" to tell process \"TeamViewer\"\n" + "if exists (first window whose name contains \"Sponsored session\") then\n" + "click (button 3 of (first window whose name contains \"Sponsored session\"))\n" + "end if\n" + "end tell'"
            )

            //send ping message
            if (appConfig.isEnablePingTelegramAlerts()) {
                Logger.logInfo("Scanner starting..")
                linkedAlertPingBot.sendMessage("Scanner starting..")
            }

        },
            onMach = { linkedPost ->

                if (linkedPost != null) {
                    infoMessage(linkedPost.profileLink)
                }

                //send alert message
                if (linkedPost != null && appConfig.isEnableTelegramAlerts()) {
                    Logger.logInfo("Mach found: ${linkedPost.link}")
                    linkedAlertBot.sendMessage(linkedPost.getAlertText())
                }

            },
            onSuccess = { selectedLinkedPosts, limitReach, failCount, blocked, scannerUsageStatus, scannerLimitStatus, failedProfiles, isMaxPostCount, processedCount, bidCount ->

                //send ping message
                if (appConfig.isEnablePingTelegramAlerts()) {
                    val limitReachStatus = if (limitReach) "*!!! limitReach !!!*" else ""
                    val blockedStatus = if (blocked) "*!!! blocked !!!*" else ""
                    val failedProfilesInfo = if (failedProfiles) "*!!! failed profiles !!!*" else ""
                    Logger.logInfo("Scanner done | failCount: $failCount, maxPost: $isMaxPostCount, processedCount: $processedCount, bidCount: $bidCount, limitReach: $limitReachStatus, blocked: $blockedStatus, failedProfilesInfo: $failedProfilesInfo")
                    linkedAlertPingBot.sendMessage("Scanner done | processed: $processedCount | bids: $bidCount | found: ${selectedLinkedPosts?.count()} | failCount: $failCount | maxPost: $isMaxPostCount | $limitReachStatus $blockedStatus $failedProfilesInfo")
                    Logger.logInfo("Scanner usage:\n\n$scannerUsageStatus")
                    linkedAlertPingBot.sendMessage("Scanner usage:\n\n$scannerUsageStatus")
                    Logger.logInfo("Scanner limit:\n\n$scannerLimitStatus")
                    linkedAlertPingBot.sendMessage("Scanner limit:\n\n$scannerLimitStatus")
                    linkedAlertPingBot.sendMessage("---------------------------------------------------------------------------------------")
                }

                infoMessage("-- Scanner stopped --")

                selectedLinkedPosts?.forEach { item ->

                    Logger.logInfo("Found item id: ${item.id}, link: ${item.link} ");
                }

                sleepWithRandom(appConfig.getScanIntervalValue() + appConfig.getScanIntervalDeltaValue() * runCount, 1000 * 20)

                postsAndJobsScanner()

                return@start
            })

    }

    // ------------------------------- Test Workers -------------------------------

    @FXML
    private fun onTestModeClick() {
        setPaneVisible(testModeGroup, !testModeGroup.isVisible)
    }

    @FXML
    private fun onTestProfileParsersClick() {
        if (isRun) {
            return
        }

        showStartUI()

        val linkedPostDB = loadLinkedPostDB()

        val profile = findChromeProfileById(linkedPostDB, appConfig.linkedScannerProfiles[0])

        profile!!.needCheck = false

        //val startUrl = "https://www.linkedin.com/in/serhiy-prytula-3a38a824a/"

        // profile where the About section has no 'more'
        //val startUrl = "https://www.linkedin.com/in/valeriy-kostinskiy/"

        // profile without an About section
        val startUrl = "https://www.linkedin.com/in/emily-chen-9675b6323/"
        //https://www.linkedin.com/in/arsen-slyvchuk/?locale=en

        // profile with Open to work
        //val startUrl = "https://www.linkedin.com/in/%D0%B0%D0%B8%D0%B4%D0%B0-%D0%B3%D0%B0%D0%B9%D0%BD%D1%83%D1%82%D0%B4%D0%B8%D0%BD%D0%BE%D0%B2%D0%B0-912818209/"

        // profile with Connect
        //val startUrl = "https://www.linkedin.com/in/almaz-yakhin-85b3469b/"

        // profile without Connect
        //val startUrl = "https://www.linkedin.com/in/yuriy-polyakov-990a35232/"

        MyChromeDriver(
            profile, linkedPostDB, startUrl, autoActions = false, isCanJobsProcess = false,
            isCanFeedProcess = false, isUrlStartPath = false, isUrlEndPath = false
        ).start(onFeedCommands = {

            Logger.logDebug(">>>>>>>>>>>>> isJSConsoleReadyOnProfilePage: ${isJSConsoleReadyOnProfilePage()}")

            Logger.logDebug(">>>>>>>>>>>>> isLinkedProfileReady: ${isProfileReady("Some title")}")

            val linkedProfile = getLinkedProfile(startUrl, "Some Profile Title")

            Logger.logDebug(">>>>>>>>>>>>> linkedProfile")
            Logger.logDebug(">>>>>>>>>>>>> id: ${linkedProfile!!.id}")
            Logger.logDebug(">>>>>>>>>>>>> url: ${linkedProfile.url}")
            Logger.logDebug(">>>>>>>>>>>>> title: ${linkedProfile.title}")
            Logger.logDebug(">>>>>>>>>>>>> description: ${linkedProfile.description}")
            Logger.logDebug(">>>>>>>>>>>>> location: ${linkedProfile.location}")
            Logger.logDebug(">>>>>>>>>>>>> about: ${linkedProfile.about}")
            Logger.logDebug(">>>>>>>>>>>>> openToWork: ${linkedProfile.openToWork}")
            Logger.logDebug(">>>>>>>>>>>>> followersConnectionsInfo: ${linkedProfile.followersConnectionsInfo}")

            linkedProfile.selfTest()

            Logger.logDebug(">>>>>>>>>>>>> getDistanceBadgeLabel: ${getDistanceBadgeLabel()}")

            Logger.logDebug(">>>>>>>>>>>>> isButtonVisibleByTextLabel('Pending'): ${isButtonVisibleOnProfileBeforeMoreMenu("Pending")}")

            Logger.logDebug(">>>>>>>>>>>>> isButtonVisibleByTextLabel('Connect'): ${isButtonVisibleOnProfileBeforeMoreMenu("Connect")}")

            Logger.logDebug(">>>>>>>>>>>>> isLinkedProfileFits: ${isLinkedProfileFits(linkedProfile)}")

            // ------------ TEST1 ------------
            // for a profile with the 'Connect' button

//            Logger.logDebug(">>>>>>>>>>>>> click 'Connect' on profile")
//            clickButtonOnProfileBeforeMoreMenu("Connect")

            // ------------ TEST2 ------------
            // for a profile without the 'Connect' button

//            Logger.logDebug(">>>>>>>>>>>>> click 'More' on profile")
//            clickButtonOnProfileBeforeMoreMenu("More")
//            sleep(1500)
//            Logger.logDebug(">>>>>>>>>>>>> click 'Connect' in dropdown")
//            clickButtonOnProfileInDropdownMoreMenu("Connect")
//            sleep(1500)
//            Logger.logDebug(">>>>>>>>>>>>> click 'Add Note': ${isNotValidPoint(clickFirstTemplatePosition(listOf("ic_add_note")))}")
//
//            // close the console
//            sendOpenJSConsoleCommand()
//            sleep(1000)
//            //old: setCustomNote(noteText)
//            sendTextToActiveInput("let's expand our network of contacts", "note input")
//            sleep(1000)
//            // open the console
//            sendOpenJSConsoleCommand()
//            sleep(1000)
//
//            Logger.logDebug(">>>>>>>>>>>>> click 'Send': ${isNotValidPoint(clickFirstTemplatePosition(listOf("ic_send")))}")

            // ------------ TEST3 ------------

//            Logger.logDebug(">>>>>>>>>>>>> bidProfile await")
//            Logger.logDebug(">>>>>>>>>>>>> bidProfile res: ${bidProfile(profile, linkedProfile, CoverLetterType.botNote, "na")}")

            // ------------ TEST4 ------------

            Logger.logDebug(">>>>>>>>>>>>> openAcceptedProfile await")
            Logger.logDebug(">>>>>>>>>>>>> openAcceptedProfile res: ${openAcceptedProfile(linkedProfile.url, linkedProfile.title, profile, linkedPostDB)}")

            sleepWithRandom(4000, 1000)
            sendCloseChromeTabCommand("onTestProfileParsersClick")

            return@start emptyList()

        }, onFeedCommandsEnd = {

        }, onSuccess = { list ->

            // update DB
            saveLinkedPostDB(linkedPostDB)
            sleepWithRandom(1500, 1500)

            Logger.logInfo("#MyChromeDriver end success: ${list != null}")

            this.showStopUI()

        })
    }

    @FXML
    private fun onTestPostsParsersClick() {
        if (isRun) {
            return
        }

        showStartUI()

        val linkedPostDB = loadLinkedPostDB()

        val profile = findChromeProfileById(linkedPostDB, appConfig.linkedScannerProfiles[0])

        profile!!.needCheck = false

        val startUrl = "https://www.linkedin.com/search/results/content/?keywords=flutter&origin=FACETED_SEARCH&sid=xlK&sortBy=%22date_posted%22"

        MyChromeDriver(
            profile, linkedPostDB, startUrl, autoActions = false, isCanJobsProcess = false,
            isCanFeedProcess = false, isUrlStartPath = false, isUrlEndPath = false
        ).start(onFeedCommands = {

            Logger.logDebug(">>>>>>>>>>>>> isLinkedPostListReady: ${isPostListReady()}")

            Logger.logDebug(">>>>>>>>>>>>> isSearchReusablesListActive: ${isFiltersBarActive()}")

            Logger.logDebug(">>>>>>>>>>>>> isShowMoreButtonExist: ${isShowMoreButtonExist()}")

            val linkedPostList = getLinkedPostList("testScannerId")

            val linkedPost = linkedPostList!!.first()

            Logger.logDebug(">>>>>>>>>>>>> linkedPost")
            Logger.logDebug(">>>>>>>>>>>>> id: ${linkedPost.id}")
            Logger.logDebug(">>>>>>>>>>>>> title: ${linkedPost.title}")
            Logger.logDebug(">>>>>>>>>>>>> description: ${linkedPost.description}")
            Logger.logDebug(">>>>>>>>>>>>> link: ${linkedPost.link}")
            Logger.logDebug(">>>>>>>>>>>>> profileLink: ${linkedPost.profileLink}")
            Logger.logDebug(">>>>>>>>>>>>> text: ${linkedPost.text}")
            Logger.logDebug(">>>>>>>>>>>>> updated: ${linkedPost.updated}")

            linkedPost.selfTest()

            Logger.logDebug(">>>>>>>>>>>>> isLinkedPostFits: ${isLinkedPostFits(linkedPost, linkedPostDB)}")

            openLinkOnNewTab(linkedPost.link)

            sleepWithRandom(4000, 1000)
            sendOpenJSConsoleCommand();

            Logger.logDebug(">>>>>>>>>>>>> isLinkedProfileReady: ${isProfileReady(linkedPost.title)}")

            val linkedProfile = getLinkedProfile(linkedPost.link, linkedPost.title)

            Logger.logDebug(">>>>>>>>>>>>> linkedProfile")
            Logger.logDebug(">>>>>>>>>>>>> id: ${linkedProfile!!.id}")
            Logger.logDebug(">>>>>>>>>>>>> url: ${linkedProfile.url}")
            Logger.logDebug(">>>>>>>>>>>>> title: ${linkedProfile.title}")
            Logger.logDebug(">>>>>>>>>>>>> description: ${linkedProfile.description}")
            Logger.logDebug(">>>>>>>>>>>>> location: ${linkedProfile.location}")
            Logger.logDebug(">>>>>>>>>>>>> about: ${linkedProfile.about}")

            linkedProfile.selfTest()

            sleepWithRandom(4000, 1000)
            sendCloseChromeTabCommand("onTestParserScriptsClick")

            return@start emptyList()

        }, onFeedCommandsEnd = {

        }, onSuccess = { list ->

            // update DB
            saveLinkedPostDB(linkedPostDB)
            sleepWithRandom(1500, 1500)

            Logger.logInfo("#MyChromeDriver end success: ${list != null}")

            this.showStopUI()

        })
    }

    @FXML
    private fun onTestMyChromeDriverClick() {
        if (isRun) {
            return
        }

        showStartUI()

        val linkedPostDB = loadLinkedPostDB()

        val profile = findChromeProfileById(linkedPostDB, appConfig.linkedScannerProfiles[0])

        profile!!.needCheck = false

        MyChromeDriver(
            profile, linkedPostDB, appConfig.startUrl, autoActions = false, isCanJobsProcess = false,
            isCanFeedProcess = false, isUrlStartPath = false, isUrlEndPath = false
        ).start(onFeedCommands = {

            //sendScriptToJSShellCommandWithResult("console.log('xxx')", 3000)

            val result = sendScriptToJSShellCommandWithResult("document.getElementsByTagName('div').length", "test js script")
            Logger.logInfo("result: $result");

            return@start emptyList()

        }, onFeedCommandsEnd = {

        }, onSuccess = { list ->

            // update DB
            saveLinkedPostDB(linkedPostDB)
            sleepWithRandom(1500, 1500)

            Logger.logInfo("#MyChromeDriver end success: ${list != null}")

            this.showStopUI()

        })
    }

    @FXML
    private fun onTestAutoActionsClick() {
        if (isRun) {
            return
        }

        showStartUI()

        val linkedPostDB = loadLinkedPostDB()

        val profile = findChromeProfileById(linkedPostDB, appConfig.linkedScannerProfiles[0])

        //set if you need
        profile!!.needCheck = true

        MyChromeDriver(profile, linkedPostDB, appConfig.startUrl, autoActions = true).start(onFeedCommands = {

            //sendScriptToJSShellCommandWithResult("console.log('xxx')", 3000)

            val result = sendScriptToJSShellCommandWithResult("document.getElementsByTagName('div').length", "test js script")
            Logger.logInfo("result: $result");

            return@start emptyList()

        }, onFeedCommandsEnd = {

        }, onSuccess = { list ->

            // update DB
            saveLinkedPostDB(linkedPostDB)
            sleepWithRandom(1500, 1500)

            Logger.logInfo("#MyChromeDriver end success: ${list != null}")

            this.showStopUI()

        })
    }

    @FXML
    private fun onTestAIClick() {
        if (isRun) {
            return
        }

        showStartUI()

        val desc =
            "Adam Peled" + "\n" + "Founder & CEO in Really Great Tech." + "\n" + "Do you need to raise millions for your start-up?\n" + "Do you need product market fit?\n" + "How about a fully experienced developer?\n" + "\n" + "I have 20+ years of experience within the tech ecosystem, ten years in various technical roles, and ten years as an entrepreneur focused on Tech - meeting Product - meeting Innovation.\n" + "\n" + "Founder and CEO of a software development agency called \"Really Great Tech\" (previously - Razor Grip) that has been running for more than ten years with 40 developers globally, doing various projects for start-ups and enterprises.\n" + "\n" + "With our service/products, our customers raised millions of dollars and doubled their sales in 3 months."

        val messageText = formatCoverLetterWithAI(desc, CoverLetterType.botRefer)

//        Logger.logDebug("messageText: $messageText")

//        val noteText = formatCoverLetterWithAI(desc, CoverLetterType.botNote)
//
//        Logger.logDebug("noteText: $noteText")
//        Logger.logDebug("noteText length: ${noteText.length}")

        val allMessageTextsStr =
            "message: Hi Jamie! I'm impressed by how DiscoverX enhances recruiter efficiency. I'd love to connect and learn more about your AI systems for personalized outreach\n" + "message: cheers for the add\n" + "message: sure happy to chat more\n"

        val allMessageTextsStr2 =
            "message: Hi Julia,\n" + "\n" + "I hope you're doing well! I came across your profile and noticed your extensive experience in recruiting talented professionals. I'm currently seeking new opportunities or projects in mobile development, particularly with Flutter, and thought you might have some insights or know of someone who could use a developer like me. If you have any leads or could point me in the right direction, I would greatly appreciate it. Thanks so much for your time and any help you can provide!\n" + "\n" + "Cheers\n" + "message: hi"

        val result = openAIRequest(allMessageTextsStr2, appConfig.isRequestConnectDetectPrompt)
        Logger.logDebug("AI result: $result")

        showStopUI()
    }

    // ------------------------------- Utils -------------------------------

    @FXML
    private fun onForceQuitClick() {
        System.exit(0)
    }
}

var logsListViewGlobal: ListView<String>? = null

fun infoMessage(message: String) {
    Platform.runLater {
        logsListViewGlobal?.items?.add(message)
        logsListViewGlobal?.items?.lastIndex?.let { logsListViewGlobal?.scrollTo(it) }
    }
}

fun setPaneVisible(pane: Pane, isVisible: Boolean) {
    pane.isVisible = !pane.isVisible
    pane.managedProperty().bind(pane.visibleProperty());
}



