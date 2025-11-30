package com.pashaoleynik97.droiddeploy.core.service

import com.pashaoleynik97.droiddeploy.core.domain.Application
import com.pashaoleynik97.droiddeploy.core.dto.application.CreateApplicationRequestDto
import com.pashaoleynik97.droiddeploy.core.dto.application.UpdateApplicationRequestDto
import java.util.UUID

interface ApplicationService {
    fun createApplication(request: CreateApplicationRequestDto): Application
    fun updateApplication(id: UUID, request: UpdateApplicationRequestDto): Application
}
