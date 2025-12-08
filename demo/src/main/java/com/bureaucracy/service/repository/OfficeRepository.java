package com.bureaucracy.service.repository;

import com.bureaucracy.service.entity.OfficeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OfficeRepository extends JpaRepository<OfficeEntity, Long> {
}