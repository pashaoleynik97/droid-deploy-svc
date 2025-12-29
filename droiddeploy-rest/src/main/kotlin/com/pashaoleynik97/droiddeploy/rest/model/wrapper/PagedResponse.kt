package com.pashaoleynik97.droiddeploy.rest.model.wrapper

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.data.domain.Page

@Schema(
    description = "Paginated response wrapper for list endpoints",
    example = """{"content": [...], "page": 0, "size": 20, "totalElements": 100, "totalPages": 5}"""
)
data class PagedResponse<out T>(
    @Schema(
        description = "List of items in the current page",
        example = "[...]"
    )
    val content: List<T>,

    @Schema(
        description = "Current page number (0-indexed)",
        example = "0"
    )
    val page: Int,

    @Schema(
        description = "Number of items per page",
        example = "20"
    )
    val size: Int,

    @Schema(
        description = "Total number of elements across all pages",
        example = "100"
    )
    val totalElements: Long,

    @Schema(
        description = "Total number of pages",
        example = "5"
    )
    val totalPages: Int
) {
    companion object {
        fun <T : Any, R : Any> from(page: Page<T>, transform: (T) -> R): PagedResponse<R> {
            return PagedResponse(
                content = page.content.map(transform),
                page = page.number,
                size = page.size,
                totalElements = page.totalElements,
                totalPages = page.totalPages
            )
        }
    }
}
