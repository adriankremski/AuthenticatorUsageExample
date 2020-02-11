package com.github.snuffix.authenticator_example

import android.app.Service
import android.content.Intent
import android.os.IBinder

class AppAuthenticatorService : Service() {
    override fun onBind(intent: Intent): IBinder? = AppAuthenticator(
        this
    ).iBinder
}
