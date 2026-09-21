package com.example.bacteriaSystem.model;

import lombok.*;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TesteBioquimico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String nome;

    @Column(columnDefinition = "NVARCHAR(MAX)", nullable = false)
    private String descricao;

    @Column(nullable = false)
    private String criterioPositivo;

    @Column(nullable = false)
    private Boolean ativo;

    //Relação TesteBioquimico : ProbabilidadeTeste
    @JsonIgnore
    @OneToMany(mappedBy = "testeBioquimico")
    private List<ProbabilidadeTeste> probabilidadeTestes = new ArrayList<>();

    //Relação TesteBioquimico : ResultadoTesteIdentificacao
    @JsonIgnore
    @OneToMany(mappedBy = "testeBioquimico")
    private List<ResultadoTesteIdentificacao> resultadoTesteIdentificacaos = new ArrayList<>();



}
