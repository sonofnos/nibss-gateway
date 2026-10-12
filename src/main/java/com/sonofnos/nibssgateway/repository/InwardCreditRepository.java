package com.sonofnos.nibssgateway.repository;

import com.sonofnos.nibssgateway.domain.InwardCredit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface InwardCreditRepository extends JpaRepository<InwardCredit, UUID> {

    Optional<InwardCredit> findByNibssReference(String nibssReference);
}
