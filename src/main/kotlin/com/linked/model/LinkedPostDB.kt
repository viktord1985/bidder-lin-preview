package com.linked.model

import org.simpleframework.xml.Element
import org.simpleframework.xml.ElementList
import org.simpleframework.xml.Root
import java.time.LocalDate

@Root(name = "LinkedPostDB", strict = false)
class LinkedPostDB {

    @field:ElementList(entry = "items", required = false)
    lateinit var items: MutableList<LinkedPost>

    @field:ElementList(entry = "jobs", required = false)
    lateinit var jobs: MutableList<LinkedJob>

    @field:ElementList(entry = "companies", required = false)
    lateinit var companies: MutableList<LinkedCompany>

    @field:ElementList(entry = "profiles", required = false)
    lateinit var profiles: MutableList<ChromeProfile>

    @field:Element(name = "searchQueryIndex", required = false)
    lateinit var searchQueryIndex: String

    @field:Element(name = "searchQueryIndexDate", required = false)
    lateinit var searchQueryIndexDate: String

    fun getSearchQueryIndexValue(arraySize: Int): Int {
        if (searchQueryIndexDate != dateFormatter.format(LocalDate.now())
            // condition in case the search keyword array was changed
            || (searchQueryIndex.toInt() > arraySize - 1)
        ) {
            searchQueryIndex = "0"
            searchQueryIndexDate = dateFormatter.format(LocalDate.now())
        }
        return searchQueryIndex.toInt()
    }

    //todo - need a shared system for distributing search queries
    // or build configs so that search keywords don't overlap

    fun goNextSearchQueryIndex(arraySize: Int) {
        if (searchQueryIndex.toInt() == (arraySize - 1)) {
            // the idea here -> stay on the last index

            //nop
            //searchQueryIndex = "0"

            //todo - because of {isMaxPostCount}, low-frequency queries only cover half a day (up to 12h)
            // maybe reset the index once a day in the second half of the day
            // current state - low-frequency queries cover only 12h (half a day)
        } else {
            searchQueryIndex = (searchQueryIndex.toInt() + 1).toString()
        }
    }

    fun isSearchQueryIndexLast(arraySize: Int): Boolean {
        return searchQueryIndex.toInt() == (arraySize - 1)
    }
}






