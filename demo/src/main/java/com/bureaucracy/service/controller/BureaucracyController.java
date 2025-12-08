package com.bureaucracy.service.controller;

import com.bureaucracy.service.EventPublisher;
import com.bureaucracy.service.config.BureaucracyRegistry;
import com.bureaucracy.service.domain.CitizenAgent;
import com.bureaucracy.service.domain.Document;
import com.bureaucracy.service.domain.RequestService; // Import the new Service
import com.bureaucracy.service.entity.Citizen;
import com.bureaucracy.service.repository.CitizenRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api/citizens")
public class BureaucracyController {

    private final BureaucracyRegistry registry;
    private final EventPublisher publisher;

    // We still need this to find/create the citizen
    private final CitizenRepository citizenRepo;

    // NEW: Inject the Transactional Service instead of raw repositories
    private final RequestService requestService;

    private final ExecutorService executor = Executors.newCachedThreadPool();

    // Updated Constructor
    public BureaucracyController(BureaucracyRegistry registry,
                                 EventPublisher publisher,
                                 CitizenRepository citizenRepo,
                                 RequestService requestService) {
        this.registry = registry;
        this.publisher = publisher;
        this.citizenRepo = citizenRepo;
        this.requestService = requestService;
    }

    @PostMapping("/apply")
    public ResponseEntity<String> apply(@RequestBody RequestData request) {
        // 1. Find or Create Citizen in DB (This is safe on the main thread)
        Citizen citizen = citizenRepo.findByName(request.name)
                .orElseGet(() -> citizenRepo.save(new Citizen(request.name)));

        // 2. Resolve Documents from Registry
        List<Document> docs = new ArrayList<>();
        for (String d : request.documents) {
            Document doc = registry.getDocumentByName(d);
            if (doc != null) docs.add(doc);
        }

        // 3. Create Agent passing the Service
        // The Agent will use 'requestService' to save data safely from its background thread
        CitizenAgent agent = new CitizenAgent(citizen, docs, publisher, requestService);

        executor.submit(agent);

        return ResponseEntity.ok("Application started for " + request.name);
    }

    public static class RequestData {
        public String name;
        public List<String> documents;
    }
}