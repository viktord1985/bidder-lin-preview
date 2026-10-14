package com.linked.bidder

import com.linked.db.loadLinkedPostDB
import com.linked.scanner.getScannersLimitInfo
import com.linked.scanner.getScannersUsageInfo
import com.linked.scanner.logic.getMaxProfilesBotBidPerDayDynamic

fun myTestX() {

    println(">>> myTest start")

    for (i in 0..8) {
        println(">>> t11_${i}: d0, w${i}, m12, r=${getMaxProfilesBotBidPerDayDynamic(0, i, 12)}")
    }

    for (i in 0..8) {
        println(">>> t12_${i}: d0, w${i}, m2, r=${getMaxProfilesBotBidPerDayDynamic(0, i, 2)}")
    }

    println(">>> -------- ")

    for (i in 0..6) {
        println(">>> t21_${i}: d${i}, w8, m12, r=${getMaxProfilesBotBidPerDayDynamic(i, 8, 12)}")
    }

    for (i in 0..6) {
        println(">>> t22_${i}: d${i}, w8, m2, r=${getMaxProfilesBotBidPerDayDynamic(i, 8, 2)}")
    }

    val i = 2 / 0
}

fun myTest() {

    println(">>> myTest start")

    val linkedPostDB = loadLinkedPostDB()

    val scannerUsageInfo= getScannersUsageInfo(linkedPostDB)

    println(scannerUsageInfo)

    val scannerLimitInfo = getScannersLimitInfo(linkedPostDB)

    println(scannerLimitInfo)

    val i = 2 / 0

    println(">>> myTest end")
}