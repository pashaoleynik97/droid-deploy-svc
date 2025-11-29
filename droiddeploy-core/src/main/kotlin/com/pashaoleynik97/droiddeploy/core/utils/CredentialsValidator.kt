package com.pashaoleynik97.droiddeploy.core.utils

object CredentialsValidator {

    private val passwordPattern = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{10,}$")

    private val loginPattern = Regex("^[a-zA-Z0-9_-]{3,20}$")

    fun isPasswordValid(password: String): Boolean {
        return password.matches(passwordPattern)
    }

    fun isLoginValid(login: String): Boolean {
        return login.matches(loginPattern)
    }

}