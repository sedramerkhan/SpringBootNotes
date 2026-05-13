package com.sm.spring_boot_notes.security

import com.sm.spring_boot_notes.database.model.RefreshToken
import com.sm.spring_boot_notes.database.model.User
import com.sm.spring_boot_notes.database.repository.RefreshTokenRepository
import com.sm.spring_boot_notes.database.repository.UserRepository
import org.bson.types.ObjectId
import org.springframework.http.HttpStatusCode
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.security.MessageDigest
import java.time.Instant
import java.util.Base64

@Service
class AuthService(
    private val jwtService: JwtService,
    private val userRepository: UserRepository,
    private val hashEncoder: HashEncoder,
    private val refreshTokenRepository: RefreshTokenRepository
) {

    data class TokenPair(
        val accessToken: String,
        val refreshToken: String
    )


    fun register(email: String, password: String): User {
        return userRepository.save(
            User(
                email = email,
                hashedPassword = hashEncoder.encode(password) ?: ""
            )
        )
    }


    fun login(email: String, password: String): TokenPair {
        val user = userRepository.findByEmail(email) ?: throw BadCredentialsException("Invalid Credentials.")

        if (!hashEncoder.matches(password, user.hashedPassword)) {
            throw BadCredentialsException("Invalid Credentials.")
        }

        val newAccessToken = jwtService.generateAccessToken(user.id.toHexString())
        val newRefreshToken = jwtService.generateRefreshToken(user.id.toHexString())

        storeRefreshToken(user.id, newRefreshToken)

        return TokenPair(newAccessToken, newRefreshToken)

    }

    /**
     *
     * Data Integrity (The "ACID" Concept):
     *     Atomicity:"All or Nothing","If saving a new token fails, the old one isn't deleted."
     *     Consistency:"Valid State",Data must follow all schema rules and constraints.
     *     Isolation:"No Interference","Concurrent requests won't see ""half-finished"" updates."
     *     Durability:"Permanent","Once a transaction is committed, it survives system crashes."
     *
     * When you mark a function as @Transactional, it follows the ACID properties. The most important one here is Atomicity.
     *
     * Success: If every line of code in the function finishes without an error, the database changes are Committed (saved permanently).
     *
     * Failure: If an exception is thrown anywhere (like your IllegalArgumentException), the database undergoes a Rollback.
     * Every database change made during that function call is undone as if it never happened.
     *
     *

     */
    @Transactional
    fun refresh(refreshToken: String): TokenPair {
        if (!jwtService.validateRefreshToken(refreshToken)) {
            throw ResponseStatusException(HttpStatusCode.valueOf(401),"Invalid Refresh Token.")
        }

        val userId = jwtService.getUserIdFromToken(refreshToken)
        val user = userRepository.findById(ObjectId(userId)).orElseThrow {
            ResponseStatusException(HttpStatusCode.valueOf(401),"Invalid Refresh Token.") //404
        }

        val hashed = hashToken(refreshToken)
        refreshTokenRepository.findByUserIdAndHashedToken(user.id, hashed)
            ?: throw  ResponseStatusException(HttpStatusCode.valueOf(401),"Refresh token not recognized (maybe used or expired?)")

        refreshTokenRepository.deleteByUserIdAndHashedToken(user.id, hashed)

        val newAccessToken = jwtService.generateAccessToken(userId)
        val newRefreshToken = jwtService.generateRefreshToken(userId)

        storeRefreshToken(user.id, newRefreshToken)

        return TokenPair(newAccessToken, newRefreshToken)

    }

    private fun storeRefreshToken(userId: ObjectId, rawRefreshToken: String) {
        val hashed = hashToken(rawRefreshToken)
        val expiryMS = jwtService.refreshTokenValidityMs
        val expiredAt = Instant.now().plusMillis(expiryMS)

        refreshTokenRepository.save(
            RefreshToken(
                userId = userId,
                expiresAt = expiredAt,
                hashedToken = hashed
            )
        )
    }

    private fun hashToken(token: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(token.encodeToByteArray())
        return Base64.getEncoder().encodeToString(hashBytes)
    }
}