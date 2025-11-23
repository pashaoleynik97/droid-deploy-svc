package com.pashaoleynik97.droiddeploy.rest.service

import com.pashaoleynik97.droiddeploy.rest.model.auth.LoginRequestDto
import com.pashaoleynik97.droiddeploy.rest.model.auth.RefreshTokenRequestDto
import com.pashaoleynik97.droiddeploy.rest.model.auth.TokenPairDto

interface AuthService {
    fun login(request: LoginRequestDto): TokenPairDto
    fun refreshToken(request: RefreshTokenRequestDto): TokenPairDto
}
