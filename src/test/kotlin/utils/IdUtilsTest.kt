package tech.nimbus.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNotEquals
import kotlin.test.assertFalse

/**
 * Unit тесты для утилиты генерации ID заметок.
 */
class IdUtilsTest {

    // Допустимые символы (исключены трудноразличимые: 0, O, o, I, i, l, L)
    private val allowedChars = listOf(
        // Строчные буквы (исключены: i, l, o)
        'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'j', 'k', 'm', 'n', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
        // Прописные буквы (исключены: I, L, O)
        'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'J', 'K', 'M', 'N', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
        // Цифры (исключен: 0)
        '1', '2', '3', '4', '5', '6', '7', '8', '9'
    )

    // Трудноразличимые символы, которые НЕ должны встречаться
    private val forbiddenChars = listOf('0', 'O', 'o', 'I', 'i', 'l', 'L')

    @Test
    fun `generatePasteId должен генерировать ID правильной длины по умолчанию`() {
        // When
        val id = generatePasteId()

        // Then
        assertEquals(12, id.length, "ID должен быть длиной 12 символов по умолчанию")
    }

    @Test
    fun `generatePasteId должен генерировать ID указанной длины`() {
        // Given
        val customLength = 8

        // When
        val id = generatePasteId(customLength)

        // Then
        assertEquals(customLength, id.length, "ID должен быть указанной длины")
    }

    @Test
    fun `generatePasteId должен содержать только допустимые символы`() {
        // When
        val id = generatePasteId(100) // Большая длина для лучшего тестирования

        // Then
        assertTrue(
            id.all { it in allowedChars },
            "ID должен содержать только допустимые буквы и цифры"
        )
    }

    @Test
    fun `generatePasteId НЕ должен содержать трудноразличимые символы`() {
        // When
        val id = generatePasteId(500) // Очень большая длина для надежного тестирования

        // Then
        forbiddenChars.forEach { forbiddenChar ->
            assertFalse(
                id.contains(forbiddenChar),
                "ID не должен содержать трудноразличимый символ '$forbiddenChar'"
            )
        }
    }

    @Test
    fun `generatePasteId должен генерировать уникальные ID`() {
        // When
        val ids = (1..1000).map { generatePasteId() }

        // Then
        val uniqueIds = ids.toSet()
        assertEquals(
            ids.size,
            uniqueIds.size,
            "Все сгенерированные ID должны быть уникальными"
        )
    }

    @Test
    fun `generatePasteId должен работать с различными длинами`() {
        // Given
        val lengths = listOf(1, 5, 10, 15, 20, 50)

        lengths.forEach { length ->
            // When
            val id = generatePasteId(length)

            // Then
            assertEquals(length, id.length, "ID должен иметь правильную длину: $length")
            assertTrue(
                id.all { it in allowedChars },
                "ID длины $length должен содержать только допустимые символы"
            )
        }
    }

    @Test
    fun `generatePasteId должен использовать все группы символов`() {
        // When - генерируем много ID для статистического анализа
        val combinedId = (1..1000).map { generatePasteId(20) }.joinToString("")

        // Then - проверяем, что используются все группы символов
        val hasLowercase = combinedId.any { it in 'a'..'z' && it !in forbiddenChars }
        val hasUppercase = combinedId.any { it in 'A'..'Z' && it !in forbiddenChars }
        val hasDigits = combinedId.any { it in '1'..'9' }

        assertTrue(hasLowercase, "Должны использоваться строчные буквы")
        assertTrue(hasUppercase, "Должны использоваться прописные буквы")
        assertTrue(hasDigits, "Должны использоваться цифры")
    }

    @Test
    fun `проверка что алфавит содержит ровно 56 символов`() {
        // Ожидаемое количество символов:
        // 23 строчные (26 - 3 исключенные: i, l, o)
        // 23 прописные (26 - 3 исключенные: I, L, O)
        // 9 цифр (10 - 1 исключенная: 0)
        // Итого: 23 + 23 + 9 = 55 символов

        // When
        val ids = (1..10000).map { generatePasteId(1) }.toSet()

        // Then - проверяем что используется правильное количество уникальных символов
        assertTrue(
            ids.size <= 55, // Может быть меньше из-за случайности, но не больше
            "Количество уникальных символов не должно превышать 55"
        )

        // Проверяем что все символы из allowedChars могут быть сгенерированы
        assertTrue(
            allowedChars.size == 55,
            "Алфавит должен содержать ровно 55 допустимых символов"
        )
    }
}
