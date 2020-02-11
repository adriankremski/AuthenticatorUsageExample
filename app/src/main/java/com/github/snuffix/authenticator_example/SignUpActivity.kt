package com.github.snuffix.authenticator_example

import android.accounts.AccountManager
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope

import com.github.snuffix.myapplication.R
import kotlinx.android.synthetic.main.activity_signup.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * In charge of the Sign up process. Since it's not an AuthenticatorActivity decendent,
 * it returns the result back to the calling activity, which is an AuthenticatorActivity,
 * and it return the result back to the Authenticator
 */
class SignUpActivity : AppCompatActivity() {

    private val accountType: String by lazy { intent.getStringExtra(AuthenticatorActivity.ARG_ACCOUNT_TYPE) }
    private val authRepository = AuthRepositoryImpl()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_signup)

        alreadyMemberButton.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }

        submitButton.setOnClickListener {
            lifecycleScope.launch {
                createAccount()
            }
        }
    }

    private suspend fun createAccount() {
        val name = accountNameInput.text.toString().trim()
        val accountEmail = accountEmailInput.text.toString().trim()
        val accountPassword = accountPasswordInput.text.toString().trim()

        val data = Bundle()

        try {
            val authToken = authRepository.userSignUp(name, accountEmail, accountPassword, AccountConstants.AUTH_TOKEN_TYPE_FULL_ACCESS)

            data.putString(AccountManager.KEY_ACCOUNT_NAME, accountEmail)
            data.putString(AccountManager.KEY_ACCOUNT_TYPE, accountType)
            data.putString(AccountManager.KEY_AUTHTOKEN, authToken)
            data.putString(AuthenticatorActivity.ARG_USER_PASSWORD, accountPassword)
        } catch (e: Exception) {
            data.putString(AuthenticatorActivity.KEY_ERROR_MESSAGE, e.message)
        }

        val intent = Intent().apply {
            putExtras(data)
        }

        if (intent.hasExtra(AuthenticatorActivity.KEY_ERROR_MESSAGE)) {
            withContext(Dispatchers.Main) {
                Toast.makeText(baseContext, intent.getStringExtra(AuthenticatorActivity.KEY_ERROR_MESSAGE), Toast.LENGTH_SHORT).show()
            }
        } else {
            setResult(RESULT_OK, intent)
            finish()
        }
    }

    override fun onBackPressed() {
        setResult(RESULT_CANCELED)
        super.onBackPressed()
    }
}
