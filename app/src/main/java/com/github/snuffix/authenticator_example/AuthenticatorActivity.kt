package com.github.snuffix.authenticator_example

import android.accounts.Account
import android.accounts.AccountAuthenticatorActivity
import android.accounts.AccountManager
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.github.snuffix.myapplication.R
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.coroutines.*


/**
 * The Authenticator activity.
 *
 * Called by the Authenticator and in charge of identifing the user.
 *
 * It sends back to the Authenticator the result.
 */
class AuthenticatorActivity : AccountAuthenticatorActivity() {

    companion object {
        const val ARG_ACCOUNT_TYPE = "ACCOUNT_TYPE"
        const val ARG_AUTH_TYPE = "AUTH_TYPE"
        const val ARG_ACCOUNT_NAME = "ACCOUNT_NAME"
        const val ARG_IS_ADDING_NEW_ACCOUNT = "IS_ADDING_ACCOUNT"
        const val KEY_ERROR_MESSAGE = "ERR_MSG"
        const val ARG_USER_PASSWORD = "USER_PASS"
        const val REQUEST_SIGN_UP_CODE = 1
    }

    private val accountManager: AccountManager by lazy { AccountManager.get(this) }
    private val authRepository = AuthRepositoryImpl()

    private val accountName: String by lazy { intent.getStringExtra(ARG_ACCOUNT_NAME) }
    private val authorizationTokenType: String by lazy { intent.getStringExtra(ARG_AUTH_TYPE) ?: AccountConstants.AUTH_TOKEN_TYPE_FULL_ACCESS }

    private var loginJob: Job? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        accountEmailInput.setText(accountName)

        submitButton.setOnClickListener {
            loginJob = GlobalScope.launch {
                login()
            }
        }

        signUpButton.setOnClickListener {
            // Since there can only be one AuthenticatorActivity, we call the sign up activity, get his results,
            // and return them in setAccountAuthenticatorResult(). See finishLogin().
            val signupIntent = Intent(baseContext, SignUpActivity::class.java)
            signupIntent.putExtras(intent.extras!!)
            startActivityForResult(signupIntent, REQUEST_SIGN_UP_CODE)
        }
    }

    private suspend fun login() {
        val userEmail = accountEmailInput.text.toString()
        val userPass = accountPasswordInput.text.toString()

        val data = Bundle()

        try {
            val authToken = authRepository.userSignIn(userEmail, userPass, authorizationTokenType)

            data.putString(AccountManager.KEY_ACCOUNT_NAME, userEmail)
            data.putString(AccountManager.KEY_ACCOUNT_TYPE, intent.getStringExtra(ARG_ACCOUNT_TYPE))
            data.putString(AccountManager.KEY_AUTHTOKEN, authToken)
            data.putString(ARG_USER_PASSWORD, userPass)

        } catch (e: Exception) {
            data.putString(KEY_ERROR_MESSAGE, e.message)
        }

        val intent = Intent().apply {
            putExtras(data)
        }

        if (intent.hasExtra(KEY_ERROR_MESSAGE)) {
            withContext(Dispatchers.Main) {
                Toast.makeText(baseContext, intent.getStringExtra(KEY_ERROR_MESSAGE), Toast.LENGTH_SHORT).show()
            }
        } else {
            finishLogin(intent)
        }
    }

    private fun finishLogin(intent: Intent) {
        val accountName = intent.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
        val accountPassword = intent.getStringExtra(ARG_USER_PASSWORD)
        val account = Account(accountName, intent.getStringExtra(AccountManager.KEY_ACCOUNT_TYPE))

        if (getIntent().getBooleanExtra(ARG_IS_ADDING_NEW_ACCOUNT, false)) {
            val authToken = intent.getStringExtra(AccountManager.KEY_AUTHTOKEN)

            // Creating the account on the device and setting the auth token we got
            // (Not setting the auth token will cause another call to the server to authenticate the user)
            accountManager.addAccountExplicitly(account, accountPassword, null)
            accountManager.setAuthToken(account, authorizationTokenType, authToken)
        } else {
            accountManager.setPassword(account, accountPassword)
        }

        setAccountAuthenticatorResult(intent.extras)
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        // The sign up activity returned that the user has successfully created an account
        if (requestCode == REQUEST_SIGN_UP_CODE && resultCode == Activity.RESULT_OK) {
            data?.let {
                finishLogin(it)
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onStop() {
        super.onStop()
        loginJob?.cancel()
    }
}
