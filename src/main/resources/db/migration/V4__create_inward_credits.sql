create table inward_credits (
    id uuid primary key default gen_random_uuid(),
    nibss_reference varchar(200) not null,
    destination_account_id uuid not null references accounts(id),
    source_bank_code varchar(10) not null,
    source_account_number text not null,
    amount numeric(19,2) not null,
    status varchar(20) not null default 'CREDITED',
    created_at timestamptz not null default now(),
    constraint uq_inward_credits_nibss_reference unique (nibss_reference)
);
