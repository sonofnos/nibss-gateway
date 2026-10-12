package com.sonofnos.nibssgateway.ledger;

import com.sonofnos.nibssgateway.domain.Account;
import com.sonofnos.nibssgateway.domain.EntryType;
import com.sonofnos.nibssgateway.domain.LedgerEntry;
import com.sonofnos.nibssgateway.repository.AccountRepository;
import com.sonofnos.nibssgateway.repository.LedgerEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Posts balanced double-entry postings. Every call locks both accounts
 * with SELECT ... FOR UPDATE in ascending id order (never caller order),
 * so two transfers that move money in opposite directions between the
 * same pair of accounts always acquire their locks in the same sequence
 * and can't deadlock each other.
 */
@Service
public class LedgerService {

    private final AccountRepository accountRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    public LedgerService(AccountRepository accountRepository, LedgerEntryRepository ledgerEntryRepository) {
        this.accountRepository = accountRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    /**
     * Debits fromAccountId and credits toAccountId by the same amount,
     * posting one LedgerEntry for each leg under the same reference.
     */
    @Transactional
    public void post(UUID fromAccountId, UUID toAccountId, BigDecimal amount, String reference, String description) {
        List<UUID> ids = fromAccountId.compareTo(toAccountId) <= 0
                ? List.of(fromAccountId, toAccountId)
                : List.of(toAccountId, fromAccountId);
        List<Account> locked = accountRepository.lockAllByIdOrderedById(ids);

        Account from = locked.stream().filter(a -> a.getId().equals(fromAccountId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown account " + fromAccountId));
        Account to = locked.stream().filter(a -> a.getId().equals(toAccountId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown account " + toAccountId));

        from.debit(amount);
        to.credit(amount);
        accountRepository.save(from);
        accountRepository.save(to);

        ledgerEntryRepository.save(new LedgerEntry(fromAccountId, EntryType.DEBIT, amount, reference, description));
        ledgerEntryRepository.save(new LedgerEntry(toAccountId, EntryType.CREDIT, amount, reference, description));
    }
}
