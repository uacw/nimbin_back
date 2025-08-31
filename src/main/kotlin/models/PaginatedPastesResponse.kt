package tech.nimbus.models

import kotlinx.serialization.Serializable

/**
 * DTO модель для пагинированного ответа с заметками.
 *
 * Используется для возврата списка заметок с метаданными пагинации.
 * Обеспечивает структурированный ответ для клиентских приложений.
 *
 * @property pastes Список заметок на текущей странице
 * @property pagination Метаданные пагинации
 */
@Serializable
data class PaginatedPastesResponse(
    val pastes: List<PasteResponse>,
    val pagination: PaginationInfo
)

/**
 * Метаданные пагинации для API ответов.
 *
 * Содержит информацию о текущем состоянии пагинации и доступности
 * дополнительных данных.
 *
 * @property total Общее количество доступных элементов
 * @property limit Максимальное количество элементов на странице
 * @property offset Смещение от начала списка
 * @property hasMore Флаг наличия дополнительных данных
 */
@Serializable
data class PaginationInfo(
    val total: Long,
    val limit: Int,
    val offset: Int,
    val hasMore: Boolean
)
