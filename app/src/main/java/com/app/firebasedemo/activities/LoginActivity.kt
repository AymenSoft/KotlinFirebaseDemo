package com.app.firebasedemo.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import com.app.firebasedemo.databinding.ActivityLoginBinding
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import java.lang.Exception

/**
 * user can connect, create new profile or ask for new password
 * @author Aymen Masmoudi[08.11.2021]
 * */
class LoginActivity : AppCompatActivity() {

    private val USED_EMAIL_ERROR = "The email address is already in use by another account."

    private lateinit var binding: ActivityLoginBinding

    private lateinit var auth: FirebaseAuth
    private lateinit var currentUser: FirebaseUser

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()
        auth.addAuthStateListener(authListener)
        if (auth.currentUser != null) {
            this.currentUser = auth.currentUser!!
        }

        binding.btnConnect.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    //try to create new user profile
                    if (task.isSuccessful) {
                        currentUser = auth.currentUser!!
                        binding.tvCurrentUser.text = currentUser.email
                    } else {
                        //if email already used, try to connect automatically
                        val error = task.exception!!.message.toString()
                        Log.e("error", error)
                        if (error == USED_EMAIL_ERROR) {
                            signin(email, password)
                        }
                    }
                }
        }

        //disconnect user
        binding.btnDisconnect.setOnClickListener {
            auth.signOut()
        }

        //go to home screen
        binding.btnHomeScreen.setOnClickListener {
            startActivity(Intent(this@LoginActivity, HomeScreenActivity::class.java))
        }

        //ask for reset password
        binding.btnResetPassword.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val action = ActionCodeSettings.newBuilder()
                .setAndroidPackageName(packageName, true, null)
                .setHandleCodeInApp(true)
                .setUrl("")
                .build()
            auth.sendPasswordResetEmail(email, action)
                .addOnSuccessListener {
                    Log.e("reset password", "sent")
                }
                .addOnFailureListener {
                    Log.e("error", it.message.toString())
                }
        }

    }

    //connect user with email and password
    private fun signin(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    updateUI()
                } else {
                    val error = it.exception!!.message.toString()
                    Log.e("error", error)
                }
            }
    }

    //update ui according to connection status
    private fun updateUI() {
        if (auth.currentUser == null) {
            binding.tvCurrentUser.text = "user not logged"
            binding.btnDisconnect.visibility = View.GONE
            binding.btnHomeScreen.visibility = View.GONE
            binding.btnConnect.visibility = View.VISIBLE
            binding.btnResetPassword.visibility = View.VISIBLE
            binding.etEmail.visibility = View.VISIBLE
            binding.etPassword.visibility = View.VISIBLE
        } else {
            binding.tvCurrentUser.text = auth.currentUser!!.email
            binding.btnDisconnect.visibility = View.VISIBLE
            binding.btnHomeScreen.visibility = View.VISIBLE
            binding.btnConnect.visibility = View.GONE
            binding.btnResetPassword.visibility = View.GONE
            binding.etEmail.visibility = View.GONE
            binding.etPassword.visibility = View.GONE
        }
    }

    //listen to authentication status to update ui
    private var authListener = FirebaseAuth.AuthStateListener {
        try {
            updateUI()
            Log.e("onAuthStateChanged", it.currentUser!!.email.toString() + "")
        } catch (ex: Exception) {
        }
    }

}