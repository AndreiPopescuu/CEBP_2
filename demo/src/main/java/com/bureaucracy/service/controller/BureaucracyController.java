package com.bureaucracy.service.controller;

import com.bureaucracy.service.EventPublisher;
import com.bureaucracy.service.config.BureaucracyRegistry;
import com.bureaucracy.service.domain.CitizenAgent;
import com.bureaucracy.service.domain.Document;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/api/citizens")
public class BureaucracyController {

    private final BureaucracyRegistry registry;
    private final EventPublisher publisher;

    public BureaucracyController(BureaucracyRegistry registry, EventPublisher publisher) {
        this.registry = registry;
        this.publisher = publisher;
    }

    @PostMapping("/apply")
    public ResponseEntity<String> apply(@RequestBody RequestData request) {
        List<Document> docs = new ArrayList<>();
        for (String d : request.documents) {
            Document doc = registry.getDocumentByName(d);
            if (doc != null) docs.add(doc);
        }

        CitizenAgent agent = new CitizenAgent(request.name, docs, publisher);
        Executors.newSingleThreadExecutor().submit(agent);

        return ResponseEntity.ok("Application started for " + request.name);
    }




    // Simple DTO class
    public static class RequestData {
        public String name;
        public List<String> documents;
    }
}