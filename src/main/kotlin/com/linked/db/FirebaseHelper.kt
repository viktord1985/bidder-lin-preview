package com.linked.db

import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.firestore.Firestore
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.cloud.FirestoreClient
import com.linked.bidder.fastBootForDebug
import com.linked.bidder.utils.Logger
import com.linked.model.FirebaseProfile
import com.linked.model.getFirebaseProfileFromSnapshot
import com.linked.model.getFirebaseProfileMap
import java.io.FileInputStream
import java.util.concurrent.TimeUnit

fun firebaseAppInit() {

    val serviceAccount = FileInputStream("serviceAccountKey.json")
    val options = FirebaseOptions.builder().setCredentials(GoogleCredentials.fromStream(serviceAccount))
        .setReadTimeout(TimeUnit.SECONDS.toMillis(18).toInt()).setDatabaseUrl("https://up-rss-tracker.firebaseio.com").build()

    FirebaseApp.initializeApp(options)
}

fun getFirebaseProfiles(botId: String): MutableList<FirebaseProfile> {
    Logger.logInfo("getFirebaseProfiles")

    val profiles = mutableListOf<FirebaseProfile>()

    if (fastBootForDebug) {
        Logger.logDebug("!!! Mock date in getFirebaseProfiles for fast boot !!!")
        profiles.add(
            FirebaseProfile(
                "botDev|send1",
                "send1",
                "Dev",
                false,
            )
        )
        return profiles
    }

    try {

        val db: Firestore = FirestoreClient.getFirestore()

        // Reference to 'profile' collection
        val collection = db.collection("profile").whereEqualTo("botId", botId)

        // Get all documents in the 'project' collection
        val querySnapshot = collection.get().get()

        // Loop through the documents and get the data
        for (document in querySnapshot.documents) {
            profiles.add(getFirebaseProfileFromSnapshot(document))
        }

    } catch (e: Exception) {
        e.printStackTrace()
        Logger.logError("Error getFirebaseProfiles: ${e.message}")
    }

    return profiles
}

fun updateFirebaseProfile(firebaseProfile: FirebaseProfile): Boolean {
    Logger.logInfo("updateFirebaseProfile analyticName:${firebaseProfile.analyticName}")
    try {
        val db: Firestore = FirestoreClient.getFirestore()

        // Reference to the specific project document
        val projectRef = db.collection("profile").document(firebaseProfile.id)

        val updates = getFirebaseProfileMap(firebaseProfile)

        // Update multiple fields with new values
        projectRef.update(updates).get()

    } catch (e: Exception) {
        e.printStackTrace()
        Logger.logError("Error updateFirebaseProfile: ${e.message}")
        return false
    }

    return true
}

fun createFirebaseProfile(firebaseProfile: FirebaseProfile) {
    Logger.logInfo("createFirebaseProfile analyticName:${firebaseProfile.analyticName}")

    try {
        val db: Firestore = FirestoreClient.getFirestore()

        // Reference to the specific project document
        val projectRef = db.collection("profile").document("bot${firebaseProfile.botId}|${firebaseProfile.analyticName}")

        val updates = getFirebaseProfileMap(firebaseProfile)

        // Update multiple fields with new values
        projectRef.set(updates).get()

    } catch (e: Exception) {
        e.printStackTrace()
        Logger.logError("Error createFirebaseProfile: ${e.message}")
    }
}

fun findFirebaseProfileItemByAnalyticName(list: List<FirebaseProfile>, analyticName: String): FirebaseProfile? {
    list.forEach { item ->
        if (item.analyticName == analyticName) {
            return item
        }
    }
    return null
}