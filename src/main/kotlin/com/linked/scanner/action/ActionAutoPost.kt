package com.linked.scanner.action

import com.linked.api.ContentPostDB
import com.linked.api.contentPostSelfTest
import com.linked.bidder.linkedAlertPingBot
import com.linked.bidder.utils.Logger
import com.linked.chromedriver.profileAnalyticName
import com.linked.model.ChromeProfile
import com.linked.model.command.clickCloseDialog
import com.linked.model.command.scrollToPageTop
import com.linked.scanner.logic.*
import com.linked.utils.*
import java.time.LocalDateTime
import java.util.*

fun autoContentPost(profile: ChromeProfile): Boolean {

    if (!profile.isCanMakePost()) {
        Logger.logInfo("isCanMakePost = false")
        return true
    }

    Logger.logInfo("Start load posts")

    val posts = ContentPostDB().getList()
    Logger.logInfo("Loaded posts count: ${posts.size}")

    // look for new posts, check they weren't published yet
    var notPosted = posts.filter { it -> contentPostSelfTest(it) && !profile.isUsedPost(it.getPostID()) }
    Logger.logInfo("NotPosted posts count step1: ${notPosted.size}")

    Logger.logInfo("isNotPersonalContent: ${profile.isNotPersonalContent()}")

    if (profile.isNotPersonalContent()) {
        notPosted = notPosted.filter { it -> it.isNotPersonal() }
    } else {
        notPosted = notPosted.filter { it -> it.isNotUsedPersonal() }
    }

    Logger.logInfo("NotPosted posts count step2: ${notPosted.size}")

    if (notPosted.isNotEmpty()) {

        // pick a random one from the list
        val post = notPosted[Random().nextInt(notPosted.size)]

        Logger.logInfo("Select for post: ${post.name}, isVideo ${post.isVideo()}, text:")
        Logger.logInfo(post.text)

        if (profile.isCanPostContent()) {

            // close all dialogs/chats
            if (!closeOpenedChatDialogs()) {
                Logger.logDebug("!!! closeOpenedChatDialogs fail !!!")
                return false
            }

            //Old: val htmlStr =
            //    sendScriptToJSShellCommandWithResult("document.querySelector('[aria-label^=\"Add media\"]')", "is [Media] btn visible?")
            //if (!isCompleteRequest(htmlStr)) {
            //    return false
            //}
            //if (htmlStr == "null") {

            //todo !!! set {ic_post_media_v1} {ic_post_media_v2} !!!
            if (isNotValidPoint(findFirstTemplatePosition(listOf("ic_post_media_v1", "ic_post_media_v2")))) {
                if (post.isVideo()) {
                    // click to choose a video
                    //old: sendScriptToJSShellCommand("document.querySelector('[aria-label^=\"Add a video\"]').click();", "click Video")
                    if (isNotValidPoint(clickFirstTemplatePosition(listOf("ic_post_video_v1", "ic_post_video_v2")))) {
                        return false;
                    }
                } else {
                    // click to choose a photo
                    //old: sendScriptToJSShellCommand("document.querySelector('[aria-label^=\"Add a photo\"]').click();", "click Photo")
                    if (isNotValidPoint(clickFirstTemplatePosition(listOf("ic_post_photo_v1", "ic_post_photo_v2")))) {
                        return false;
                    }
                }
            } else {
                // click to choose a video
                //old: sendScriptToJSShellCommand("document.querySelector('[aria-label^=\"Add media\"]').click();", "click Video")
                if (isNotValidPoint(clickFirstTemplatePosition(listOf("ic_post_media_v1", "ic_post_media_v2")))) {
                    return false;
                }
            }

            // longer delay for the dialog to open
            sleep(9000)

            //sendScriptToJSShellCommand(
            //    "document.querySelector('.media-editor-file-selector__upload-media-button label').click();",
            //    "click [Upload from computer]"
            //)
            //
            //sleep(3000)

            Logger.logInfo("selectFileInOpenedChrome")
            post.files.first().localPath?.let { selectFileInOpenedChrome(it) }
            sleepWithRandom(6000, 3000)

            // wait for next
            //Old: sendScriptToJSShellCommand("document.querySelector('[aria-label^=\"Next\"]').click();", "click Next")
            if (isNotValidPoint(clickFirstTemplatePosition(listOf("ic_next")))) {
                return false;
            }
            sleepWithRandom(6000, 3000)

            // paste the text
            //Old: sendScriptToJSShellCommand(
            //    "function simulateTyping(div, text) { div.focus(); document.execCommand('selectAll', false, null); document.execCommand('insertText', false, text); } simulateTyping(document.querySelector('div[contenteditable=\"true\"]'), \"${
            //        convertXmlTextToJS(
            //            post.text
            //                .replace("hashtag#", "#")
            //                .replace("—", "-")
            //        )
            //    }\");",
            //    "set msg text"
            //)
            sendTextToActiveInput(post.text.replace("hashtag#", "#").replace("—", "-"), "post input")
            sleepWithRandom(12000, 3000)

            // click Post
            //Old: sendScriptToJSShellCommand("document.querySelector('.share-box_actions button').click();", "click Post")
            if (isNotValidPoint(clickFirstTemplatePosition(listOf("ic_post")))) {
                return false;
            }

            profile.incContentPostCount()

            profile.setLastPostDateTime(LocalDateTime.now())

            profile.addUsedPost(post.getPostID())

            if (!profile.isNotPersonalContent()) {
                //set used content flag
                ContentPostDB().updatePersonal(post.id, profileAnalyticName)
            }

            // need to return focus to the JS console
            refocusJSConsole()

            waitUntilTextExistOnScreen("Uploading...")

            if (isTextExistOnScreen("Verify now")) {
                clickCloseDialog()
                scrollToPageTop() // sometimes the close button is elsewhere in the feed; the screen scrolls and must be scrolled back
                sleep(3000)
            }

            sleepWithRandom(12000, 3000)

            linkedAlertPingBot.sendMessage("*Published a new post* notPersonal: ${profile.isNotPersonalContent()}, left: ${notPosted.size}")
        } else {
            linkedAlertPingBot.sendMessage("Found a new post - limits exhausted")
        }
    } else {
        linkedAlertPingBot.sendMessage("*!!! Out of content !!!*")
    }

    return true
}
