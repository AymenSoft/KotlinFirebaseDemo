package com.app.firebasedemo.messaging

import android.util.Log
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging

/**
 * get firebase messaging token to save it on server side
 * @author Aymen Masmoudi[08.11.2021]
 * */
class Token {

    fun getToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("error", task.exception)
                return@OnCompleteListener
            } else {
                val token = task.result
                //TODO Implement this method to send token to your server
            }
        })
    }

}