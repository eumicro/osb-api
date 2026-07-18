-- Keep boolean placeholders quoted so the HTTP template remains valid JSON in the admin editor.
-- Runtime coerces "true"/"false" strings via JsonTemplateValues.

UPDATE templates
SET content = replace(
    content,
    '"temporary":${parameters.adminPasswordTemporary}',
    '"temporary":"${parameters.adminPasswordTemporary}"'
)
WHERE id = 'tpl-kc-realm-create'
  AND content LIKE '%"temporary":${parameters.adminPasswordTemporary}%';
