package com.example.bacteriaSystem.repository;

import com.example.bacteriaSystem.model.RankingIdentificacao;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RankingRepository extends JpaRepository<RankingIdentificacao, Long> {

}
