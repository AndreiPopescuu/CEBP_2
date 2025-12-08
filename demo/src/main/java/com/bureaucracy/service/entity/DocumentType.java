package com.bureaucracy.service.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "document_types")
public class DocumentType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String name;

    // LINK TO OFFICE: Who issues this?
    @ManyToOne
    @JoinColumn(name = "office_id")
    private OfficeEntity issuingOffice;

    // --- THE MAGIC: RECURSIVE RELATIONSHIP ---
    // A document can have many "prerequisites" (other documents)
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "document_prerequisites",
            joinColumns = @JoinColumn(name = "document_id"),
            inverseJoinColumns = @JoinColumn(name = "required_doc_id")
    )
    private List<DocumentType> prerequisites = new ArrayList<>();

    public DocumentType() {}
    public DocumentType(String name, OfficeEntity office) {
        this.name = name;
        this.issuingOffice = office;
    }

    public void addPrerequisite(DocumentType doc) {
        this.prerequisites.add(doc);
    }

    // Getters
    public String getName() { return name; }
    public List<DocumentType> getPrerequisites() { return prerequisites; }
    public OfficeEntity getIssuingOffice() { return issuingOffice; }
}