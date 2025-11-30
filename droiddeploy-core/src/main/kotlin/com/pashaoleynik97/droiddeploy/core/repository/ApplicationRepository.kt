package com.pashaoleynik97.droiddeploy.core.repository

import com.pashaoleynik97.droiddeploy.core.domain.Application
import com.pashaoleynik97.droiddeploy.core.domain.ApplicationVersion
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.UUID

interface ApplicationRepository {
    fun save(application: Application): Application
    fun findById(id: UUID): Application?
    fun findByBundleId(bundleId: String): Application?
    fun existsByBundleId(bundleId: String): Boolean
    fun hasVersions(applicationId: UUID): Boolean
    fun findAll(pageable: Pageable): Page<Application>
    fun deleteById(id: UUID)

    // ApplicationVersion operations
    fun saveVersion(applicationVersion: ApplicationVersion): ApplicationVersion
    fun findMaxVersionCode(applicationId: UUID): Int?
    fun versionExists(applicationId: UUID, versionCode: Int): Boolean
}
