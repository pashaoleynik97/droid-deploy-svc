package com.pashaoleynik97.droiddeploy.rest.model.user

data class CreateUserRequestDto(
    val login: String,
    val password: String,
    val role: String  // "ADMIN" or "CI"
)
