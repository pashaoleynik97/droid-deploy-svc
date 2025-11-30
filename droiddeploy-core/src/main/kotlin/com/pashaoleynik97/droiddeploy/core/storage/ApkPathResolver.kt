package com.pashaoleynik97.droiddeploy.core.storage

import java.nio.file.Path
import java.util.UUID

object ApkPathResolver {

    fun relativePath(applicationId: UUID, versionCode: Long): Path {
        return Path.of(
            "app",
            applicationId.toString(),
            "ver",
            versionCode.toString(),
            "base.apk"
        )
    }
}