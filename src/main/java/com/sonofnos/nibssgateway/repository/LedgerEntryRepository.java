package com.sonofnos.nibssgateway.repository;

import com.sonofnos.nibssgateway.domain.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {

    List<LedgerEntry> findByReference(String reference);

    List<LedgerEntry> findByAccountId(UUID accountId);
}
