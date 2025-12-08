package com.bureaucracy.service.repository;

import com.bureaucracy.service.entity.Citizen;
import com.bureaucracy.service.entity.Request;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RequestRepository extends JpaRepository<Request, Long> {
    // This is the magic SQL query generator:
    // It checks if a specific citizen already has a specific document type
    boolean existsByCitizenAndDocumentType_Name(Citizen citizen, String documentTypeName);
}