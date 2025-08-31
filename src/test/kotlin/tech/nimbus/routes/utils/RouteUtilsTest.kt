package tech.nimbus.routes.utils

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.call
import io.ktor.server.testing.*
import io.ktor.server.routing.*
import io.ktor.server.response.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import tech.nimbus.exceptions.*

/**
 * Интеграционные тесты для RouteUtils.
 * Проверяет корректность обработки ошибок в HTTP контексте.
 */
@DisplayName("RouteUtils Integration Tests")
class RouteUtilsTest {

    @Nested
    @DisplayName("Pagination Utils")
    inner class PaginationUtilsTest {

        @Test
        @DisplayName("Should parse valid pagination parameters")
        fun shouldParseValidPaginationParameters() = testApplication {
            routing {
                get("/test") {
                    val params = call.getPaginationParams()
                    call.respond(HttpStatusCode.OK, "page=${params.page}&limit=${params.limit}&offset=${params.offset}")
                }
            }

            val response = client.get("/test?page=2&limit=50")
            assertEquals(HttpStatusCode.OK, response.status)

            val responseBody = response.bodyAsText()
            assertTrue(responseBody.contains("page=2"))
            assertTrue(responseBody.contains("limit=50"))
            assertTrue(responseBody.contains("offset=50"))
        }

        @Test
        @DisplayName("Should use default values for missing parameters")
        fun shouldUseDefaultValuesForMissingParameters() = testApplication {
            routing {
                get("/test") {
                    val params = call.getPaginationParams()
                    call.respond(HttpStatusCode.OK, "page=${params.page}&limit=${params.limit}&offset=${params.offset}")
                }
            }

            val response = client.get("/test")
            assertEquals(HttpStatusCode.OK, response.status)

            val responseBody = response.bodyAsText()
            assertTrue(responseBody.contains("page=1"))
            assertTrue(responseBody.contains("limit=20"))
            assertTrue(responseBody.contains("offset=0"))
        }

        @Test
        @DisplayName("Should handle invalid page parameter with custom validation")
        fun shouldHandleInvalidPageParameterWithCustomValidation() = testApplication {
            routing {
                get("/test") {
                    try {
                        val params = call.getPaginationParams()
                        call.respond(HttpStatusCode.OK, "page=${params.page}")
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.BadRequest, "error: ${e.message}")
                    }
                }
            }

            val response = client.get("/test?page=0")
            assertEquals(HttpStatusCode.OK, response.status) // getPaginationParams использует defaults для невалидных значений

            val responseBody = response.bodyAsText()
            assertTrue(responseBody.contains("page=1")) // default значение
        }

        @Test
        @DisplayName("Should handle invalid limit parameter with custom validation")
        fun shouldHandleInvalidLimitParameterWithCustomValidation() = testApplication {
            routing {
                get("/test") {
                    try {
                        val params = call.getPaginationParams()
                        call.respond(HttpStatusCode.OK, "limit=${params.limit}")
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.BadRequest, "error: ${e.message}")
                    }
                }
            }

            val response = client.get("/test?limit=0")
            assertEquals(HttpStatusCode.OK, response.status) // getPaginationParams использует defaults для невалидных значений

            val responseBody = response.bodyAsText()
            assertTrue(responseBody.contains("limit=20")) // default значение
        }
    }

    @Nested
    @DisplayName("Exception Response Tests")
    inner class ExceptionResponseTests {

        @Test
        @DisplayName("Should create ValidationException response correctly")
        fun shouldCreateValidationExceptionResponseCorrectly() {
            val exception = ValidationException.InvalidInput("Test validation error")
            assertEquals("Test validation error", exception.message)
            assertTrue(exception is AppException)
        }

        @Test
        @DisplayName("Should create AuthException response correctly")
        fun shouldCreateAuthExceptionResponseCorrectly() {
            val exception = AuthException.InvalidCredentials()
            assertEquals("Invalid username/email or password", exception.message)
            assertTrue(exception is AppException)
        }

        @Test
        @DisplayName("Should create PasteException response correctly")
        fun shouldCreatePasteExceptionResponseCorrectly() {
            val exception = PasteException.PasteNotFound("test123")
            assertTrue(exception.message!!.contains("test123"))
            assertTrue(exception is AppException)
        }

        @Test
        @DisplayName("Should create UserException response correctly")
        fun shouldCreateUserExceptionResponseCorrectly() {
            val exception = UserException.UserNotFound("user123")
            assertTrue(exception.message!!.contains("user123"))
            assertTrue(exception is AppException)
        }

        @Test
        @DisplayName("Should create DatabaseException response correctly")
        fun shouldCreateDatabaseExceptionResponseCorrectly() {
            val exception = DatabaseException.ConnectionFailed()
            assertEquals("Database connection failed", exception.message)
            assertTrue(exception is AppException)
        }

        @Test
        @DisplayName("Should create PaginationException response correctly")
        fun shouldCreatePaginationExceptionResponseCorrectly() {
            val exception = PaginationException.InvalidLimit(150)
            assertTrue(exception.message!!.contains("150"))
            assertTrue(exception is AppException)
        }
    }

    @Nested
    @DisplayName("Route Utils Functions")
    inner class RouteUtilsFunctionsTest {

        @Test
        @DisplayName("Should parse pagination parameters correctly")
        fun shouldParsePaginationParametersCorrectly() = testApplication {
            routing {
                get("/test") {
                    val params = call.getPaginationParams()
                    call.respond(HttpStatusCode.OK, "Page: ${params.page}, Limit: ${params.limit}, Offset: ${params.offset}")
                }
            }

            val response = client.get("/test?page=3&limit=25")
            assertEquals(HttpStatusCode.OK, response.status)

            val responseBody = response.bodyAsText()
            assertTrue(responseBody.contains("Page: 3"))
            assertTrue(responseBody.contains("Limit: 25"))
            assertTrue(responseBody.contains("Offset: 50"))
        }

        @Test
        @DisplayName("Should handle missing pagination parameters with defaults")
        fun shouldHandleMissingPaginationParametersWithDefaults() = testApplication {
            routing {
                get("/test") {
                    val params = call.getPaginationParams()
                    call.respond(HttpStatusCode.OK, "Page: ${params.page}, Limit: ${params.limit}, Offset: ${params.offset}")
                }
            }

            val response = client.get("/test")
            assertEquals(HttpStatusCode.OK, response.status)

            val responseBody = response.bodyAsText()
            assertTrue(responseBody.contains("Page: 1"))
            assertTrue(responseBody.contains("Limit: 20"))
            assertTrue(responseBody.contains("Offset: 0"))
        }
    }
}
