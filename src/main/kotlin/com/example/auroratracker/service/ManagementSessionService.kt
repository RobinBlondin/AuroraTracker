package com.example.auroratracker.service

import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.util.UUID

@Service
class ManagementSessionService {
    fun establish(request: HttpServletRequest, userId: String) {
        request.getSession(false)?.invalidate()
        request.getSession(true).apply {
            maxInactiveInterval = 1800
            setAttribute("managementUserId", userId)
            setAttribute("managementExpiresAt", Instant.now().plusSeconds(1800))
            setAttribute("managementCsrf", UUID.randomUUID().toString())
        }
    }

    fun requireUser(request: HttpServletRequest, mutation: Boolean = false): String {
        val session = request.getSession(false)
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED)
        val expires = session.getAttribute("managementExpiresAt") as? Instant
        val userId = session.getAttribute("managementUserId") as? String
        if (expires == null || !Instant.now().isBefore(expires) || userId == null) {
            session.invalidate()
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED)
        }
        if (mutation && session.getAttribute("managementCsrf") != request.getHeader("X-CSRF-Token")) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN)
        }
        return userId
    }
}
