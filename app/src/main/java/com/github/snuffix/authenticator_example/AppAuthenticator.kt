package com.github.snuffix.authenticator_example

import android.accounts.*
import android.accounts.AccountManager.KEY_BOOLEAN_RESULT
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import kotlinx.coroutines.runBlocking

class AppAuthenticator(private val context: Context) : AbstractAccountAuthenticator(context) {

    private val authRepository = AuthRepositoryImpl()

    @Throws(NetworkErrorException::class)
    override fun addAccount(response: AccountAuthenticatorResponse?, accountType: String?, authTokenType: String?, requiredFeatures: Array<out String>?, options: Bundle?): Bundle {

        val intent = Intent(context, AuthenticatorActivity::class.java)
        intent.putExtra(AuthenticatorActivity.ARG_ACCOUNT_TYPE, accountType)
        intent.putExtra(AuthenticatorActivity.ARG_AUTH_TYPE, authTokenType)
        intent.putExtra(AuthenticatorActivity.ARG_IS_ADDING_NEW_ACCOUNT, true)
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response)

        val bundle = Bundle()
        bundle.putParcelable(AccountManager.KEY_INTENT, intent)
        return bundle
    }

    @Throws(NetworkErrorException::class)
    override fun getAuthToken(response: AccountAuthenticatorResponse, account: Account, authTokenType: String, options: Bundle): Bundle {
        // If the caller requested an authToken type we don't support, then
        // return an error
        if (authTokenType != AccountConstants.AUTHTOKEN_TYPE_FULL_ACCESS && authTokenType != AccountConstants.AUTHTOKEN_TYPE_READ_ONLY) {
            val result = Bundle()
            result.putString(AccountManager.KEY_ERROR_MESSAGE, "invalid authTokenType")
            return result
        }

        // Extract the username and password from the Account Manager, and ask
        // the server for an appropriate AuthToken.
        val accountManager = AccountManager.get(context)

        var authToken = accountManager.peekAuthToken(account, authTokenType)

        // Lets give another try to authenticate the user
        if (TextUtils.isEmpty(authToken)) {
            val password = accountManager.getPassword(account)
            if (password != null) {
                try {
                    runBlocking {
                        authToken = authRepository.userSignIn(account.name, password, authTokenType)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }
        }

        // If we get an authToken - we return it
        if (!TextUtils.isEmpty(authToken)) {
            val result = Bundle()
            result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name)
            result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type)
            result.putString(AccountManager.KEY_AUTHTOKEN, authToken)
            return result
        }

        // If we get here, then we couldn't access the user's password - so we
        // need to re-prompt them for their credentials. We do that by creating
        // an intent to display our AuthenticatorActivity.
        val intent = Intent(context, AuthenticatorActivity::class.java)
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response)
        intent.putExtra(AuthenticatorActivity.ARG_ACCOUNT_TYPE, account.type)
        intent.putExtra(AuthenticatorActivity.ARG_AUTH_TYPE, authTokenType)
        intent.putExtra(AuthenticatorActivity.ARG_ACCOUNT_NAME, account.name)
        val bundle = Bundle()
        bundle.putParcelable(AccountManager.KEY_INTENT, intent)
        return bundle
    }


    @Throws(NetworkErrorException::class)
    override fun getAuthTokenLabel(authTokenType: String) = when (authTokenType) {
        AccountConstants.AUTHTOKEN_TYPE_FULL_ACCESS -> AccountConstants.AUTHTOKEN_TYPE_FULL_ACCESS_LABEL
        AccountConstants.AUTHTOKEN_TYPE_READ_ONLY -> AccountConstants.AUTHTOKEN_TYPE_READ_ONLY_LABEL
        else -> "$authTokenType (Label)"
    }

    @Throws(NetworkErrorException::class)
    override fun hasFeatures(response: AccountAuthenticatorResponse, account: Account, features: Array<String>): Bundle {
        val result = Bundle()
        result.putBoolean(KEY_BOOLEAN_RESULT, false)
        return result
    }

    override fun editProperties(response: AccountAuthenticatorResponse, accountType: String): Bundle? {
        return null
    }

    @Throws(NetworkErrorException::class)
    override fun confirmCredentials(response: AccountAuthenticatorResponse, account: Account, options: Bundle): Bundle? {
        return null
    }

    @Throws(NetworkErrorException::class)
    override fun updateCredentials(response: AccountAuthenticatorResponse, account: Account, authTokenType: String, options: Bundle): Bundle? {
        return null
    }
}
