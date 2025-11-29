package com.pashaoleynik97.droiddeploy.core.exception

/**
 * Exception thrown when attempting to create a user with a login that already exists
 */
class LoginAlreadyExistsException(
    login: String
) : DroidDeployException("Login '$login' already exists")
