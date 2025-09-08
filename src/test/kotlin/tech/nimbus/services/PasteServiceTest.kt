package tech.nimbus.services

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.*
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import tech.nimbus.database.repositories.PasteWithAuthor
import tech.nimbus.database.repositories.PasteSortOrder
import tech.nimbus.database.repositories.interfaces.IPasteRepository
import tech.nimbus.exceptions.*
import tech.nimbus.models.Paste
import tech.nimbus.models.PasteVisibility
import tech.nimbus.models.User
import tech.nimbus.shared.dto.PasteVisibility as SharedPasteVisibility
import tech.nimbus.shared.dto.request.CreatePasteRequestDto
import java.time.LocalDateTime

/**
 * Unit тесты для PasteService.
 * Использует моки для изоляции бизнес-логики от слоя данных.
 */
@DisplayName("PasteService Tests")
class PasteServiceTest {

    private lateinit var mockRepository: IPasteRepository
    private lateinit var pasteService: PasteService

    @BeforeEach
    fun setUp() {
        mockRepository = mock(IPasteRepository::class.java)
        pasteService = PasteService(mockRepository)
    }

    @Nested
    @DisplayName("Get Public Pastes")
    inner class GetPublicPastesTest {

        @Test
        @DisplayName("Should return formatted pastes when repository returns data")
        fun shouldReturnFormattedPastesWhenRepositoryReturnsData() = runTest {
            // Arrange
            val testUser = User(
                id = "user1",
                username = "testuser",
                displayName = "Test User",
                email = "test@example.com",
                passwordHash = "hash",
                createdAt = LocalDateTime.now().toString()
            )

            val testPaste = Paste(
                id = "testPaste123", // 12 символов
                title = "Test Paste",
                content = "Test content",
                userId = "user1",
                visibility = PasteVisibility.PUBLIC,
                createdAt = LocalDateTime.now().toString(),
                syntaxLanguage = "kotlin"
            )

            val pasteWithAuthor = PasteWithAuthor(testPaste, testUser)

            whenever(mockRepository.getPublicPastesWithAuthors(any(), any(), any()))
                .thenReturn(listOf(pasteWithAuthor))

            // Act
            val result = pasteService.getPublicPastes(20, 0, PasteSortOrder.NEWEST_FIRST)

            // Assert
            assertEquals(1, result.size)
            assertEquals("testPaste123", result[0].id)
            assertEquals("Test Paste", result[0].title)
            assertEquals("testuser", result[0].authorUsername)
            assertEquals("Test User", result[0].authorDisplayName)

            verify(mockRepository).getPublicPastesWithAuthors(20, 0, PasteSortOrder.NEWEST_FIRST)
        }

        @Test
        @DisplayName("Should throw PaginationException for invalid limit")
        fun shouldThrowPaginationExceptionForInvalidLimit() = runTest {
            // Act & Assert
            assertThrows<PaginationException.InvalidLimit> {
                pasteService.getPublicPastes(101, 0, PasteSortOrder.NEWEST_FIRST)
            }
        }

        @Test
        @DisplayName("Should throw PaginationException for negative offset")
        fun shouldThrowPaginationExceptionForNegativeOffset() = runTest {
            // Act & Assert
            assertThrows<PaginationException.InvalidOffset> {
                pasteService.getPublicPastes(20, -1, PasteSortOrder.NEWEST_FIRST)
            }
        }

        @Test
        @DisplayName("Should throw DatabaseException when repository fails")
        fun shouldThrowDatabaseExceptionWhenRepositoryFails() = runTest {
            // Arrange
            whenever(mockRepository.getPublicPastesWithAuthors(any(), any(), any()))
                .thenThrow(RuntimeException("Database connection failed"))

            // Act & Assert
            assertThrows<DatabaseException.QueryFailed> {
                pasteService.getPublicPastes(20, 0, PasteSortOrder.NEWEST_FIRST)
            }
        }
    }

    @Nested
    @DisplayName("Get Paste By ID")
    inner class GetPasteByIdTest {

        @Test
        @DisplayName("Should return paste when access is allowed")
        fun shouldReturnPasteWhenAccessIsAllowed() = runTest {
            // Arrange
            val testPaste = Paste(
                id = "validPaste12", // 12 символов
                title = "Test Paste",
                content = "Content",
                userId = "user1",
                visibility = PasteVisibility.PUBLIC,
                createdAt = LocalDateTime.now().toString(),
                syntaxLanguage = "plaintext"
            )

            val pasteWithAuthor = PasteWithAuthor(testPaste, null)

            whenever(mockRepository.canAccessPaste("validPaste12", "user1"))
                .thenReturn(true)
            whenever(mockRepository.getPasteWithAuthor("validPaste12"))
                .thenReturn(pasteWithAuthor)
            whenever(mockRepository.incrementViewCount("validPaste12"))
                .thenReturn(Unit)

            // Act
            val result = pasteService.getPasteById("validPaste12", "user1")

            // Assert
            assertEquals("validPaste12", result?.id)
            assertEquals("Test Paste", result?.title)
            assertEquals("plaintext", result?.syntaxLanguage)

            verify(mockRepository).canAccessPaste("validPaste12", "user1")
            verify(mockRepository).getPasteWithAuthor("validPaste12")
            verify(mockRepository).incrementViewCount("validPaste12")
        }

        @Test
        @DisplayName("Should return null when access is not allowed")
        fun shouldReturnNullWhenAccessIsNotAllowed() = runTest {
            // Arrange
            whenever(mockRepository.canAccessPaste("privatePas12", "user2"))
                .thenReturn(false)

            // Act
            val result = pasteService.getPasteById("privatePas12", "user2")

            // Assert
            assertNull(result, "Should return null when access is denied")
        }

        @Test
        @DisplayName("Should throw InvalidPasteId for invalid ID format")
        fun shouldThrowInvalidPasteIdForInvalidIdFormat() = runTest {
            // Act & Assert
            assertThrows<PasteException.InvalidPasteId> {
                pasteService.getPasteById("invalid", "user1")
            }
        }

        @Test
        @DisplayName("Should return null when paste doesn't exist")
        fun shouldReturnNullWhenPasteDoesntExist() = runTest {
            // Arrange
            whenever(mockRepository.canAccessPaste("validPaste12", "user1"))
                .thenReturn(true)
            whenever(mockRepository.getPasteWithAuthor("validPaste12"))
                .thenReturn(null)

            // Act
            val result = pasteService.getPasteById("validPaste12", "user1")

            // Assert
            assertNull(result, "Should return null when paste doesn't exist")
        }
    }

    @Nested
    @DisplayName("Create Paste")
    inner class CreatePasteTest {

        @Test
        @DisplayName("Should create paste with valid data")
        fun shouldCreatePasteWithValidData() = runTest {
            // Arrange
            val request = CreatePasteRequestDto(
                title = "Valid Title",
                content = "Valid content here",
                visibility = SharedPasteVisibility.PUBLIC,
                syntaxLanguage = "kotlin"
            )

            val createdPaste = Paste(
                id = "newPaste123",
                title = "Valid Title",
                content = "Valid content here",
                userId = "user1",
                visibility = PasteVisibility.PUBLIC,
                createdAt = LocalDateTime.now().toString(),
                syntaxLanguage = "kotlin"
            )

            val pasteWithAuthor = PasteWithAuthor(createdPaste, null)

            whenever(mockRepository.createPaste(any()))
                .thenReturn(createdPaste)
            whenever(mockRepository.getPasteWithAuthor("newPaste123"))
                .thenReturn(pasteWithAuthor)

            // Act
            val result = pasteService.createPaste(request, "user1")

            // Assert
            assertEquals("newPaste123", result.id)
            assertEquals("Valid Title", result.title)
            assertEquals("Valid content here", result.content)

            verify(mockRepository).createPaste(any())
            verify(mockRepository).getPasteWithAuthor("newPaste123")
        }

        @Test
        @DisplayName("Should throw validation error for blank title")
        fun shouldThrowValidationErrorForBlankTitle() = runTest {
            // Arrange
            val request = CreatePasteRequestDto(
                title = "",
                content = "Content",
                visibility = SharedPasteVisibility.PUBLIC,
                syntaxLanguage = "plaintext"
            )

            // Act & Assert
            assertThrows<ValidationException.InvalidInput> {
                pasteService.createPaste(request, "user1")
            }
        }

        @Test
        @DisplayName("Should throw PrivatePasteRequiresAuth for anonymous private paste")
        fun shouldThrowPrivatePasteRequiresAuthForAnonymousPrivatePaste() = runTest {
            // Arrange
            val request = CreatePasteRequestDto(
                title = "Private Paste",
                content = "Secret content",
                visibility = SharedPasteVisibility.PRIVATE,
                syntaxLanguage = "plaintext"
            )

            // Act & Assert
            assertThrows<PasteException.PrivatePasteRequiresAuth> {
                pasteService.createPaste(request, null)
            }
        }

        @Test
        @DisplayName("Should throw PasteCreationFailed when repository returns null")
        fun shouldThrowPasteCreationFailedWhenRepositoryReturnsNull() = runTest {
            // Arrange
            val request = CreatePasteRequestDto(
                title = "Title",
                content = "Content",
                visibility = SharedPasteVisibility.PUBLIC,
                syntaxLanguage = "plaintext"
            )

            whenever(mockRepository.createPaste(any()))
                .thenReturn(null)

            // Act & Assert
            assertThrows<PasteException.PasteCreationFailed> {
                pasteService.createPaste(request, "user1")
            }
        }
    }

    @Nested
    @DisplayName("Delete Paste")
    inner class DeletePasteTest {

        @Test
        @DisplayName("Should delete paste successfully")
        fun shouldDeletePasteSuccessfully() = runTest {
            // Arrange
            whenever(mockRepository.deletePaste("validPaste12", "user1"))
                .thenReturn(true)

            // Act
            val result = pasteService.deletePaste("validPaste12", "user1")

            // Assert
            assertTrue(result)
            verify(mockRepository).deletePaste("validPaste12", "user1")
        }

        @Test
        @DisplayName("Should return false when repository returns false")
        fun shouldReturnFalseWhenRepositoryReturnsFalse() = runTest {
            // Arrange
            whenever(mockRepository.deletePaste("validPaste12", "user1"))
                .thenReturn(false)

            // Act
            val result = pasteService.deletePaste("validPaste12", "user1")

            // Assert
            assertFalse(result, "Should return false when deletion fails")
            verify(mockRepository).deletePaste("validPaste12", "user1")
        }

        @Test
        @DisplayName("Should throw validation error for invalid paste ID")
        fun shouldThrowValidationErrorForInvalidPasteId() = runTest {
            // Act & Assert
            assertThrows<PasteException.InvalidPasteId> {
                pasteService.deletePaste("short", "user1")
            }
        }
    }
}
