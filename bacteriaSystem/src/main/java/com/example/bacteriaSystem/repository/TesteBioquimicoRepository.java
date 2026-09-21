package com.example.bacteriaSystem.repository;

import com.example.bacteriaSystem.model.TesteBioquimico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TesteBioquimicoRepository extends JpaRepository<TesteBioquimico, Long> {
    TesteBioquimico findByNome(String s);

    List<TesteBioquimico> findByAtivoTrue();
}
