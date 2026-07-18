-- Plan OSB schemas + admin UI schema for provision parameters

ALTER TABLE service_plans
    ADD COLUMN schemas jsonb NOT NULL DEFAULT '{}'::jsonb,
    ADD COLUMN parameters_ui_schema jsonb NOT NULL DEFAULT '{}'::jsonb;

-- Plan schemas for Realtest offerings are inserted with the plans in V7.
