package com.bureaucracy.service.domain;

import com.bureaucracy.service.entity.Citizen;
import com.bureaucracy.service.entity.DocumentType;
import com.bureaucracy.service.entity.Request;
import com.bureaucracy.service.repository.DocumentTypeRepository;
import com.bureaucracy.service.repository.RequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RequestService {

    private final RequestRepository requestRepo;
    private final DocumentTypeRepository docTypeRepo;

    public RequestService(RequestRepository requestRepo, DocumentTypeRepository docTypeRepo) {
        this.requestRepo = requestRepo;
        this.docTypeRepo = docTypeRepo;
    }

    // This annotation creates a dedicated transaction for this save action
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveRequest(Citizen citizen, String documentName) {
        try {
            DocumentType docType = docTypeRepo.findByName(documentName)
                    .orElseThrow(() -> new RuntimeException("Doc not found: " + documentName));

            Request newRequest = new Request(citizen, docType);
            requestRepo.save(newRequest);

            System.out.println("✅ DATABASE SUCCESS: Saved " + documentName + " for " + citizen.getName());
        } catch (Exception e) {
            System.err.println("❌ DATABASE ERROR: Could not save request: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Transactional(readOnly = true)
    public boolean hasDocument(Citizen citizen, String documentName) {
        return requestRepo.existsByCitizenAndDocumentType_Name(citizen, documentName);
    }
}