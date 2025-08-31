package tech.nimbus.validation

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested

/**
 * Unit тесты для ValidationService.
 * Проверяет корректность валидации всех типов входных данных.
 */
@DisplayName("ValidationService Tests")
class ValidationServiceTest {

    @Nested
    @DisplayName("Registration Validation")
    inner class RegistrationValidationTest {

        @Test
        @DisplayName("Should succeed with valid registration data")
        fun shouldSucceedWithValidRegistrationData() {
            val result = ValidationService.validateRegistration(
                username = "testuser123",
                email = "test@example.com",
                password = "password123"
            )

            assertTrue(result is ValidationResult.Success)
        }

        @Test
        @DisplayName("Should fail with blank username")
        fun shouldFailWithBlankUsername() {
            val result = ValidationService.validateRegistration(
                username = "",
                email = "test@example.com",
                password = "password123"
            )

            assertTrue(result is ValidationResult.Errors)
            val errors = (result as ValidationResult.Errors).errors
            assertTrue(errors.any { it.field == "username" && it.message.contains("blank") })
        }

        @Test
        @DisplayName("Should fail with short username")
        fun shouldFailWithShortUsername() {
            val result = ValidationService.validateRegistration(
                username = "ab",
                email = "test@example.com",
                password = "password123"
            )

            assertTrue(result is ValidationResult.Errors)
            val errors = (result as ValidationResult.Errors).errors
            assertTrue(errors.any { it.field == "username" && it.message.contains("3 characters") })
        }

        @Test
        @DisplayName("Should fail with long username")
        fun shouldFailWithLongUsername() {
            val result = ValidationService.validateRegistration(
                username = "a".repeat(51),
                email = "test@example.com",
                password = "password123"
            )

            assertTrue(result is ValidationResult.Errors)
            val errors = (result as ValidationResult.Errors).errors
            assertTrue(errors.any { it.field == "username" && it.message.contains("50 characters") })
        }

        @Test
        @DisplayName("Should fail with invalid username characters")
        fun shouldFailWithInvalidUsernameCharacters() {
            val result = ValidationService.validateRegistration(
                username = "test user!",
                email = "test@example.com",
                password = "password123"
            )

            assertTrue(result is ValidationResult.Errors)
            val errors = (result as ValidationResult.Errors).errors
            assertTrue(errors.any { it.field == "username" && it.message.contains("letters, numbers, and underscores") })
        }

        @Test
        @DisplayName("Should fail with invalid email format")
        fun shouldFailWithInvalidEmailFormat() {
            val result = ValidationService.validateRegistration(
                username = "testuser",
                email = "invalid-email",
                password = "password123"
            )

            assertTrue(result is ValidationResult.Errors)
            val errors = (result as ValidationResult.Errors).errors
            assertTrue(errors.any { it.field == "email" && it.message.contains("Invalid email format") })
        }

        @Test
        @DisplayName("Should fail with short password")
        fun shouldFailWithShortPassword() {
            val result = ValidationService.validateRegistration(
                username = "testuser",
                email = "test@example.com",
                password = "12345"
            )

            assertTrue(result is ValidationResult.Errors)
            val errors = (result as ValidationResult.Errors).errors
            assertTrue(errors.any { it.field == "password" && it.message.contains("6 characters") })
        }

        @Test
        @DisplayName("Should support cyrillic in display name context")
        fun shouldSupportCyrillicInDisplayNameContext() {
            // Username должен содержать только латинские символы
            val result = ValidationService.validateRegistration(
                username = "testuser_rus",
                email = "тест@example.com", // Но email может содержать кириллицу
                password = "пароль123"
            )

            // Проверяем, что username валидируется корректно
            assertFalse(result is ValidationResult.Success) // email с кириллицей должен быть невалидным
        }
    }

    @Nested
    @DisplayName("Login Validation")
    inner class LoginValidationTest {

        @Test
        @DisplayName("Should succeed with valid login data")
        fun shouldSucceedWithValidLoginData() {
            val result = ValidationService.validateLogin(
                email = "test@example.com",
                password = "password123"
            )

            assertTrue(result is ValidationResult.Success)
        }

        @Test
        @DisplayName("Should fail with blank credentials")
        fun shouldFailWithBlankCredentials() {
            val result = ValidationService.validateLogin(
                email = "",
                password = ""
            )

            assertTrue(result is ValidationResult.Errors)
            val errors = (result as ValidationResult.Errors).errors
            assertEquals(2, errors.size)
            assertTrue(errors.any { it.field == "email" })
            assertTrue(errors.any { it.field == "password" })
        }
    }

    @Nested
    @DisplayName("Paste Creation Validation")
    inner class PasteCreationValidationTest {

        @Test
        @DisplayName("Should succeed with valid paste data")
        fun shouldSucceedWithValidPasteData() {
            val result = ValidationService.validatePasteCreation(
                title = "Test Paste",
                content = "This is a test paste content."
            )

            assertTrue(result is ValidationResult.Success)
        }

        @Test
        @DisplayName("Should fail with blank title")
        fun shouldFailWithBlankTitle() {
            val result = ValidationService.validatePasteCreation(
                title = "",
                content = "Content"
            )

            assertTrue(result is ValidationResult.Errors)
            val errors = (result as ValidationResult.Errors).errors
            assertTrue(errors.any { it.field == "title" && it.message.contains("blank") })
        }

        @Test
        @DisplayName("Should fail with too long title")
        fun shouldFailWithTooLongTitle() {
            val result = ValidationService.validatePasteCreation(
                title = "a".repeat(256),
                content = "Content"
            )

            assertTrue(result is ValidationResult.Errors)
            val errors = (result as ValidationResult.Errors).errors
            assertTrue(errors.any { it.field == "title" && it.message.contains("255") })
        }

        @Test
        @DisplayName("Should fail with blank content")
        fun shouldFailWithBlankContent() {
            val result = ValidationService.validatePasteCreation(
                title = "Title",
                content = ""
            )

            assertTrue(result is ValidationResult.Errors)
            val errors = (result as ValidationResult.Errors).errors
            assertTrue(errors.any { it.field == "content" && it.message.contains("blank") })
        }

        @Test
        @DisplayName("Should fail with too long content")
        fun shouldFailWithTooLongContent() {
            val result = ValidationService.validatePasteCreation(
                title = "Title",
                content = "a".repeat(1_000_001)
            )

            assertTrue(result is ValidationResult.Errors)
            val errors = (result as ValidationResult.Errors).errors
            assertTrue(errors.any { it.field == "content" && it.message.contains("1MB") })
        }

        @Test
        @DisplayName("Should support cyrillic content")
        fun shouldSupportCyrillicContent() {
            val result = ValidationService.validatePasteCreation(
                title = "Кириллица тест",
                content = "Это тест с русским текстом и эмодзи 🚀"
            )

            assertTrue(result is ValidationResult.Success)
        }
    }

    @Nested
    @DisplayName("Profile Update Validation")
    inner class ProfileUpdateValidationTest {

        @Test
        @DisplayName("Should succeed with valid profile data")
        fun shouldSucceedWithValidProfileData() {
            val result = ValidationService.validateProfileUpdate(
                username = "newusername",
                displayName = "New Display Name"
            )

            assertTrue(result is ValidationResult.Success)
        }

        @Test
        @DisplayName("Should succeed with null values")
        fun shouldSucceedWithNullValues() {
            val result = ValidationService.validateProfileUpdate(
                username = null,
                displayName = null
            )

            assertTrue(result is ValidationResult.Success)
        }

        @Test
        @DisplayName("Should fail with invalid username")
        fun shouldFailWithInvalidUsername() {
            val result = ValidationService.validateProfileUpdate(
                username = "ab", // Too short
                displayName = "Display Name"
            )

            assertTrue(result is ValidationResult.Errors)
            val errors = (result as ValidationResult.Errors).errors
            assertTrue(errors.any { it.field == "username" })
        }

        @Test
        @DisplayName("Should fail with too long display name")
        fun shouldFailWithTooLongDisplayName() {
            val result = ValidationService.validateProfileUpdate(
                username = "username",
                displayName = "a".repeat(101)
            )

            assertTrue(result is ValidationResult.Errors)
            val errors = (result as ValidationResult.Errors).errors
            assertTrue(errors.any { it.field == "displayName" && it.message.contains("100") })
        }

        @Test
        @DisplayName("Should support cyrillic display name")
        fun shouldSupportCyrillicDisplayName() {
            val result = ValidationService.validateProfileUpdate(
                username = "username",
                displayName = "Иван Петров"
            )

            assertTrue(result is ValidationResult.Success)
        }
    }
}
