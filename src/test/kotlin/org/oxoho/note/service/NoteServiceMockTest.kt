package org.oxoho.note.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.bson.types.ObjectId
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.oxoho.note.database.model.Note
import org.oxoho.note.database.repository.NoteRepository
import org.oxoho.note.dto.NoteRequest
import org.oxoho.note.exception.AccessDeniedException
import org.oxoho.note.exception.ResourceNotFoundException
import java.time.Instant
import java.util.*

/**
 * NoteService Unit Tests with Mocked Repository
 *
 * Why use mocks instead of real database?
 * - **Fast**: No database setup/teardown
 * - **Isolated**: Test only the service logic
 * - **Controlled**: We control exactly what the repository returns
 *
 * What is Mockito?
 * - A library that creates fake (mock) objects
 * - We tell it what to return when methods are called
 * - We verify that methods were called correctly
 *
 * This approach is perfect for unit testing service logic!
 */
class NoteServiceMockTest {

    // Create a mock repository (fake database)
    private val noteRepository: NoteRepository = mock()

    // Create real service with mocked repository
    private val noteService = NoteService(noteRepository)

    // Test data
    private val userId = ObjectId.get().toHexString()
    private val noteId = ObjectId.get()

    // ========================================
    // CREATE NOTE TESTS
    // ========================================

    @Test
    fun `should create new note when id is null`() {
        // Given: A new note request
        val request = NoteRequest(
            id = null,
            title = "Test Note",
            content = "Test Content",
            color = 0xFF5733
        )

        // Mock: When repository saves, return a note with generated ID
        whenever(noteRepository.save(any<Note>())).thenAnswer { invocation ->
            val note = invocation.getArgument<Note>(0)
            note.copy(id = ObjectId.get())
        }

        // When: Creating the note
        val response = noteService.createOrUpdateNote(request, userId)

        // Then: Should create note
        assertThat(response.id).isNotNull()
        assertThat(response.title).isEqualTo("Test Note")
        assertThat(response.content).isEqualTo("Test Content")

        // Verify repository.save was called once
        verify(noteRepository, times(1)).save(any<Note>())
    }

    @Test
    fun `should update existing note when id is provided`() {
        // Given: Update request with existing ID
        val existingId = ObjectId.get().toHexString()
        val request = NoteRequest(
            id = existingId,
            title = "Updated Title",
            content = "Updated Content",
            color = 0x00FF00
        )

        // Mock: Repository saves and returns updated note
        whenever(noteRepository.save(any<Note>())).thenAnswer { invocation ->
            invocation.getArgument<Note>(0)
        }

        // When: Updating the note
        val response = noteService.createOrUpdateNote(request, userId)

        // Then: Should update note
        assertThat(response.id).isEqualTo(existingId)
        assertThat(response.title).isEqualTo("Updated Title")

        // Verify save was called with correct ID
        verify(noteRepository).save(argThat { note ->
            note.id.toHexString() == existingId &&
            note.title == "Updated Title"
        })
    }

    // ========================================
    // READ NOTES TESTS
    // ========================================

    @Test
    fun `should return all notes for user`() {
        // Given: User has 2 notes in database
        val userObjectId = ObjectId(userId)
        val note1 = Note(
            id = ObjectId.get(),
            title = "Note 1",
            content = "Content 1",
            color = 0x123456,
            createdAt = Instant.now(),
            ownerId = userObjectId
        )
        val note2 = Note(
            id = ObjectId.get(),
            title = "Note 2",
            content = "Content 2",
            color = 0x789ABC,
            createdAt = Instant.now(),
            ownerId = userObjectId
        )

        // Mock: Repository returns user's notes
        whenever(noteRepository.findByOwnerId(userObjectId))
            .thenReturn(listOf(note1, note2))

        // When: Getting user's notes
        val notes = noteService.findNotesByUserId(userId)

        // Then: Should return 2 notes
        assertThat(notes).hasSize(2)
        assertThat(notes.map { it.title }).containsExactly("Note 1", "Note 2")

        // Verify repository was called correctly
        verify(noteRepository).findByOwnerId(userObjectId)
    }

    @Test
    fun `should return empty list when user has no notes`() {
        // Given: User has no notes
        val userObjectId = ObjectId(userId)
        whenever(noteRepository.findByOwnerId(userObjectId))
            .thenReturn(emptyList())

        // When: Getting user's notes
        val notes = noteService.findNotesByUserId(userId)

        // Then: Should return empty list
        assertThat(notes).isEmpty()
    }

    // ========================================
    // DELETE NOTE TESTS
    // ========================================

    @Test
    fun `should delete note when user owns it`() {
        // Given: Note exists and belongs to user
        val userObjectId = ObjectId(userId)
        val note = Note(
            id = noteId,
            title = "My Note",
            content = "My Content",
            color = 0xFFFFFF,
            createdAt = Instant.now(),
            ownerId = userObjectId
        )

        // Mock: Repository finds the note
        whenever(noteRepository.findById(noteId))
            .thenReturn(Optional.of(note))

        // When: Deleting the note
        noteService.deleteNote(noteId.toHexString(), userId)

        // Then: Repository should be called to delete
        verify(noteRepository).deleteById(noteId)
    }

    @Test
    fun `should throw AccessDeniedException when user does not own note`() {
        // Given: Note belongs to different user
        val differentUserId = ObjectId.get()
        val note = Note(
            id = noteId,
            title = "Someone Else's Note",
            content = "Not my content",
            color = 0x000000,
            createdAt = Instant.now(),
            ownerId = differentUserId  // Different owner!
        )

        // Mock: Repository finds the note
        whenever(noteRepository.findById(noteId))
            .thenReturn(Optional.of(note))

        // When & Then: Should throw AccessDeniedException
        assertThatThrownBy {
            noteService.deleteNote(noteId.toHexString(), userId)
        }
            .isInstanceOf(AccessDeniedException::class.java)
            .hasMessageContaining("permission")

        // Verify delete was NOT called
        verify(noteRepository, never()).deleteById(any<ObjectId>())
    }

    @Test
    fun `should throw ResourceNotFoundException when note does not exist`() {
        // Given: Note doesn't exist
        whenever(noteRepository.findById(noteId))
            .thenReturn(Optional.empty())

        // When & Then: Should throw ResourceNotFoundException
        assertThatThrownBy {
            noteService.deleteNote(noteId.toHexString(), userId)
        }
            .isInstanceOf(ResourceNotFoundException::class.java)
            .hasMessageContaining("not found")

        // Verify delete was NOT called
        verify(noteRepository, never()).deleteById(any<ObjectId>())
    }

    // ========================================
    // EDGE CASE TESTS
    // ========================================

    @Test
    fun `should handle empty content`() {
        // Given: Note with empty content
        val request = NoteRequest(
            id = null,
            title = "Title Only",
            content = "",
            color = 0x0000FF
        )

        whenever(noteRepository.save(any<Note>())).thenAnswer { invocation ->
            invocation.getArgument<Note>(0)
        }

        // When: Creating note
        val response = noteService.createOrUpdateNote(request, userId)

        // Then: Should accept empty content
        assertThat(response.content).isEmpty()
    }

    @Test
    fun `should preserve special characters`() {
        // Given: Note with special characters
        val request = NoteRequest(
            id = null,
            title = "Hello 世界 🌍",
            content = "Special \n\t \"'<>&",
            color = 0x123456
        )

        whenever(noteRepository.save(any<Note>())).thenAnswer { invocation ->
            invocation.getArgument<Note>(0)
        }

        // When: Creating note
        val response = noteService.createOrUpdateNote(request, userId)

        // Then: Should preserve all characters
        assertThat(response.title).isEqualTo("Hello 世界 🌍")
        assertThat(response.content).contains("\n", "\"", "'")
    }

    @Test
    fun `should convert ObjectId to hex string correctly`() {
        // Given: Note with specific ObjectId
        val specificId = ObjectId("507f1f77bcf86cd799439011")
        val note = Note(
            id = specificId,
            title = "Test",
            content = "Test",
            color = 0x000000,
            createdAt = Instant.now(),
            ownerId = ObjectId(userId)
        )

        whenever(noteRepository.save(any<Note>())).thenReturn(note)

        // When: Creating note
        val request = NoteRequest(null, "Test", "Test", 0x000000)
        val response = noteService.createOrUpdateNote(request, userId)

        // Then: ID should be hex string
        assertThat(response.id).isEqualTo("507f1f77bcf86cd799439011")
    }
}
