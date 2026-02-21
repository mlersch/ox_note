package org.oxoho.note.service

import org.oxoho.note.database.model.Note
import org.oxoho.note.database.repository.NoteRepository
import org.oxoho.note.dto.NoteRequest
import org.oxoho.note.dto.NoteResponse
import org.oxoho.note.exception.AccessDeniedException
import org.oxoho.note.exception.ResourceNotFoundException
import org.bson.types.ObjectId
import org.springframework.stereotype.Service
import java.time.Instant

/**
 * Note Service
 *
 * This service handles all note-related business logic:
 * - Creating new notes
 * - Updating existing notes
 * - Retrieving notes for a user
 * - Deleting notes (with ownership verification)
 *
 * Security:
 * - Users can only access their own notes
 * - Ownership is verified before deletion
 * - User ID comes from JWT token (can't be faked)
 */
@Service // Marks this as a Spring service component
class NoteService(
    private val noteRepository: NoteRepository  // Database access for notes
) {

    /**
     * Create a new note or update an existing one
     *
     * How it works:
     * - If request.id is null → Creates new note with auto-generated ID
     * - If request.id is provided → Updates note with that ID
     *
     * Note: This method doesn't verify ownership for updates!
     * In a production app, you'd want to check if userId owns the note before updating
     *
     * @param request Contains note data (id, title, content, color)
     * @param userId The logged-in user's ID (from JWT token)
     * @return NoteResponse with the created/updated note
     */
    fun createOrUpdateNote(request: NoteRequest, userId: String): NoteResponse {
        val note = noteRepository.save(
            Note(
                // If id provided, use it (update). Otherwise generate new ID (create)
                id = request.id?.let { ObjectId(it) } ?: ObjectId.get(),
                title = request.title,
                content = request.content,
                color = request.color,
                createdAt = Instant.now(),  // Always set to current time
                ownerId = ObjectId(userId)   // Link note to the logged-in user
            )
        )
        return note.toResponse()  // Convert database model to API response
    }

    /**
     * Get all notes belonging to a specific user
     *
     * Security: Only returns notes where ownerId matches the logged-in user
     * Users cannot see other people's notes
     *
     * @param userId The user ID (from JWT token)
     * @return List of NoteResponse objects (empty list if user has no notes)
     */
    fun findNotesByUserId(userId: String): List<NoteResponse> {
        return noteRepository.findByOwnerId(ObjectId(userId))
            .map { it.toResponse() }  // Convert each Note to NoteResponse
    }

    /**
     * Delete a note
     *
     * Security:
     * 1. Verifies note exists
     * 2. Verifies the logged-in user owns this note
     * 3. Only then deletes it
     *
     * This prevents users from deleting other people's notes
     * (even if they somehow get the note ID)
     *
     * @param noteId The ID of the note to delete
     * @param userId The logged-in user's ID (from JWT token)
     * @throws ResourceNotFoundException if note doesn't exist
     * @throws AccessDeniedException if user doesn't own the note
     */
    fun deleteNote(noteId: String, userId: String) {
        // Find the note (or throw error if doesn't exist)
        val note = noteRepository.findById(ObjectId(noteId))
            .orElseThrow { ResourceNotFoundException("Note not found") }

        // Check ownership: Does this note belong to the logged-in user?
        if (note.ownerId.toHexString() != userId) {
            throw AccessDeniedException("You don't have permission to delete this note")
        }

        // Only delete if user owns the note
        noteRepository.deleteById(ObjectId(noteId))
    }

    /**
     * Extension function to convert Note (database model) to NoteResponse (API response)
     *
     * Why separate models?
     * - Database model (Note) has ObjectId types and internal fields
     * - API response (NoteResponse) has String IDs and only user-facing fields
     * - Keeps API clean and hides database implementation details
     *
     * @return NoteResponse with data from this Note
     */
    private fun Note.toResponse() = NoteResponse(
        id = id.toHexString(),       // Convert ObjectId to String
        title = title,
        content = content,
        color = color,
        createdAt = createdAt
    )
}
