package com.app.firebasedemo

import android.app.Application
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
/**
 * set firebase settings when application start
 * @author Aymen Masmoudi[08.11.2021]
 * */
class FirebaseDemoApplication: Application() {

    private lateinit var analytics: FirebaseAnalytics

    var isUserConnected = true

    override fun onCreate() {
        super.onCreate()

        //enable firebase offline mode
        Firebase.database.setPersistenceEnabled(true)

        //enable firebase analytics
        analytics = Firebase.analytics
        val params = Bundle()
        //send analytics event
        params.putString("start_application", "start")
        analytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, params)

        //get user internet status
        val connectedRef = Firebase.database.getReference(".info/connected")
        connectedRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java) ?: false
                isUserConnected = connected
            }

            override fun onCancelled(error: DatabaseError) {
                isUserConnected = false
            }
        })

    }

}