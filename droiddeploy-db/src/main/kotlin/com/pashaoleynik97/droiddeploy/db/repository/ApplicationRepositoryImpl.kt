package com.pashaoleynik97.droiddeploy.db.repository

import com.pashaoleynik97.droiddeploy.core.domain.Application
import com.pashaoleynik97.droiddeploy.core.repository.ApplicationRepository
import com.pashaoleynik97.droiddeploy.db.entity.ApplicationEntity
import mu.KotlinLogging
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import java.util.UUID

private val logger = KotlinLogging.logger {}

@Component
class ApplicationRepositoryImpl(
    private val jpaApplicationRepository: JpaApplicationRepository
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
}
