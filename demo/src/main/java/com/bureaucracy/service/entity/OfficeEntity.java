package com.bureaucracy.service.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "offices")
public class OfficeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    public OfficeEntity() {}
    public OfficeEntity(String name) { this.name = name; }

    public Long getId() { return id; }
    public String getName() { return name; }
}