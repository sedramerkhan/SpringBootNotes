package com.sm.spring_boot_notes.security

import jakarta.servlet.DispatcherType
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.HttpStatusEntryPoint
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter


@Configuration
class SecurityConfig(
    private val jwtAuthFilter: JwtAuthFilter
) {

    @Bean
    fun filterChain(httpSecurity: HttpSecurity): SecurityFilterChain {
        return httpSecurity
            .csrf { it.disable() }
            .sessionManagement {
                it.sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS
                )
            }.authorizeHttpRequests { auth ->
                auth.requestMatchers("/auth/**").permitAll().dispatcherTypeMatchers(
                    DispatcherType.ERROR,
                    DispatcherType.FORWARD
                ).permitAll().anyRequest().authenticated()
            }.exceptionHandling { configurer ->
               configurer.authenticationEntryPoint (HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
            }
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter::class.java)
            .build()
    }
}

/**
 * Security Configuration Summary
 * Incoming Request: A request hits your backend (e.g., GET /notes).
 *
 * JWT Filter: Your jwtAuthFilter checks the Authorization header.
 *
 * If valid: Security context is populated with user details.
 *
 * If missing/invalid: The request continues as "anonymous."
 *
 * Authorization Manager: The request hits the rules you defined:
 *
 * Is it /auth/? → Pass.
 *
 * Is it an internal ERROR? → Pass.
 *
 * Is it anything else? → Check authentication.
 *
 * Final Outcome:
 *
 * If the JWT filter succeeded, the user sees their notes.
 *
 * If the JWT filter was missing or failed, the authenticationEntryPoint triggers and sends a 401 back to the mobile app.
 */