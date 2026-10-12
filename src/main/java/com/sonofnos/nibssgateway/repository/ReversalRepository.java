package com.sonofnos.nibssgateway.repository;

import com.sonofnos.nibssgateway.domain.Reversal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ReversalRepository extends JpaRepository<Reversal, UUID> {

    Optional<Reversal> findByNibssReference(String nibssReference);
}
