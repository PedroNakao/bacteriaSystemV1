package com.example.bacteriaSystem.repository;

import com.example.bacteriaSystem.model.Bacteria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BacteriaRepository extends JpaRepository<Bacteria, Long> {
    Bacteria findByNomeCientifico(String s);

    List<Bacteria> findByAtivoTrue();
}
