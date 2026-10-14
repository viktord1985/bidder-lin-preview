package com.linked.scanner.logic

import com.linked.model.LinkedPost
import com.linked.scanner.action.getSuggestedLinkedPost

fun getDecisionMakers(people: List<LinkedPost>): List<LinkedPost> {
    val weightedKeywords = mapOf(
        // Priority 0–1 (highest)
        "ceo" to 0, "chief executive" to 0, "founder" to 0, "co-founder" to 0,
        "owner" to 1, "partner" to 1, "president" to 1, "managing partner" to 1,
        "general partner" to 1, "principal" to 1, "visionary" to 1,
        "creator" to 1, "co-creator" to 1, "architect" to 1, "steward" to 1,
        "chief" to 1, "head of" to 1,

        // Priority 2–3 (mid level)
        "vp" to 2, "vice president" to 2, "director" to 2, "lead" to 3,
        "team lead" to 3, "head" to 3, "executive" to 3, "strategy lead" to 3,
        "growth lead" to 3, "innovation lead" to 3, "product lead" to 3,
        "product owner" to 3, "marketing lead" to 3, "technical lead" to 3,
        "business development lead" to 3, "ops lead" to 3,

        // Priority 4–5 (influence signals)
        "strategist" to 4, "decision" to 4, "key person" to 4, "growth hacker" to 4,
        "evangelist" to 4, "builder" to 4, "product maker" to 4, "culture shaper" to 4,
        "leadership" to 4, "authority" to 4, "operations" to 4,

        // Priority 6 (disguised titles/euphemisms)
        "explorer" to 6, "tinkerer" to 6, "alchemist" to 6, "wizard" to 6,
        "magician" to 6, "consultant" to 6, "analyst" to 6
    )

    return people
        .mapNotNull { person ->
            val match = weightedKeywords.entries.minByOrNull { (keyword, _) ->
                if (person.description.contains(keyword, ignoreCase = true)) 0 else Int.MAX_VALUE
            }

            match?.let { (keyword, priority) ->
                if (person.description.contains(keyword, ignoreCase = true)) person to priority else null
            }
        }
        .sortedBy { it.second }
        .map { it.first }
}

fun testDecisionMakers() {
    val list = mutableListOf<LinkedPost>()
    val excludeListIds = mutableListOf<String>()

    excludeListIds.add("https://www.linkedin.com/in/p11/") //dev11
    excludeListIds.add("https://www.linkedin.com/in/p2/") //ceo

    val item = LinkedPost()
    item.profileLink = "https://www.linkedin.com/in/p1/"
    item.description = "dev"
    list.add(item)

    val item11 = LinkedPost()
    item11.profileLink = "https://www.linkedin.com/in/p11/"
    item11.description = "dev11"
    list.add(item11)

    val item2 = LinkedPost()
    item2.profileLink = "https://www.linkedin.com/in/p2/"
    item2.description = "ceo"
    list.add(item2)

    val item3 = LinkedPost()
    item3.profileLink = "https://www.linkedin.com/in/p3/"
    item3.description = "hr"
    list.add(item3)

    val item4 = LinkedPost()
    item4.profileLink = "https://www.linkedin.com/in/p4/"
    item4.description = "diz"
    list.add(item4)

    val item5 = LinkedPost()
    item5.profileLink = "https://www.linkedin.com/in/p5/"
    item5.description = "VP xxx"
    list.add(item5)

    val suggestedLinkedPost = getSuggestedLinkedPost(list, excludeListIds)

    println("desc: " + suggestedLinkedPost?.description)
}