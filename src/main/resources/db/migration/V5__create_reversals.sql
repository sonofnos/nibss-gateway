create table reversals (
    id uuid primary key default gen_random_uuid(),
    nibss_reference varchar(200) not null,
    original_transfer_id uuid not null references transfers(id),
    reason varchar(50) not null,
    amount numeric(19,2) not null,
    created_at timestamptz not null default now(),
    constraint uq_reversals_nibss_reference unique (nibss_reference)
);
