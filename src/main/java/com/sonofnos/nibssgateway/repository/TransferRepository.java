package com.sonofnos.nibssgateway.repository;

import com.sonofnos.nibssgateway.domain.Transfer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransferRepository extends JpaRepository<Transfer, UUID> {

    Optional<Transfer> findByClientReference(String clientReference);

    Optional<Transfer> findByNibssSessionId(String nibssSessionId);

    List<Transfer> findAllByOrderByCreatedAtAsc();
}
