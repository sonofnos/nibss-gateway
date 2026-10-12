create table transfers (
    id uuid primary key default gen_random_uuid(),
    client_reference varchar(200) not null,
    source_account_id uuid not null references accounts(id),
    destination_bank_code varchar(10) not null,
    destination_account_number text not null,
    destination_account_name varchar(200),
    amount numeric(19,2) not null,
    narration varchar(500),
    status varchar(30) not null,
    nibss_session_id varchar(40),
    requery_count int not null default 0,
    version bigint not null default 0,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint uq_transfers_client_reference unique (client_reference)
);

create index idx_transfers_nibss_session_id on transfers(nibss_session_id);
