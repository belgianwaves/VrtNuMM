package com.bw.vrtnumm.androidApp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bw.vrtnumm.shared.Api
import com.bw.vrtnumm.shared.utils.DebugLog
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.ExperimentalTime

class EntryActivity : AppCompatActivity() {
    @ExperimentalTime
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "messages"
            val channel = NotificationChannel(channelId, "messages" , NotificationManager.IMPORTANCE_DEFAULT)
            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
        CheckEpisodes.scheduleDailyRequest(applicationContext)

        val self = Graph.repo.getCurrentUser()
        if (self != null) {
            val api = Api(
                self.email,
                self.password
            )

            if (BuildConfig.USE_FIREBASE) {
                val auth = Firebase.auth
                auth.signInWithEmailAndPassword(self.email, self.password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            DebugLog.i("successfully signed in firebase user")
                        } else {
                            DebugLog.e("failed signing in firebase user", task.exception)
                        }
                    }
            }

            initApp(api)
        } else {
            showLoginDialog()
        }

        DebugLog.i("build.model: ${Build.MODEL}")
    }

    @ExperimentalTime
    private fun showLoginDialog() {
        val prompt = LayoutInflater.from(this).inflate(R.layout.login, null)
        val email = prompt.findViewById<EditText>(R.id.login_email)
        val password = prompt.findViewById<EditText>(R.id.login_password)

        val dialog = AlertDialog.Builder(this, 0).apply {
            setTitle("VRT NU Login")
            setMessage("Please provide your VRT NU credentials to use this application")
            setView(prompt)
            setCancelable(false)
            setPositiveButton("Ok") { _, _ ->
                tryLogin(email.text.toString().trim(), password.text.toString().trim())
            }
            setNegativeButton("Cancel") { _, _ ->
                finish()
            }
        }.create()

        password.setOnEditorActionListener { _, actionId, _ ->
            return@setOnEditorActionListener when (actionId) {
                EditorInfo.IME_ACTION_GO -> {
                    tryLogin(email.text.toString().trim(), password.text.toString().trim())
                    dialog.dismiss()
                    true
                }
                else -> false
            }
        }

        dialog.show()
    }

    @ExperimentalTime
    private fun tryLogin(email: String, password: String) = lifecycleScope.launch {
        val api = Api(email, password)
        val success = withContext(Dispatchers.IO) { api.login() }
        if (!success) {
            showLoginDialog()
        } else {
            Graph.repo.createCurrentUser(email, password)
            if (BuildConfig.USE_FIREBASE) {
                val auth = Firebase.auth
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this@EntryActivity) { task ->
                        if (task.isSuccessful) {
                            DebugLog.i("successfully created firebase user")
                        } else {
                            DebugLog.e("failed creating firebase user", task.exception)
                        }
                    }
            }

            initApp(api)
        }
    }

    private fun initApp(api: Api) {
        Graph.init(api)

        val uri = intent.data

        val useFullCompose = !BuildConfig.USE_HYBRID // currently the 'full' compose version still has issues, probably due to bug in compose
        val intent = Intent(
            this,
            if (useFullCompose) MainActivityCompose::class.java else MainActivityHybrid::class.java
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            data = uri
        }
        startActivity(intent)
    }
}