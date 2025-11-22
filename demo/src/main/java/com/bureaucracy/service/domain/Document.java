package com.bureaucracy.service.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Document {
    private final String name;
    private List<Document> prerequisites = new ArrayList<>();
    private Office emittingOffice;

    public Document(String name) { this.name = name; }

    public String getName() { return name; }

    public List<Document> getPrerequisites() { return Collections.unmodifiableList(prerequisites); }

    public Document addPrerequisite(Document prerequisite) {
        this.prerequisites.add(prerequisite);
        return this;
    }

    public Office getEmittingOffice() { return emittingOffice; }
    public void setEmittingOffice(Office office) { this.emittingOffice = office; }

    @Override public String toString() { return name; }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Document)) return false;
        Document document = (Document) o;
        return Objects.equals(name, document.name);
    }
}