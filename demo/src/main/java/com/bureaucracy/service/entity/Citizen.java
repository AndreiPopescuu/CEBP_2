package com.bureaucracy.service.entity;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "citizens")
public class Citizen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    // We track requests history here
    @OneToMany(mappedBy = "citizen", cascade = CascadeType.ALL)
    private List<Request> history;

    // Constructors
    public Citizen() {} // JPA needs empty constructor
    public Citizen(String name) { this.name = name; }

    // Getters
    public Long getId() { return id; }
    public String getName() { return name; }
}