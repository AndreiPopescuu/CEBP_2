package com.bureaucracy.service.config;

import com.bureaucracy.service.EventPublisher;
import com.bureaucracy.service.domain.Counter;
import com.bureaucracy.service.entity.DocumentType;
import com.bureaucracy.service.entity.OfficeEntity;
import com.bureaucracy.service.repository.DocumentTypeRepository;
import com.bureaucracy.service.repository.OfficeRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class BureaucracyRegistry {

    private final OfficeRepository officeRepo;
    private final DocumentTypeRepository docRepo;
    private final EventPublisher publisher;

    // Harta cu oficiile "Active" (cele care au thread-uri și ghișee funcționale)
    private final Map<String, com.bureaucracy.service.domain.Office> activeOffices = new HashMap<>();

    public BureaucracyRegistry(OfficeRepository officeRepo, DocumentTypeRepository docRepo, EventPublisher publisher) {
        this.officeRepo = officeRepo;
        this.docRepo = docRepo;
        this.publisher = publisher;
    }

    @PostConstruct
    @Transactional
    public void init() {
        // 1. Populăm Baza de Date dacă e goală
        if (officeRepo.count() == 0) {
            seedDatabase();
        }

        // 2. Pornim ghișeele (Workerii) în memorie
        setupActiveCounters();

        System.out.println("--- BUREAUCRACY SYSTEM STARTED (DB + COUNTERS READY) ---");
    }

    // --- METODA CARE ÎȚI LIPSEA (Corectată) ---
    // Aceasta face legătura între Controller (care cere un Document) și Baza de Date
    @Transactional
    public com.bureaucracy.service.domain.Document getDocumentByName(String name) {
        // 1. Căutăm definiția în Baza de Date
        Optional<DocumentType> entityOpt = docRepo.findByName(name);

        if (entityOpt.isEmpty()) {
            return null; // Documentul nu există în baza de date
        }

        // 2. Convertim din Entitate de Bază de Date -> în Obiect de Simulare (Domain)
        return convertToDomain(entityOpt.get());
    }

    // Convertor recursiv: Transformă structura din DB în structura necesară pentru Agent
    private com.bureaucracy.service.domain.Document convertToDomain(DocumentType entity) {
        com.bureaucracy.service.domain.Document domainDoc = new com.bureaucracy.service.domain.Document(entity.getName());

        if (entity.getIssuingOffice() != null) {
            String officeName = entity.getIssuingOffice().getName();

            // --- DEBUG: Print if we can't find the office ---
            com.bureaucracy.service.domain.Office activeOffice = activeOffices.get(officeName);
            if (activeOffice == null) {
                System.err.println("❌ REGISTRY WARNING: Database has office '" + officeName +
                        "' but no Active Office found in memory map! Keys: " + activeOffices.keySet());
            } else {
                domainDoc.setEmittingOffice(activeOffice);
            }
        }

        for (DocumentType prereqEntity : entity.getPrerequisites()) {
            domainDoc.addPrerequisite(convertToDomain(prereqEntity));
        }

        return domainDoc;
    }

    // --- RESTUL CODULUI DE INITIALIZARE ---

    private void seedDatabase() {
        System.out.println("🌱 Seeding Database with Offices and Documents...");

        OfficeEntity primarieInfo = officeRepo.save(new OfficeEntity("Primarie"));
        OfficeEntity anafInfo = officeRepo.save(new OfficeEntity("ANAF"));
        OfficeEntity pasapoarteInfo = officeRepo.save(new OfficeEntity("Directia Pasapoarte"));

        DocumentType certNastere = new DocumentType("Certificat de nastere", primarieInfo);
        DocumentType certFiscal = new DocumentType("Certificat fiscal", anafInfo);
        DocumentType cererePasaport = new DocumentType("Cerere pasaport", anafInfo);
        DocumentType chitanta = new DocumentType("Chitanta taxa pasaport", anafInfo);

        certNastere = docRepo.save(certNastere);
        certFiscal = docRepo.save(certFiscal);
        cererePasaport = docRepo.save(cererePasaport);
        chitanta = docRepo.save(chitanta);

        DocumentType carteIdentitate = new DocumentType("Carte de identitate", primarieInfo);
        carteIdentitate.addPrerequisite(certNastere);
        carteIdentitate = docRepo.save(carteIdentitate);

        DocumentType certCasatorie = new DocumentType("Certificat de casatorie", primarieInfo);
        certCasatorie.addPrerequisite(certNastere);
        docRepo.save(certCasatorie);

        DocumentType adeverinta = new DocumentType("Adeverinta de domiciliu", primarieInfo);
        adeverinta.addPrerequisite(carteIdentitate);
        docRepo.save(adeverinta);

        DocumentType pasaport = new DocumentType("Pasaport", pasapoarteInfo);
        pasaport.addPrerequisite(carteIdentitate);
        pasaport.addPrerequisite(cererePasaport);
        pasaport.addPrerequisite(chitanta);
        docRepo.save(pasaport);

        System.out.println("🌱 Seeding Complete!");
    }

    private void setupActiveCounters() {
        createActiveOffice("Primarie", 2);
        createActiveOffice("ANAF", 2);
        createActiveOffice("Directia Pasapoarte", 1);
    }

    private void createActiveOffice(String name, int counterCount) {
        com.bureaucracy.service.domain.Office office = new com.bureaucracy.service.domain.Office(name);
        for (int i = 1; i <= counterCount; i++) {
            office.addCounter(new Counter("Ghiseu " + name + " " + i, publisher));
        }
        office.startAllCounters();
        activeOffices.put(name, office);
    }
}