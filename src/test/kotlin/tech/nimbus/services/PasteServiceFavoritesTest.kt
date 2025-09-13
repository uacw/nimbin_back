package tech.nimbus.services

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import tech.nimbus.database.repositories.PasteSortOrder
import tech.nimbus.database.repositories.PasteWithAuthor
import tech.nimbus.database.repositories.interfaces.IFavoritesRepository
import tech.nimbus.database.repositories.interfaces.IPasteRepository
import tech.nimbus.models.Paste
import tech.nimbus.models.PasteVisibility
import tech.nimbus.models.User
import java.time.LocalDateTime

@DisplayName("PasteService Favorites Tests")
class PasteServiceFavoritesTest {

    private lateinit var mockRepo: IPasteRepository
    private lateinit var mockFav: IFavoritesRepository
    private lateinit var service: PasteService

    @BeforeEach
    fun setUp() {
        mockRepo = mock(IPasteRepository::class.java)
        mockFav = mock(IFavoritesRepository::class.java)
        service = PasteService(mockRepo, mockFav)
    }

    private fun paste(id: String, userId: String? = "u1"): Paste {
        val now = LocalDateTime.now().toString()
        return Paste(
            id = id,
            title = "t-$id",
            content = "c-$id",
            userId = userId,
            visibility = PasteVisibility.PUBLIC,
            createdAt = now,
            updatedAt = now,
            syntaxLanguage = "plaintext"
        )
    }

    @Test
    fun `public list marks favorites when user provided`() = runTest {
        // Arrange
        val p1 = paste("p11111111111")
        val p2 = paste("p22222222222")
        val author = User("u1", "john", "John", "john@example.com", "hash", LocalDateTime.now().toString())

        whenever(mockRepo.getPublicPastesWithAuthors(any(), any(), any())).thenReturn(
            listOf(PasteWithAuthor(p1, author), PasteWithAuthor(p2, author))
        )
        whenever(mockFav.listFavoritePasteIds(eq("uX"), any(), any())).thenReturn(listOf("p11111111111"))

        // Act
        val result = service.getPublicPastes(20, 0, PasteSortOrder.NEWEST_FIRST, currentUserId = "uX")

        // Assert
        assertEquals(2, result.size)
        val r1 = result.first { it.id == "p11111111111" }
        val r2 = result.first { it.id == "p22222222222" }
        assertEquals(true, r1.isFavorite)
        assertEquals(false, r2.isFavorite)
    }

    @Test
    fun `get by id returns isFavorite for current user`() = runTest {
        val p = paste("p33333333333")
        whenever(mockRepo.canAccessPaste("p33333333333", "uX")).thenReturn(true)
        whenever(mockRepo.getPasteWithAuthor("p33333333333")).thenReturn(PasteWithAuthor(p, null))
        whenever(mockRepo.incrementViewCount("p33333333333")).thenReturn(Unit)
        whenever(mockFav.isFavorite("uX", "p33333333333")).thenReturn(true)

        val dto = service.getPasteById("p33333333333", "uX")
        assertNotNull(dto)
        assertEquals(true, dto!!.isFavorite)
    }

    @Test
    fun `my pastes with favoriteOnly returns only favorites`() = runTest {
        // Arrange: favorite list has two ids; repo returns paste for both ids
        whenever(mockFav.listFavoritePasteIds(eq("me"), any(), any())).thenReturn(listOf("pAaaaaaaaaaa", "pBbbbbbbbbbb"))
        whenever(mockRepo.getPasteById("pAaaaaaaaaaa")).thenReturn(paste("pAaaaaaaaaaa", userId = "me"))
        whenever(mockRepo.getPasteById("pBbbbbbbbbbb")).thenReturn(paste("pBbbbbbbbbbb", userId = "me"))
        whenever(mockRepo.canAccessPaste(any(), any())).thenReturn(true)

        // Act
        val result = service.getUserPastes("me", 20, 0, favoriteOnly = true)

        // Assert
        assertEquals(2, result.size)
        assertTrue(result.all { it.isFavorite == true })
        assertEquals(setOf("pAaaaaaaaaaa", "pBbbbbbbbbbb"), result.map { it.id }.toSet())
    }
}
