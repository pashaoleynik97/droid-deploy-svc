package com.pashaoleynik97.droiddeploy.db.repository

import com.pashaoleynik97.droiddeploy.core.domain.Application
import com.pashaoleynik97.droiddeploy.core.domain.ApplicationVersion
import com.pashaoleynik97.droiddeploy.core.repository.ApplicationRepository
import com.pashaoleynik97.droiddeploy.db.entity.ApplicationEntity
import com.pashaoleynik97.droiddeploy.db.entity.ApplicationVersionEntity
import mu.KotlinLogging
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import java.util.UUID

private val logger = KotlinLogging.logger {}

@Component
class ApplicationRepositoryImpl(
    private val jpaApplicationRepository: JpaApplicationRepository,
    private val jpaApplicationVersionRepository: JpaApplicationVersionRepository
) : ApplicationRepository {

    override fun save(application: Application): Application {
        logger.debug { "Saving application to database: name=${application.name}, id=${application.id}" }
        val entity = ApplicationEntity.fromDomain(application)
        val saved = jpaApplicationRepository.save(entity)
        logger.trace { "Application saved successfully: id=${saved.id}" }
        return saved.toDomain()
    }

    override fun findById(id: UUID): Application? {
        logger.trace { "Querying database for application by id: $id" }
        return jpaApplicationRepository.findByIdOrNull(id)?.toDomain()
    }

    override fun findByBundleId(bundleId: String): Application? {
        logger.trace { "Querying database for application by bundleId: $bundleId" }
        return jpaApplicationRepository.findByBundleId(bundleId)?.toDomain()
    }

    override fun existsByBundleId(bundleId: String): Boolean {
        logger.trace { "Checking existence of application with bundleId: $bundleId" }
        return jpaApplicationRepository.existsByBundleId(bundleId)
    }

    override fun hasVersions(applicationId: UUID): Boolean {
        logger.trace { "Checking if application has versions: $applicationId" }
        return jpaApplicationVersionRepository.existsByApplicationId(applicationId)
    }

    override fun findAll(pageable: Pageable): Page<Application> {
        logger.debug { "Querying database for applications: page=${pageable.pageNumber}, size=${pageable.pageSize}" }
        val result = jpaApplicationRepository.findAll(pageable)
        logger.trace { "Found ${result.totalElements} applications" }
        return result.map(ApplicationEntity::toDomain)
    }

    override fun deleteById(id: UUID) {
        logger.debug { "Deleting application from database: id=$id" }
        jpaApplicationRepository.deleteById(id)
        logger.trace { "Application deleted successfully: id=$id" }
    }

    override fun saveVersion(applicationVersion: ApplicationVersion): ApplicationVersion {
        logger.debug { "Saving application version to database: versionCode=${applicationVersion.versionCode}, applicationId=${applicationVersion.application.id}" }
        val applicationEntity = jpaApplicationRepository.findByIdOrNull(applicationVersion.application.id)
            ?: throw IllegalStateException("Application not found: ${applicationVersion.application.id}")
        val entity = ApplicationVersionEntity.fromDomain(applicationVersion, applicationEntity)
        val saved = jpaApplicationVersionRepository.save(entity)
        logger.trace { "Application version saved successfully: id=${saved.id}" }
        return saved.toDomain()
    }

    override fun findMaxVersionCode(applicationId: UUID): Int? {
        logger.trace { "Querying database for max version code: applicationId=$applicationId" }
        return jpaApplicationVersionRepository.findMaxVersionCodeByApplicationId(applicationId)?.toInt()
    }

    override fun versionExists(applicationId: UUID, versionCode: Int): Boolean {
        logger.trace { "Checking existence of version: applicationId=$applicationId, versionCode=$versionCode" }
        return jpaApplicationVersionRepository.existsByApplicationIdAndVersionCode(applicationId, versionCode.toLong())
    }
}
