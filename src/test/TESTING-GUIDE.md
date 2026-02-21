# Testing Guide for Notes Application

## Overview

This guide explains the testing approach used in the Notes application.

## Testing Strategy

We use **unit tests with mocked dependencies** rather than integration tests with real/embedded databases because:

1. **Speed**: Tests run in milliseconds, not seconds
2. **Reliability**: No external dependencies (no database setup required)
3. **Isolation**: Each test is completely independent
4. **Focus**: Tests only the service logic, not database behavior

## Test Structure

### NoteServiceMockTest

Location: `src/test/kotlin/org/oxoho/note/service/NoteServiceMockTest.kt`

This test class covers all `NoteService` functionality:

#### Create Operations
- ✅ Creating new notes (with null ID)
- ✅ Updating existing notes (with provided ID)
- ✅ Handling empty content
- ✅ Preserving special characters

#### Read Operations
- ✅ Retrieving all notes for a user
- ✅ Returning empty list when user has no notes

#### Delete Operations
- ✅ Deleting notes when user owns them
- ✅ Throwing `AccessDeniedException` when user doesn't own note
- ✅ Throwing `ResourceNotFoundException` when note doesn't exist

#### Edge Cases
- ✅ ObjectId to hex string conversion
- ✅ Special character preservation
- ✅ Empty content handling

## How Mocking Works

### What is Mockito?

Mockito is a testing framework that creates **fake objects** (mocks) that simulate real dependencies.

```kotlin
// Create a fake repository (no real database)
private val noteRepository: NoteRepository = mock()

// Tell the mock what to return when methods are called
whenever(noteRepository.findById(noteId))
    .thenReturn(Optional.of(note))

// Verify methods were called correctly
verify(noteRepository).deleteById(noteId)
```

### Benefits of Mocking

1. **Control**: We decide exactly what the repository returns
2. **Speed**: No database I/O operations
3. **Predictability**: Same results every time
4. **Verification**: We can verify method calls and arguments

## Running Tests

### Run all tests
```bash
./gradlew test
```

### Run specific test class
```bash
./gradlew test --tests "org.oxoho.note.service.NoteServiceMockTest"
```

### Run specific test method
```bash
./gradlew test --tests "org.oxoho.note.service.NoteServiceMockTest.should create new note when id is null"
```

## Test Coverage

Current test coverage for `NoteService`:

| Method | Coverage |
|--------|----------|
| `createOrUpdateNote()` | ✅ 100% |
| `findNotesByUserId()` | ✅ 100% |
| `deleteNote()` | ✅ 100% |

## Writing New Tests

### Template for a New Test

```kotlin
@Test
fun `should do something when condition happens`() {
    // Given: Setup test data
    val request = NoteRequest(...)

    // Mock: Define what repository should return
    whenever(noteRepository.save(any<Note>())).thenReturn(...)

    // When: Execute the method being tested
    val result = noteService.createOrUpdateNote(request, userId)

    // Then: Verify the result
    assertThat(result.title).isEqualTo("Expected Title")

    // Verify: Check repository was called correctly
    verify(noteRepository).save(any<Note>())
}
```

### Naming Convention

Tests use the format: **"should [expected behavior] when [condition]"**

Examples:
- `should create new note when id is null`
- `should throw AccessDeniedException when user does not own note`
- `should return empty list when user has no notes`

## Key Testing Concepts

### AAA Pattern

All tests follow the **Arrange-Act-Assert** pattern:

1. **Arrange** (Given): Set up test data and mocks
2. **Act** (When): Execute the method being tested
3. **Assert** (Then): Verify the results

### Verification

We verify two things:
1. **Return values**: Does the method return the correct data?
2. **Behavior**: Was the repository called with the right arguments?

```kotlin
// Verify return value
assertThat(response.title).isEqualTo("Expected")

// Verify behavior
verify(noteRepository).save(argThat { note ->
    note.title == "Expected"
})
```

## Dependencies

### Test Dependencies

```kotlin
// Mockito Kotlin - Better Kotlin support for Mockito
testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")

// Spring Boot Test - Includes JUnit, Mockito, AssertJ
testImplementation("org.springframework.boot:spring-boot-starter-test")
```

### Why Mockito Kotlin?

Regular Mockito has issues with Kotlin's non-nullable types. Mockito Kotlin provides:
- `mock()` - Creates mocks that work with Kotlin
- `whenever()` - More Kotlin-friendly than `when()`
- `any()` - Works with Kotlin's type system
- `argThat()` - Type-safe argument matchers

## Common Assertions

### AssertJ Assertions

```kotlin
// Equality
assertThat(value).isEqualTo(expected)

// Collections
assertThat(list).hasSize(3)
assertThat(list).isEmpty()
assertThat(list).containsExactly("a", "b", "c")

// Exceptions
assertThatThrownBy { service.deleteNote(...) }
    .isInstanceOf(AccessDeniedException::class.java)
    .hasMessageContaining("permission")

// Nullability
assertThat(value).isNotNull()
assertThat(value).isNull()
```

## Best Practices

1. **One assertion per test**: Focus on testing one thing
2. **Clear test names**: Describe what's being tested
3. **Arrange-Act-Assert**: Follow the AAA pattern
4. **Mock only dependencies**: Don't mock the class being tested
5. **Verify behavior**: Check that methods were called correctly
6. **Use test data builders**: Create reusable test data
7. **Clean up**: Tests should be independent

## FAQ

### Q: Why not use a real database for tests?

**A**: Real databases are slow and require setup. Mocks are fast and don't need external dependencies.

### Q: Don't we need integration tests?

**A**: Yes, but for different purposes:
- **Unit tests** (with mocks): Test business logic
- **Integration tests**: Test database queries and API endpoints

For now, we focus on unit tests since they provide the most value for development speed.

### Q: How do I know if my mocks are realistic?

**A**: Good question! That's why integration tests exist. Unit tests verify logic, integration tests verify the whole system works together.

### Q: Can I mix mocking and real databases?

**A**: Yes! Some teams use embedded MongoDB for integration tests and mocks for unit tests. We chose mocks for simplicity and speed.

## Next Steps

To add integration tests with embedded MongoDB:

1. Add dependency: `de.flapdoodle.embed:de.flapdoodle.embed.mongo.spring3x`
2. Use `@SpringBootTest` annotation
3. Test actual database operations
4. Keep these separate from unit tests (different package or suffix)

## Resources

- [Mockito Documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
- [Mockito Kotlin](https://github.com/mockito/mockito-kotlin)
- [AssertJ](https://assertj.github.io/doc/)
- [JUnit 5](https://junit.org/junit5/docs/current/user-guide/)
