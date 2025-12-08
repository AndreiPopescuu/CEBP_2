package com.bureaucracy.service.repository;

import com.bureaucracy.service.entity.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface DocumentTypeRepository extends JpaRepository<DocumentType, Long> {
    Optional<DocumentType> findByName(String name);
}