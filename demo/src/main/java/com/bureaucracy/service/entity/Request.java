package com.bureaucracy.service.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "requests")
public class Request {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "citizen_id")
    private Citizen citizen;

    @ManyToOne
    @JoinColumn(name = "document_id")
    private DocumentType documentType;

    private LocalDateTime timestamp;

    private String status; // "COMPLETED", "IN_PROGRESS"

    public Request() {}
    public Request(Citizen citizen, DocumentType documentType) {
        this.citizen = citizen;
        this.documentType = documentType;
        this.timestamp = LocalDateTime.now();
        this.status = "COMPLETED"; // For now, we assume success
    }

    public DocumentType getDocumentType() { return documentType; }
}