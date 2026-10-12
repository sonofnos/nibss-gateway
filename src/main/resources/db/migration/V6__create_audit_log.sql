create table audit_log (
    id uuid primary key default gen_random_uuid(),
    event_type varchar(60) not null,
    entity_type varchar(60) not null,
    entity_id varchar(100) not null,
    details text,
    created_at timestamptz not null default now()
);

create index idx_audit_log_entity on audit_log(entity_type, entity_id);

-- Enforced immutability, not just convention: no application code path
-- ever updates or deletes an audit_log row (AuditLogRepository exposes
-- no such method), and these two rules make any attempt a no-op at the
-- database level too.
create rule audit_log_no_update as on update to audit_log do instead nothing;
create rule audit_log_no_delete as on delete to audit_log do instead nothing;
