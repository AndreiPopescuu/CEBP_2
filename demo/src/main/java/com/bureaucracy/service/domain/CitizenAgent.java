package com.bureaucracy.service.domain;

import com.bureaucracy.service.EventPublisher;
import java.util.ArrayList;
import java.util.List;

public class CitizenAgent implements Runnable {
    private final String name;
    private final List<Document> currDocuments = new ArrayList<>();
    private final List<Document> neededDocuments;
    private final EventPublisher publisher;

    public CitizenAgent(String name, List<Document> neededDocuments, EventPublisher publisher) {
        this.name = name;
        this.neededDocuments = new ArrayList<>(neededDocuments);
        this.publisher = publisher;
    }

    public String getName() { return name; }

    public synchronized void onDocumentIssued(Document doc) {
        if (!currDocuments.contains(doc)) currDocuments.add(doc);
        notifyAll();
    }

    public synchronized boolean hasDocument(Document doc) { return currDocuments.contains(doc); }

    @Override
    public void run() {
        publisher.publishEvent("CLIENT_START", name, "Started with needs: " + neededDocuments);
        for (Document doc : neededDocuments) {
            obtainDocument(doc);
        }
        publisher.publishEvent("CLIENT_FINISH", name, "Finished. Acquired: " + currDocuments);
    }

    private void obtainDocument(Document doc) {
        for (Document pre : doc.getPrerequisites()) obtainDocument(pre);

        synchronized (this) { if (hasDocument(doc)) return; }

        Office office = doc.getEmittingOffice();
        office.submit(this, doc);

        synchronized (this) {
            while (!hasDocument(doc)) {
                try { wait(); } catch (InterruptedException ignored) { }
            }
        }
    }
}