-- Миграция базы данных: добавление столбца visibility и удаление is_public

-- Добавляем новый столбец visibility
ALTER TABLE pastes ADD COLUMN IF NOT EXISTS visibility VARCHAR(20) DEFAULT 'PUBLIC' NOT NULL;

-- Мигрируем данные из is_public в visibility (если столбец is_public существует)
UPDATE pastes
SET visibility = CASE
    WHEN is_public = true THEN 'PUBLIC'
    WHEN is_public = false THEN 'PRIVATE'
    ELSE 'PUBLIC'
END
WHERE visibility = 'PUBLIC'; -- только для записей, которые еще не были мигрированы

-- Удаляем старый столбец is_public (если существует)
ALTER TABLE pastes DROP COLUMN IF EXISTS is_public;

-- Проверяем результат
SELECT COUNT(*) as total_pastes, visibility, COUNT(*) as count_by_visibility
FROM pastes
GROUP BY visibility;
