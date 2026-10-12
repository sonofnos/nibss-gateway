create table accounts (
    id uuid primary key default gen_random_uuid(),
    account_number text not null,
    bvn text,
    account_name varchar(200) not null,
    bank_code varchar(10) not null,
    balance numeric(19,2) not null default 0,
    allow_negative boolean not null default false,
    version bigint not null default 0,
    created_at timestamptz not null default now()
);

-- The settlement suspense account (see ledger.SettlementAccounts) is
-- seeded by SuspenseAccountSeeder at application startup, not here: its
-- account_number column goes through the same AES-GCM field encryption
-- as every other account, and the active encryption key is only known
-- to the running application, not to a SQL migration.
