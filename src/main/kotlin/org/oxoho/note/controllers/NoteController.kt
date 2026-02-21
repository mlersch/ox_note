package org.oxoho.note.controllers

import org.oxoho.note.dto.NoteRequest
import org.oxoho.note.dto.NoteResponse
import org.oxoho.note.service.NoteService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

/**
 * REST Controller for managing user notes
 *
 * This controller handles all note-related operations:
 * - Creating new notes
 * - Updating existing notes
 * - Retrieving all notes for logged-in user
 * - Deleting notes
 *
 * All endpoints require authentication (must be logged in)
 * The user ID is automatically extracted from the JWT token via @AuthenticationPrincipal
 */
@RestController
@RequestMapping("/api/v1/notes") // All endpoints start with /api/v1/notes
class NoteController(
    // Spring automatically injects NoteService instance
    private val noteService: NoteService
) {

    /**
     * Create a new note or update an existing one
     *
     * Endpoint: POST /api/v1/notes
     *
     * How it works:
     * - If request.id is null → Creates a brand new note
     * - If request.id is provided → Updates the existing note with that ID
     *
     * Security:
     * - Only authenticated users can create notes
     * - Notes are automatically linked to the logged-in user
     *
     * @param request Contains note details (id, title, content, color)
     * @param userId Automatically extracted from JWT token (the logged-in user's ID)
     * @return NoteResponse with the created/updated note details
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED) // Returns 201 CREATED status
    fun createOrUpdateNote(
        @Valid @RequestBody request: NoteRequest, // @Valid checks that title is not blank
        @AuthenticationPrincipal userId: String   // Spring Security fills this with user ID from JWT
    ): NoteResponse {
        return noteService.createOrUpdateNote(request, userId)
    }

    /**
     * Get all notes belonging to the logged-in user
     *
     * Endpoint: GET /api/v1/notes
     *
     * What it does:
     * - Retrieves all notes where ownerId matches the logged-in user
     * - Returns them as a list (empty list if user has no notes)
     *
     * Security:
     * - Users can ONLY see their own notes
     * - User ID comes from JWT token, so it can't be faked
     *
     * @param userId Automatically extracted from JWT token
     * @return List of all notes belonging to this user
     */
    @GetMapping
    fun getNotes(@AuthenticationPrincipal userId: String): List<NoteResponse> {
        return noteService.findNotesByUserId(userId)
    }

    /**
     * Delete a specific note
     *
     * Endpoint: DELETE /api/v1/notes/{id}
     *
     * What it does:
     * 1. Finds the note by ID
     * 2. Verifies the logged-in user owns this note
     * 3. Deletes it from the database
     *
     * Security:
     * - Users can ONLY delete their own notes
     * - Attempting to delete someone else's note throws AccessDeniedException
     *
     * @param id The note ID from the URL path (e.g., /notes/507f1f77bcf86cd799439011)
     * @param userId Automatically extracted from JWT token
     * @throws ResourceNotFoundException if note doesn't exist
     * @throws AccessDeniedException if user doesn't own this note
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT) // Returns 204 NO CONTENT on success
    fun deleteNote(
        @PathVariable id: String,              // Extracts {id} from URL path
        @AuthenticationPrincipal userId: String
    ) {
        noteService.deleteNote(id, userId)
    }
}