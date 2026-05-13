package com.sm.spring_boot_notes.database.model

import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "refresh_tokens")
data class RefreshToken(
    val userId: ObjectId,
    @Indexed(expireAfter = "0s")
    val expiresAt: Instant,
    val hashedToken: String,
    val createdAt: Instant = Instant.now(),
    )


/**
 *
 * The TTL Mechanism (expireAfter = "0s")
 * Usually, indexes are just for speed. A TTL Index is a special index that MongoDB uses to automatically remove documents from a collection after a certain amount of time.
 *
 * How it works: MongoDB has a background process that reads this index and deletes documents when the current time is greater than the value in the expiresAt field.
 *
 * Why "0s"?: By setting the offset to zero, you are telling MongoDB: "Delete this document the exact second the clock hits the timestamp stored in expiresAt."
 */

