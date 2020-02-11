package com.github.snuffix.authenticator_example

import android.accounts.Account
import android.accounts.AccountManager
import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import com.github.snuffix.myapplication.R
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : Activity() {

    private val accountManager: AccountManager by lazy { AccountManager.get(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        addAccountButton.setOnClickListener { addNewAccount(AccountConstants.ACCOUNT_TYPE, AccountConstants.AUTH_TOKEN_TYPE_FULL_ACCESS) }

        getAuthTokenButton.setOnClickListener { showAccountPicker(AccountConstants.AUTH_TOKEN_TYPE_FULL_ACCESS, false) }

        getAuthTokenByFeaturesButton.setOnClickListener {
            getTokenForAccountCreateIfNeeded(AccountConstants.ACCOUNT_TYPE, AccountConstants.AUTH_TOKEN_TYPE_FULL_ACCESS)
        }

        invalidateAuthTokenButton.setOnClickListener { showAccountPicker(AccountConstants.AUTH_TOKEN_TYPE_FULL_ACCESS, true) }

        mockServerExceptionButton.setOnClickListener {
            Server.throwInvalidUserExceptionManually = true
        }
    }

    /**
     * Add new account to the account manager
     */
    private fun addNewAccount(accountType: String, authTokenType: String) {
        accountManager.addAccount(accountType, authTokenType, null, null, this, { future ->
            try {
                val result = future.result
                showMessage("Account was created")
            } catch (e: Exception) {
                e.printStackTrace()
                showMessage(e.message)
            }
        }, null)
    }

    /**
     * Show all the accounts registered on the account manager. Request an auth token upon user select.
     * @param authTokenType
     */
    private fun showAccountPicker(authTokenType: String, invalidate: Boolean) {
        val availableAccounts = accountManager.getAccountsByType(AccountConstants.ACCOUNT_TYPE)

        if (availableAccounts.isEmpty()) {
            Toast.makeText(this, "No accounts", Toast.LENGTH_SHORT).show()
        } else {
            val accountNames = availableAccounts.map { it.name }.toTypedArray()

            AlertDialog.Builder(this).setTitle("Pick Account").setAdapter(ArrayAdapter(baseContext, android.R.layout.simple_list_item_1, accountNames)) { dialog, which ->
                if (invalidate) {
                    invalidateAuthToken(availableAccounts[which], authTokenType)
                } else {
                    getExistingAccountAuthToken(availableAccounts[which], authTokenType)
                }
            }.create().show()
        }
    }

    /**
     * Get the auth token for an existing account on the AccountManager
     * @param account
     * @param authTokenType
     */
    private fun getExistingAccountAuthToken(account: Account, authTokenType: String) {
        accountManager.getAuthToken(account, authTokenType, null, this, { future ->
            try {
                val authToken = future.result.getString(AccountManager.KEY_AUTHTOKEN)

                if (authToken != null) {
                    showMessage("Token: $authToken")
                } else {
                    showMessage("Fail")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                showMessage(e.message)
            }
        }, null)
    }

    /**
     * Invalidates the auth token for the account
     * @param account
     * @param authTokenType
     */
    private fun invalidateAuthToken(account: Account, authTokenType: String) {
        accountManager.getAuthToken(account, authTokenType, null, this, { future ->
            try {
                val authToken = future.result.getString(AccountManager.KEY_AUTHTOKEN)
                accountManager.invalidateAuthToken(account.type, authToken) // Removes an auth token from the AccountManager's cache.
                showMessage(account.name + " invalidated")
            } catch (e: Exception) {
                e.printStackTrace()
                showMessage(e.message)
            }
        }, null)
    }

    /**
     * Get an auth token for the account.
     * If not exist - add it and then return its auth token.
     * If one exist - return its auth token.
     * If more than one exists - show a picker and return the select account's auth token.
     */
    private fun getTokenForAccountCreateIfNeeded(accountType: String, authTokenType: String) {
        accountManager.getAuthTokenByFeatures(
            accountType, authTokenType, null, this, null, null,
            { future ->
                try {
                    val authToken = future.result.getString(AccountManager.KEY_AUTHTOKEN)

                    if (authToken != null) {
                        showMessage("Token: $authToken")
                    } else {
                        showMessage("Fail")
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                    showMessage(e.message)
                }
            }, null
        )
    }

    private fun showMessage(msg: String?) {
        runOnUiThread { Toast.makeText(this, msg, Toast.LENGTH_SHORT).show() }
    }
}
