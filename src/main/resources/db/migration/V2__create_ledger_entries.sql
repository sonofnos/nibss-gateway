create table ledger_entries (
    id uuid primary key default gen_random_uuid(),
    account_id uuid not null references accounts(id),
    entry_type varchar(10) not null,
    amount numeric(19,2) not null,
    reference varchar(200) not null,
    description varchar(500),
    created_at timestamptz not null default now()
);

create index idx_ledger_entries_account_id on ledger_entries(account_id);
create index idx_ledger_entries_reference on ledger_entries(reference);
