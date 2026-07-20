-- Fix OPS-synced author_article rows where JacksonTypeHandler JSON columns were invalid.
-- privilege_types: scalar "2" -> "[2]" (List<Integer>)
-- match_scheme: empty string -> NULL (List<MatchBaseVO>)
USE `shenyu-member`;

UPDATE author_article
SET privilege_types = CONCAT('[', privilege_types, ']'),
    update_time = NOW()
WHERE privilege_types IS NOT NULL
  AND privilege_types NOT LIKE '[%'
  AND deleted = 0;

UPDATE author_article
SET match_scheme = NULL,
    update_time = NOW()
WHERE match_scheme IS NOT NULL
  AND TRIM(match_scheme) = ''
  AND deleted = 0;
