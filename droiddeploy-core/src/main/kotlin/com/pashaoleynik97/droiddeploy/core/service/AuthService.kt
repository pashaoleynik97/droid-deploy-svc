package com.pashaoleynik97.droiddeploy.core.service

import com.pashaoleynik97.droiddeploy.core.dto.auth.LoginRequestDto
import com.pashaoleynik97.droiddeploy.core.dto.auth.RefreshTokenRequestDto
import com.pashaoleynik97.droiddeploy.core.dto.auth.TokenPairDto

interface AuthService {
    fun login(request: LoginRequestDto): TokenPairDto
    fun refreshToken(request: RefreshTokenRequestDto): TokenPairDto
}