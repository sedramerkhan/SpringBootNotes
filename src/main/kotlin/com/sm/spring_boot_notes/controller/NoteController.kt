package com.sm.spring_boot_notes.controller

import com.sm.spring_boot_notes.database.model.Note
import com.sm.spring_boot_notes.database.repository.NoteRepository
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.bson.types.ObjectId
import org.springframework.http.HttpStatus
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.time.Instant


// POST http:localhost:8080/notes
// GET http:localhost:8080/notes?ownerId=123
// DELETE http:localhost:8080/123
@RestController
@RequestMapping("/notes")
class NoteController(
    private val noteRepository: NoteRepository
) {


    data class NoteRequest(
        val id: String?,
        @field:NotBlank(message = "Title can't be blank.")
        val title: String,
        val content: String,
        val color: Long,
        val isImportant: Boolean? = null,
    )

    data class NoteResponse(
        val id: String?,
        val title: String,
        val content: String,
        val color: Long,
        val createdAt: Instant,
        val isImportant: Boolean,
    )


    @PostMapping
    fun save(@Valid @RequestBody body: NoteRequest): NoteResponse {
        val ownerId = SecurityContextHolder.getContext().authentication?.principal as? String

        println("owner id is $ownerId")
        val note = noteRepository.save(
            Note(
                id = body.id?.let { ObjectId(it) } ?: ObjectId.get(),
                title = body.title,
                content = body.content,
                color = body.color,
                createdAt = Instant.now(),
                ownerId = ObjectId(ownerId),
                isImportant = body.isImportant ?: false,
            ))

        return note.toResponse()
    }

    @GetMapping
    fun findByOwnerId(@RequestParam important: Boolean?): List<NoteResponse> {
        val ownerId = SecurityContextHolder.getContext().authentication?.principal as? String
        val notes = if (important == true) {
            noteRepository.findByOwnerIdAndIsImportant(ObjectId(ownerId), true)
        } else {
            noteRepository.findByOwnerId(ObjectId(ownerId))
        }
        return notes.map { it.toResponse() }
    }

    @GetMapping(path=["/{id}"])
    fun findById(@PathVariable("id") id: String): NoteResponse {
        val ownerId = SecurityContextHolder.getContext().authentication?.principal as? String
        val note = noteRepository.findById(ObjectId(id)).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Note not found")
        }
        if (note.ownerId.toHexString() != ownerId) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Note not found")
        }
        return note.toResponse()
    }

    @DeleteMapping(path=["/{id}"])
    fun deleteById(@PathVariable("id") id: String) {
        val note = noteRepository.findById(ObjectId(id)).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Note not found")
        }

        val ownerId = SecurityContextHolder.getContext().authentication?.principal as? String

        if (note.ownerId.toHexString() != ownerId) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "You don't own this note")
        }
        noteRepository.deleteById(ObjectId(id))
    }

//    @GetMapping
//    fun getAll(
//    ): List<NoteResponse> {
//        return repository.findAll() .map {
//            it.toResponse()
//        }
//    }

    private fun Note.toResponse() = NoteResponse(
        id = id.toHexString(),
        title = title,
        content = content,
        color = color,
        createdAt = createdAt,
        isImportant = isImportant,
    )

}