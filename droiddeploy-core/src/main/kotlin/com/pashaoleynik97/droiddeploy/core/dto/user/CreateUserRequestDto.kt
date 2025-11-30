package com.pashaoleynik97.droiddeploy.core.dto.user

data class CreateUserRequestDto(
    val login: String,
    val password: String,
    val role: String  // "ADMIN" or "CI"
)