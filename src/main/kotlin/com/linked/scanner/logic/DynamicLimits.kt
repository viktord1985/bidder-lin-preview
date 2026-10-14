package com.linked.scanner.logic

// --- limit ramp-up logic

val STOP_BID_DAYS = 4

fun getMaxProfilesBotBidPerDayDynamic(daysFromLastActivity: Int, weeksFromCreated: Int, maxBid: Int): Int {

    var daysFromLastActivityK = 1.0

    if (daysFromLastActivity == 0) {
        daysFromLastActivityK = 1.0
    } else if (daysFromLastActivity == 1) {
        daysFromLastActivityK = 0.7
    } else if (daysFromLastActivity == 2) {
        daysFromLastActivityK = 0.5
    } else if (daysFromLastActivity == 3) {
        daysFromLastActivityK = 0.3
    } else if (daysFromLastActivity >= STOP_BID_DAYS) {
        daysFromLastActivityK = 0.0
    }

    var weeksFromCreatedK = 1.0

    if (weeksFromCreated == 0 || weeksFromCreated == 1 || weeksFromCreated == 2) {
        weeksFromCreatedK = 0.3
    } else if (weeksFromCreated == 3) {
        weeksFromCreatedK = 0.5
    } else if (weeksFromCreated == 4 * 2) { // longer warm-up -> brand-new accounts probably need about a year to avoid bans for activity
        weeksFromCreatedK = 0.7
    } else if (weeksFromCreated >= 5 * 3) { // longer warm-up -> brand-new accounts probably need about a year to avoid bans for activity
        weeksFromCreatedK = 1.0
    }

    var result = maxBid * daysFromLastActivityK * weeksFromCreatedK

    if (result < 1) {
        result = 1.0
    }

    return result.toInt()
}