package com.bureaucracy.service.config;

import com.bureaucracy.service.EventPublisher;
import com.bureaucracy.service.domain.*;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;

@Component
public class BureaucracyRegistry {
    private final Map<String, Document> documentMap = new HashMap<>();
    private final EventPublisher publisher;

    public BureaucracyRegistry(EventPublisher publisher) {
        this.publisher = publisher;
    }

    @PostConstruct
    public void init() {
        // 1. Definim Oficiile
        Office primarie = new Office("Primarie");
        Office anaf = new Office("ANAF");
        Office pasapoarte = new Office("Directia Pasapoarte");

        // 2. Cream Ghișeele (Counters) - Punem mai multe ca să meargă repede
        primarie.addCounter(new Counter("Ghiseu Primarie 1", publisher));
        primarie.addCounter(new Counter("Ghiseu Primarie 2", publisher));

        anaf.addCounter(new Counter("Ghiseu ANAF 1", publisher));
        anaf.addCounter(new Counter("Ghiseu ANAF 2", publisher));

        pasapoarte.addCounter(new Counter("Ghiseu Pasapoarte", publisher));

        // 3. Definim Documentele și Dependențele (Codul tău original)
        // Notă: Am scos diacriticele din string-uri ("nastere" vs "naștere")
        // pentru a fi compatibil 100% cu butoanele din Aplicația Client făcută anterior.

        Document certNastere = new Document("Certificat de nastere");
        Document cererePasaport = new Document("Cerere pasaport");
        Document certificatFiscal = new Document("Certificat fiscal");
        Document chitantaTaxaPasaport = new Document("Chitanta taxa pasaport");

        Document carteIdentitate = new Document("Carte de identitate")
                .addPrerequisite(certNastere);

        Document pasaport = new Document("Pasaport")
                .addPrerequisite(carteIdentitate)
                .addPrerequisite(cererePasaport)
                .addPrerequisite(chitantaTaxaPasaport);

        Document adeverintaDomiciliu = new Document("Adeverinta de domiciliu")
                .addPrerequisite(carteIdentitate);

        Document certificatCasatorie = new Document("Certificat de casatorie")
                .addPrerequisite(certNastere);

        // 4. Le înregistrăm în MAP (Dicționarul API-ului)
        // Asta permite Clientului să ceară documentul după nume
        documentMap.put("Certificat de nastere", certNastere);
        documentMap.put("Cerere pasaport", cererePasaport);
        documentMap.put("Certificat fiscal", certificatFiscal);
        documentMap.put("Chitanta taxa pasaport", chitantaTaxaPasaport);
        documentMap.put("Carte de identitate", carteIdentitate);
        documentMap.put("Pasaport", pasaport);
        documentMap.put("Adeverinta de domiciliu", adeverintaDomiciliu);
        documentMap.put("Certificat de casatorie", certificatCasatorie);

        // 5. Distribuim documentele la Oficii (Cine ce emite?)

        // Primăria emite actele de stare civilă
        primarie.addDocument(certNastere);
        primarie.addDocument(carteIdentitate);
        primarie.addDocument(adeverintaDomiciliu);
        primarie.addDocument(certificatCasatorie);

        // ANAF emite taxele și cererile fiscale
        anaf.addDocument(certificatFiscal);
        anaf.addDocument(chitantaTaxaPasaport);
        anaf.addDocument(cererePasaport); // Presupunem că formularul se ia de la ANAF/Finanțe

        // Direcția Pașapoarte emite doar Pașaportul final
        pasapoarte.addDocument(pasaport);

        // 6. Pornim toate ghișeele
        primarie.startAllCounters();
        anaf.startAllCounters();
        pasapoarte.startAllCounters();

        System.out.println("--- BUREAUCRACY SYSTEM STARTED (Full Configuration) ---");
    }

    public Document getDocumentByName(String name) {
        return documentMap.get(name);
    }
}