package com.sm.spring_boot_notes.controller

import com.sm.spring_boot_notes.database.model.Note
import com.sm.spring_boot_notes.database.repository.NoteRepository
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.bson.types.ObjectId
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
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
    )

    data class NoteResponse(
        val id: String?,
        val title: String,
        val content: String,
        val color: Long,
        val createdAt: Instant
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
                ownerId = ObjectId(ownerId)

            ))

        return note.toResponse()
    }

    @GetMapping
    fun findByOwnerId(): List<NoteResponse> {
        val ownerId = SecurityContextHolder.getContext().authentication?.principal as? String
        return noteRepository.findByOwnerId(ObjectId(ownerId)).map {
            it.toResponse()
        }
    }

    @DeleteMapping(path=["/{id}"])
    fun deleteById(@PathVariable("id") id: String) {
        val note = noteRepository.findById(ObjectId(id)).orElseThrow {
            IllegalArgumentException("Note not found")
        }

        val ownerId = SecurityContextHolder.getContext().authentication?.principal as? String

        if(note.ownerId.toHexString() == ownerId){
            noteRepository.deleteById(ObjectId(id))
        }
    }

//    @GetMapping
//    fun getAll(
//    ): List<NoteResponse> {
//        return repository.findAll() .map {
//            it.toResponse()
//        }
//    }

    private fun Note.toResponse(): NoteController.NoteResponse =
        NoteResponse(
            id = id.toHexString(),
            title = title,
            content = content,
            color = color,
            createdAt = createdAt,
        )

}