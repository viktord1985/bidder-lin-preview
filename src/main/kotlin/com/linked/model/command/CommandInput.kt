package com.linked.model.command

import com.linked.bidder.infoMessage
import com.linked.bidder.linkedAlertPingBot
import com.linked.bidder.utils.Config
import com.linked.bidder.utils.Logger
import com.linked.model.LinkedPostDB
import com.linked.utils.*
import java.util.*

class CommandInput(
    val name: String,
    val place: String,
    val type: String,
    val text: String,
    val delay: String,
    val probability: String,
) {
    fun run(linkedPostDB: LinkedPostDB): Boolean {
        if (shouldPerformAction(probability.toDouble())) {

            infoMessage(".. (${name}/${type})")

            linkedAlertPingBot.sendMessage(".. (${name}/${type})")

            if (type == "navbarFocus") {
                // pick the variant for this session
                initNavbarTextIndex(text, linkedPostDB)
                if (!setNavbarFocus(place, getNavbarTextByIndex(text))) {
                    return true
                }
            } else if (type == "navbarText") {
                setNavbarText(place, getNavbarTextByIndex(text))
            } else if (type == "navbarSubmit") {
                setNavbarSubmit(place, getNavbarTextByIndex(text))

                navbarTextQuery = getNavbarTextByIndex(text)

                if (navbarTextLog.isEmpty()) {
                    navbarTextLog = navbarTextQuery
                } else {
                    navbarTextLog = "$navbarTextLog, $navbarTextQuery"
                }

                linkedAlertPingBot.sendMessage("navbarText: *${getNavbarTextByIndex(text)}* random: ${Config.isRandomSearchQuery} index: $navbarTextIndex size: $navbarTextArraySize")
            } else {
                Logger.logError("!!! Unknown input type !!!")
            }
            sleepWithRandom(delay.toInt(), delay.toInt())
        } else {
            Logger.logInfo("[Skip input type: $type]")
        }
        return true
    }
}

var navbarTextLog = ""

var navbarTextQuery = ""

var navbarTextIndex = -1

var navbarTextArraySize = -1

fun initNavbarTextIndex(text: String, linkedPostDB: LinkedPostDB) {

    navbarTextArraySize = text.split("/").size

    if (!text.contains("/")) {
        navbarTextIndex = -1
    } else {
        if (Config.isRandomSearchQuery) {
            navbarTextIndex = Random().nextInt(navbarTextArraySize)
        } else {
            navbarTextIndex = linkedPostDB.getSearchQueryIndexValue(navbarTextArraySize)
        }
    }

    Logger.logDebug("navbarTextIndex: $navbarTextIndex random: ${Config.isRandomSearchQuery} size: $navbarTextArraySize")
}

fun getNavbarTextByIndex(text: String): String {
    if (navbarTextIndex == -1) {
        return text
    }
    return text.split("/")[navbarTextIndex]
}

const val getNavbarInputV1Test = "document.querySelector('.global-nav__content')"
const val getNavbarInputFieldHome = "document.querySelector('.global-nav__content').querySelector('.search-global-typeahead__input')"
const val getNavbarInputFieldHomeV2 = "document.querySelectorAll('[data-testid=\"typeahead-input\"]')[0]"

const val getNavbarInputFieldJobs = "document.querySelector('.global-nav__content').querySelector('.jobs-search-global-typeahead__input')"
const val getNavbarInputFieldJobsV2 = "document.querySelectorAll('[data-testid=\"typeahead-input\"]')[0]"

const val getNavbarInputFieldJobsLocation =
    "document.querySelector('.global-nav__content').querySelector('.jobs-search-box__input--location').querySelector('input')"
const val getNavbarInputFieldJobsLocationV2 = "document.querySelectorAll('[data-testid=\"typeahead-input\"]')[1]"

const val dispatchEventFocus = "dispatchEvent(new Event('focus', { bubbles: true, cancelable: true }))"
const val dispatchEventFocusV2 = "dispatchEvent(new Event('focusin', { bubbles: true, cancelable: true }))"
const val dispatchEventInput = "dispatchEvent(new Event('input', { bubbles: true, cancelable: true }))"
const val dispatchEventSetValue =
    "const el = PPP1; const previousValue = el.value; el.value = 'PPP2'; const tracker = el._valueTracker; if (tracker) tracker.setValue(previousValue);"
const val dispatchEventBlur = "dispatchEvent(new Event('blur'))"
const val dispatchEventKeydownEnter = "dispatchEvent(new KeyboardEvent('keydown', {  key: 'Enter', code: 'Enter', which: 13, keyCode: 13}))"
const val dispatchEventKeydownEnterV2 =
    "dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter', code: 'Enter', bubbles: true, cancelable: true, composed: true }))"

fun setNavbarFocus(place: String, text: String): Boolean {

    Logger.logInfo("setNavbarFocus place: $place text: $text")

    if (!testAndFixIframe()) {
        return false
    }

    var isV1 = false
    var getNavbarInputField = ""
    if (place == "home") {
        if (sendScriptToJSShellCommandWithResult(getNavbarInputV1Test, "getNavbarInputV1Test test") != "null") {
            isV1 = true
            getNavbarInputField = getNavbarInputFieldHome
        } else {
            getNavbarInputField = getNavbarInputFieldHomeV2
        }
    } else if (place == "jobs") {
        if (sendScriptToJSShellCommandWithResult(getNavbarInputV1Test, "getNavbarInputV1Test test") != "null") {
            isV1 = true
            getNavbarInputField = getNavbarInputFieldJobs
        } else {
            getNavbarInputField = getNavbarInputFieldJobsV2
        }
    } else if (place == "location") {
        if (sendScriptToJSShellCommandWithResult(getNavbarInputV1Test, "getNavbarInputV1Test test") != "null") {
            isV1 = true
            getNavbarInputField = getNavbarInputFieldJobsLocation
        } else {
            getNavbarInputField = getNavbarInputFieldJobsLocationV2
        }
    }

    sleepWithRandom(2000, 1000)

    if (isV1) {
        //set focus event
        sendScriptToJSShellCommand(
            "$getNavbarInputField.$dispatchEventFocus;",
            "setNavbarFocus | focus event"
        )

        sleepWithRandom(2000, 1000)

        //set text with random
        sendScriptToJSShellCommand(
            "$getNavbarInputField.value = '${removeRandomChar(text)}';",
            "setNavbarFocus | set text with random"
        )

        sleepWithRandom(2000, 1000)

        //set input event
        sendScriptToJSShellCommand(
            "$getNavbarInputField.$dispatchEventInput;",
            "setNavbarFocus | input event"
        )
    } else {
        //set focus event
        sendScriptToJSShellCommand(
            "$getNavbarInputField.$dispatchEventFocusV2;",
            "setNavbarFocus | focus event V2"
        )

        sleepWithRandom(2000, 1000)

        val dispatchEventSetValueWithParams = dispatchEventSetValue.replace("PPP1", getNavbarInputField)
            .replace("PPP2", removeRandomChar(text))

        //set text with random
        sendScriptToJSShellCommand(
            dispatchEventSetValueWithParams,
            "setNavbarFocus | set text with random V2"
        )

        //sleepWithRandom(2000, 1000)
        //
        //Note: this mostly works for V1, kept here just in case
        //set input event
        //sendScriptToJSShellCommand(
        //    "$getNavbarInputField.$dispatchEventInput;",
        //    "setNavbarFocus | input event"
        //)
    }

    return true
}

fun setNavbarText(place: String, text: String) {

    Logger.logInfo("setNavbarText place: $place text: $text")

    var isV1 = false
    var getNavbarInputField = ""
    if (place == "home") {
        if (sendScriptToJSShellCommandWithResult(getNavbarInputV1Test, "getNavbarInputV1Test test") != "null") {
            isV1 = true
            getNavbarInputField = getNavbarInputFieldHome
        } else {
            getNavbarInputField = getNavbarInputFieldHomeV2
        }
    } else if (place == "jobs") {
        if (sendScriptToJSShellCommandWithResult(getNavbarInputV1Test, "getNavbarInputV1Test test") != "null") {
            isV1 = true
            getNavbarInputField = getNavbarInputFieldJobs
        } else {
            getNavbarInputField = getNavbarInputFieldJobsV2
        }
    } else if (place == "location") {
        if (sendScriptToJSShellCommandWithResult(getNavbarInputV1Test, "getNavbarInputV1Test test") != "null") {
            isV1 = true
            getNavbarInputField = getNavbarInputFieldJobsLocation
        } else {
            getNavbarInputField = getNavbarInputFieldJobsLocationV2
        }
    }

    sleepWithRandom(3000, 1000)

    if (isV1) {

        //set text with random
        sendScriptToJSShellCommand(
            "$getNavbarInputField.value = '${removeRandomChar(text)}';",
            "setNavbarText | set text with random"
        )

        sleepWithRandom(3000, 1000)

        //set input event
        sendScriptToJSShellCommand(
            "$getNavbarInputField.$dispatchEventInput;",
            "setNavbarText | input event"
        )
    } else {

        sleepWithRandom(3000, 1000)

        val dispatchEventSetValueWithParams = dispatchEventSetValue.replace("PPP1", getNavbarInputField)
            .replace("PPP2", removeRandomChar(text))

        //set text with random
        sendScriptToJSShellCommand(
            dispatchEventSetValueWithParams,
            "setNavbarFocus | set text with random V2"
        )

        //sleepWithRandom(2000, 1000)
        //
        //Note: this mostly works for V1, kept here just in case
        //set input event
        //sendScriptToJSShellCommand(
        //    "$getNavbarInputField.$dispatchEventInput;",
        //    "setNavbarFocus | input event"
        //)
    }
}

fun setNavbarSubmit(place: String, text: String) {

    Logger.logInfo("setNavbarSubmit place: $place text: $text")

    var isV1 = false
    var getNavbarInputField = ""
    if (place == "home") {
        if (sendScriptToJSShellCommandWithResult(getNavbarInputV1Test, "getNavbarInputV1Test test") != "null") {
            isV1 = true
            getNavbarInputField = getNavbarInputFieldHome
        } else {
            getNavbarInputField = getNavbarInputFieldHomeV2
        }
    } else if (place == "jobs") {
        if (sendScriptToJSShellCommandWithResult(getNavbarInputV1Test, "getNavbarInputV1Test test") != "null") {
            isV1 = true
            getNavbarInputField = getNavbarInputFieldJobs
        } else {
            getNavbarInputField = getNavbarInputFieldJobsV2
        }
    } else if (place == "location") {
        if (sendScriptToJSShellCommandWithResult(getNavbarInputV1Test, "getNavbarInputV1Test test") != "null") {
            isV1 = true
            getNavbarInputField = getNavbarInputFieldJobsLocation
        } else {
            getNavbarInputField = getNavbarInputFieldJobsLocationV2
        }
    }

    sleepWithRandom(3000, 1000)

    if (isV1) {

        //set text
        sendScriptToJSShellCommand(
            "$getNavbarInputField.value = '$text';",
            "setNavbarSubmit | set text"
        )

        sleepWithRandom(3000, 1000)

        //set input event
        sendScriptToJSShellCommand(
            "$getNavbarInputField.$dispatchEventInput;",
            "setNavbarText | input event"
        )

        sleepWithRandom(3000, 1000)

        //set key enter event
        sendScriptToJSShellCommand(
            "$getNavbarInputField.$dispatchEventKeydownEnter;",
            "setNavbarSubmit | key enter event"
        )

        sleepWithRandom(3000, 1000)

        //set clear focus event
        sendScriptToJSShellCommand(
            "$getNavbarInputField.$dispatchEventBlur;",
            "setNavbarSubmit | clear focus event"
        )
    } else {

        sleepWithRandom(3000, 1000)

        val dispatchEventSetValueWithParams = dispatchEventSetValue.replace("PPP1", getNavbarInputField)
            .replace("PPP2", text)

        //set text with random
        sendScriptToJSShellCommand(
            dispatchEventSetValueWithParams,
            "setNavbarFocus | set text V2"
        )

        sleepWithRandom(3000, 1000)

        //set input event (like on V1)
        sendScriptToJSShellCommand(
            "$getNavbarInputField.$dispatchEventInput;",
            "setNavbarText | input event"
        )

        sleepWithRandom(3000, 1000)

        //set input event
        sendScriptToJSShellCommand(
            "$getNavbarInputField.$dispatchEventKeydownEnterV2;",
            "setNavbarText | input event V2"
        )
    }
}

fun getCommandInput(text: String): CommandInput {

    return CommandInput(
        text.split("|")[0],
        text.split("|")[1],
        text.split("|")[2],
        text.split("|")[3],
        text.split("|")[4],
        text.split("|")[5],
    )
}

//Note: struggles with finding how to focus the field
//function setReactInputValue(selector, value) {
//  const opts = { bubbles: true, cancelable: true, composed: true };
//
//  // --- find the element ---
//  let el =
//    document.querySelector(selector) ||
//    document.querySelector('input[placeholder*="Search" i]') ||
//    document.querySelector('input[type="search"]');
//
//  // Shadow DOM support
//  if (el && el.shadowRoot) {
//    const inner = el.shadowRoot.querySelector('input');
//    if (inner) el = inner;
//  }
//
//  if (!el) {
//    console.warn('❌ Input not found for selector:', selector);
//    return;
//  }
//
//  // --- focus (via React-friendly events) ---
//  el.dispatchEvent(new MouseEvent('mousedown', opts));
//  el.dispatchEvent(new MouseEvent('mouseup', opts));
//  HTMLElement.prototype.focus.call(el);
//  el.dispatchEvent(new Event('focus', opts));
//  el.dispatchEvent(new Event('focusin', opts));
//
//  // --- set the value ---
//  const previousValue = el.value;
//  el.value = value;
//
//  // update the React internal tracker, if present
//  const tracker = el._valueTracker;
//  if (tracker) tracker.setValue(previousValue);
//
//  // --- input event ---
//  el.dispatchEvent(new Event('input', opts));
//
//  // --- simulate pressing Enter ---
//  el.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter', code: 'Enter', ...opts }));
//  el.dispatchEvent(new KeyboardEvent('keypress', { key: 'Enter', code: 'Enter', ...opts }));
//  el.dispatchEvent(new KeyboardEvent('keyup', { key: 'Enter', code: 'Enter', ...opts }));
//
//  console.log(`✅ setReactInputValue: "${value}" set + focused + Enter simulated`);
//}; setReactInputValue('[data-testid="typeahead-input"]', 'flutter developer');

