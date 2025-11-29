package com.pashaoleynik97.droiddeploy.rest.model.wrapper

import org.springframework.data.domain.Page

data class PagedResponse<out T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
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
