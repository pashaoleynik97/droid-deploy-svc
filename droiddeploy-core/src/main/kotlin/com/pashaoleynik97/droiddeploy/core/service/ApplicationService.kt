package com.pashaoleynik97.droiddeploy.core.service

import com.pashaoleynik97.droiddeploy.core.domain.Application
import com.pashaoleynik97.droiddeploy.core.dto.application.ApplicationResponseDto
import com.pashaoleynik97.droiddeploy.core.dto.application.CreateApplicationRequestDto
import com.pashaoleynik97.droiddeploy.core.dto.application.UpdateApplicationRequestDto
import com.pashaoleynik97.droiddeploy.core.dto.application.VersionDto
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.UUID

interface ApplicationService {
    fun createApplication(request: CreateApplicationRequestDto): Application
    fun updateApplication(id: UUID, request: UpdateApplicationRequestDto): Application
    fun listApplications(pageable: Pageable): Page<Application>
    fun getApplicationById(id: UUID): ApplicationResponseDto
    fun deleteApplication(id: UUID)
    fun uploadNewVersion(applicationId: UUID, apkContent: ByteArray): VersionDto
    fun updateVersionStability(applicationId: UUID, versionCode: Long, stable: Boolean): VersionDto
    fun deleteVersion(applicationId: UUID, versionCode: Long)
}
