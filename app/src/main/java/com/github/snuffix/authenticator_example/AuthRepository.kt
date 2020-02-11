package com.github.snuffix.authenticator_example

interface AuthRepository {
    suspend fun userSignUp(name: String, email: String, pass: String, authType: String): String
    suspend fun userSignIn(user: String, pass: String, authType: String): String
}
