package com.linked.model

import com.google.cloud.firestore.QueryDocumentSnapshot

class FirebaseProfile(
    val id: String,
    val analyticName: String,
    val botId: String,
    var needCheck: Boolean,
)

fun getFirebaseProfileFromSnapshot(snapshot: QueryDocumentSnapshot): FirebaseProfile {
    return FirebaseProfile(
        snapshot.id,
        snapshot.getString("analyticName") ?: "",
        snapshot.getString("botId") ?: "",
        snapshot.getBoolean("needCheck") ?: false,
    )
}

fun getFirebaseProfileMap(firebaseProfile: FirebaseProfile): Map<String, Any> {
    return mapOf(
        "analyticName" to firebaseProfile.analyticName,
        "botId" to firebaseProfile.botId,
        "needCheck" to firebaseProfile.needCheck,
    )
}

