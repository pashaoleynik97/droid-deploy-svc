package com.pashaoleynik97.droiddeploy.core.service

import com.pashaoleynik97.droiddeploy.core.domain.Application
import com.pashaoleynik97.droiddeploy.core.dto.application.CreateApplicationRequestDto

interface ApplicationService {
    fun createApplication(request: CreateApplicationRequestDto): Application
}
