package com.example.bacteriaSystem.repository;

import com.example.bacteriaSystem.model.Identificacao;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdentificacaoRepository extends JpaRepository<Identificacao, Long> {
}
