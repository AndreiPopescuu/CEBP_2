package com.bureaucracy.service.domain;

import com.bureaucracy.service.EventPublisher;
import com.bureaucracy.service.entity.Citizen;

import java.util.ArrayList;
import java.util.List;

public class CitizenAgent implements Runnable {
    private final Citizen citizenEntity;
    private final List<Document> neededDocuments;
    private final EventPublisher publisher;

    // Use the Service instead of Repos
    private final RequestService requestService;

    public CitizenAgent(Citizen citizenEntity,
                        List<Document> neededDocuments,
                        EventPublisher publisher,
                        RequestService requestService) { // Changed this param
        this.citizenEntity = citizenEntity;
        this.neededDocuments = new ArrayList<>(neededDocuments);
        this.publisher = publisher;
        this.requestService = requestService;
    }

    public String getName() { return citizenEntity.getName(); }

    public synchronized void onDocumentIssued(Document doc) {
        notifyAll();
        // Delegate saving to the Transactional Service
        requestService.saveRequest(citizenEntity, doc.getName());
    }

    public boolean hasDocument(Document doc) {
        return requestService.hasDocument(citizenEntity, doc.getName());
    }

    @Override
    public void run() {
        publisher.publishEvent("CLIENT_START", getName(), "Started with needs: " + neededDocuments);
        for (Document doc : neededDocuments) {
            obtainDocument(doc);
        }
        publisher.publishEvent("CLIENT_FINISH", getName(), "Finished process.");
    }

    private void obtainDocument(Document doc) {
        // 1. Prerequisites recursion
        for (Document pre : doc.getPrerequisites()) {
            obtainDocument(pre);
        }

        // 2. Check DB
        if (hasDocument(doc)) {
            publisher.publishEvent("DB_CHECK", getName(),
                    "Already owns '" + doc + "' in Database. Skipping queue.");
            return;
        }

        // 3. DEBUG: Check if we found the office
        Office office = doc.getEmittingOffice();
        if (office == null) {
            System.err.println("❌ CRITICAL ERROR: No active Office found for document: " + doc.getName());
            System.err.println("   (This means BureaucracyRegistry failed to link the DB entity to the Active Office)");
            return; // Stop to avoid crash
        }

        // 4. Submit to office
        System.out.println("DEBUG: " + getName() + " is going to office: " + office.getName());
        office.submit(this, doc);

        synchronized (this) {
            while (!hasDocument(doc)) {
                try { wait(); } catch (InterruptedException ignored) { }
            }
        }
    }
}