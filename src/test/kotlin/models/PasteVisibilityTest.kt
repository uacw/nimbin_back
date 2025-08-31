package tech.nimbus.models

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * Unit тесты для enum PasteVisibility.
 */
class PasteVisibilityTest {

    @Test
    fun `PasteVisibility должен содержать все три типа`() {
        val values = PasteVisibility.values()

        assertEquals(3, values.size, "Должно быть ровно 3 типа видимости")
        assertTrue(values.contains(PasteVisibility.PUBLIC), "Должен содержать PUBLIC")
        assertTrue(values.contains(PasteVisibility.UNLISTED), "Должен содержать UNLISTED")
        assertTrue(values.contains(PasteVisibility.PRIVATE), "Должен содержать PRIVATE")
    }

    @Test
    fun `PasteVisibility должен корректно сериализоваться в JSON`() {
        val publicVisibility = PasteVisibility.PUBLIC
        val unlistedVisibility = PasteVisibility.UNLISTED
        val privateVisibility = PasteVisibility.PRIVATE

        val publicJson = Json.encodeToString(publicVisibility)
        val unlistedJson = Json.encodeToString(unlistedVisibility)
        val privateJson = Json.encodeToString(privateVisibility)

        assertEquals("\"PUBLIC\"", publicJson)
        assertEquals("\"UNLISTED\"", unlistedJson)
        assertEquals("\"PRIVATE\"", privateJson)
    }

    @Test
    fun `PasteVisibility должен корректно десериализоваться из JSON`() {
        val publicFromJson = Json.decodeFromString<PasteVisibility>("\"PUBLIC\"")
        val unlistedFromJson = Json.decodeFromString<PasteVisibility>("\"UNLISTED\"")
        val privateFromJson = Json.decodeFromString<PasteVisibility>("\"PRIVATE\"")

        assertEquals(PasteVisibility.PUBLIC, publicFromJson)
        assertEquals(PasteVisibility.UNLISTED, unlistedFromJson)
        assertEquals(PasteVisibility.PRIVATE, privateFromJson)
    }
}
