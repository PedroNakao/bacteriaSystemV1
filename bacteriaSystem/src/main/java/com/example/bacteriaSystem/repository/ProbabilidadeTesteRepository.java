package com.example.bacteriaSystem.repository;

import com.example.bacteriaSystem.model.ProbabilidadeTeste;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;

public interface ProbabilidadeTesteRepository extends JpaRepository<ProbabilidadeTeste,Long>{
    @Query("""
    SELECT pt.percentualPositividade
    FROM ProbabilidadeTeste pt
    WHERE pt.bacteria.id = :id_bacteria
    AND pt.testeBioquimico.id = :id_teste
    """)
    BigDecimal pegarProbabilidade(@Param("id_bacteria") Long id,@Param("id_teste") Long idTeste );

}
