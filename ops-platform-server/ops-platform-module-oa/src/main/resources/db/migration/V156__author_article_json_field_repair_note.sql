-- ADR-054 addendum: document shenyu-member author_article JSON field repair (V156).
-- member-server ArticleDO uses JacksonTypeHandler on privilege_types (List<Integer>).
-- OPS bridge must write JSON array "[2]" not DB default scalar "2".
-- Data repair: scripts/integration-config/apply-author-article-json-fields.py

SELECT 1;
