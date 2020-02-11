package com.github.snuffix.authenticator_example

import kotlinx.coroutines.delay
import java.lang.Exception
import java.util.*

class AuthRepositoryImpl : AuthRepository {
    override suspend fun userSignUp(name: String, email: String, pass: String, authType: String): String {
        return Server.userSignUp(name, email, pass, authType)
    }

    override suspend fun userSignIn(name: String, pass: String, authType: String): String {
        return Server.userSignIn(name, pass, authType)
    }
}

object Server {
    var throwInvalidUserExceptionManually = false

    private val users = List(10) {
        User(name = "test$it", email = "test$it@gmail.com", password = "test$it", token = UUID.randomUUID().toString())
    }.toMutableList()

    suspend fun userSignUp(name: String, email: String, pass: String, authType: String): String {
        delay(2000)

        if (users.indexOfFirst { it.email == email } == -1) {
            val newUser = User(name = name, email = email, password = pass, token = UUID.randomUUID().toString())
            return newUser.token
        } else {
            throw UserExistsException()
        }
    }

    suspend fun userSignIn(email: String, pass: String, authType: String): String {
        delay(2000)

        if (throwInvalidUserExceptionManually) {
            throwInvalidUserExceptionManually = false
            throw InvalidUserException()
        } else {
            return users.firstOrNull { it.email == email && it.password == pass }?.token ?: throw InvalidUserException()
        }
    }
}

class UserExistsException : Exception("User already exists")
class InvalidUserException : Exception("Invalid user")

data class User(val name: String, val email: String, val password: String, val token: String)
