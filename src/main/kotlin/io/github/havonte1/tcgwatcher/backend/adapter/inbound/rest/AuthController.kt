package io.github.havonte1.tcgwatcher.backend.adapter.inbound.rest

import io.github.havonte1.tcgwatcher.backend.application.AuthResponse
import io.github.havonte1.tcgwatcher.backend.application.AuthenticationService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
class AuthController(
    private val authenticationService: AuthenticationService,
) {
    private val logger = KotlinLogging.logger {}

    @PostMapping("/register")
    fun register(@RequestBody request: RegisterRequest): ResponseEntity<AuthResponse> {
        logger.info { "Register request for email: ${request.email}" }
        return try {
            val user = authenticationService.register(request.email, request.password)
            val authResponse = authenticationService.generateTokens(user)
            ResponseEntity.ok(authResponse)
        } catch (e: IllegalArgumentException) {
            logger.warn { "Registration failed: ${e.message}" }
            ResponseEntity.status(HttpStatus.BAD_REQUEST).build()
        }
    }

    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest): ResponseEntity<AuthResponse> {
        logger.info { "Login request for email: ${request.email}" }
        return try {
            val user = authenticationService.authenticate(request.email, request.password)
            val authResponse = authenticationService.generateTokens(user)
            ResponseEntity.ok(authResponse)
        } catch (e: IllegalArgumentException) {
            logger.warn { "Login failed: ${e.message}" }
            ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }
    }

    @PostMapping("/refresh")
    fun refresh(@RequestBody request: RefreshRequest): ResponseEntity<AuthResponse> {
        return try {
            val authResponse = authenticationService.refreshToken(request.refreshToken)
            ResponseEntity.ok(authResponse)
        } catch (e: IllegalArgumentException) {
            logger.warn { "Token refresh failed: ${e.message}" }
            ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }
    }

    @PostMapping("/logout")
    fun logout(@RequestBody request: LogoutRequest, authentication: Authentication): ResponseEntity<Unit> {
        val userId = authentication.principal as Long
        authenticationService.logout(userId, request.refreshToken)
        return ResponseEntity.ok().build()
    }
}

data class RegisterRequest(
    val email: String,
    val password: String,
)

data class LoginRequest(
    val email: String,
    val password: String,
)

data class RefreshRequest(
    val refreshToken: String,
)

data class LogoutRequest(
    val refreshToken: String,
)
