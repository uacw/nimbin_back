package tech.nimbus.services

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.mockito.Mockito.*
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import tech.nimbus.database.repositories.PasteWithAuthor
import tech.nimbus.database.repositories.interfaces.IPasteRepository
import tech.nimbus.database.repositories.interfaces.IFavoritesRepository
import tech.nimbus.exceptions.PasteException
import tech.nimbus.models.Paste
import tech.nimbus.models.PasteVisibility
import tech.nimbus.shared.dto.PasteVisibility as SharedPasteVisibility
import tech.nimbus.shared.dto.request.CreatePasteRequestDto
import java.time.LocalDateTime

class PasteServiceGuestTest {

    private lateinit var repo: IPasteRepository
    private lateinit var fav: IFavoritesRepository
    private lateinit var service: PasteService

    @BeforeEach
    fun setUp() {
        repo = mock(IPasteRepository::class.java)
        fav = mock(IFavoritesRepository::class.java)
        service = PasteService(repo, fav)
    }

    @Test
    fun `guest can create PUBLIC paste`() = runTest {
        val req = CreatePasteRequestDto(
            title = "Guest Public",
            content = "guest content",
            visibility = SharedPasteVisibility.PUBLIC,
            syntaxLanguage = "plaintext"
        )
        val now = LocalDateTime.now().toString()
        val created = Paste(
            id = "guestOk00001", // 12 символов
            title = req.title,
            content = req.content,
            userId = null,
            visibility = PasteVisibility.PUBLIC,
            createdAt = now,
            updatedAt = now,
            syntaxLanguage = "plaintext",
            guestId = "g-1"
        )
        whenever(repo.createPaste(any())).thenReturn(created)
        whenever(repo.getPasteWithAuthor("guestOk00001")).thenReturn(PasteWithAuthor(created, null))

        val dto = service.createPaste(req, userId = null, guestId = "g-1")
        assertEquals("guestOk00001", dto.id)
        assertEquals("Guest Public", dto.title)
    }

    @Test
    fun `guest cannot create PRIVATE paste`() = runTest {
        val req = CreatePasteRequestDto(
            title = "Private Guest",
            content = "secret",
            visibility = SharedPasteVisibility.PRIVATE,
            syntaxLanguage = "plaintext"
        )
        var thrown = false
        try {
            service.createPaste(req, userId = null, guestId = "g-1")
            fail("Expected PrivatePasteRequiresAuth")
        } catch (e: PasteException.PrivatePasteRequiresAuth) {
            thrown = true
        }
        assertTrue(thrown)
    }

    @Test
    fun `getPasteById respects guest access`() = runTest {
        val now = LocalDateTime.now().toString()
        val pasteId = "gPst00000001" // 12 символов
        val p = Paste(
            id = pasteId,
            title = "Guest View",
            content = "c",
            userId = null,
            visibility = PasteVisibility.PRIVATE,
            createdAt = now,
            updatedAt = now,
            syntaxLanguage = "plaintext",
            guestId = "g-allow"
        )
        // Доступ гостя проверяется сервисом по данным пасты, мок canAccessPaste не нужен
        whenever(repo.getPasteWithAuthor(pasteId)).thenReturn(PasteWithAuthor(p, null))
        whenever(repo.incrementViewCount(pasteId)).thenReturn(Unit)

        val dto = service.getPasteById(pasteId, userId = null, guestId = "g-allow")
        assertNotNull(dto)
        assertEquals(pasteId, dto!!.id)
    }
}
