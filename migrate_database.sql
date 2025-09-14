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
WHERE EXISTS (
    SELECT 1 FROM information_schema.columns c
    WHERE c.table_name = 'pastes' AND c.column_name = 'is_public'
)
AND visibility = 'PUBLIC';

-- Удаляем старый столбец is_public (если существует)
ALTER TABLE pastes DROP COLUMN IF EXISTS is_public;

-- === Новые изменения для ETag и синтаксиса ===

-- 1) Добавить updated_at, заполнить для существующих строк и выставить DEFAULT/NOT NULL
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'pastes' AND column_name = 'updated_at'
    ) THEN
        ALTER TABLE pastes ADD COLUMN updated_at TIMESTAMP WITHOUT TIME ZONE;
    END IF;
END$$;

-- Заполняем updated_at для всех строк, где он NULL (используем created_at или NOW())
UPDATE pastes SET updated_at = COALESCE(updated_at, created_at, NOW()) WHERE updated_at IS NULL;

-- Выставляем DEFAULT и NOT NULL
ALTER TABLE pastes ALTER COLUMN updated_at SET DEFAULT NOW();
ALTER TABLE pastes ALTER COLUMN updated_at SET NOT NULL;

-- 2) Переименование language -> syntax_language (с сохранением данных)
-- Добавляем целевой столбец, если его нет
ALTER TABLE pastes ADD COLUMN IF NOT EXISTS syntax_language VARCHAR(50) DEFAULT 'plaintext' NOT NULL;

-- Если существует старый столбец language, переносим значения
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'pastes' AND column_name = 'language'
    ) THEN
        EXECUTE 'UPDATE pastes SET syntax_language = language WHERE language IS NOT NULL AND language <> '''''' ';
        -- Удаляем старый столбец
        ALTER TABLE pastes DROP COLUMN language;
    END IF;
END$$;

-- 3) Гарантируем наличие view_count с дефолтом 0
ALTER TABLE pastes ADD COLUMN IF NOT EXISTS view_count INTEGER DEFAULT 0 NOT NULL;

-- 4) Гарантируем корректный DEFAULT для created_at (на всякий случай)
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'pastes' AND column_name = 'created_at'
    ) THEN
        EXECUTE 'ALTER TABLE pastes ALTER COLUMN created_at SET DEFAULT NOW()';
    END IF;
END$$;

-- Проверка (необязательная): подсчёт по visibility
-- SELECT visibility, COUNT(*) FROM pastes GROUP BY visibility;

-- Create favorites table for users and pastes (idempotent)
CREATE TABLE IF NOT EXISTS user_favorites (
    user_id  VARCHAR(36) NOT NULL,
    paste_id VARCHAR(12) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_user_favorites PRIMARY KEY (user_id, paste_id),
    CONSTRAINT fk_user_favorites_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_favorites_paste FOREIGN KEY (paste_id)
        REFERENCES pastes(id) ON DELETE CASCADE
);

-- Helpful indexes (if you frequently filter by user or paste)
CREATE INDEX IF NOT EXISTS idx_user_favorites_user  ON user_favorites(user_id);
CREATE INDEX IF NOT EXISTS idx_user_favorites_paste ON user_favorites(paste_id);

-- === Guest mode groundwork: add guest_id columns and indexes (idempotent) ===

-- 1) pastes.guest_id
ALTER TABLE pastes ADD COLUMN IF NOT EXISTS guest_id VARCHAR(36);
CREATE INDEX IF NOT EXISTS idx_pastes_guest_id ON pastes(guest_id);

-- 2) user_favorites.guest_id (keep existing schema; do not drop PK)
ALTER TABLE user_favorites ADD COLUMN IF NOT EXISTS guest_id VARCHAR(36);
CREATE INDEX IF NOT EXISTS idx_user_favorites_guest_id ON user_favorites(guest_id);

-- 3) Unique constraints to prevent duplicates for user and guest separately
DO $$
BEGIN
    -- If primary key already enforces (user_id, paste_id), we skip creating a redundant unique index for user
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes WHERE schemaname = 'public' AND indexname = 'user_favorites_pkey'
    ) AND NOT EXISTS (
        SELECT 1 FROM pg_indexes WHERE schemaname = 'public' AND indexname = 'ux_user_favorites_user'
    ) THEN
        EXECUTE 'CREATE UNIQUE INDEX ux_user_favorites_user ON user_favorites(user_id, paste_id) WHERE user_id IS NOT NULL';
    END IF;

    -- Unique for guest favorites
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes WHERE schemaname = 'public' AND indexname = 'ux_user_favorites_guest'
    ) THEN
        EXECUTE 'CREATE UNIQUE INDEX ux_user_favorites_guest ON user_favorites(guest_id, paste_id) WHERE guest_id IS NOT NULL';
    END IF;
END$$;
